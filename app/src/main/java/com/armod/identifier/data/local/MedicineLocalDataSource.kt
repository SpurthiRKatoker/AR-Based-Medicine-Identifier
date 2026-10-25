package com.armod.identifier.data.local

import com.armod.identifier.data.model.MedicineRecord

/**
 * Interface contract for reading and querying local medicine dataset.
 */
interface MedicineLocalDataSource {
    suspend fun getAllMedicines(): List<MedicineRecord>
    suspend fun getMedicineById(id: String): MedicineRecord?
    suspend fun searchByName(query: String): List<MedicineRecord>
    suspend fun findByExactNameAndStrength(name: String, strength: String): MedicineRecord?
    suspend fun getRecordCount(): Int
}

