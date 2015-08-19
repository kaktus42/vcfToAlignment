package de.kaktus42;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilTest {

    @Test
    public void testPaddedBaseString() throws Exception {
        new Util();
        assertEquals("ABCD", Util.paddedBaseString("ABCD", -3));
        assertEquals("ABCD", Util.paddedBaseString("ABCD", 3));
        assertEquals("ABCD--", Util.paddedBaseString("ABCD", 6));
    }

    @Test
    public void testPaddedString() throws Exception {
        assertEquals("ABCD  ", Util.paddedString("ABCD", 6, ' '));
        assertEquals("ABCD____", Util.paddedString("ABCD", 8, '_'));
    }
}