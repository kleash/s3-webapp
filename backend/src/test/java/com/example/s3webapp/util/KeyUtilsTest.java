package com.example.s3webapp.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KeyUtilsTest {

    @Test
    void normalizePrefixAddsTrailingSlash() {
        assertThat(KeyUtils.normalizePrefix("folder")).isEqualTo("folder/");
        assertThat(KeyUtils.normalizePrefix("folder/")).isEqualTo("folder/");
        assertThat(KeyUtils.normalizePrefix(null)).isEqualTo("");
    }

    @Test
    void folderNameFromPrefixExtractsChild() {
        assertThat(KeyUtils.folderNameFromPrefix("", "logs/app/")).isEqualTo("logs/app");
        assertThat(KeyUtils.folderNameFromPrefix("logs/", "logs/app/")).isEqualTo("app");
    }

    @Test
    void extractNameReturnsLastSegment() {
        assertThat(KeyUtils.extractName("a/b/c.txt")).isEqualTo("c.txt");
        assertThat(KeyUtils.extractName("file.txt")).isEqualTo("file.txt");
    }

    @Test
    void wildcardToRegexTransformsCorrectly() {
        String regex = KeyUtils.wildcardToRegex("trade_2025_*.csv");
        assertThat("trade_2025_file.csv".matches(regex)).isTrue();
        assertThat("trade_2026_file.csv".matches(regex)).isFalse();
    }
}
