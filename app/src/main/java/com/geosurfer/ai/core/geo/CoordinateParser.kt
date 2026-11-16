package com.geosurfer.ai.core.geo

import kotlin.math.abs
import kotlin.math.floor

data class LatLon(val lat: Double, val lon: Double)

/**
 * Parses WGS84 coordinates typed by the user.
 * Accepts decimal degrees, degrees-minutes-seconds and degrees-decimal minutes,
 * with optional N/S/E/W letters. Returns null when the text is not understood.
 */
object CoordinateParser {

    private val TOKEN = Regex("""[NSEW]|-?\d+(?:\.\d+)?|[|]""")
    private val IGNORED =
        Regex("[\\s\\u00B0\\u00BA'\\u2032\\u2019\\u2018\"\\u2033\\u201D\\u201C]")

    private class Part(val hemi: Char?, val value: Double)

    fun parse(raw: String): LatLon? {
        val s = normalize(raw)
        val tokens = TOKEN.findAll(s).map { it.value }.toList()
        // Anything that is not a number, a letter N/S/E/W or a known symbol is an error.
        if (IGNORED.replace(TOKEN.replace(s, ""), "").isNotEmpty()) return null

        val groups = split(tokens) ?: return null
        val a = decode(groups[0]) ?: return null
        val b = decode(groups[1]) ?: return null

        val ha = a.hemi
        val hb = b.hemi
        val (lat, lon) = when {
            ha == null && hb == null -> Pair(a.value, b.value)
            ha != null && hb != null -> {
                val aLat = ha == 'N' || ha == 'S'
                val bLat = hb == 'N' || hb == 'S'
                when {
                    aLat && !bLat -> Pair(a.value, b.value)
                    !aLat && bLat -> Pair(b.value, a.value)
                    else -> return null
                }
            }
            else -> return null
        }

        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null
        return LatLon(lat, lon)
    }

    private fun normalize(raw: String): String {
        val sb = StringBuilder()
        for (c in raw) {
            when (c) {
                in '\u0660'..'\u0669' -> sb.append('0' + (c - '\u0660'))
                in '\u06F0'..'\u06F9' -> sb.append('0' + (c - '\u06F0'))
                '\u066B' -> sb.append('.')
                '\u060C', ';', ',' -> sb.append('|')
                '\u2212', '\u2013' -> sb.append('-')
                else -> sb.append(c)
            }
        }
        return sb.toString().uppercase()
    }

    private fun isHemi(t: String) = t.length == 1 && t[0] in "NSEW"

    private fun split(tokens: List<String>): List<List<String>>? {
        if (tokens.isEmpty()) return null

        if ("|" in tokens) {
            val parts = mutableListOf(mutableListOf<String>())
            for (t in tokens) {
                if (t == "|") parts.add(mutableListOf()) else parts.last().add(t)
            }
            return if (parts.size == 2 && parts.all { it.isNotEmpty() }) parts else null
        }

        if (tokens.none { isHemi(it) }) {
            val n = tokens.size
            if (n != 2 && n != 4 && n != 6) return null
            val h = n / 2
            return listOf(tokens.subList(0, h), tokens.subList(h, n))
        }

        val groups = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        if (isHemi(tokens[0])) {
            // Letter first: N 15 37 50 E 32 32 31
            for (t in tokens) {
                if (isHemi(t)) {
                    if (current.isNotEmpty()) groups.add(current)
                    current = mutableListOf(t)
                } else {
                    current.add(t)
                }
            }
            if (current.isNotEmpty()) groups.add(current)
        } else {
            // Letter last: 15 37 50 N 32 32 31 E
            for (t in tokens) {
                current.add(t)
                if (isHemi(t)) {
                    groups.add(current)
                    current = mutableListOf()
                }
            }
            if (current.isNotEmpty()) return null
        }
        return if (groups.size == 2) groups else null
    }

    private fun decode(group: List<String>): Part? {
        val letters = group.filter { isHemi(it) }
        if (letters.size > 1) return null
        val hemi = letters.firstOrNull()?.get(0)

        val nums = group.filter { !isHemi(it) }
        if (nums.size !in 1..3) return null
        if (nums.drop(1).any { it.startsWith("-") }) return null

        val negative = nums[0].startsWith("-")
        val d = abs(nums[0].toDouble())
        val m = nums.getOrNull(1)?.toDouble() ?: 0.0
        val sec = nums.getOrNull(2)?.toDouble() ?: 0.0

        if (nums.size >= 2 && (d != floor(d) || m >= 60.0)) return null
        if (nums.size == 3 && (m != floor(m) || sec >= 60.0)) return null

        var v = d + m / 60.0 + sec / 3600.0
        if (negative || hemi == 'S' || hemi == 'W') v = -v
        return Part(hemi, v)
    }
}
