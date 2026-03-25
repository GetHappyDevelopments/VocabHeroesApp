package com.vocabheroes.kioskbrowser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {

    private val policy = UrlPolicy(setOf("example.org", "vocabheroes.app"))

    @Test
    fun allowsHttpsOnAllowedHost() {
        assertTrue(policy.isAllowed("https://example.org"))
        assertTrue(policy.isAllowed("https://www.example.org/path"))
        assertTrue(policy.isAllowed("https://sub.vocabheroes.app/learn"))
    }

    @Test
    fun blocksUnknownHostAndHttp() {
        assertFalse(policy.isAllowed("https://evil.example.com"))
        assertFalse(policy.isAllowed("http://example.org"))
        assertFalse(policy.isAllowed("javascript:alert('x')"))
    }

    @Test
    fun blocksEmptyAndMalformedUrls() {
        assertFalse(policy.isAllowed(""))
        assertFalse(policy.isAllowed("not_a_url"))
        assertFalse(policy.isAllowed(null))
    }
}

