package com.armod.identifier.domain

import com.armod.identifier.data.model.MedicineRecord

/**
 * High-level identification states representing the confidence gate outcomes.
 */
sealed interface IdentificationState {
    data object Idle : IdentificationState
    data object Scanning : IdentificationState
    data class ConfidentMatch(val medicine: MedicineRecord, val parsedStrength: String?, val parsedExpiry: String?) : IdentificationState
    data class NeedsConfirmation(val candidates: List<MedicineRecord>, val reason: String) : IdentificationState
    data class Unmatched(val reason: String) : IdentificationState
    data class Unreadable(val reason: String) : IdentificationState
}
