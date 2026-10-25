package com.armod.identifier.data.local

import com.armod.identifier.data.model.MedicineRecord
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Pure Kotlin parser and validator for the bundled medicine dataset.
 */
object JsonMedicineParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parses and strictly validates a JSON string representing an array of MedicineRecord objects.
     * 
     * @throws MedicineDataException.EmptyDataset if content is empty or contains no records.
     * @throws MedicineDataException.MalformedJson if JSON syntax is invalid.
     * @throws MedicineDataException.MissingRequiredField if id, sourceTitle, or datasetVersion is missing/blank.
     * @throws MedicineDataException.DuplicateId if two or more records share the same id.
     */
    fun parseAndValidate(jsonContent: String): List<MedicineRecord> {
        if (jsonContent.isBlank()) {
            throw MedicineDataException.EmptyDataset("JSON content is blank or empty")
        }

        val records: List<MedicineRecord> = try {
            json.decodeFromString(jsonContent)
        } catch (e: SerializationException) {
            throw MedicineDataException.MalformedJson("Failed to parse medicine JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw MedicineDataException.MalformedJson("Invalid medicine JSON structure: ${e.message}", e)
        }

        if (records.isEmpty()) {
            throw MedicineDataException.EmptyDataset("Medicine dataset contains 0 records")
        }

        val seenIds = mutableSetOf<String>()
        for ((index, record) in records.withIndex()) {
            if (record.id.isBlank()) {
                throw MedicineDataException.MissingRequiredField("index_$index", "id")
            }
            if (record.sourceTitle.isBlank()) {
                throw MedicineDataException.MissingRequiredField(record.id, "sourceTitle")
            }
            if (record.datasetVersion.isBlank()) {
                throw MedicineDataException.MissingRequiredField(record.id, "datasetVersion")
            }
            if (!seenIds.add(record.id)) {
                throw MedicineDataException.DuplicateId(record.id)
            }
        }

        return records
    }
}
