package com.footballradar.app.data.update

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubReleaseDtoTest {
    @Test
    fun `maps GitHub release name body and publication date`() {
        val dto = Gson().fromJson(
            """
                {
                  "tag_name": "v1.2.0",
                  "name": "Football Radar 1.2.0",
                  "body": "  - Faster live updates\n- Improved match cards  ",
                  "published_at": "2026-10-03T10:15:30Z",
                  "html_url": "https://github.com/example/app/releases/tag/v1.2.0",
                  "assets": []
                }
            """.trimIndent(),
            GitHubReleaseDto::class.java,
        )

        val releaseInfo = dto.toReleaseInfo()

        assertEquals("v1.2.0", releaseInfo.version)
        assertEquals("Football Radar 1.2.0", releaseInfo.title)
        assertEquals("- Faster live updates\n- Improved match cards", releaseInfo.notes)
        assertEquals("2026-10-03T10:15:30Z", releaseInfo.publishedAt)
    }

    @Test
    fun `uses tag when release has no title and leaves missing notes and date empty`() {
        val dto = Gson().fromJson(
            """{"tag_name":"1.0"}""",
            GitHubReleaseDto::class.java,
        )

        val releaseInfo = dto.toReleaseInfo()

        assertEquals("1.0", releaseInfo.title)
        assertNull(releaseInfo.notes)
        assertNull(releaseInfo.publishedAt)
    }
}
