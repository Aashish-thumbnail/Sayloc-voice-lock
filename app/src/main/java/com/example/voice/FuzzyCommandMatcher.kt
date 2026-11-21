package com.example.voice

import com.example.data.VoiceCommand
import kotlin.math.max
import kotlin.math.min

data class MatchResult(
    val command: VoiceCommand,
    val matchedCandidate: String,
    val score: Float
)

/**
 * Offline fuzzy command matcher supporting English, Hindi (Devanagari), and Hinglish spelling variants.
 * Uses transliteration + phonetic folding + Levenshtein similarity across tokens and phrases.
 */
object FuzzyCommandMatcher {

    fun findBestMatch(
        candidates: List<String>,
        commands: List<VoiceCommand>,
        sensitivity: Float // 0.2 (strict) .. 0.95 (very relaxed)
    ): MatchResult? {
        val enabledCommands = commands.filter { it.isEnabled && it.name.isNotBlank() }
        if (enabledCommands.isEmpty() || candidates.isEmpty()) return null

        // Higher sensitivity slider => lower similarity threshold needed to trigger
        // sensitivity 0.2 -> threshold 0.90 (strict)
        // sensitivity 0.65 -> threshold 0.74 (balanced)
        // sensitivity 0.95 -> threshold 0.60 (relaxed)
        val threshold = (0.96f - (sensitivity.coerceIn(0.2f, 0.95f) * 0.38f)).coerceIn(0.58f, 0.92f)

        var best: MatchResult? = null

        for (rawCandidate in candidates) {
            val cleanedCandidate = normalizeRaw(rawCandidate)
            if (cleanedCandidate.isEmpty()) continue
            val candidatePhonetic = phoneticFold(cleanedCandidate)
            val candidateTokens = candidatePhonetic.split(" ").filter { it.isNotEmpty() }

            for (command in enabledCommands) {
                val triggers = buildList {
                    add(command.name)
                    addAll(command.aliases)
                }

                for (trigger in triggers) {
                    val cleanTrigger = normalizeRaw(trigger)
                    if (cleanTrigger.isEmpty()) continue
                    val triggerPhonetic = phoneticFold(cleanTrigger)
                    if (triggerPhonetic.isEmpty()) continue

                    val score = scoreMatch(
                        candidateFull = candidatePhonetic,
                        candidateTokens = candidateTokens,
                        triggerFull = triggerPhonetic
                    )

                    if (score >= threshold && (best == null || score > best.score)) {
                        best = MatchResult(
                            command = command,
                            matchedCandidate = rawCandidate,
                            score = score
                        )
                    }
                }
            }
        }
        return best
    }

    private fun scoreMatch(
        candidateFull: String,
        candidateTokens: List<String>,
        triggerFull: String
    ): Float {
        if (candidateFull == triggerFull) return 1.0f

        val triggerTokens = triggerFull.split(" ").filter { it.isNotEmpty() }
        if (triggerTokens.isEmpty()) return 0f

        // Direct substring boundary match (e.g. "hey riya lock" contains "riya")
        if (triggerFull.length >= 3 && (" $candidateFull ").contains(" $triggerFull ")) {
            return 0.98f
        }

        // Single-word trigger: compare against each word in spoken sentence + whole sentence
        if (triggerTokens.size == 1) {
            var maxScore = similarity(candidateFull, triggerFull)
            for (token in candidateTokens) {
                if (token == triggerFull) return 0.99f
                val sim = similarity(token, triggerFull)
                if (sim > maxScore) maxScore = sim
                // Also handle merged words like "goodnight" vs "good night"
                if (token.length >= triggerFull.length + 2 && token.contains(triggerFull) && triggerFull.length >= 4) {
                    maxScore = max(maxScore, 0.90f)
                }
            }
            return maxScore
        }

        // Multi-word trigger (e.g. "good night"): sliding window of matching word count
        var bestScore = similarity(candidateFull, triggerFull)
        val joinedCandidate = candidateFull.replace(" ", "")
        val joinedTrigger = triggerFull.replace(" ", "")
        bestScore = max(bestScore, similarity(joinedCandidate, joinedTrigger))

        val windowSize = triggerTokens.size
        if (candidateTokens.size >= windowSize) {
            for (i in 0..(candidateTokens.size - windowSize)) {
                val slice = candidateTokens.subList(i, i + windowSize).joinToString(" ")
                val sim = similarity(slice, triggerFull)
                if (sim > bestScore) bestScore = sim
            }
        }
        return bestScore
    }

