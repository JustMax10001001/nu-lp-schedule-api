package com.justsoft.nulpschedule.api.model

import java.time.DayOfWeek

data class ApiScheduleClass(
    val subjectId: Long,
    val scheduleId: Long,
    val teacherName: String,
    val classDescription: String,
    val dayOfWeek: DayOfWeek,
    val index: Int,
    val flags: Int,
    val url: String?,
    val id: Long = generateId(subjectId, dayOfWeek, index, flags)
) {
    companion object {
        private fun generateId(
            subjectId: Long,
            dayOfWeek: DayOfWeek,
            classIndex: Int,
            flags: Int
        ): Long {
            return (subjectId) xor (dayOfWeek.ordinal.toLong() shl 7) xor (classIndex.toLong() shl 4) xor flags.toLong()
        }

        const val FLAG_SUBGROUP_1 = 1 shl 0
        const val FLAG_SUBGROUP_2 = 1 shl 1

        const val FLAG_NUMERATOR = 1 shl 2
        const val FLAG_DENOMINATOR = 1 shl 3
    }
}