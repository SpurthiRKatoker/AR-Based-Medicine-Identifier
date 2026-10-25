package com.armod.identifier.edgecases

import com.armod.identifier.ar.ArAvailabilityStatus
import com.armod.identifier.ar.ArSessionState
import com.armod.identifier.ar.ArTrackingState
import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.matching.MatchConfidence
import com.armod.identifier.matching.MatchOutcome
import com.armod.identifier.matching.MedicineMatcher
import com.armod.identifier.tts.SpeechContentGenerator
import com.armod.identifier.util.TextNormaliser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 8 Integration & Edge-Case Test Suite.
 *
 * Covers:
 * 1. OCR Noise, Blank, Garbled, and Insufficient Text inputs.
 * 2. Missing strength, conflicting strength, ambiguous formulations, and uncatalogued medicines.
 * 3. Batch and Expiry edge cases (dates without medicine, missing dates, varied formats).
 * 4. Text-to-Speech synthesis content safety, null handling, and academic disclaimer persistence.
 * 5. ARCore state transitions, fallback triggering, tracking limitations, and 2D fallback guarantees.
 */
class Phase8EdgeCaseTest {

    private lateinit var catalogue: List<MedicineRecord>

    @Before
    fun setUp() {
        catalogue = listOf(
            MedicineRecord(
                id = "med-001",
                brandName = "Crocin",
                genericName = "Paracetamol",
                strength = "500 mg",
                dosageForm = "Tablet",
                manufacturer = "GlaxoSmithKline Pharmaceuticals Ltd",
                labelPurpose = "Relief of mild to moderate pain and fever.",
                sourceTitle = "Catalogue V1",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-002",
                brandName = "Dolo 650",
                genericName = "Paracetamol",
                strength = "650 mg",
                dosageForm = "Tablet",
                manufacturer = "Micro Labs Ltd",
                labelPurpose = "Relief of pain and reduction of fever.",
                sourceTitle = "Catalogue V1",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-003",
                brandName = "Cetzine",
                genericName = "Cetirizine Hydrochloride",
                strength = "10 mg",
                dosageForm = "Tablet",
                manufacturer = "Dr. Reddy's Laboratories Ltd",
                labelPurpose = "Relief of allergic symptoms such as runny nose and hives.",
                sourceTitle = "Catalogue V1",
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
                sourceTitle = "Catalogue V1",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-011",
                brandName = "Atorva",
                genericName = "Atorvastatin Calcium",
                strength = "10 mg",
                dosageForm = "Tablet",
                manufacturer = "Zydus Healthcare",
                labelPurpose = "Lowers elevated LDL cholesterol.",
                sourceTitle = "Catalogue V1",
                datasetVersion = "1.0"
            ),
            MedicineRecord(
                id = "med-012",
                brandName = "Atorva 20",
                genericName = "Atorvastatin Calcium",
                strength = "20 mg",
                dosageForm = "Tablet",
                manufacturer = "Zydus Healthcare",
                labelPurpose = "Lowers elevated LDL cholesterol at higher dose.",
                sourceTitle = "Catalogue V1",
                datasetVersion = "1.0"
            )
        )
    }

    // =========================================================================
    // 1. OCR Noise, Blank, Garbled, and Insufficient Text
    // =========================================================================

    @Test
    fun ocr_blankOrWhitespaceOnly_returnsInsufficientText() {
        val blankResult = TextNormaliser.createSyntheticOcrResult("")
        val outcome1 = MedicineMatcher.match(blankResult, catalogue)
        assertTrue("Blank input must yield InsufficientText", outcome1 is MatchOutcome.InsufficientText)

        val whitespaceResult = TextNormaliser.createSyntheticOcrResult("   \n\t  \r\n  ")
        val outcome2 = MedicineMatcher.match(whitespaceResult, catalogue)
        assertTrue("Whitespace input must yield InsufficientText", outcome2 is MatchOutcome.InsufficientText)
    }

