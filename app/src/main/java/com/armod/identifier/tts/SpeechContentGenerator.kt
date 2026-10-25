package com.armod.identifier.tts

import com.armod.identifier.matching.MatchOutcome
import com.armod.identifier.ocr.OcrResult
import com.armod.identifier.util.TextNormaliser

/**
 * Data structure representing spoken text generated for an identification result.
 */
data class SpokenResult(
    val title: String,
    val summary: String,
    val basicNameAndDosage: String,
    val warningsAndSideEffects: String,
    val fullSpeech: String,
    val disclaimer: String
)

/**
 * Domain component that transforms typed [MatchOutcome] and optional [OcrResult]
 * into clear, structured, accessible spoken English text.
 *
 * Rules:
 * - 100% pure Kotlin; can be unit-tested without Android framework or TTS engine mocks.
 * - Never invents missing fields or infers unverified strengths.
 * - Explicitly communicates uncertainty on ambiguous or uncatalogued results.
 * - Always includes the mandatory academic prototype safety disclaimer.
 */
object SpeechContentGenerator {

    const val ACADEMIC_DISCLAIMER =
        "Notice: This is an academic research prototype with a limited local catalogue. " +
        "This result is not a substitute for checking your physical package or consulting a qualified doctor or pharmacist."

    /**
     * Generates a [SpokenResult] tailored to the given [MatchOutcome].
     */
    fun generateSpeech(outcome: MatchOutcome, ocrResult: OcrResult? = null): SpokenResult {
        return when (outcome) {
            is MatchOutcome.ConfidentMatch -> {
                val record = outcome.record
                val brand = record.brandName ?: record.genericName ?: "Unknown medicine"
                val strength = record.strength.orEmpty()
                val form = record.dosageForm.orEmpty()
                val speechStrength = expandStrengthForSpeech(strength)

                val title = buildString {
                    append(brand)
                    if (strength.isNotBlank()) append(" $strength")
                    if (form.isNotBlank()) append(" $form")
                }

                val basicNameAndDosage = buildString {
                    append(brand)
                    if (strength.isNotBlank()) append(", $speechStrength")
                    if (form.isNotBlank()) append(", $form")
                    append(".")
                }

                val summary = buildString {
                    append("Identified: $brand")
                    if (strength.isNotBlank()) append(", $speechStrength")
                    if (form.isNotBlank()) append(", $form")
                    append(".")
                }

                val warningsAndSideEffects = buildString {
                    append("Warnings and side effects for $brand. ")
                    val hasWarnings = record.warnings.isNotEmpty()
                    val hasSideEffects = record.commonSideEffects.isNotEmpty()
                    if (hasWarnings) {
                        append("Warnings: ${record.warnings.joinToString(". ")}. ")
                    }
                    if (hasSideEffects) {
                        append("Common side effects: ${record.commonSideEffects.joinToString(", ")}. ")
                    }
                    if (!hasWarnings && !hasSideEffects) {
                        append("No specific warnings or side effects recorded for this medicine in the local catalogue. ")
                    }
                    append(ACADEMIC_DISCLAIMER)
                }

                val sb = StringBuilder()
                sb.append(summary).append(" ")
                if (!record.genericName.isNullOrBlank() && record.genericName != brand) {
                    sb.append("Active ingredient: ${record.genericName}. ")
                }
                if (!record.manufacturer.isNullOrBlank()) {
                    sb.append("Manufacturer: ${record.manufacturer}. ")
                }

                // Batch and Expiry from matching outcome or OCR text
                val batch = outcome.extractedBatch
                    ?: ocrResult?.rawText?.let { TextNormaliser.extractBatchCandidate(it) }
                val expiry = outcome.extractedExpiry
                    ?: ocrResult?.rawText?.let { TextNormaliser.extractExpiryCandidate(it) }

                if (!batch.isNullOrBlank()) {
                    sb.append("Batch code on package: $batch. ")
                }
                if (!expiry.isNullOrBlank()) {
                    sb.append("Expiry date on package: $expiry. ")
                }

                if (!record.labelPurpose.isNullOrBlank()) {
                    sb.append("Purpose: ${record.labelPurpose}. ")
                }
                if (!record.labelDirections.isNullOrBlank()) {
                    sb.append("Directions: ${record.labelDirections}. ")
                }
                if (record.warnings.isNotEmpty()) {
                    sb.append("Warning: ${record.warnings.joinToString(". ")}. ")
                }
                if (record.commonSideEffects.isNotEmpty()) {
                    sb.append("Common side effects: ${record.commonSideEffects.joinToString(", ")}. ")
                }
                sb.append(ACADEMIC_DISCLAIMER)

                SpokenResult(
                    title = title,
                    summary = summary,
                    basicNameAndDosage = basicNameAndDosage,
                    warningsAndSideEffects = warningsAndSideEffects,
                    fullSpeech = sb.toString(),
                    disclaimer = ACADEMIC_DISCLAIMER
                )
            }

            is MatchOutcome.AmbiguousMatch -> {
                val title = "Identity Uncertain"
                val count = outcome.candidates.size
                val basicNameAndDosage = "Identity is uncertain. The label matches $count possible medicines."
                val summary = "Identity is uncertain. The label matches $count possible medicines."

                val warningsAndSideEffects = "Identity is uncertain. Warnings and side effects cannot be confirmed until the exact medicine is verified. Please check your physical package directly. $ACADEMIC_DISCLAIMER"

                val sb = StringBuilder()
                sb.append("Identity is uncertain. The scanned label matches $count possible medicines in the local catalogue. ")
                outcome.candidates.forEachIndexed { index, candidate ->
                    val candBrand = candidate.record.brandName ?: candidate.record.genericName ?: "Unknown"
                    val candSpeechStrength = expandStrengthForSpeech(candidate.record.strength.orEmpty())
                    val candForm = candidate.record.dosageForm.orEmpty()
                    sb.append("Candidate ${index + 1}: $candBrand, $candSpeechStrength, $candForm. ")
                }
                sb.append("Because the exact strength or formulation cannot be confirmed, please check your physical package directly before taking any medicine. ")
                sb.append(ACADEMIC_DISCLAIMER)

                SpokenResult(
                    title = title,
                    summary = summary,
                    basicNameAndDosage = basicNameAndDosage,
                    warningsAndSideEffects = warningsAndSideEffects,
                    fullSpeech = sb.toString(),
                    disclaimer = ACADEMIC_DISCLAIMER
                )
            }

            is MatchOutcome.NoMatch -> {
                val title = "Medicine Not Found"
                val basicNameAndDosage = "No matching medicine found in the offline reference catalogue."
                val summary = "No matching medicine found in the local catalogue."
                val warningsAndSideEffects = "No matching medicine was found in the offline reference catalogue. Warnings and side effects are unavailable. $ACADEMIC_DISCLAIMER"
                val full = "No matching medicine was found in the offline reference catalogue. " +
                    "Please ensure the package label is clearly visible under good lighting, or check your physical package directly. " +
                    ACADEMIC_DISCLAIMER

                SpokenResult(
                    title = title,
                    summary = summary,
                    basicNameAndDosage = basicNameAndDosage,
                    warningsAndSideEffects = warningsAndSideEffects,
                    fullSpeech = full,
                    disclaimer = ACADEMIC_DISCLAIMER
                )
            }

            is MatchOutcome.InsufficientText -> {
                val title = "Unreadable Label"
                val basicNameAndDosage = "The package label could not be read reliably."
                val summary = "The package label could not be read reliably."
                val warningsAndSideEffects = "The package label could not be read reliably. Warnings and side effects are unavailable. Please hold steady with good lighting and try scanning again. $ACADEMIC_DISCLAIMER"
                val full = "The package label could not be read reliably. " +
                    "Please hold your phone steady, ensure the label is well lit and free of glare, and try scanning again. " +
                    ACADEMIC_DISCLAIMER

                SpokenResult(
                    title = title,
                    summary = summary,
                    basicNameAndDosage = basicNameAndDosage,
                    warningsAndSideEffects = warningsAndSideEffects,
                    fullSpeech = full,
                    disclaimer = ACADEMIC_DISCLAIMER
                )
            }
        }
    }

    /**
     * Expands abbreviated strength units into clear spoken words for natural TTS pronunciation.
     * e.g. "20mg" -> "20 milligrams", "500 mcg" -> "500 micrograms", "10ml" -> "10 milliliters".
     */
    fun expandStrengthForSpeech(strength: String?): String {
        if (strength.isNullOrBlank()) return "unspecified strength"
        return strength
            .replace(Regex("(?i)(\\d+(?:\\.\\d+)?)\\s*mg\\b"), "$1 milligrams")
            .replace(Regex("(?i)(\\d+(?:\\.\\d+)?)\\s*mcg\\b"), "$1 micrograms")
            .replace(Regex("(?i)(\\d+(?:\\.\\d+)?)\\s*g\\b"), "$1 grams")
            .replace(Regex("(?i)(\\d+(?:\\.\\d+)?)\\s*ml\\b"), "$1 milliliters")
            .replace(Regex("(?i)(\\d+(?:\\.\\d+)?)\\s*iu\\b"), "$1 international units")
            .replace(Regex("(?i)(\\d+(?:\\.\\d+)?)\\s*%"), "$1 percent")
    }
}