    internal fun normalizeRaw(input: String): String {
        val transliterated = transliterateDevanagari(input.trim().lowercase())
        val sb = StringBuilder(transliterated.length)
        var lastWasSpace = true
        for (ch in transliterated) {
            when {
                ch.isLetterOrDigit() -> {
                    sb.append(ch)
                    lastWasSpace = false
                }
                !lastWasSpace -> {
                    sb.append(' ')
                    lastWasSpace = true
                }
            }
        }
        return sb.toString().trim()
    }

    internal fun phoneticFold(normalized: String): String {
        if (normalized.isEmpty()) return ""
        var s = normalized
            .replace("ph", "f")
            .replace("sh", "s")
            .replace("chh", "ch")
            .replace("kh", "k")
            .replace("gh", "g")
            .replace("bh", "b")
            .replace("dh", "d")
            .replace("th", "t")
            .replace("jh", "j")
            .replace("ck", "k")
            .replace("qu", "k")
            .replace("q", "k")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("aa", "a")
            .replace("v", "w")
            .replace("z", "j")
            .replace("y", "i")

        // Collapse consecutive duplicate characters (e.g., "aashu" -> "asu", "riiya" -> "ria")
        val collapsed = StringBuilder(s.length)
        var prev = '\u0000'
        for (ch in s) {
            if (ch != prev) {
                collapsed.append(ch)
                prev = ch
            }
        }
        return collapsed.toString().trim()
    }

    private fun similarity(a: String, b: String): Float {
        if (a == b) return 1.0f
        if (a.isEmpty() || b.isEmpty()) return 0f
        val maxLen = max(a.length, b.length)
        val dist = levenshtein(a, b)
        return (1.0f - (dist.toFloat() / maxLen.toFloat())).coerceIn(0f, 1f)
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)

        for (i in 1..m) {
            curr[0] = i
            val ca = a[i - 1]
            for (j in 1..n) {
                val cost = if (ca == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[n]
    }

    /**
     * Lightweight offline Devanagari-to-Latin transliterator so Hindi ASR results
     * match English/Hinglish trigger words (e.g. "रिया" -> "riya", "लॉक" -> "lok").
     */
    private fun transliterateDevanagari(text: String): String {
        var hasDevanagari = false
        for (c in text) {
            if (c in '\u0900'..'\u097F') {
                hasDevanagari = true
                break
            }
        }
        if (!hasDevanagari) return text

        val out = StringBuilder(text.length * 2)
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val consonant = DEVANAGARI_CONSONANTS[ch]
            if (consonant != null) {
                out.append(consonant)
                val next = text.getOrNull(i + 1)
                if (next == '\u094D') { // halant/virama suppresses inherent 'a'
                    i += 2
                    continue
                }
                val matra = next?.let { DEVANAGARI_MATRAS[it] }
                if (matra != null) {
                    out.append(matra)
                    i += 2
                    continue
                }
                // Add inherent 'a' unless at end of word
                val isEndOfWord = next == null || next.isWhitespace()
                if (!isEndOfWord) {
                    out.append('a')
                }
                i++
            } else {
                val vowel = DEVANAGARI_VOWELS[ch]
                if (vowel != null) {
                    out.append(vowel)
                } else {
                    out.append(ch)
                }
                i++
            }
        }
        return out.toString()
    }

    private val DEVANAGARI_VOWELS = mapOf(
        'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee",
        'उ' to "u", 'ऊ' to "oo", 'ए' to "e", 'ऐ' to "ai",
        'ओ' to "o", 'औ' to "au", 'ऑ' to "o", 'ऋ' to "ri",
        'ं' to "n", 'ँ' to "n", 'ः' to "h", '़' to ""
    )

    private val DEVANAGARI_MATRAS = mapOf(
        'ा' to "a", 'ि' to "i", 'ी' to "i", 'ु' to "u", 'ू' to "u",
        'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au", 'ॉ' to "o", 'ृ' to "ri"
    )

    private val DEVANAGARI_CONSONANTS = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "n",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "n",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "f", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h",
        'ळ' to "l", 'क़' to "k", 'ख़' to "kh", 'ग़' to "g", 'ज़' to "z", 'ड़' to "d", 'ढ़' to "dh", 'फ़' to "f"
    )
}
