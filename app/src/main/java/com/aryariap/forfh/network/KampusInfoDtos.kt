package com.aryariap.forfh.network

import com.aryariap.forfh.data.db.KampusInfoEntity
import com.aryariap.forfh.data.db.KampusInfoSnapshot
import com.aryariap.forfh.data.db.PresensiRecapEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Envelope GET /api/campus/info (ForFH web) — semua jenis info kampus + rekap presensi
 * dikirim sekaligus; UI menyaring per jenis.
 *
 *   { "connected": bool, "lastSyncAt": ISO|null,
 *     "items": [ { "jenis": string, "data": [...], "updatedAt": ISO }, ... ] }
 *
 * Satu-satunya shape TERNORMALISASI adalah jenis "presensi" (presensiToRecap web →
 * [ {code, name, tm, hadir, persen} ]). Jenis lain (status_mhs, peserta_mk, dst.) adalah
 * baris MENTAH UPPERCASE_SNAKE dari libapp.so — dipertahankan verbatim (pola web:
 * campusData.dataJson + rendering label-value generik), jadi tanpa @SerialName per field.
 */
@Serializable
data class KampusInfoEnvelopeDto(
    val connected: Boolean = false,
    val lastSyncAt: String? = null,
    val items: List<KampusInfoItemDto> = emptyList(),
)

@Serializable
data class KampusInfoItemDto(
    val jenis: String,
    val data: JsonElement = JsonNull, // presensi: [PresensiRecapDto]; lainnya: baris mentah
    val updatedAt: String? = null,
)

/** Rekap presensi per MK — server mengirim TERNORMALISASI (camelCase dari web). */
@Serializable
data class PresensiRecapDto(
    val code: String = "",
    val name: String = "",
    val tm: Int? = null,
    val hadir: Int? = null,
    val persen: Int? = null,
)

const val JENIS_PRESENSI = "presensi"

fun KampusInfoEnvelopeDto.toSnapshot(): KampusInfoSnapshot {
    val presensi = ArrayList<PresensiRecapEntity>()
    val info = ArrayList<KampusInfoEntity>()
    for (item in items) {
        if (item.jenis == JENIS_PRESENSI) {
            // Toleran seperti web: data_json rusak dilewati (bukan crash/500).
            // kotlinx-serialization 1.11: reified decodeFromJsonElement(element) dan
            // listSerializer() sudah dihapus — pakai bentuk dua-arg dgn serializer
            // eksplisit (cek API jar 1.11.0).
            val rows = runCatching {
                ApiClient.forfhJson.decodeFromJsonElement(ListSerializer(PresensiRecapDto.serializer()), item.data)
            }.getOrDefault(emptyList())
            for (r in rows) presensi += r.toEntity()
        } else {
            info += KampusInfoEntity(
                jenis = item.jenis,
                dataJson = rawRowsJson(item.data),
                updatedAt = item.updatedAt.orEmpty(),
            )
        }
    }
    return KampusInfoSnapshot(
        connected = connected,
        lastSyncAt = lastSyncAt,
        presensi = presensi,
        info = info,
    )
}

private fun PresensiRecapDto.toEntity(): PresensiRecapEntity = PresensiRecapEntity(
    kode = code,
    nama = name,
    tm = tm,
    hadir = hadir,
    persen = persen,
)

/** Baris mentah disimpan verbatim; "[]" saat null/kosong (pola web utk rows kosong). */
private fun rawRowsJson(data: JsonElement): String = when (data) {
    is JsonArray, is JsonObject -> data.toString()
    else -> "[]"
}
