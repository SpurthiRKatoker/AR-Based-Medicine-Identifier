package com.armod.identifier.data.local

import android.content.Context
import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.util.TextNormaliser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Concrete offline implementation of MedicineLocalDataSource that reads and caches
 * the bundled JSON dataset.
 */
class JsonMedicineLocalDataSource(
    private val jsonProvider: suspend () -> String
) : MedicineLocalDataSource {

    /**
     * Primary Android constructor using AssetManager.
     */
    constructor(context: Context, assetFileName: String = DEFAULT_ASSET_FILE) : this(
        jsonProvider = {
            withContext(Dispatchers.IO) {
                try {
                    context.assets.open(assetFileName).bufferedReader().use { it.readText() }
                } catch (e: IOException) {
                    throw MedicineDataException.AssetNotFound("Could not open asset: $assetFileName", e)
                }
            }
        }
    )

    private var cachedRecords: List<MedicineRecord>? = null
    private var idMap: Map<String, MedicineRecord> = emptyMap()

    private suspend fun ensureLoaded(): List<MedicineRecord> {
        val current = cachedRecords
        if (current != null) return current

        return withContext(Dispatchers.Default) {
            val jsonText = jsonProvider()
            val parsed = JsonMedicineParser.parseAndValidate(jsonText)
            cachedRecords = parsed
            idMap = parsed.associateBy { it.id }
            parsed
        }
    }

    override suspend fun getAllMedicines(): List<MedicineRecord> {
        return ensureLoaded()
    }

    override suspend fun getMedicineById(id: String): MedicineRecord? {
        ensureLoaded()
        return idMap[id]
    }

    override suspend fun searchByName(query: String): List<MedicineRecord> {
        val records = ensureLoaded()
        val normalisedQuery = TextNormaliser.normalise(query)
        if (normalisedQuery.isBlank()) return emptyList()

        return records.filter { record ->
            val brandMatch = record.brandName?.let { TextNormaliser.normalise(it).contains(normalisedQuery) } ?: false
            val genericMatch = record.genericName?.let { TextNormaliser.normalise(it).contains(normalisedQuery) } ?: false
            brandMatch || genericMatch
        }
    }

    override suspend fun findByExactNameAndStrength(name: String, strength: String): MedicineRecord? {
        val records = ensureLoaded()
        val targetName = TextNormaliser.normalise(name)
        val targetStrength = TextNormaliser.normalise(strength)

        return records.firstOrNull { record ->
            val brandMatch = record.brandName?.let { TextNormaliser.normalise(it) == targetName } ?: false
            val genericMatch = record.genericName?.let { TextNormaliser.normalise(it) == targetName } ?: false
            val strengthMatch = record.strength?.let { TextNormaliser.normalise(it) == targetStrength } ?: false

            (brandMatch || genericMatch) && strengthMatch
        }
    }

    override suspend fun getRecordCount(): Int {
        return ensureLoaded().size
    }

    companion object {
        const val DEFAULT_ASSET_FILE = "medicines.json"
    }
}
