package com.footballradar.app.domain.update

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun comparesNumericSegmentsNumericallyAndIgnoresLeadingVAndTrailingZeroSegments() {
        assertEquals(1, VersionComparator.compare("v1.10.0", "1.9.9"))
        assertEquals(0, VersionComparator.compare("v2.4", "2.4.0"))
        assertEquals(-1, VersionComparator.compare("1.0.0", "1.0.1"))
    }

    @Test
    fun stableReleaseIsNewerThanPrereleaseAndPrereleaseIdentifiersAreOrdered() {
        assertEquals(1, VersionComparator.compare("1.2.0", "1.2.0-rc.1"))
        assertEquals(-1, VersionComparator.compare("1.2.0-beta.2", "1.2.0-beta.11"))
        assertEquals(-1, VersionComparator.compare("1.2.0-alpha", "1.2.0-beta"))
    }

    @Test
    fun ignoresBuildMetadata() {
        assertEquals(0, VersionComparator.compare("1.2.3+build.7", "v1.2.3+release"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidVersion() {
        VersionComparator.compare("1.x.0", "1.0.0")
    }
}
