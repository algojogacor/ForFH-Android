package com.aryariap.forfh.alarm

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

data class ClassExtras(
    val scheduleId: String,
    val offsetMinutes: Int,
    val occurrenceDate: String,
    val triggerAtMillis: Long,
)

/**
 * Parse ketat extras alarm dari Intent. T6 menulis semua extras sebagai String
 * (Bundle.getIntExtra/getLongExtra pada nilai String melempar internal CCE → defaultValue -1,
 * yang membuat guard handler mati diam-diam). Murni, tanpa Android, bisa unit-test.
 */
object AlarmFlowExtras {

    /** Satu-satunya sumber kebenaran slot tugas (AlarmFlowHandler alias ke ini). */
    val TASK_SLOTS = listOf(9, 15, 20)

    /** null-safe + parse ketat; offset/trigger negatif → null. */
    fun parseClassExtras(
        scheduleId: String?,
        offsetStr: String?,
        occurrenceDate: String?,
        triggerStr: String?,
    ): ClassExtras? {
        val id = scheduleId ?: return null
        val offset = offsetStr?.toIntOrNull() ?: return null
        val date = occurrenceDate ?: return null
        val trigger = triggerStr?.toLongOrNull() ?: return null
        if (offset < 0 || trigger < 0) return null
        return ClassExtras(id, offset, date, trigger)
    }

    /** slotHour deterministik: trigger slot S tanggal D = D pukul S:00 WIB. Parse date gagal → null, bukan throw. */
    fun resolveTaskSlot(occurrenceDate: String, trigger: Long, zone: ZoneId): Int? {
        val date = try {
            LocalDate.parse(occurrenceDate)
        } catch (e: DateTimeParseException) {
            return null
        }
        return TASK_SLOTS.firstOrNull { slot ->
            date.atTime(slot, 0).atZone(zone).toInstant().toEpochMilli() == trigger
        }
    }
}
