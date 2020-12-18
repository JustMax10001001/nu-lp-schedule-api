package com.justsoft.nulpschedule.api.model

import java.time.LocalDateTime

data class ApiSchedule(
    val instituteName: String,
    val groupName: String,
    val updateTime: LocalDateTime,
    val wasNumeratorOnUpdate: Boolean,
    val addTime: LocalDateTime,
    val scheduleType: ScheduleType,
    val id: Long = generateId(instituteName, groupName, scheduleType),
    val subgroup: Int = 1,
    val position: Int? = Int.MAX_VALUE
) {

    companion object {
        private fun generateId(instituteName: String, groupName: String, scheduleType: ScheduleType): Long {
            val instituteHashCode = instituteName.hashCode().toLong()
            val groupNameHashCode = groupName.hashCode().toLong()
            val scheduleTypeCode = scheduleType.ordinal
            return (instituteHashCode shl 31) xor (groupNameHashCode shl (15 - scheduleTypeCode))
        }
    }
}