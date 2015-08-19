package de.kaktus42.Nucleobase;

import org.junit.Test;

import static org.junit.Assert.*;

public class NucleobaseTest {

    @Test
    public void testConstructors() throws Exception {
        Character baseCharacter = 'A';
        Character baseCharacterLower = 'a';

        assertEquals("constructor Nucleobase(Character)",
                baseCharacter.toString(), new Nucleobase(baseCharacter).toString());
        assertEquals("constructor Nucleobase(char)",
                baseCharacter.toString(), new Nucleobase(baseCharacter.charValue()).toString());

        assertEquals("constructor Nucleobase(Character)",
                baseCharacter.toString(), new Nucleobase(baseCharacterLower).toString());
        assertEquals("constructor Nucleobase(char)",
                baseCharacter.toString(), new Nucleobase(baseCharacterLower.charValue()).toString());

        try {
            new Nucleobase('X');
            fail("Nucleobase did not throw NucelobaseException: 'Nucleobase is not one of [" + Nucleobase.translationMatrixIdx + "]");
        } catch (NucelobaseException expectedException) {
            assertTrue(true);
        }
    }

    @Test
    public void testCreateGroupRepresentation() throws Exception {
        assertEquals("A", Nucleobase.createGroupRepresentation('a'));
        assertEquals("A", Nucleobase.createGroupRepresentation('A'));
        assertEquals("AT", Nucleobase.createGroupRepresentation('W'));
        assertEquals("ACGT", Nucleobase.createGroupRepresentation('N'));
        assertEquals("ACGT", Nucleobase.createGroupRepresentation(new Nucleobase('N')));
    }

    @Test
    public void testRepresents() throws Exception {
        assertTrue("Nucleobase('A').represents('a')", new Nucleobase('A').represents('a'));
        assertTrue("Nucleobase('A').represents('a')", new Nucleobase('A').represents('A'));
        assertTrue("Nucleobase('A').represents('a')", new Nucleobase('R').represents('A'));
        assertFalse("Nucleobase('A').represents('a')", new Nucleobase('R').represents('T'));
        assertTrue("Nucleobase('A').represents('a')", new Nucleobase('N').represents('T'));
        assertTrue("Nucleobase('A').represents('a')", new Nucleobase('N').represents(new Character('C')));
    }

    @Test
    public void testEquals() throws Exception {
        assertTrue("Nucleobase('A').equals('a')", new Nucleobase('A').equals('a'));
        assertTrue("Nucleobase('A').equals('A')", new Nucleobase('A').equals('A'));
        assertFalse("Nucleobase('R').equals('A')", new Nucleobase('R').equals('A'));
        assertFalse("Nucleobase('R').equals('T')", new Nucleobase('R').equals('T'));
        assertFalse("Nucleobase('N').equals('T')", new Nucleobase('N').equals('T'));
        assertFalse("Nucleobase('N').equals('C')", new Nucleobase('N').equals(new Character('C')));
        assertFalse("Nucleobase('N').equals('G')", new Nucleobase('N').equals(new Nucleobase('G')));
    }
}