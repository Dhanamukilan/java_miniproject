package com.wipro.chatmod.service;

import com.wipro.chatmod.bean.ChatMessage;
import com.wipro.chatmod.util.BannedPatterns;
import com.wipro.chatmod.util.ModerationException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * MessageModeratorService
 * ------------------------
 * Contains the core business logic of the bot: given a single ChatMessage,
 * decide whether it is ALLOWED, FLAGGED or BLOCKED.
 *
 * This class is stateless (no instance fields are mutated between calls),
 * so a single shared instance can safely be used by multiple consumer
 * threads concurrently without any additional synchronization.
 */
public class MessageModeratorService {

    /** Classification result constants - kept here so callers never hardcode raw strings. */
    public static final String ALLOWED = "ALLOWED";
    public static final String FLAGGED = "FLAGGED";
    public static final String BLOCKED = "BLOCKED";

    /**
     * Applies the moderation rules to a single message, in priority order:
     *
     *   1. Null/empty/blank content       -> throws ModerationException
     *   2. Repeated (spammy) URL          -> BLOCKED
     *   3. Banned word present            -> BLOCKED
     *   4. Excessive repeated characters  -> FLAGGED
     *   5. Otherwise                      -> ALLOWED
     *
     * @param msg the message to moderate
     * @return exactly one of ALLOWED / FLAGGED / BLOCKED
     * @throws ModerationException if the message content is null or empty
     */
    public String moderate(ChatMessage msg) throws ModerationException {

        if (msg == null || msg.getContent() == null || msg.getContent().trim().isEmpty()) {
            throw new ModerationException("Message content cannot be null or empty");
        }

        String content = msg.getContent();

        // Rule 1: repeated URL spam takes the highest priority.
        if (hasRepeatedUrl(content)) {
            return BLOCKED;
        }

        // Rule 2: any banned word anywhere in the message.
        if (BannedPatterns.BANNED_WORD_PATTERN.matcher(content).find()) {
            return BLOCKED;
        }

        // Rule 3: excessive repeated characters (e.g. "heyyyyyy").
        if (BannedPatterns.REPEAT_CHAR_PATTERN.matcher(content).find()) {
            return FLAGGED;
        }

        // No rule triggered -> message is clean.
        return ALLOWED;
    }

    /**
     * Scans the message for every URL occurrence and returns true if the
     * exact same URL (case-insensitive) appears more than once, which is
     * treated as spam per the assignment's classification rules.
     *
     * @param content the raw message text
     * @return true if a repeated URL was found
     */
    private boolean hasRepeatedUrl(String content) {
        Matcher matcher = BannedPatterns.SPAM_PATTERN.matcher(content);
        Map<String, Integer> urlOccurrences = new HashMap<>();

        while (matcher.find()) {
            String url = matcher.group().toLowerCase();
            urlOccurrences.merge(url, 1, Integer::sum);
        }

        for (int count : urlOccurrences.values()) {
            if (count > 1) {
                return true;
            }
        }
        return false;
    }
}
