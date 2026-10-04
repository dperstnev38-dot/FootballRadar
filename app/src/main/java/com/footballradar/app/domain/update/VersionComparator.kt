package com.footballradar.app.domain.update

import java.math.BigInteger

object VersionComparator {
    private val VERSION_PATTERN =
        Regex("^v?(\\d+(?:\\.\\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$")

    fun compare(first: String, second: String): Int {
        val firstVersion = parse(first)
        val secondVersion = parse(second)
        val segmentCount = maxOf(firstVersion.numbers.size, secondVersion.numbers.size)
        for (index in 0 until segmentCount) {
            val firstNumber = firstVersion.numbers.getOrElse(index) { BigInteger.ZERO }
            val secondNumber = secondVersion.numbers.getOrElse(index) { BigInteger.ZERO }
            val numberComparison = firstNumber.compareTo(secondNumber)
            if (numberComparison != 0) return numberComparison
        }

        val firstPrerelease = firstVersion.prerelease
        val secondPrerelease = secondVersion.prerelease
        if (firstPrerelease == null) return if (secondPrerelease == null) 0 else 1
        if (secondPrerelease == null) return -1
        return comparePrerelease(firstPrerelease, secondPrerelease)
    }

    private fun parse(version: String): ParsedVersion {
        val match = VERSION_PATTERN.matchEntire(version.trim())
            ?: throw IllegalArgumentException("Invalid version: $version")
        val numbers = match.groupValues[1].split('.').map { segment ->
            if (segment.length > 1 && segment.startsWith('0')) {
                throw IllegalArgumentException("Version segments cannot contain leading zeroes: $version")
            }
            segment.toBigInteger()
        }
        val prerelease = match.groupValues[2].takeIf(String::isNotEmpty)?.split('.')
        if (prerelease != null && prerelease.any { it.isEmpty() }) {
            throw IllegalArgumentException("Invalid prerelease version: $version")
        }
        return ParsedVersion(numbers, prerelease)
    }

    private fun comparePrerelease(first: List<String>, second: List<String>): Int {
        for (index in 0 until minOf(first.size, second.size)) {
            val left = first[index]
            val right = second[index]
            val leftNumber = left.toBigIntegerOrNull()
            val rightNumber = right.toBigIntegerOrNull()
            val comparison = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                leftNumber != null -> -1
                rightNumber != null -> 1
                else -> left.compareTo(right)
            }
            if (comparison != 0) return comparison
        }
        return first.size.compareTo(second.size)
    }

    private data class ParsedVersion(
        val numbers: List<BigInteger>,
        val prerelease: List<String>?,
    )
}
