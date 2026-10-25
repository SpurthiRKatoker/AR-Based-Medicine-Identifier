package com.armod.identifier.tts

import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.matching.MatchCandidate
import com.armod.identifier.matching.MatchConfidence
import com.armod.identifier.matching.MatchedField
import com.armod.identifier.matching.MatchOutcome
import com.armod.identifier.ocr.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechContentGeneratorTest {

    private val sampleRecord = MedicineRecord(
        id = "med_omeprazole_20",
        brandName = "Omez",
        genericName = "Omeprazole",
        strength = "20mg",
        dosageForm = "Capsule",
        manufacturer = "Dr. Reddy's Laboratories",
        labelPurpose = "Acid Reflux and Heartburn",
        labelDirections = "Take once daily before a meal",
        warnings = listOf("Do not crush or chew capsules"),
        commonSideEffects = listOf("Headache", "Abdominal pain"),
        sourceTitle = "FDA DailyMed",
        datasetVersion = "1.0.0"
    )

    private val sampleCandidate = MatchCandidate(
        record = sampleRecord,
        score = 0.95f,
        matchedFields = setOf(MatchedField.BRAND_NAME, MatchedField.STRENGTH, MatchedField.DOSAGE_FORM),
        strengthMatches = true,
        strengthConflicting = false,
        reason = "Exact brand and strength verified"
    )

    @Test
    fun `expandStrengthForSpeech properly expands standard dosage units`() {
        assertEquals("20 milligrams", SpeechContentGenerator.expandStrengthForSpeech("20mg"))
        assertEquals("500 milligrams", SpeechContentGenerator.expandStrengthForSpeech("500 mg"))
        assertEquals("10 micrograms", SpeechContentGenerator.expandStrengthForSpeech("10mcg"))
        assertEquals("5 grams", SpeechContentGenerator.expandStrengthForSpeech("5g"))
        assertEquals("10 milliliters", SpeechContentGenerator.expandStrengthForSpeech("10ml"))
        assertEquals("500 international units", SpeechContentGenerator.expandStrengthForSpeech("500iu"))
        assertEquals("1 percent", SpeechContentGenerator.expandStrengthForSpeech("1%"))
        assertEquals("unspecified strength", SpeechContentGenerator.expandStrengthForSpeech(""))
    }

    @Test
    fun `confident match generates comprehensive speech with name, strength, label directions, and disclaimer`() {
        val outcome = MatchOutcome.ConfidentMatch(
            record = sampleRecord,
            candidate = sampleCandidate,
            confidence = MatchConfidence.HIGH,
            extractedStrength = "20mg",
            extractedDosageForm = "Capsule",
            extractedExpiry = "12/2027",
            extractedBatch = "T535",
            explanation = "Verified candidate"
        )
        val ocrResult = OcrResult(
            rawText = "Omez 20mg B.No: T535 EXP: 12/2027"
        )

        val spoken = SpeechContentGenerator.generateSpeech(outcome, ocrResult)

        assertTrue("Summary should contain brand name", spoken.summary.contains("Omez"))
        assertTrue("Summary should contain expanded strength", spoken.summary.contains("20 milligrams"))
        assertTrue("Summary should contain dosage form", spoken.summary.contains("Capsule"))
        assertTrue("Basic name and dosage should contain brand and expanded strength", spoken.basicNameAndDosage.contains("Omez, 20 milligrams, Capsule"))
        assertTrue("Warnings and side effects should contain warnings", spoken.warningsAndSideEffects.contains("Do not crush or chew capsules"))
        assertTrue("Warnings and side effects should contain side effects", spoken.warningsAndSideEffects.contains("Headache, Abdominal pain"))
        assertTrue("Warnings and side effects should include disclaimer", spoken.warningsAndSideEffects.contains(SpeechContentGenerator.ACADEMIC_DISCLAIMER))
        assertTrue("Full speech should contain active ingredient", spoken.fullSpeech.contains("Omeprazole"))
        assertTrue("Full speech should contain manufacturer", spoken.fullSpeech.contains("Dr. Reddy's Laboratories"))
        assertTrue("Full speech should contain batch code", spoken.fullSpeech.contains("T535"))
        assertTrue("Full speech should contain expiry date", spoken.fullSpeech.contains("12/2027"))
        assertTrue("Full speech should contain directions", spoken.fullSpeech.contains("Take once daily before a meal"))
        assertTrue("Full speech should contain warning", spoken.fullSpeech.contains("Do not crush or chew capsules"))
        assertTrue("Full speech should contain side effects", spoken.fullSpeech.contains("Headache, Abdominal pain"))
        assertTrue("Full speech should include academic disclaimer", spoken.fullSpeech.contains(SpeechContentGenerator.ACADEMIC_DISCLAIMER))
    }

    @Test
    fun `confident match without OCR attributes does not fabricate batch or expiry`() {
        val outcome = MatchOutcome.ConfidentMatch(
            record = sampleRecord,
            candidate = sampleCandidate,
            confidence = MatchConfidence.HIGH,
            extractedStrength = "20mg",
            extractedDosageForm = "Capsule",
            extractedExpiry = null,
            extractedBatch = null,
            explanation = "Verified candidate"
        )
        val ocrResult = OcrResult(rawText = "Omez 20mg only")

        val spoken = SpeechContentGenerator.generateSpeech(outcome, ocrResult)

        assertFalse("Should not fabricate batch code", spoken.fullSpeech.contains("Batch code on package"))
        assertFalse("Should not fabricate expiry date", spoken.fullSpeech.contains("Expiry date on package"))
        assertTrue("Still includes disclaimer", spoken.fullSpeech.contains(SpeechContentGenerator.ACADEMIC_DISCLAIMER))
    }

    @Test
    fun `ambiguous match explicitly announces uncertainty and all candidate options`() {
        val rec1 = sampleRecord.copy(id = "med_1", brandName = "Paracetamol", strength = "500mg", dosageForm = "Tablet")
        val rec2 = sampleRecord.copy(id = "med_2", brandName = "Paracetamol", strength = "650mg", dosageForm = "Tablet")

        val cand1 = MatchCandidate(
            record = rec1,
            score = 0.75f,
            matchedFields = setOf(MatchedField.BRAND_NAME),
            strengthMatches = false,
            strengthConflicting = false,
            reason = "Brand matched but strength ambiguous"
        )
        val cand2 = MatchCandidate(
            record = rec2,
            score = 0.75f,
            matchedFields = setOf(MatchedField.BRAND_NAME),
            strengthMatches = false,
            strengthConflicting = false,
            reason = "Brand matched but strength ambiguous"
        )

        val outcome = MatchOutcome.AmbiguousMatch(
            candidates = listOf(cand1, cand2),
            extractedStrength = null,
            extractedDosageForm = "Tablet",
            extractedExpiry = null,
            extractedBatch = null,
            explanation = "Multiple catalogued strengths for Paracetamol"
        )

        val spoken = SpeechContentGenerator.generateSpeech(outcome)

        assertEquals("Identity Uncertain", spoken.title)
        assertTrue(spoken.summary.contains("Identity is uncertain"))
        assertTrue(spoken.summary.contains("2 possible medicines"))
        assertTrue(spoken.basicNameAndDosage.contains("Identity is uncertain"))
        assertTrue(spoken.warningsAndSideEffects.contains("cannot be confirmed"))
        assertTrue(spoken.fullSpeech.contains("Candidate 1: Paracetamol, 500 milligrams, Tablet"))
        assertTrue(spoken.fullSpeech.contains("Candidate 2: Paracetamol, 650 milligrams, Tablet"))
        assertTrue(spoken.fullSpeech.contains("check your physical package directly"))
        assertTrue(spoken.fullSpeech.contains(SpeechContentGenerator.ACADEMIC_DISCLAIMER))
    }

    @Test
    fun `no match generates clear explanation without guessing`() {
        val outcome = MatchOutcome.NoMatch("Package not found in local catalogue")
        val spoken = SpeechContentGenerator.generateSpeech(outcome)

        assertEquals("Medicine Not Found", spoken.title)
        assertTrue(spoken.summary.contains("No matching medicine found"))
        assertTrue(spoken.basicNameAndDosage.contains("No matching medicine found"))
        assertTrue(spoken.warningsAndSideEffects.contains("unavailable"))
        assertTrue(spoken.fullSpeech.contains("offline reference catalogue"))
        assertTrue(spoken.fullSpeech.contains(SpeechContentGenerator.ACADEMIC_DISCLAIMER))
    }

    @Test
    fun `insufficient text outcome invites user to rescan`() {
        val outcome = MatchOutcome.InsufficientText("Text unreadable")
        val spoken = SpeechContentGenerator.generateSpeech(outcome)

        assertEquals("Unreadable Label", spoken.title)
        assertTrue(spoken.summary.contains("could not be read reliably"))
        assertTrue(spoken.basicNameAndDosage.contains("could not be read reliably"))
        assertTrue(spoken.warningsAndSideEffects.contains("unavailable"))
        assertTrue(spoken.fullSpeech.contains("hold your phone steady"))
        assertTrue(spoken.fullSpeech.contains("try scanning again"))
        assertTrue(spoken.fullSpeech.contains(SpeechContentGenerator.ACADEMIC_DISCLAIMER))
    }
}
