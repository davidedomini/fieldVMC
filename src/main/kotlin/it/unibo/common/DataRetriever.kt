package it.unibo.common

import java.io.File

object DataRetriever {
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
                    throw IllegalArgumentException("Column mismatch in file: ${file.name}")
                }
                collectedRows.add(lastRow)
            }
            writeCleanedData(path, experiment, columnNames!!, collectedRows)
            data.putAll(computeMeanValues(experiment, columnNames, collectedRows))
        }
        return data
    }

    private fun getExperimentFiles(
        path: String,
        experiment: String,
    ): Array<File> = File(path).listFiles { _, name -> name.startsWith(experiment) && name.endsWith(".csv") } ?: emptyArray()

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

    private fun writeCleanedData(
        path: String,
        experiment: String,
        columnNames: List<String>,
        collectedRows: List<List<String>>,
    ) {
        val outputFile = File("$path/cleaned_$experiment.csv")
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

    fun meanOnCleanedData(
        experiments: List<String>,
        path: String,
    ): Map<String, Double> {
        val data = mutableMapOf<String, Double>()
        experiments.forEach { experiment ->
            val cleanedFile = File("$path/cleaned_$experiment.csv")
            if (!cleanedFile.exists()) {
                throw IllegalArgumentException("No cleaned file found for experiment: $experiment")
            }
            // todo if the file does not exist, try to create it
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
            //for each row in the file, compute the mean value for each column
            val mean = lines.last().split(" ").map { it.trim() }
            data.putAll(computeMeanValues(experiment, header, listOf(mean)))
        }
        return data
    }
}
