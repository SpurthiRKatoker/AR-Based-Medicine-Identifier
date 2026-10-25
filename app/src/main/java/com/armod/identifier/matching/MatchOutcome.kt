package com.armod.identifier.matching

import com.armod.identifier.data.model.MedicineRecord

/**
 * Categorical confidence rating for medicine identification.
 */
enum class MatchConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NONE
}

/**
 * Specific fields contributing to candidate evidence.
 */
enum class MatchedField {
    BRAND_NAME,
    GENERIC_NAME,
    STRENGTH,
    DOSAGE_FORM,
    MANUFACTURER
}

/**
 * An individual candidate evaluation against OCR text.
 */
data class MatchCandidate(
    val record: MedicineRecord,
    val score: Float,
    val matchedFields: Set<MatchedField>,
    val strengthMatches: Boolean,
    val strengthConflicting: Boolean,
    val reason: String
)

/**
 * Sealed hierarchy of explicit matching outcomes.
 * Guarantees that uncertain or ambiguous evidence is never presented as confirmed identity.
 */
sealed interface MatchOutcome {

    /**
     * Confident identification where name and strength are verified with high fidelity.
     */
    data class ConfidentMatch(
        val record: MedicineRecord,
        val candidate: MatchCandidate,
        val confidence: MatchConfidence,
        val extractedStrength: String?,
        val extractedDosageForm: String?,
        val extractedExpiry: String?,
        val extractedBatch: String?,
        val explanation: String
    ) : MatchOutcome

    /**
     * Multiple plausible records match or strength could not be differentiated.
     */
    data class AmbiguousMatch(
        val candidates: List<MatchCandidate>,
        val extractedStrength: String?,
        val extractedDosageForm: String?,
        val extractedExpiry: String?,
        val extractedBatch: String?,
        val explanation: String
    ) : MatchOutcome

    /**
     * No reference catalogue record matched the recognized text above minimum safety threshold.
     */
    data class NoMatch(
        val explanation: String,
        val extractedExpiry: String? = null,
        val extractedBatch: String? = null,
        val topCandidate: MatchCandidate? = null
    ) : MatchOutcome

    /**
     * The input OCR result contains no or insufficient text to attempt identification.
     */
    data class InsufficientText(
        val explanation: String
    ) : MatchOutcome
}
