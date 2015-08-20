package de.kaktus42;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilTest {

    @Test
    public void testPaddedBaseString() throws Exception {
        new Util();
        assertEquals("ABCD", Util.rightPaddedBaseString("ABCD", -3));
        assertEquals("ABCD", Util.rightPaddedBaseString("ABCD", 3));
        assertEquals("ABCD--", Util.rightPaddedBaseString("ABCD", 6));
        assertEquals("A0CD--", Util.rightPaddedBaseString("A0CD", 6));
    }

    @Test
    public void testPaddedString() throws Exception {
        assertEquals("ABCD  ", Util.rightPaddedString("ABCD", 6, ' '));
        assertEquals("ABCD____", Util.rightPaddedString("ABCD", 8, '_'));
        assertEquals("A0CD____", Util.rightPaddedString("A0CD", 8, '_'));
    }
}