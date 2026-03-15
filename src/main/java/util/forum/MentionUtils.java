package util.forum;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionUtils {

    private static final Pattern MENTION_PATTERN =
        Pattern.compile("(?<![\\w.])@([A-Za-z0-9._-]{1,50})");

    private MentionUtils() {
    }

    public static Set<String> extractMentionUsernames(String messageText) {
        Set<String> mentions = new LinkedHashSet<>();
        if (messageText == null || messageText.isBlank()) {
            return mentions;
        }
        Matcher matcher = MENTION_PATTERN.matcher(messageText);
        while (matcher.find()) {
            mentions.add(normalizeUsername(matcher.group(1)));
        }
        return mentions;
    }

    public static String usernameFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        String value = at > 0 ? email.substring(0, at) : email;
        return normalizeUsername(value);
    }

    public static String normalizeUsername(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
