package com.armod.identifier.data.local

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class MedicineDataValidationTest {

    private val bundledAssetFile = File("src/main/assets/medicines.json")

    @Test
    fun bundledDataset_existsAndContains25TraceableRecords() {
        assertTrue("Bundled medicines.json asset must exist", bundledAssetFile.exists())
        val jsonContent = bundledAssetFile.readText()
        assertTrue("medicines.json must not be empty", jsonContent.isNotBlank())

        val records = JsonMedicineParser.parseAndValidate(jsonContent)
        assertTrue("Target dataset must contain at least 25 curated records", records.size >= 25)

        // Validate provenance and integrity of every record
        for (record in records) {
            assertTrue("ID must not be blank for ${record.id}", record.id.isNotBlank())
            assertTrue("Source title must not be blank for ${record.id}", record.sourceTitle.isNotBlank())
            assertTrue("Source URL must not be blank for ${record.id}", record.sourceUrl?.isNotBlank() == true)
            assertTrue("Dataset version must be 1.0 for ${record.id}", record.datasetVersion == "1.0")
            assertNotNull("Generic name must be present for ${record.id}", record.genericName)
            assertNotNull("Strength must be present for ${record.id}", record.strength)
            assertNotNull("Dosage form must be present for ${record.id}", record.dosageForm)
            assertTrue("Warnings must be non-empty for ${record.id}", record.warnings.isNotEmpty())
        }
    }

    @Test
    fun parser_detectsDuplicateId() {
        val duplicateJson = """
            [
              {
                "id": "med-duplicate",
                "genericName": "Paracetamol",
                "sourceTitle": "Test Source",
                "datasetVersion": "1.0"
              },
              {
                "id": "med-duplicate",
                "genericName": "Ibuprofen",
                "sourceTitle": "Test Source",
                "datasetVersion": "1.0"
              }
            ]
        """.trimIndent()

        try {
            JsonMedicineParser.parseAndValidate(duplicateJson)
            fail("Expected MedicineDataException.DuplicateId")
        } catch (e: MedicineDataException.DuplicateId) {
            assertEquals("med-duplicate", e.duplicateId)
        }
    }

    @Test
    fun parser_detectsMissingId() {
        val missingIdJson = """
            [
              {
                "id": "",
                "genericName": "Paracetamol",
                "sourceTitle": "Test Source",
                "datasetVersion": "1.0"
              }
            ]
        """.trimIndent()

        try {
            JsonMedicineParser.parseAndValidate(missingIdJson)
            fail("Expected MedicineDataException.MissingRequiredField for id")
        } catch (e: MedicineDataException.MissingRequiredField) {
            assertEquals("id", e.fieldName)
        }
    }

    @Test
    fun parser_detectsMissingSourceTitle() {
        val missingSourceJson = """
            [
              {
                "id": "med-valid",
                "genericName": "Paracetamol",
                "sourceTitle": "",
                "datasetVersion": "1.0"
              }
            ]
        """.trimIndent()

        try {
            JsonMedicineParser.parseAndValidate(missingSourceJson)
            fail("Expected MedicineDataException.MissingRequiredField for sourceTitle")
        } catch (e: MedicineDataException.MissingRequiredField) {
            assertEquals("sourceTitle", e.fieldName)
            assertEquals("med-valid", e.recordId)
        }
    }

    @Test
    fun parser_detectsMissingDatasetVersion() {
        val missingVersionJson = """
            [
              {
                "id": "med-valid-2",
                "genericName": "Paracetamol",
                "sourceTitle": "Test Source",
                "datasetVersion": ""
              }
            ]
        """.trimIndent()

        try {
            JsonMedicineParser.parseAndValidate(missingVersionJson)
            fail("Expected MedicineDataException.MissingRequiredField for datasetVersion")
        } catch (e: MedicineDataException.MissingRequiredField) {
            assertEquals("datasetVersion", e.fieldName)
            assertEquals("med-valid-2", e.recordId)
        }
    }

    @Test
    fun parser_detectsMalformedJson() {
        val malformedJson = """
            [
              { "id": "broken", "genericName":
        """.trimIndent()

        try {
            JsonMedicineParser.parseAndValidate(malformedJson)
            fail("Expected MedicineDataException.MalformedJson")
        } catch (e: MedicineDataException.MalformedJson) {
            assertTrue(e.message?.contains("Failed to parse") == true)
        }
    }

    @Test
    fun parser_detectsEmptyDataset() {
        val emptyJson = "[]"
        try {
            JsonMedicineParser.parseAndValidate(emptyJson)
            fail("Expected MedicineDataException.EmptyDataset")
        } catch (e: MedicineDataException.EmptyDataset) {
            assertTrue(e.message?.contains("0 records") == true)
        }

        try {
            JsonMedicineParser.parseAndValidate("   ")
            fail("Expected MedicineDataException.EmptyDataset for blank string")
        } catch (e: MedicineDataException.EmptyDataset) {
            assertTrue(e.message?.contains("blank or empty") == true)
        }
    }

    @Test
    fun parser_safelyIgnoresUnknownOptionalFields() {
        val unknownFieldJson = """
            [
              {
                "id": "med-with-extra",
                "brandName": "ExtraBrand",
                "genericName": "ExtraGeneric",
                "sourceTitle": "Source",
                "datasetVersion": "1.0",
                "unexpectedFutureField": "ignored_safely",
                "anotherUnknownObject": { "nested": 123 }
              }
            ]
        """.trimIndent()

        val records = JsonMedicineParser.parseAndValidate(unknownFieldJson)
        assertEquals(1, records.size)
        assertEquals("med-with-extra", records[0].id)
        assertEquals("ExtraBrand", records[0].brandName)
    }

    @Test
    fun localDataSource_performsRetrievalAndLookupCorrectly() = runBlocking {
        val jsonContent = bundledAssetFile.readText()
        val dataSource = JsonMedicineLocalDataSource(jsonProvider = { jsonContent })

        // 1. Total records
        assertTrue(dataSource.getRecordCount() >= 25)
        assertEquals(dataSource.getRecordCount(), dataSource.getAllMedicines().size)

        // 2. Lookup by exact ID
        val crocin = dataSource.getMedicineById("med-nlem-001")
        assertNotNull(crocin)
        assertEquals("Crocin", crocin?.brandName)
        assertEquals("Paracetamol", crocin?.genericName)
        assertEquals("500 mg", crocin?.strength)

        // 3. Lookup non-existent ID
        assertNull(dataSource.getMedicineById("non-existent-id"))

        // 4. Search by generic name
        val paracetamolResults = dataSource.searchByName("paracetamol")
        assertEquals("Should find both Crocin and Dolo 650", 2, paracetamolResults.size)

        // 5. Search by brand name
        val doloResults = dataSource.searchByName("dolo")
        assertEquals(1, doloResults.size)
        assertEquals("Dolo 650", doloResults[0].brandName)

        // 6. Find by exact name and strength
        val match500 = dataSource.findByExactNameAndStrength("Paracetamol", "500 mg")
        assertNotNull(match500)
        assertEquals("Crocin", match500?.brandName)

        val match650 = dataSource.findByExactNameAndStrength("Dolo 650", "650 mg")
        assertNotNull(match650)
        assertEquals("Dolo 650", match650?.brandName)

        // 7. Mismatched strength returns null
        val mismatch = dataSource.findByExactNameAndStrength("Paracetamol", "1000 mg")
        assertNull("Strength mismatch must return null", mismatch)
    }
}
