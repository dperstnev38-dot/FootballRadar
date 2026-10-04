package com.footballradar.app.data.update

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("tag_name")
    val tagName: String?,
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
