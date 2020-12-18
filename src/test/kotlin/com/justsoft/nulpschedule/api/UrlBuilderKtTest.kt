package com.justsoft.nulpschedule.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UrlBuilderKtTest {

    @Test
    fun testBase(){
        val baseUrl = "https://google.com/"
        assertEquals(baseUrl, buildUrl(baseUrl) { }.toExternalForm())
    }

    @Test
    fun testPath() {
        val baseUrl = "https://google.com/"
        val url = buildUrl(baseUrl) {
            path("search")
            path("custom")
        }
        assertEquals("${baseUrl}search/custom", url.toExternalForm())
    }

    @Test
    fun testArgs() {
        val baseUrl = "https://google.com/"
        val url = buildUrl(baseUrl) {
            path("search")
            argument("req" to "image")
            argument("req_text" to "тест")
        }
        assertEquals("${baseUrl}search?req=image&req_text=%D1%82%D0%B5%D1%81%D1%82", url.toExternalForm())
    }
}