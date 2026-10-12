package com.wipro.chatmod.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * BannedPatterns
 * --------------
 * Central, immutable holder for every regex rule and banned-word list used
 * by the moderation engine. Keeping all pattern definitions in one utility
 * class means:
 *   1. Regex is compiled exactly once (Pattern.compile is expensive), and
 *      reused across every thread and every message - important for
 *      performance under concurrent load.
 *   2. Moderation rules can be tuned/extended in a single place without
 *      touching the business logic in MessageModeratorService.
 *
 * This class is declared final with a private constructor because it is a
 * pure static utility/constant holder and must never be instantiated.
 */
public final class BannedPatterns {

    private BannedPatterns() {
        // Utility class - prevent instantiation.
    }

    /**
     * List of words that are never allowed in a chat message.
     * Any (case-insensitive, whole-word) match forces the message to BLOCKED.
     */
    public static final List<String> BANNED_WORDS = Collections.unmodifiableList(
            Arrays.asList("badword", "hate", "abuse", "violence", "scam")
    );

    /**
     * Detects a single character repeated 4 or more times consecutively,
     * e.g. "heyyyyyy" (y repeated), "noooooooo" (o repeated),
     * "whyyyyyyyy" (y repeated).
     *
     * (.)      -> capture any one character into group 1
     * \\1{3,}  -> require that same character to repeat at least 3 MORE
     *             times immediately after (4 total occurrences minimum)
     */
    public static final String REPEAT_CHAR_REGEX = "(.)\\1{3,}";

    /**
     * Detects an http/https URL or a "www." style URL inside a message.
     * Used to find every URL occurrence so the service can decide whether
     * the SAME url appears more than once (spam).
     *
     * https?://\\S+  -> matches http:// or https:// followed by any
     *                    non-whitespace characters
     * www\\.\\S+     -> matches a bare "www." address followed by any
     *                    non-whitespace characters
     */
    public static final String SPAM_REGEX = "(https?://\\S+|www\\.\\S+)";

    /** Pre-compiled pattern for the repeated-character rule. */
    public static final Pattern REPEAT_CHAR_PATTERN = Pattern.compile(REPEAT_CHAR_REGEX);

    /** Pre-compiled pattern for URL detection (case-insensitive: HTTP/HTTPS/WWW all match). */
    public static final Pattern SPAM_PATTERN = Pattern.compile(SPAM_REGEX, Pattern.CASE_INSENSITIVE);

    /** Pre-compiled pattern that matches any banned word as a whole word, case-insensitive. */
    public static final Pattern BANNED_WORD_PATTERN = buildBannedWordPattern();

    /**
     * Builds a single alternation regex out of BANNED_WORDS, e.g.
     * \b(badword|hate|abuse|violence|scam)\b
     *
     * Using one combined pattern (rather than looping BANNED_WORDS and
     * running find() once per word) means only one Matcher pass is needed
     * per message, which is more efficient.
     *
     * \\b           -> word boundary, so "hated" is NOT wrongly matched by "hate"... 
     *                  Note: \b still allows "hate" inside "hate!" or "Hate," because
     *                  boundary is defined by word-char vs non-word-char, which is
     *                  exactly the desired behaviour here.
     * Pattern.quote -> escapes each banned word literally, in case special
     *                  regex characters are ever added to the list.
     */
    private static Pattern buildBannedWordPattern() {
        StringBuilder regex = new StringBuilder("\\b(");
        for (int i = 0; i < BANNED_WORDS.size(); i++) {
            regex.append(Pattern.quote(BANNED_WORDS.get(i)));
            if (i < BANNED_WORDS.size() - 1) {
                regex.append("|");
            }
        }
        regex.append(")\\b");
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }
}
