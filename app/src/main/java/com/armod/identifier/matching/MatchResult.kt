package com.armod.identifier.matching

import com.armod.identifier.data.model.MedicineRecord

/**
 * Local candidate matching result and score.
 */
data class MatchResult(
    val candidate: MedicineRecord,
    val score: Float,
    val isStrengthVerified: Boolean,
    val matchReason: String? = null
)