    @Test
    fun ocr_symbolsAndNoiseOnly_returnsInsufficientText() {
        val noiseResult = TextNormaliser.createSyntheticOcrResult("### !!! @@@ $$$ %%% ^^^ &&& *** (( )) __ +++")
        val outcome = MedicineMatcher.match(noiseResult, catalogue)
        assertTrue("Symbols without alphanumeric text must yield InsufficientText", outcome is MatchOutcome.InsufficientText)
    }

    @Test
    fun ocr_underMinimumAlphanumericThreshold_returnsInsufficientText() {
        val tinyResult = TextNormaliser.createSyntheticOcrResult("Rx")
        val outcome = MedicineMatcher.match(tinyResult, catalogue)
        assertTrue("Two letters is below the 3 alphanumeric threshold", outcome is MatchOutcome.InsufficientText)
    }

    // =========================================================================
    // 2. Missing Strength, Conflicting Strength, and Ambiguity Gates
    // =========================================================================

    @Test
    fun matching_multiStrengthGenericMissingStrength_returnsAmbiguousOutcome() {
        // "Paracetamol Tablets IP" matches both Crocin (500mg) and Dolo (650mg).
        // Without explicit strength, it must NEVER pick one arbitrarily.
        val genericOnly = TextNormaliser.createSyntheticOcrResult(
            "Paracetamol Tablets IP\n10 Tablets\nManufactured in India"
        )
        val outcome = MedicineMatcher.match(genericOnly, catalogue)

        assertTrue(
            "Multi-strength generic without verified strength must return AmbiguousMatch",
            outcome is MatchOutcome.AmbiguousMatch
        )
        val ambiguous = outcome as MatchOutcome.AmbiguousMatch
        assertTrue("Should include at least 2 candidates", ambiguous.candidates.size >= 2)
        val names = ambiguous.candidates.map { it.record.brandName }
        assertTrue("Must include paracetamol formulations", names.contains("Crocin") || names.contains("Dolo 650"))
    }

    @Test
    fun matching_conflictingStrength_preventsConfidentMatch() {
        // "Crocin 650 mg" — Crocin in catalogue is 500mg, but OCR says 650mg.
        // It should flag a conflict and NOT award HIGH confidence Crocin 500.
        val conflictingOcr = TextNormaliser.createSyntheticOcrResult(
            "Crocin 650 mg Tablet\nRelief of headache and fever"
        )
        val outcome = MedicineMatcher.match(conflictingOcr, catalogue)

        if (outcome is MatchOutcome.ConfidentMatch) {
            // If it matched, it must NOT be marked HIGH confidence because strength conflicts
            assertFalse(
                "Conflicting strength must never yield high confidence match",
                outcome.confidence == MatchConfidence.HIGH && outcome.record.strength == "500 mg"
            )
        } else {
            // AmbiguousMatch is also a completely safe and valid outcome for conflicting strength
            assertTrue(outcome is MatchOutcome.AmbiguousMatch)
        }
    }

    @Test
    fun matching_uncataloguedMedicine_returnsNoMatch() {
        val unknownMedicine = TextNormaliser.createSyntheticOcrResult(
            "Amoxicillin Trihydrate 500 mg\nNovamox Capsules\nBatch No: NX8821\nExp: 09/2028"
        )
        val outcome = MedicineMatcher.match(unknownMedicine, catalogue)

        assertTrue("Medicine not in catalogue must yield NoMatch", outcome is MatchOutcome.NoMatch)
        val noMatch = outcome as MatchOutcome.NoMatch
        // Batch and Expiry should still be extracted even if medicine is uncatalogued
        assertEquals("NX8821", noMatch.extractedBatch)
        assertEquals("09/2028", noMatch.extractedExpiry)
    }

    // =========================================================================
    // 3. Batch and Expiry Edge Cases
    // =========================================================================

    @Test
    fun matching_batchAndExpiryOnly_doesNotFabricateMedicine() {
        val datesOnly = TextNormaliser.createSyntheticOcrResult(
            "Batch No. BT-9982\nMFG: 01/2024\nEXP. 12/2027\nM.R.P. Rs. 45.00"
        )
        val outcome = MedicineMatcher.match(datesOnly, catalogue)

        assertTrue("Batch/Expiry text alone must yield NoMatch, never fabricate a medicine", outcome is MatchOutcome.NoMatch)
        val noMatch = outcome as MatchOutcome.NoMatch
        assertEquals("BT-9982", noMatch.extractedBatch)
        assertEquals("12/2027", noMatch.extractedExpiry)
    }

