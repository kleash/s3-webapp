package com.example.s3webapp.util;

import java.util.regex.Pattern;

public final class KeyUtils {
    private KeyUtils() {}

    public static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return "";
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    public static String folderNameFromPrefix(String parentPrefix, String childPrefix) {
        String trimmed = childPrefix;
        if (parentPrefix != null && !parentPrefix.isBlank()) {
            trimmed = childPrefix.substring(parentPrefix.length());
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static String extractName(String key) {
        if (key == null || key.isBlank()) return "";
        int idx = key.lastIndexOf('/') + 1;
        return key.substring(idx);
    }

    public static String wildcardToRegex(String wildcard) {
        if (wildcard == null || wildcard.isBlank()) {
            return ".*";
        }
        String escaped = Pattern.quote(wildcard).replace("*", "\\E.*\\Q");
        return "^" + escaped + "$";
    }
}
