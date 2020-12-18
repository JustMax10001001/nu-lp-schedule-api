package com.justsoft.nulpschedule.api.model

data class ApiSubject(
    val id: Long,
    val scheduleId: Long,
    val subjectName: String
) {

    companion object {
        fun generateId(schedule: ApiSchedule, subjectName: String): Long {
            val subjectHashCode = subjectName.hashCode().toLong()
            return (schedule.id shl 8) xor subjectHashCode
        }
    }
}