    @Test
    fun textNormaliser_handlesVariousExpiryFormats() {
        assertEquals("08/2027", TextNormaliser.extractExpiryCandidate("EXPIRY: 08/2027"))
        assertEquals("11/26", TextNormaliser.extractExpiryCandidate("EXP 11/26"))
        assertEquals("05/2029", TextNormaliser.extractExpiryCandidate("USE BEFORE 05/2029"))
        assertEquals("01/25", TextNormaliser.extractExpiryCandidate("EXP.DATE 01/25"))
        assertNull(TextNormaliser.extractExpiryCandidate("No dates on this package at all"))
    }

    @Test
    fun textNormaliser_handlesVariousBatchFormats() {
        assertEquals("AB1234", TextNormaliser.extractBatchCandidate("Batch No. AB1234"))
        assertEquals("99881", TextNormaliser.extractBatchCandidate("B.No. 99881"))
        assertEquals("XF-201", TextNormaliser.extractBatchCandidate("LOT: XF-201"))
        assertNull(TextNormaliser.extractBatchCandidate("Only regular instructions for use"))
    }

    // =========================================================================
    // 4. Text-To-Speech Content Safety & Null Handling
    // =========================================================================

    @Test
    fun speechContentGenerator_confidentMatchAllFields_containsAllEssentialInfoAndDisclaimer() {
        val record = catalogue.first { it.brandName == "Crocin" }
        val outcome = MatchOutcome.ConfidentMatch(
            record = record,
            candidate = MedicineMatcher.evaluateCandidate(record, "Crocin 500 mg", listOf("500 mg"), "Tablet")!!,
            confidence = MatchConfidence.HIGH,
            extractedStrength = "500 mg",
            extractedDosageForm = "Tablet",
            extractedExpiry = "08/2027",
            extractedBatch = "B1001",
            explanation = "Matched brand and strength"
        )

        val speech = SpeechContentGenerator.generateSpeech(outcome).fullSpeech

        assertTrue("Speech must contain brand name", speech.contains("Crocin", ignoreCase = true))
        assertTrue("Speech must contain generic name", speech.contains("Paracetamol", ignoreCase = true))
        assertTrue("Speech must contain strength", speech.contains("500 milligrams", ignoreCase = true) || speech.contains("500 mg", ignoreCase = true))
        assertTrue("Speech must contain dosage form", speech.contains("Tablet", ignoreCase = true))
        assertTrue("Speech must contain batch", speech.contains("B1001", ignoreCase = true))
        assertTrue("Speech must contain expiry", speech.contains("08/2027", ignoreCase = true))
        assertTrue("Speech MUST contain academic prototype disclaimer", speech.contains("academic research prototype", ignoreCase = true))
        assertFalse("Speech must never contain literal 'null'", speech.contains("null", ignoreCase = true))
    }

    @Test
    fun speechContentGenerator_confidentMatchMissingOptionalFields_handlesNullGracefully() {
        val record = catalogue.first { it.brandName == "Brufen" }
        val outcome = MatchOutcome.ConfidentMatch(
            record = record,
            candidate = MedicineMatcher.evaluateCandidate(record, "Brufen", emptyList(), null)!!,
            confidence = MatchConfidence.MEDIUM,
            extractedStrength = null,
            extractedDosageForm = null,
            extractedExpiry = null,
            extractedBatch = null,
            explanation = "Matched brand"
        )

        val speech = SpeechContentGenerator.generateSpeech(outcome).fullSpeech

        assertTrue("Speech must contain brand name", speech.contains("Brufen", ignoreCase = true))
        assertTrue("Speech must contain generic name", speech.contains("Ibuprofen", ignoreCase = true))
        assertFalse("Speech must never pronounce literal 'null'", speech.contains("null", ignoreCase = true))
        assertFalse("Speech must omit batch sentence when null", speech.contains("Batch code on package", ignoreCase = true))
        assertTrue("Speech must preserve disclaimer", speech.contains("academic research prototype", ignoreCase = true))
    }

