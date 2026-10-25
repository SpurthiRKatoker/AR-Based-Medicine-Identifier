package com.armod.identifier.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Logical entity representing a curated medicine record in the local dataset.
 * Conforms to AR_Medicine_Identifier_Backend_Schema.md.
 */
@Serializable
data class MedicineRecord(
    @SerialName("id")
    val id: String,

    @SerialName("brandName")
    val brandName: String? = null,

    @SerialName("genericName")
    val genericName: String? = null,

    @SerialName("strength")
    val strength: String? = null,

    @SerialName("dosageForm")
    val dosageForm: String? = null,

    @SerialName("manufacturer")
    val manufacturer: String? = null,

    @SerialName("labelPurpose")
    val labelPurpose: String? = null,

    @SerialName("labelDirections")
    val labelDirections: String? = null,

    @SerialName("warnings")
    val warnings: List<String> = emptyList(),

    @SerialName("commonSideEffects")
    val commonSideEffects: List<String> = emptyList(),

    @SerialName("sourceTitle")
    val sourceTitle: String,

    @SerialName("sourceUrl")
    val sourceUrl: String? = null,

    @SerialName("sourceAccessDate")
    val sourceAccessDate: String? = null,

    @SerialName("datasetVersion")
    val datasetVersion: String,

    @SerialName("notes")
    val notes: String? = null
)

