package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduleEntity

object ClassAlarmText {
    fun title(schedule: ScheduleEntity): String = schedule.courseName

    fun body(schedule: ScheduleEntity): String {
        val jam = "${schedule.startTime}–${schedule.endTime}"
        val tempat = when {
            !schedule.room.isNullOrBlank() -> schedule.room
            !schedule.onlineUrl.isNullOrBlank() -> "Daring"
            else -> null
        }
        return if (tempat == null) jam else "$tempat · $jam"
    }
}
