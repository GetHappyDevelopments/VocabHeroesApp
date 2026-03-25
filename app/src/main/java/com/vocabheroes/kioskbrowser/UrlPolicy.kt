package com.vocabheroes.kioskbrowser

import java.net.URI

class UrlPolicy(private val allowedHosts: Set<String>) {

    fun isAllowed(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.isAbsolute) return false

        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "https") return false

        val host = uri.host?.lowercase() ?: return false
        if (allowedHosts.isEmpty()) return false

        return allowedHosts.any { allowed ->
            host == allowed || host.endsWith(".$allowed")
        }
    }
}

