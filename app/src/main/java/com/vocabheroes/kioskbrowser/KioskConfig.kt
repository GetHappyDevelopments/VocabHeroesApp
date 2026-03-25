package com.vocabheroes.kioskbrowser

object KioskConfig {
    val startUrl: String = BuildConfig.START_URL

    val allowedHosts: Set<String> = BuildConfig.ALLOWED_HOSTS
        .split(',')
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()
}

