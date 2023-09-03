package com.justsoft.nulpschedule.api

open class ScheduleParseException(override val message: String?): Exception(message)

class ElementNotFoundException(elementName: String): ScheduleParseException("Could not find \'$elementName\' in the DOM")