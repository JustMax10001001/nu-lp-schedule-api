package com.justsoft.nulpschedule.api

import com.justsoft.nulpschedule.api.model.*
import com.justsoft.nulpschedule.api.model.ApiScheduleClass.Companion.FLAG_DENOMINATOR
import com.justsoft.nulpschedule.api.model.ApiScheduleClass.Companion.FLAG_NUMERATOR
import com.justsoft.nulpschedule.api.model.ApiScheduleClass.Companion.FLAG_SUBGROUP_1
import com.justsoft.nulpschedule.api.model.ApiScheduleClass.Companion.FLAG_SUBGROUP_2
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import javax.net.ssl.SSLSocketFactory

internal typealias Schedule = ApiSchedule
internal typealias Subject = ApiSubject
internal typealias ScheduleClass = ApiScheduleClass

@Suppress("unused")
class ScheduleApi constructor(
    private val sslSocketFactory: SSLSocketFactory,
) {

    fun getInstitutes(scheduleType: ScheduleType = ScheduleType.STUDENT): Result<List<String>> {
        return runCatching {
            if (scheduleType == ScheduleType.STUDENT || scheduleType == ScheduleType.STUDENT_PART_TIME) {
                val url = buildUrl(BASE_ENDPOINT) {
                    path(ENDPOINT_MAP.getOrDefault(scheduleType, ""))
                }
                val doc = getDocument(url.toString())
                getInstitutesFromDocument(doc)
            } else
                throw IllegalArgumentException("There are no institutes for scheduleType $scheduleType")
        }
    }


    fun getGroups(institute: String = "", scheduleType: ScheduleType = ScheduleType.STUDENT): Result<List<String>> =
        runCatching {
            val url = buildUrl(BASE_ENDPOINT) {
                path(ENDPOINT_MAP.getOrDefault(scheduleType, ""))

                if (institute.isNotEmpty())
                    argument(ARGUMENT_INSTITUTE to institute)
            }
            val doc = getDocument(url.toString())
            getGroupsFromDocument(doc)
        }

    fun getSchedule(
        institute: String = "",
        group: String,
        scheduleType: ScheduleType = ScheduleType.STUDENT,
        semesterDuration: SemesterDuration = SemesterDuration.WHOLE_SEMESTER,
    ): Result<ScheduleRequestResult> {
        if (institute == "All" || group == "All")
            throw IllegalArgumentException("institute or group name is \'All\'")
        return runCatching {
            val url = buildUrl(BASE_ENDPOINT) {
                path(ENDPOINT_MAP.getOrDefault(scheduleType, ""))

                if (scheduleType == ScheduleType.STUDENT || scheduleType == ScheduleType.STUDENT_PART_TIME)
                    argument(ARGUMENT_INSTITUTE to institute)
                argument(ARGUMENT_GROUP to group)
                argument(ARGUMENT_SEMESTER_DURATION to semesterDuration.argument.toString())
            }
            val doc = getDocument(url.toString())
            getScheduleFromDocument(doc, institute, group, scheduleType)
        }
    }

    private fun getDocument(url: String): Document {
        return Jsoup.connect(url)
            .sslSocketFactory(sslSocketFactory)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36")
            .header("Accept", "*/*")
            .get()
    }

    private fun getScheduleFromDocument(
        document: Document,
        institute: String,
        group: String,
        scheduleType: ScheduleType
    ): ScheduleRequestResult {
        val schedule = Schedule(
            institute.trim(),
            group.trim(),
            LocalDateTime.now(),
            isNumerator(document),
            LocalDateTime.now(),
            scheduleType
        )

        val blockSystemMain = document.getElementById("block-system-main")
            ?: throw ElementNotFoundException("block-system-main")

        val viewStudentsSchedule = blockSystemMain.getElementsByClass("view-students-schedule").first()
            ?: throw ElementNotFoundException("view-students-schedule")

        val scheduleViewContent = viewStudentsSchedule.getElementsByClass("view-content").first()
            ?: throw ElementNotFoundException("view-content")

        val subjectsMap = mutableMapOf<Long, Subject>()
        val classesList = mutableListOf<ScheduleClass>()
        scheduleViewContent
            .children()
            .forEach { parseViewGrouping(it, schedule, subjectsMap, classesList) }
        return ScheduleRequestResult(schedule, subjectsMap.values.toList(), classesList)
    }

    private fun parseViewGrouping(
        element: Element,
        schedule: Schedule,
        subjects: MutableMap<Long, Subject>,
        classes: MutableList<ScheduleClass>
    ) {
        val header = element.getElementsByClass("view-grouping-header")[0]
            .text()
        val dayOfWeek = getDayOfWeekForHeader(header)

        val viewGroupingContent = element.getElementsByClass("view-grouping-content")[0]
        val vcgChildren = viewGroupingContent.children()
        for (i in 0 until vcgChildren.size / 2) {
            val classIndex = vcgChildren[2 * i].text().toInt()
            for (subjectElement in vcgChildren[2 * i + 1].children()) {
                val groupContent = subjectElement.child(0)
                val groupContentTextNodes = groupContent.textNodes()

                val flags = getFlagsForId(subjectElement.id())

                val getNodeText: (Int) -> String =
                    { groupContentTextNodes.getOrNull(it)?.text()?.trimTrailingComma() ?: "" }
                val subjectName = getNodeText(0)
                val teacher = getNodeText(1)
                val classDescription = getNodeText(2)

                val subjectId = ApiSubject.generateId(schedule, subjectName)
                val subject =
                    subjects[subjectId] ?: createSubject(schedule, subjectName).also {
                        subjects[subjectId] = it
                    }

                val a = groupContent.getElementsByTag("a")
                val url: String? = a.firstOrNull()?.absUrl("href")

                classes.add(
                    ScheduleClass(
                        subject.id,
                        schedule.id,
                        teacher,
                        classDescription,
                        dayOfWeek,
                        classIndex,
                        flags,
                        url
                    )
                )
            }
        }
    }

    private fun getFlagsForId(id: String?): Int {
        return when (id) {
            "group_full" -> FLAG_DENOMINATOR or FLAG_NUMERATOR or FLAG_SUBGROUP_1 or FLAG_SUBGROUP_2
            "group_chys" -> FLAG_NUMERATOR or FLAG_SUBGROUP_1 or FLAG_SUBGROUP_2
            "group_znam" -> FLAG_DENOMINATOR or FLAG_SUBGROUP_1 or FLAG_SUBGROUP_2
            "sub_1_full" -> FLAG_DENOMINATOR or FLAG_NUMERATOR or FLAG_SUBGROUP_1
            "sub_2_full" -> FLAG_DENOMINATOR or FLAG_NUMERATOR or FLAG_SUBGROUP_2
            "sub_1_chys" -> FLAG_NUMERATOR or FLAG_SUBGROUP_1
            "sub_1_znam" -> FLAG_DENOMINATOR or FLAG_SUBGROUP_1
            "sub_2_chys" -> FLAG_NUMERATOR or FLAG_SUBGROUP_2
            "sub_2_znam" -> FLAG_DENOMINATOR or FLAG_SUBGROUP_2
            else -> throw IllegalArgumentException("Could not match class type $id")
        }
    }

    private fun createSubject(
        schedule: ApiSchedule,
        subjectName: String
    ): Subject {
        val subjectId = ApiSubject.generateId(schedule, subjectName)
        return Subject(subjectId, schedule.id, subjectName)
    }

    private fun isNumerator(document: Document): Boolean {
        val groupChys = document.getElementById("group_chys")
        val sub1Chys = document.getElementById("sub_1_chys")
        val sub2Chys = document.getElementById("sub_2_chys")
        return groupChys?.hasClass("week_color")
            ?: (sub1Chys?.hasClass("week_color")
                ?: sub2Chys?.hasClass("week_color")) ?: false
    }

    private fun <T> List<T>.get(i: Int, default: T): T =
        if (this.size > i) this[i] else default

    private fun String.trimTrailingComma(): String {
        val trimmed = this.trim().replace(", ,", ",")
        return if (trimmed.endsWith(","))
            trimmed.substring(0, trimmed.length - 1)
        else
            trimmed
    }

    private fun getDayOfWeekForHeader(header: String): DayOfWeek {
        return when (header.lowercase(Locale.ROOT).trim()) {
            "пн" -> MONDAY
            "вт" -> TUESDAY
            "ср" -> WEDNESDAY
            "чт" -> THURSDAY
            "пт" -> FRIDAY
            "сб" -> SATURDAY
            "нд" -> SUNDAY
            else -> throw IllegalArgumentException("Illegal header $header")
        }
    }

    private fun getInstitutesFromDocument(document: Document): List<String> {
        val selector = document.getElementById("edit-departmentparent-abbrname-selective")
            ?: throw ElementNotFoundException("edit-departmentparent-abbrname-selective")

        return selector.children()
            .stream()
            .map { it.attr("value") }
            .collect(Collectors.toList())
    }

    private fun getGroupsFromDocument(document: Document): List<String> {
        val selector = document.getElementById("edit-studygroup-abbrname-selective")
            ?: throw ElementNotFoundException("edit-studygroup-abbrname-selective")

        return selector.children()
            .stream()
            .map { it.attr("value") }
            .collect(Collectors.toList())
    }

    companion object {
        private const val BASE_ENDPOINT = "https://student.lpnu.ua/"

        private const val ARGUMENT_INSTITUTE = "departmentparent_abbrname_selective"
        private const val ARGUMENT_GROUP = "studygroup_abbrname_selective"
        private const val ARGUMENT_SEMESTER_DURATION = "semestrduration"

        private val ENDPOINT_MAP = mapOf(
            ScheduleType.STUDENT to "students_schedule",
            ScheduleType.POSTGRADUATE to "postgraduate_schedule",
            ScheduleType.SELECTIVE to "schedule_selective",
            ScheduleType.STUDENT_PART_TIME to "parttime_schedule",
            ScheduleType.POSTGRADUATE_PART_TIME to "postgraduate_parttime"
        )
    }

    data class ScheduleRequestResult constructor(
        val schedule: ApiSchedule,
        val subjects: List<ApiSubject>,
        val classes: List<ApiScheduleClass>
    )
}
