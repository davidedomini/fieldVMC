package it.unibo.common

import java.io.File
import kotlin.text.contains
import kotlin.text.isNotBlank
import kotlin.text.startsWith

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
}

// private fun retrieveData(): Map<String, Double> {
//    val path = Paths.get("").toAbsolutePath().toString() + "/data"
//    val data = mutableMapOf<String, Double>()
//    listOf("classic-vmc", "fixed-leader").forEach { experiment ->
//        val baseFileName = "$path/$experiment.csv"
//        val inputFile = File(baseFileName)
//        val outputFile = File("$path/cleaned_$experiment.csv")
//        val lines = inputFile.readLines()
//        val dataStartIndex = lines.indexOfFirst { it.startsWith("# time") }
//        if (dataStartIndex == -1) {
//            IllegalArgumentException("No valid data found in the file.")
//        }
//        val cleanedLines = lines.subList(dataStartIndex, lines.size)
//            .mapIndexed { index, line -> if (index == 0) line.removePrefix("# ") else line }
//            .takeWhile { !it.contains("#", ignoreCase = true) }
//            .filter { it.isNotBlank() }
//        outputFile.writeText(cleanedLines.joinToString("\n"))
//        // Extract column names and last row values
//        val header = cleanedLines.first().split(" ").map { it.trim() }
//        val lastRow = cleanedLines.last().split(" ").map { it.trim().toDoubleOrNull() ?: Double.NaN }
//        // Store column to last row value mapping
//        // Store column to last row value mapping with experiment prefix
//        header.zip(lastRow).forEach { (column, value) ->
//            data["$experiment@$column"] = value
//        }
//    }
//    return data
// }
