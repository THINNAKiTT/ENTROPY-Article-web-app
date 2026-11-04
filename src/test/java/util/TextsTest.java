package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextsTest {

    @Test
    public void slugifyKeepsLettersAndDigitsOnly() {
        assertEquals("hello-world", Texts.slugify("Hello, World!"));
        assertEquals("c-tips", Texts.slugify("c++ tips"));
        assertEquals("33", Texts.slugify("33"));
    }

    @Test
    public void slugifyFlattensAccents() {
        assertEquals("world-uber-cafe", Texts.slugify("Wörld über Café!!"));
    }

    @Test
    public void slugifyOfNothingUsableIsEmpty() {
        assertEquals("", Texts.slugify("!!!???"));
        assertEquals("", Texts.slugify(null));
    }

    @Test
    public void excerptCutsOnAWordBoundary() {
        String text = "one two three four five six seven eight nine ten";
        String cut = Texts.excerpt(text, 20);
        assertTrue(cut.endsWith("…"));
        assertTrue(cut.length() <= 21);
        assertFalse("must not cut a word in half", cut.replace("…", "").endsWith("thre"));
    }

    @Test
    public void excerptLeavesShortTextAlone() {
        assertEquals("short text", Texts.excerpt("short \n text", 50)); // whitespace is collapsed
        assertEquals("", Texts.excerpt(null, 10));
    }

    @Test
    public void isBlank() {
        assertTrue(Texts.isBlank(null));
        assertTrue(Texts.isBlank("   "));
        assertFalse(Texts.isBlank(" a "));
    }
}
