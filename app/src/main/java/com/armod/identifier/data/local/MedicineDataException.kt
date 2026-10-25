package com.armod.identifier.data.local

/**
 * Exceptions thrown during medicine dataset parsing, schema validation, and retrieval.
 */
sealed class MedicineDataException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MalformedJson(message: String, cause: Throwable? = null) : MedicineDataException(message, cause)
    class MissingRequiredField(val recordId: String?, val fieldName: String) : 
        MedicineDataException("Medicine record '${recordId ?: "unknown"}' is missing required field '$fieldName'")
    class DuplicateId(val duplicateId: String) : 
        MedicineDataException("Duplicate medicine ID found in dataset: '$duplicateId'")
    class EmptyDataset(message: String = "Medicine dataset is empty") : 
        MedicineDataException(message)
    class AssetNotFound(message: String, cause: Throwable? = null) : 
        MedicineDataException(message, cause)
}
