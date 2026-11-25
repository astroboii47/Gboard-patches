package dev.jason.gboardpatches.extension.calculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class GboardGifQueryTokenTest {
    @Test
    public void acceptsQueryAtStartOrAfterWhitespace() {
        GboardShortcodeRuntime.GifToken start =
                GboardShortcodeRuntime.GifToken.from("?sad face?");
        assertEquals("sad face", start.query);
        assertEquals(10, start.length);

        GboardShortcodeRuntime.GifToken afterText =
                GboardShortcodeRuntime.GifToken.from("hello ?party cat?");
        assertEquals("party cat", afterText.query);
        assertEquals(11, afterText.length);
    }

    @Test
    public void ignoresOrdinaryQuestionsAndInvalidQueries() {
        assertNull(GboardShortcodeRuntime.GifToken.from("Are you? okay?"));
        assertNull(GboardShortcodeRuntime.GifToken.from("??"));
        assertNull(GboardShortcodeRuntime.GifToken.from("?a?"));
        assertNull(GboardShortcodeRuntime.GifToken.from("?two?questions?"));
    }
}
