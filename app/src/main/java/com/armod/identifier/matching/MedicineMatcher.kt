package com.armod.identifier.matching

import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.ocr.OcrResult
import com.armod.identifier.util.TextNormaliser
import kotlin.math.max

/**
 * On-device medicine matching engine and confidence gate.
 *
 * Implements conservative matching against local bundled medicine catalogue:
 * - Decouples OCR text into normalized tokens, candidate strengths, and dosage forms.
 * - Enforces the Exact Identity Rule: ambiguous strengths or missing distinctions cannot be ConfidentMatch.
 * - Protects against medication look-alike errors and conflicting strengths.
 * - Operates entirely offline with zero cloud dependency.
 */
object MedicineMatcher {

    private const val MIN_CONFIDENT_SCORE = 60f
    private const val AMBIGUOUS_THRESHOLD_SCORE = 35f
    private const val MARGIN_OF_AMBIGUITY = 15f

    // Regex to capture strengths like "500 mg", "650mg", "20 mg", "21.8 g", "50 mcg", "4 mg", "100 mg", "400 mg", "10 mg"
    // Also captures OCR glued strings like "P20.mg", "CapsuesP20.mg", "500mg.", etc.
    private val STRENGTH_REGEX = Regex(
        pattern = """(?:\b|[a-zA-Z])(\d+(?:\.\d+)?)[.\s]*(mg|mcg|g|ml|iu|%)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    // Common dosage forms
    private val DOSAGE_FORM_PATTERNS = listOf(
        "chewable tablet" to "Chewable Tablet",
        "chewable" to "Chewable Tablet",
        "tablet" to "Tablet",
        "tablets" to "Tablet",
        "tab" to "Tablet",
        "tabet" to "Tablet",
        "capsule" to "Capsule",
        "capsules" to "Capsule",
        "capsues" to "Capsule",
        "capsue" to "Capsule",
        "cap" to "Capsule",
        "powder" to "Powder",
        "syrup" to "Syrup",
        "suspension" to "Suspension",
        "ointment" to "Ointment"
    )

    // Words that should not be treated as medicine names
    private val STOP_WORDS = setOf(
        "ip", "bp", "usp", "mfg", "exp", "expiry", "batch", "lot", "tab", "tablet",
        "tablets", "cap", "capsule", "capsules", "for", "use", "date", "no", "contains",
        "each", "film", "coated", "keep", "out", "of", "reach", "children", "store",
        "below", "protect", "from", "light", "moisture", "dosage", "directed", "by", "physician"
    )

    /**
     * Matches the given OCR result against the reference catalogue and returns an explicit MatchOutcome.
     */
    fun match(ocrResult: OcrResult, catalogue: List<MedicineRecord>): MatchOutcome {
        val rawText = ocrResult.rawText
        val alphanumericChars = rawText.count { it.isLetterOrDigit() }

        if (ocrResult.isEmpty || alphanumericChars < 3) {
            return MatchOutcome.InsufficientText(
                explanation = "The captured frame contains insufficient readable text to identify a medicine."
            )
        }

        val extractedExpiry = TextNormaliser.extractExpiryCandidate(rawText)
        val extractedBatch = TextNormaliser.extractBatchCandidate(rawText)
        val extractedStrengths = extractAllStrengths(rawText)
        val extractedDosageForm = extractDosageForm(rawText)

        // Evaluate all catalogue records
        val candidates = catalogue.mapNotNull { record ->
            evaluateCandidate(
                record = record,
                rawOcrText = rawText,
                extractedStrengths = extractedStrengths,
                extractedDosageForm = extractedDosageForm
            )
        }.sortedByDescending { it.score }

        if (candidates.isEmpty() || candidates.first().score < AMBIGUOUS_THRESHOLD_SCORE) {
            return MatchOutcome.NoMatch(
                explanation = "No matching medicine found in the local catalogue for the recognized text.",
                extractedExpiry = extractedExpiry,
                extractedBatch = extractedBatch,
                topCandidate = candidates.firstOrNull()
            )
        }

        val topCandidate = candidates.first()
        val runnerUp = candidates.getOrNull(1)

        // Ambiguity Check 1: Multi-strength generic ambiguity
        // If multiple candidates share the same generic name in catalogue (e.g. Paracetamol 500mg vs 650mg, Atorvastatin 10mg vs 20mg)
        // and strength is unverified in OCR, it MUST be ambiguous.
        val hasMultiStrengthPeers = catalogue.count {
            it.genericName.equals(topCandidate.record.genericName, ignoreCase = true)
        } > 1

        if (hasMultiStrengthPeers && !topCandidate.strengthMatches) {
            val genericPeers = candidates.filter {
                it.record.genericName.equals(topCandidate.record.genericName, ignoreCase = true)
            }
            return MatchOutcome.AmbiguousMatch(
                candidates = if (genericPeers.size >= 2) genericPeers else candidates.take(2),
                extractedStrength = extractedStrengths.firstOrNull(),
                extractedDosageForm = extractedDosageForm,
                extractedExpiry = extractedExpiry,
                extractedBatch = extractedBatch,
                explanation = "Recognized generic active ingredient '${topCandidate.record.genericName}', but the package strength could not be verified. Multiple formulations exist."
            )
        }

        // Ambiguity Check 2: Score closeness
        if (runnerUp != null && (topCandidate.score - runnerUp.score) < MARGIN_OF_AMBIGUITY && runnerUp.score >= AMBIGUOUS_THRESHOLD_SCORE) {
            // Two candidates are very close in score
            return MatchOutcome.AmbiguousMatch(
                candidates = listOf(topCandidate, runnerUp),
                extractedStrength = extractedStrengths.firstOrNull(),
                extractedDosageForm = extractedDosageForm,
                extractedExpiry = extractedExpiry,
                extractedBatch = extractedBatch,
                explanation = "Multiple medicines match the package text with similar confidence. Re-scan closer to verify brand or strength."
            )
        }

        // Confident Match Check
        if (topCandidate.score >= MIN_CONFIDENT_SCORE && !topCandidate.strengthConflicting) {
            val confidence = when {
                topCandidate.score >= 80f && topCandidate.strengthMatches -> MatchConfidence.HIGH
                topCandidate.score >= 60f -> MatchConfidence.MEDIUM
                else -> MatchConfidence.LOW
            }

            return MatchOutcome.ConfidentMatch(
                record = topCandidate.record,
                candidate = topCandidate,
                confidence = confidence,
                extractedStrength = extractedStrengths.firstOrNull(),
                extractedDosageForm = extractedDosageForm,
                extractedExpiry = extractedExpiry,
                extractedBatch = extractedBatch,
                explanation = topCandidate.reason
            )
        }

        // If score is between AMBIGUOUS_THRESHOLD_SCORE and MIN_CONFIDENT_SCORE without a decisive lead
        return MatchOutcome.AmbiguousMatch(
            candidates = listOf(topCandidate),
            extractedStrength = extractedStrengths.firstOrNull(),
            extractedDosageForm = extractedDosageForm,
            extractedExpiry = extractedExpiry,
            extractedBatch = extractedBatch,
            explanation = "Partial medicine match detected, but evidence is insufficient to guarantee identity safely."
        )
    }

    /**
     * Evaluates a single catalogue record against OCR evidence.
     */
    fun evaluateCandidate(
        record: MedicineRecord,
        rawOcrText: String,
        extractedStrengths: List<String>,
        extractedDosageForm: String?
    ): MatchCandidate? {
        val normalisedText = TextNormaliser.normalise(rawOcrText)
        val ocrTokens = normalisedText.split(Regex("""[^a-z0-9]+""")).filter { it.isNotBlank() && it !in STOP_WORDS }

        var score = 0f
        val matchedFields = mutableSetOf<MatchedField>()
        val reasons = mutableListOf<String>()

        // 1. Brand Name Match
        val brandName = record.brandName
        var brandMatched = false
        if (!brandName.isNullOrBlank()) {
            val normalisedBrand = TextNormaliser.normalise(brandName)
            if (normalisedText.contains(normalisedBrand)) {
                score += 45f
                brandMatched = true
                matchedFields.add(MatchedField.BRAND_NAME)
                reasons.add("Brand '${record.brandName}' matched exactly")
            } else {
                // Check word token similarity
                val brandTokens = normalisedBrand.split(Regex("""[^a-z0-9]+""")).filter { it.isNotBlank() && it !in STOP_WORDS }
                val bestSim = findBestTokenSequenceSimilarity(brandTokens, ocrTokens)
                if (bestSim >= 0.82f) {
                    val points = 35f * bestSim
                    score += points
                    brandMatched = true
                    matchedFields.add(MatchedField.BRAND_NAME)
                    reasons.add("Brand '${record.brandName}' matched with token similarity ${(bestSim * 100).toInt()}%")
                }
            }
        }

        // 2. Generic Name Match
        val genericName = record.genericName
        var genericMatched = false
        if (!genericName.isNullOrBlank()) {
            val normalisedGeneric = TextNormaliser.normalise(genericName)
            // Remove salt suffixes like "hydrochloride", "potassium", "calcium", "sodium" for fuzzy name base
            val baseGeneric = normalisedGeneric.replace(Regex("""\b(hydrochloride|potassium|calcium|sodium|tartrate|sulfate|maleate|besylate)\b"""), "").trim()

            if (normalisedText.contains(normalisedGeneric)) {
                score += 40f
                genericMatched = true
                matchedFields.add(MatchedField.GENERIC_NAME)
                reasons.add("Generic '${record.genericName}' matched")
            } else if (baseGeneric.isNotBlank() && normalisedText.contains(baseGeneric)) {
                score += 35f
                genericMatched = true
                matchedFields.add(MatchedField.GENERIC_NAME)
                reasons.add("Base generic '$baseGeneric' matched")
            } else {
                val genericTokens = baseGeneric.split(Regex("""[^a-z0-9]+""")).filter { it.isNotBlank() && it !in STOP_WORDS }
                val bestSim = findBestTokenSequenceSimilarity(genericTokens, ocrTokens)
                if (bestSim >= 0.82f) {
                    val points = 35f * bestSim
                    score += points
                    genericMatched = true
                    matchedFields.add(MatchedField.GENERIC_NAME)
                    reasons.add("Generic '${record.genericName}' matched with similarity ${(bestSim * 100).toInt()}%")
                }
            }
        }

        // If neither brand nor generic matched with any reasonable similarity, skip record
        if (!brandMatched && !genericMatched) {
            return null
        }

        // 3. Strength Comparison
        var strengthMatches = false
        var strengthConflicting = false
        val recordStrength = record.strength

        if (!recordStrength.isNullOrBlank()) {
            val normRecordStrength = normalizeStrengthString(recordStrength)
            val matchedAnyStrength = extractedStrengths.any { normalizeStrengthString(it) == normRecordStrength }

            if (matchedAnyStrength) {
                strengthMatches = true
                score += 25f
                matchedFields.add(MatchedField.STRENGTH)
                reasons.add("Strength '$recordStrength' confirmed")
            } else if (extractedStrengths.isNotEmpty()) {
                // OCR detected a strength, but it does NOT match this record's strength!
                strengthConflicting = true
                score -= 40f
                reasons.add("Conflicting strength detected in OCR (${extractedStrengths.joinToString()})")
            }
        }

        // 4. Dosage Form Comparison
        if (extractedDosageForm != null && !record.dosageForm.isNullOrBlank()) {
            if (extractedDosageForm.equals(record.dosageForm, ignoreCase = true) ||
                record.dosageForm.contains(extractedDosageForm, ignoreCase = true)
            ) {
                score += 10f
                matchedFields.add(MatchedField.DOSAGE_FORM)
                reasons.add("Dosage form '$extractedDosageForm' confirmed")
            }
        }

        // 5. Manufacturer Corroboration
        val manufacturer = record.manufacturer
        if (!manufacturer.isNullOrBlank()) {
            val mfgTokens = TextNormaliser.normalise(manufacturer)
                .split(Regex("""[^a-z0-9]+"""))
                .filter { it.length > 3 && it !in STOP_WORDS && it !in setOf("ltd", "laboratories", "pharma", "pharmaceuticals", "india") }

            val mfgMatches = mfgTokens.any { mfgToken ->
                ocrTokens.any { ocrToken ->
                    ocrToken.contains(mfgToken) || mfgToken.contains(ocrToken) || calculateSimilarity(mfgToken, ocrToken) >= 0.80f
                }
            }

            if (mfgMatches) {
                score += 10f
                matchedFields.add(MatchedField.MANUFACTURER)
                reasons.add("Manufacturer matched")
            }
        }

        return MatchCandidate(
            record = record,
            score = max(0f, score),
            matchedFields = matchedFields,
            strengthMatches = strengthMatches,
            strengthConflicting = strengthConflicting,
            reason = reasons.joinToString("; ")
        )
    }

    /**
     * Extracts all strengths from raw text, preserving values like "500 mg", "650 mg", "20 mg", "21.8 g".
     */
    fun extractAllStrengths(text: String): List<String> {
        return STRENGTH_REGEX.findAll(text).map { match ->
            val num = match.groupValues[1]
            val unit = match.groupValues[2].lowercase()
            "$num $unit"
        }.distinct().toList()
    }

    /**
     * Extracts single prominent dosage form from text.
     * Safely handles glued OCR characters for forms with length >= 5 (e.g. "CapsuesP20.mg").
     */
    fun extractDosageForm(text: String): String? {
        val lower = text.lowercase()
        for ((pattern, canonical) in DOSAGE_FORM_PATTERNS) {
            val regex = if (pattern.length >= 5) {
                Regex("""\b${Regex.escape(pattern)}""", RegexOption.IGNORE_CASE)
            } else {
                Regex("""\b${Regex.escape(pattern)}\b""", RegexOption.IGNORE_CASE)
            }
            if (regex.containsMatchIn(lower)) {
                return canonical
            }
        }
        return null
    }

    /**
     * Normalizes a strength string for robust comparison ("500 mg" == "500mg" == "500 MG").
     */
    fun normalizeStrengthString(strength: String): String {
        return strength.trim().lowercase().replace(Regex("""\s+"""), "")
    }

    /**
     * Finds highest sequence similarity between target tokens and candidate tokens.
     */
    private fun findBestTokenSequenceSimilarity(targetTokens: List<String>, ocrTokens: List<String>): Float {
        if (targetTokens.isEmpty() || ocrTokens.isEmpty()) return 0f

        var maxSimilarity = 0f
        for (target in targetTokens) {
            for (ocr in ocrTokens) {
                val sim = calculateSimilarity(target, ocr)
                if (sim > maxSimilarity) {
                    maxSimilarity = sim
                }
            }
        }
        return maxSimilarity
    }

    /**
     * Normalized Levenshtein similarity metric (0.0 = completely different, 1.0 = identical).
     */
    fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 || len2 == 0) return 0.0f

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        val distance = dp[len1][len2]
        val maxLen = max(len1, len2)
        return (maxLen - distance).toFloat() / maxLen.toFloat()
    }
}
