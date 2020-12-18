package com.justsoft.nulpschedule.api

import java.net.URL
import java.net.URLEncoder

internal class UrlBuilderContext {
    private val pathSegments = mutableListOf<String>()
    private val arguments = mutableMapOf<String, String>()

    infix fun path(segment: String) {
        pathSegments.add(segment)
    }

    infix fun argument(keyValue: Pair<String, String>) {
        arguments[keyValue.first] = keyValue.second
    }

    fun build(baseUrl: String): String {
        val encode = { value: String -> URLEncoder.encode(value, "UTF-8") }

        val builder = StringBuilder(baseUrl)
        for (segment in pathSegments) {
            when {
                builder.endsWith('/') && segment.startsWith('/') -> builder.append(segment.substring(1))
                builder.endsWith('/') xor segment.startsWith('/') -> builder.append(segment)
                !builder.endsWith('/') && !segment.startsWith('/') -> builder.append("/$segment")
            }
        }

        if (arguments.isNotEmpty())
            builder.append('?')

        for ((key, value) in arguments) {
            if (!builder.endsWith('?') && !builder.endsWith('&'))
                builder.append('&')
            builder.append("${encode(key)}=${encode(value)}")
        }
        return builder.toString()
    }
}

internal inline fun buildUrl(baseUrl: String, init: UrlBuilderContext.() -> Unit): URL {
    with(UrlBuilderContext().also(init)) {
        return URL(build(baseUrl))
    }
}