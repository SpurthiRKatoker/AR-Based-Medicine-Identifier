package com.armod.identifier.matching

import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.util.TextNormaliser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM Unit tests for on-device medicine matching engine and confidence gate.
 * Covers all required acceptance criteria without requiring Android runtime or device hardware.
 */
class MedicineMatcherTest {

    private lateinit var testCatalogue: List<MedicineRecord>

    @Before
    fun setUp() {
        testCatalogue = listOf(
            MedicineRecord(
                id = "med-001",
                brandName = "Crocin",
                genericName = "Paracetamol",
                strength = "500 mg",
                dosageForm = "Tablet",
                manufacturer = "GlaxoSmithKline Pharmaceuticals Ltd",
                labelPurpose = "Relief of mild to moderate pain and fever.",
                sourceTitle = "Test Catalog",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-002",
                brandName = "Dolo 650",
                genericName = "Paracetamol",
                strength = "650 mg",
                dosageForm = "Tablet",
                manufacturer = "Micro Labs Ltd",
                labelPurpose = "Relief of fever and aches.",
                sourceTitle = "Test Catalog",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-003",
                brandName = "Cetzine",
                genericName = "Cetirizine Hydrochloride",
                strength = "10 mg",
                dosageForm = "Tablet",
                manufacturer = "Dr. Reddy's Laboratories Ltd",
                labelPurpose = "Allergic rhinitis and urticaria.",
                sourceTitle = "Test Catalog",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-004",
                brandName = "Brufen",
                genericName = "Ibuprofen",
                strength = "400 mg",
                dosageForm = "Tablet",
                manufacturer = "Abbott India Ltd",
                labelPurpose = "Anti-inflammatory pain relief.",
                sourceTitle = "Test Catalog",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-007",
                brandName = "Omez",
                genericName = "Omeprazole",
                strength = "20 mg",
                dosageForm = "Capsule",
                manufacturer = "Dr. Reddy's Laboratories Ltd",
                labelPurpose = "Acid reflux and ulcers.",
                sourceTitle = "Test Catalog",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-011",
                brandName = "Atorva",
                genericName = "Atorvastatin Calcium",
                strength = "10 mg",
                dosageForm = "Tablet",
                manufacturer = "Zydus Healthcare",
                labelPurpose = "Cholesterol lowering.",
                sourceTitle = "Test Catalog",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-012",
                brandName = "Atorva 20",
                genericName = "Atorvastatin Calcium",
                strength = "20 mg",
                dosageForm = "Tablet",
                manufacturer = "Zydus Healthcare",
                labelPurpose = "Cholesterol lowering higher dose.",
                sourceTitle = "Test Catalog",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-024",
                brandName = "Electral",
                genericName = "Oral Rehydration Salts",
                strength = "21.8 g",
                dosageForm = "Powder",
                manufacturer = "FDC Limited",
                labelPurpose = "Electrolyte restoration.",
                sourceTitle = "Test Catalog",
                datasetVersion = "1.0"
            )
        )
    }

    @Test
    fun match_exactNameStrengthAndDosageForm_returnsConfidentMatch() {
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = """
                Crocin 500
                Paracetamol Tablets IP
                Each tablet contains: Paracetamol IP 500 mg
                Expiry: 08/2027 Batch No. ABC123
            """.trimIndent()
        )

        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue("Expected ConfidentMatch for exact match", outcome is MatchOutcome.ConfidentMatch)

