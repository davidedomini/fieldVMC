package it.unibo.common

import java.io.File

/**
 * This object is responsible for retrieving data from the simulation files.
 * It can retrieve data from the original files or from the cleaned files.
 * The cleaned files are created by removing the comments and the empty lines from the original files.
 * The data is then stored in a map where the key is the experiment name and the column name separated by a '@'.
 * The value is the mean value of the column for the experiment.
 * The mean value is computed by averaging the values of the last row of each file.
 */
object DataRetriever {
    /**
     * Retrieve data from the simulation files.
     * The data is stored in a map where the key is the experiment name and the column name separated by a '@'.
     * The value is the mean value of the column for the [experiments] at the given [path].
     * The mean value is computed by averaging the values of the last row of each file.
     * The data is also written to a cleaned file for each experiment.
     */
    fun retrieveData(
        experiments: List<String>,
        path: String,
    ): Map<String, Double> {
        val data = mutableMapOf<String, Double>()
        experiments.forEach { experiment ->
            val experimentFiles = getExperimentFiles(path, experiment)
            if (experimentFiles.isEmpty()) {
                throw IllegalArgumentException("No files found for experiment: $experiment")
            }
            val collectedRows = mutableListOf<List<String>>()
            var columnNames: List<String>? = null
            experimentFiles.forEach { file ->
                val (header, lastRow) = processFile(file)
                if (columnNames == null) {
                    columnNames = header
                } else if (columnNames != header) {
                    throw IllegalArgumentException(
                        "Column mismatch in file: ${file.name}.\n" +
                            "Columns should be: $columnNames\nbut found: $header",
                    )
                }
                collectedRows.add(lastRow)
            }
            writeCleanedData(path, experiment, columnNames!!, collectedRows)
            data.putAll(computeMeanValues(experiment, columnNames, collectedRows))
        }
        return data
    }

    /**
     * Retrieves the list of files for a given [experiment] in the specified [path].
     * The files must start with the experiment name and end with ".csv".
     */
    private fun getExperimentFiles(
        path: String,
        experiment: String,
    ): Array<File> = File(path).listFiles { _, name -> name.startsWith(experiment) && name.endsWith(".csv") } ?: emptyArray()

    /**
     * Processes the given [file] and returns the header and the last row of the data.
     */
    private fun processFile(file: File): Pair<List<String>, List<String>> {
        val lines = file.readLines()
        val dataStartIndex = lines.indexOfFirst { it.startsWith("# time") }
        if (dataStartIndex == -1) {
            throw IllegalArgumentException("No valid data found in file: ${file.name}")
        }
        val cleanedLines =
            lines
                .subList(dataStartIndex, lines.size)
                .mapIndexed { index, line -> if (index == 0) line.removePrefix("# ") else line }
                .takeWhile { !it.contains("#", ignoreCase = true) }
                .filter { it.isNotBlank() }
        val header =
            cleanedLines
                .first()
                .split(" ")
                .map { it.trim() }
                .dropLastWhile { it.isEmpty() }
        var lastRow = cleanedLines.last().split(" ").map { it.trim() }

        return Pair(header, lastRow)
    }

    /**
     * Writes the cleaned data to a new file for the given [experiment] in the specified [path].
     * The output file will be named `cleaned_<experiment>.csv`.
     * The first row of the file will contain the [columnNames],
     * and each subsequent row will contain the last row of data from each file,
     * passed as [collectedRows].
     */
    private fun writeCleanedData(
        path: String,
        experiment: String,
        columnNames: List<String>,
        collectedRows: List<List<String>>,
    ) {
        val outputFile = File("$path${File.separator}cleaned${File.separator}cleaned_$experiment.csv")
        val outputContent =
            buildString {
                append(columnNames.joinToString(" ")) // Header row
                append("\n")
                collectedRows.forEach { row ->
                    append(row.joinToString(" ")) // Append each last row
                    append("\n")
                }
            }
        outputFile.writeText(outputContent)
    }

    /**
     * Computes the mean value for each column in the given [experiment] data.
     * The mean value is computed by averaging the values of the last row of each [columnNames]
     * in the [collectedRows].
     */
    private fun computeMeanValues(
        experiment: String,
        columnNames: List<String>,
        collectedRows: List<List<String>>,
    ): Map<String, Double> =
        columnNames
            .mapIndexed { index, column ->
                val meanValue = collectedRows.mapNotNull { it[index].toDoubleOrNull() }.average()
                "$experiment@$column" to meanValue
            }.toMap()

    /**
     * Computes the mean value for each column in the cleaned data files for the given [experiments] at the specified [path].
     * The mean value is computed by averaging the values of the last row of each file.
     */
    fun meanOnCleanedData(
        experiments: List<String>,
        path: String,
    ): Map<String, Double> {
        val data = mutableMapOf<String, Double>()
        retrieveData(experiments, path) // to update the cleaned files
        experiments.forEach { experiment ->
            // check if so is windows or linux
            var cleanedFile = File("$path${File.separator}cleaned_$experiment.csv")
            if (!cleanedFile.exists()) {
                try {
                    retrieveData(listOf(experiment), path)
                    cleanedFile = File("$path${File.separator}cleaned_$experiment.csv")
                } catch (e: IllegalArgumentException) {
                    if (e.message?.contains("No files found for experiment") == true) {
                        throw IllegalArgumentException("No cleaned file found for experiment: $experiment")
                    }
                }
            }
            val lines = cleanedFile.readLines()
            val dataStartIndex = lines.indexOfFirst { it.startsWith("time") }
            if (dataStartIndex == -1) {
                throw IllegalArgumentException("No valid data found in file: ${cleanedFile.name}")
            }
            val header =
                lines
                    .first()
                    .split(" ")
                    .map { it.trim() }
                    .dropLastWhile { it.isEmpty() }
            // for each row in the file, compute the mean value for each column
            val mean = lines.last().split(" ").map { it.trim() }
            data.putAll(computeMeanValues(experiment, header, listOf(mean)))
        }
        return data
    }
}
