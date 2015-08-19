package de.kaktus42.Nucleobase;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class NucleobaseSequenceTest {

    @Test
    public void testConstructors() throws Exception {
        String sequenceString = "AAAA";
        NucleobaseSequence nsA = new NucleobaseSequence(sequenceString);

        ArrayList<Nucleobase> al = new ArrayList<>();
        al.add(new Nucleobase('A'));
        al.add(new Nucleobase('A'));
        al.add(new Nucleobase('A'));
        al.add(new Nucleobase('A'));
        assertEquals("constructor NucleobaseSequence(String)", al, nsA.sequence);
        assertTrue("constructor NucleobaseSequence(String)", nsA.equals(sequenceString));

        NucleobaseSequence nsB = new NucleobaseSequence(nsA);
        assertEquals("constructor NucleobaseSequence(NucleobaseSequence)", al, nsB.sequence);
        assertTrue("constructor NucleobaseSequence(NucleobaseSequence)", nsB.equals(sequenceString));

        NucleobaseSequence nsC = new NucleobaseSequence(al);
        assertEquals("constructor NucleobaseSequence(ArrayList)", al, nsC.sequence);
        assertTrue("constructor NucleobaseSequence(ArrayList)", nsC.equals(sequenceString));

        try {
            new NucleobaseSequence("AXA");
            fail("NucleobaseSequence did not throw NucelobaseException: 'NucleobaseSequence contains not only [" + Nucleobase.translationMatrixIdx + "]");
        } catch (NucelobaseException expectedException) {
            assertTrue(true);
        }
    }

    @Test
    public void testAppendFront() throws Exception {
        NucleobaseSequence nsA = new NucleobaseSequence("AAAA");

        nsA.appendFront("TTTT");
        assertTrue("AAAA.appendFront(TTTT) must be TTTTAAAA", nsA.equals("TTTTAAAA"));

        nsA.appendFront(new NucleobaseSequence("AAAA"));
        assertTrue("TTTTAAAA.appendFront(AAAA) must be AAAATTTTAAAA", nsA.equals("AAAATTTTAAAA"));
    }

    @Test
    public void testAppendBack() throws Exception {
        NucleobaseSequence nsA = new NucleobaseSequence("AAAA");

        nsA.appendBack("TTTT");
        assertTrue("AAAA.appendBack(TTTT) must be AAAATTTT", nsA.equals("AAAATTTT"));

        nsA.appendBack(new NucleobaseSequence("AAAA"));
        assertTrue("AAAATTTT.appendBack(AAAA) must be AAAATTTTAAAA", nsA.equals("AAAATTTTAAAA"));
    }

    @Test
    public void testSubSequence() throws Exception {
        NucleobaseSequence nsA = new NucleobaseSequence("ACGTGCA");

        assertTrue("ACGT.subSequence(1) must be CGTGCA", nsA.subSequence(1).equals("CGTGCA"));
        assertTrue("ACGT.subSequence(1,3) must be CG", nsA.subSequence(1, 3).equals("CG"));
    }

    @Test
    public void testCombineWith() throws Exception {
        NucleobaseSequence nsA;
        try {
            nsA =                new NucleobaseSequence("ACGT");
            nsA.combineWith(new NucleobaseSequence("GGNG"), -5);
            fail("combineWith did not throw NucelobaseException: 'ACGT.combineWith(GGNG, -5) would create gaps in the front.'");
        } catch (NucelobaseException expectedException) {
            assertTrue(true);
        }

        try {
            nsA =      new NucleobaseSequence("ACGT");
            nsA.combineWith(new NucleobaseSequence("GGNG"), 5);
            fail("combineWith did not throw NucelobaseException: 'ACGT.combineWith(GGNG, 5) would create gaps in the back.'");
        } catch (NucelobaseException expectedException) {
            assertTrue(true);
        }

        nsA =           new NucleobaseSequence(           "ACGT" );
        nsA.combineWith(new NucleobaseSequence(       "GGNG"     ), -4);
        assertTrue("ACGT.combineWith(GGNG, -4) must be GGNGACGT", nsA.equals("GGNGACGT"));

        nsA =           new NucleobaseSequence(         "ACGT"   );
        nsA.combineWith(new NucleobaseSequence(       "GGNG"     ), -2);
        assertTrue("ACGT.combineWith(GGNG, -2) must be GGNSGT", nsA.equals("GGNSGT"));

        nsA =           new NucleobaseSequence(      "ACGT"      );
        nsA.combineWith(new NucleobaseSequence(      "GGNG"      ), 0);
        assertTrue("ACGT.combineWith(GGNG, 0) must be RSNK", nsA.equals("RSNK"));

        nsA =           new NucleobaseSequence(   "ACGT"         );
        nsA.combineWith(new NucleobaseSequence(   "GGNG"         ));
        assertTrue("ACGT.combineWith(GGNG) must be RSNK", nsA.equals("RSNK"));

        nsA =           new NucleobaseSequence(      "ACGT"      );
        nsA.combineWith(new NucleobaseSequence(        "GGNG"    ), 2);
        assertTrue("ACGT.combineWith(GGNG, 2) must be ACGKNG", nsA.equals("ACGKNG"));

        nsA =           new NucleobaseSequence(      "ACGT"      );
        nsA.combineWith(new NucleobaseSequence(         "GGNG"   ), 4);
        assertTrue("ACGT.combineWith(GGNG, 4) must be ACGTGGNG", nsA.equals("ACGTGGNG"));

        nsA =           new NucleobaseSequence(  "ACGT"   );
        nsA.combineWith(new NucleobaseSequence(  "GG"     ));
        assertTrue("ACGT.combineWith(GG) must be RSGT", nsA.equals("RSGT"));

        nsA =          new NucleobaseSequence(     "ACGT" );
        nsA.combineWith(new NucleobaseSequence(     "GG"  ), 1);
        assertTrue("ACGT.combineWith(GG, 1) must be ASGT", nsA.equals("ASGT"));

        nsA =            new NucleobaseSequence(         "ACGT"   );
        nsA.combineWith(new NucleobaseSequence(         "GGNGGG"  ), -1);
        assertTrue("ACGT.combineWith(GGNGGG, -1) must be GRNGKG", nsA.equals("GRNGKG"));

        nsA =            new NucleobaseSequence(        "ACGT"    );
        nsA.combineWith(new NucleobaseSequence(        "GGGNG"    ), -1);
        assertTrue("ACGT.combineWith(GGGNG, -1) must be GRSNK", nsA.equals("GRSNK"));

        nsA =           new NucleobaseSequence(    "ACGT"         );
        nsA.combineWith(new NucleobaseSequence(    "GGGNG"        ));
        assertTrue("ACGT.combineWith(GGGNG) must be RSGNG", nsA.equals("RSGNG"));

        NucleobaseSequence nsB = new NucleobaseSequence("ACGT");
        nsB.combineWith("GGGNG");
        assertEquals(nsA, nsB);

        nsB = new NucleobaseSequence("ACGT");
        nsB.combineWith("GGGNG", 0);
        assertEquals(nsA, nsB);
    }

    @Test
    public void testClone() throws Exception {
        NucleobaseSequence nsA = new NucleobaseSequence("ACGT");
        assertNotSame("Cloning must not return the same object", nsA, nsA.clone());
        assertEquals("Cloned Instance must have the same state", nsA, nsA.clone());
    }

    @Test
    public void testSize() throws Exception {
        NucleobaseSequence nsA = new NucleobaseSequence("ACGT");
        assertEquals("Cloned Instance must have the same state", 4, nsA.length());
    }
}