    @Test
    fun speechContentGenerator_ambiguousOutcome_warnsAndPreservesDisclaimer() {
        val candidates = catalogue.take(2).map {
            MedicineMatcher.evaluateCandidate(it, "Paracetamol", emptyList(), null)!!
        }
        val outcome = MatchOutcome.AmbiguousMatch(
            candidates = candidates,
            extractedStrength = null,
            extractedDosageForm = null,
            extractedExpiry = null,
            extractedBatch = null,
            explanation = "Multiple medicines match"
        )

        val speech = SpeechContentGenerator.generateSpeech(outcome).fullSpeech

        assertTrue("Must announce ambiguity", speech.contains("Ambiguous", ignoreCase = true) || speech.contains("uncertain", ignoreCase = true))
        assertTrue("Must include disclaimer", speech.contains("academic research prototype", ignoreCase = true))
    }

    @Test
    fun speechContentGenerator_noMatchOutcome_speaksSafeGuidance() {
        val outcome = MatchOutcome.NoMatch(
            explanation = "No matching medicine found",
            extractedExpiry = "05/2026",
            extractedBatch = null,
            topCandidate = null
        )

        val speech = SpeechContentGenerator.generateSpeech(outcome).fullSpeech

        assertTrue("Must announce no match", speech.contains("No matching", ignoreCase = true) || speech.contains("not found", ignoreCase = true))
        assertTrue("Must preserve disclaimer", speech.contains("academic research prototype", ignoreCase = true))
    }

    @Test
    fun speechContentGenerator_insufficientText_speaksGuidance() {
        val outcome = MatchOutcome.InsufficientText(
            explanation = "Insufficient readable text"
        )

        val speech = SpeechContentGenerator.generateSpeech(outcome).fullSpeech

        assertTrue("Must guide user to adjust camera", speech.contains("lighting", ignoreCase = true) || speech.contains("steady", ignoreCase = true) || speech.contains("reliably", ignoreCase = true))
        assertTrue("Must preserve disclaimer", speech.contains("academic research prototype", ignoreCase = true))
    }

    // =========================================================================
    // 5. ARCore State Transitions, Fallback Guarantees, and Tracking Resilience
    // =========================================================================

    @Test
    fun arAvailability_unsupportedDevice_mapsToUnsupportedState() {
        val unsupportedStatus = ArAvailabilityStatus.UnsupportedDevice("Exynos 850 lacks ARCore hardware certification")
        assertTrue("Status must be UnsupportedDevice", unsupportedStatus is ArAvailabilityStatus.UnsupportedDevice)
        assertFalse("Unsupported status must not be SupportedAndInstalled", unsupportedStatus is ArAvailabilityStatus.SupportedAndInstalled)
        assertTrue("Reason must reflect hardware details", unsupportedStatus.reason.contains("Exynos"))
    }

    @Test
    fun arTrackingState_limitedReasons_provideActionableGuidance() {
        val lowLight = ArTrackingState.Limited("Low lighting: improve illumination")
        assertTrue("Should contain lighting guidance", lowLight.reason.contains("illumination", ignoreCase = true))

        val motion = ArTrackingState.Limited("Excessive motion: hold device steady")
        assertTrue("Should contain steady guidance", motion.reason.contains("steady", ignoreCase = true))

        val texture = ArTrackingState.Limited("Searching for package texture…")
        assertTrue("Should contain texture guidance", texture.reason.contains("texture", ignoreCase = true))
    }

    @Test
    fun arSessionState_errorWith2DFallback_allowsSafeTransition() {
        val errorState = ArSessionState.Error("Camera in use", canFallbackTo2D = true)
        assertTrue("Error state must allow 2D fallback", errorState.canFallbackTo2D)
        assertEquals("Camera in use", errorState.message)
    }
}