        val match = outcome as MatchOutcome.ConfidentMatch
        assertEquals("med-001", match.record.id)
        assertEquals("Crocin", match.record.brandName)
        assertEquals("500 mg", match.record.strength)
        assertEquals(MatchConfidence.HIGH, match.confidence)
        assertEquals("08/2027", match.extractedExpiry)
        assertEquals("ABC123", match.extractedBatch)
        assertTrue(match.candidate.matchedFields.contains(MatchedField.BRAND_NAME))
        assertTrue(match.candidate.matchedFields.contains(MatchedField.STRENGTH))
    }

    @Test
    fun match_handlesSpacingAndCapitalizationDifferences() {
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = "   cEtZiNe   10   MG   tablet   dr. reddy   "
        )

        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue("Expected ConfidentMatch with case and spacing differences", outcome is MatchOutcome.ConfidentMatch)

        val match = outcome as MatchOutcome.ConfidentMatch
        assertEquals("med-003", match.record.id)
        assertEquals("Cetzine", match.record.brandName)
        assertEquals("10 mg", match.record.strength)
    }

    @Test
    fun match_handlesOcrCharacterNoiseAndGluedTokens() {
        // Simulates realistic OCR output seen on Redmi Note 11 (Omeprazole CapsuesP20.mg pr REDDYS ABORATORIES)
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = """
                pr REDDYS ABORATORIES
                Omeprazole CapsuesP20.mg
                Sore pelow26CA
            """.trimIndent()
        )

        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue("Expected ConfidentMatch with OCR noise and glued tokens", outcome is MatchOutcome.ConfidentMatch)

        val match = outcome as MatchOutcome.ConfidentMatch
        assertEquals("med-007", match.record.id)
        assertEquals("Omez", match.record.brandName)
        assertEquals("Omeprazole", match.record.genericName)
        assertEquals("20 mg", match.extractedStrength)
        assertEquals("Capsule", match.extractedDosageForm)
    }

    @Test
    fun match_conflictingStrength_penalizesAndDisqualifiesWrongStrength() {
        // OCR text clearly says 650 mg; should match Dolo 650 and NOT Crocin 500
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = "Paracetamol Tablets IP 650 mg Micro Labs"
        )

        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue(outcome is MatchOutcome.ConfidentMatch)

        val match = outcome as MatchOutcome.ConfidentMatch
        assertEquals("med-002", match.record.id)
        assertEquals("Dolo 650", match.record.brandName)
        assertEquals("650 mg", match.record.strength)
    }

    @Test
    fun match_missingStrengthWithMultiStrengthGeneric_returnsAmbiguousMatch() {
        // Paracetamol has both Crocin 500 mg and Dolo 650 mg in catalogue.
        // If strength is absent, system must return AmbiguousMatch to prevent medication error.
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = "Paracetamol Tablets IP Fast Relief"
        )

        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue("Multi-strength medicine with missing strength must be AmbiguousMatch", outcome is MatchOutcome.AmbiguousMatch)

        val ambiguous = outcome as MatchOutcome.AmbiguousMatch
        assertTrue("Must include candidate records", ambiguous.candidates.size >= 2)
        val candidateIds = ambiguous.candidates.map { it.record.id }
        assertTrue(candidateIds.contains("med-001")) // Crocin
        assertTrue(candidateIds.contains("med-002")) // Dolo
        assertNull(ambiguous.extractedStrength)
    }

    @Test
    fun match_atorvastatinMultiStrength_differentiatesBetween10mgAnd20mg() {
        val ocr10 = TextNormaliser.createSyntheticOcrResult("Atorva 10 mg Atorvastatin Tablets")
        val outcome10 = MedicineMatcher.match(ocr10, testCatalogue)
        assertTrue(outcome10 is MatchOutcome.ConfidentMatch)
        assertEquals("med-011", (outcome10 as MatchOutcome.ConfidentMatch).record.id)

        val ocr20 = TextNormaliser.createSyntheticOcrResult("Atorvastatin Calcium Tablets 20 mg")
        val outcome20 = MedicineMatcher.match(ocr20, testCatalogue)
        assertTrue(outcome20 is MatchOutcome.ConfidentMatch)
        assertEquals("med-012", (outcome20 as MatchOutcome.ConfidentMatch).record.id)
    }

    @Test
    fun match_powderDosageFormAndGramsStrength_matchesElectral() {
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = "Electral Oral Rehydration Salts 21.8 g Powder for oral solution FDC"
        )

        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue(outcome is MatchOutcome.ConfidentMatch)

        val match = outcome as MatchOutcome.ConfidentMatch
        assertEquals("med-024", match.record.id)
        assertEquals("Electral", match.record.brandName)
        assertEquals("21.8 g", match.extractedStrength)
        assertEquals("Powder", match.extractedDosageForm)
    }

    @Test
    fun match_noMatch_whenRecognizedTextUnrelatedToCatalogue() {
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = "Herbal Coconut Hair Oil 200 ml Pure and Natural"
        )

        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue("Unrelated text must produce NoMatch", outcome is MatchOutcome.NoMatch)
    }

    @Test
    fun match_emptyOrInsufficientText_returnsInsufficientText() {
        val emptyOcr = TextNormaliser.createSyntheticOcrResult("")
        val emptyOutcome = MedicineMatcher.match(emptyOcr, testCatalogue)
        assertTrue(emptyOutcome is MatchOutcome.InsufficientText)

        val straySymbols = TextNormaliser.createSyntheticOcrResult(".. --- //")
        val strayOutcome = MedicineMatcher.match(straySymbols, testCatalogue)
        assertTrue(strayOutcome is MatchOutcome.InsufficientText)
    }

    @Test
    fun match_expiryAndBatchOnly_doesNotTriggerMedicineMatch() {
        // Date and batch numbers alone must never be mistaken for medicine names or strengths
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = """
                EXPIRY: 08/2027
                BATCH NO: B789X
                MFG DATE: 09/2024
            """.trimIndent()
        )

        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue("Date/batch without medicine name must be NoMatch or Insufficient", outcome is MatchOutcome.NoMatch)
        val noMatch = outcome as MatchOutcome.NoMatch
        assertEquals("08/2027", noMatch.extractedExpiry)
        assertEquals("B789X", noMatch.extractedBatch)
    }

    @Test
    fun strengthExtraction_extractsVariousUnitsAndFormats() {
        val strengths = MedicineMatcher.extractAllStrengths("Sample 500 mg, 650mg, 21.8 g, 50 mcg, P20.mg")
        assertEquals(5, strengths.size)
        assertTrue(strengths.contains("500 mg"))
        assertTrue(strengths.contains("650 mg"))
        assertTrue(strengths.contains("21.8 g"))
        assertTrue(strengths.contains("50 mcg"))
        assertTrue(strengths.contains("20 mg"))
    }

    @Test
    fun dosageFormExtraction_handlesCanonicalAndPluralVariations() {
        assertEquals("Tablet", MedicineMatcher.extractDosageForm("Film-coated tablets IP"))
        assertEquals("Capsule", MedicineMatcher.extractDosageForm("Hard gelatin capsues 20 mg"))
        assertEquals("Chewable Tablet", MedicineMatcher.extractDosageForm("Chewable Tablet for children"))
        assertEquals("Powder", MedicineMatcher.extractDosageForm("Oral rehydration powder 21.8 g"))
        assertNull(MedicineMatcher.extractDosageForm("Just plain medicine text"))
    }

    @Test
    fun match_realDeviceCapture_calciumPackage_returnsNoMatchWithExtractedBatch() {
        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = """
                CALCIUM WITH VITAASIN 03 TABLETS
                CALCIUM ITH VITAMIN D3 TABLETS
                STANDARD THERAPEUTICS LOCOST
                Batch No. T535
            """.trimIndent()
        )
        val outcome = MedicineMatcher.match(ocr, testCatalogue)
        assertTrue("Calcium is not in the test catalogue; must return NoMatch safely without false match", outcome is MatchOutcome.NoMatch)
        val noMatch = outcome as MatchOutcome.NoMatch
        assertEquals("T535", noMatch.extractedBatch)
    }

    @Test
    fun match_fullBundledDataset_paracetamolWithoutStrength_returnsAmbiguousBetweenCrocinAndDolo() {
        val jsonFile = java.io.File("src/main/assets/medicines.json")
        val json = jsonFile.readText()
        val fullCatalogue = com.armod.identifier.data.local.JsonMedicineParser.parseAndValidate(json)
        assertTrue(fullCatalogue.size >= 25)

        val ocr = TextNormaliser.createSyntheticOcrResult("Paracetamol Tablets IP")
        val outcome = MedicineMatcher.match(ocr, fullCatalogue)
        assertTrue("Paracetamol without strength across 25 records must be AmbiguousMatch", outcome is MatchOutcome.AmbiguousMatch)
        val ambiguous = outcome as MatchOutcome.AmbiguousMatch
        val candidateNames = ambiguous.candidates.map { it.record.brandName }
        assertTrue(candidateNames.contains("Crocin"))
        assertTrue(candidateNames.contains("Dolo 650"))
    }

    @Test
    fun match_fullBundledDataset_omeprazoleRealCapture_matchesOmez20mg() {
        val jsonFile = java.io.File("src/main/assets/medicines.json")
        val json = jsonFile.readText()
        val fullCatalogue = com.armod.identifier.data.local.JsonMedicineParser.parseAndValidate(json)

        val ocr = TextNormaliser.createSyntheticOcrResult(
            rawText = """
                pr REDDYS ABORATORIES
                Omeprazole CapsuesP20.mg
                Sore pelow26CA
            """.trimIndent()
        )
        val outcome = MedicineMatcher.match(ocr, fullCatalogue)
        assertTrue(outcome is MatchOutcome.ConfidentMatch)
        val match = outcome as MatchOutcome.ConfidentMatch
        assertEquals("med-nlem-007", match.record.id)
        assertEquals("Omez", match.record.brandName)
        assertEquals("Omeprazole", match.record.genericName)
        assertEquals("20 mg", match.extractedStrength)
        assertEquals("Capsule", match.extractedDosageForm)
    }
}
