package com.footballradar.app.data.update

import com.footballradar.app.BuildConfig

object GitHubReleaseConfig {
    val repository: String = BuildConfig.GITHUB_RELEASES_REPOSITORY.trim()

    val isConfigured: Boolean
        get() = REPOSITORY_PATTERN.matches(repository)

    private val REPOSITORY_PATTERN = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
}
