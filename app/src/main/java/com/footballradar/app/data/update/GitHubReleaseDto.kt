package com.footballradar.app.data.update

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("tag_name")
    val tagName: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("body")
    val body: String?,
    @SerializedName("published_at")
    val publishedAt: String?,
    @SerializedName("html_url")
    val pageUrl: String?,
    @SerializedName("assets")
    val assets: List<GitHubReleaseAssetDto>?,
)

data class GitHubReleaseAssetDto(
    @SerializedName("name")
    val name: String?,
    @SerializedName("browser_download_url")
    val downloadUrl: String?,
)

fun GitHubReleaseDto.toReleaseInfo(): ReleaseInfo {
    val version = tagName?.takeIf(String::isNotBlank)
        ?: throw IllegalArgumentException("GitHub Release is missing its tag")
    return ReleaseInfo(
        version = version,
        title = name?.takeIf(String::isNotBlank) ?: version,
        notes = body?.trim()?.takeIf(String::isNotEmpty),
        publishedAt = publishedAt?.takeIf(String::isNotBlank),
    )
}
