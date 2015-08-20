package de.kaktus42;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.variant.vcf.VCFFileReader;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VcfToAlignmentConverterTest {

    VcfToAlignmentConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new VcfToAlignmentConverter();
        converter = new VcfToAlignmentConverter(false, 4);
/*
        String variantData = "##fileformat=VCFv4.1\n" +
                "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tacs0291\tacs0292\tacs0293\tacs8671\n" +
                "1\t16\t.\tA\tC,G\t3311.21\t.\tAC=5,0;AF=0.029,0.00;AN=174;DP=5326;set=Intersection\tGT:DP:GQ:PGT:PID\t0/0:13:38\t0/1:37:99\t1/1:25:74\t2/2:49:99\n" +
                "1\t16\t.\tAA\tA\t3311.21\t.\tAC=5,0;AF=0.029,0.00;AN=174;DP=5326;set=Intersection\tGT:DP:GQ:PGT:PID\t0/1:13:38\t1/0:37:99\t1/1:25:74\t0/0:49:99\n" +
                "1\t16\t.\tA\tAT\t3311.21\t.\tAC=5,0;AF=0.029,0.00;AN=174;DP=5326;set=Intersection\tGT:DP:GQ:PGT:PID\t0/1:13:38\t1/0:37:99\t1/1:25:74\t0/0:49:99";
        tempVariantFile = File.createTempFile("temp",".vcf");
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempVariantFile));
        bw.write(variantData);
        bw.close();

        String referenceData = ">1\n" +
                "CCATTTTAAAGGTCAAATGTGACCCAGAGCAGGCAAAACCCAAATTTTATCGATTTTCGTGTGCAATAGT\n" +
                "CTATGGAGTTTTTGGTGATCTGGAATTCCGACATAAGTTATGCTAAAAAATTTTGTGTACGTCTGTTAAG\n" +
                "ACCTTAGCTATGAAGCCAGTTAGCCCTCACGGCCAAAACATACCATTTTGAAGGTCAAATATGCCCCGAA\n" +
                "TCTGGTAAACCCCCCAATTTGCCGATTATCATGTGCTATAGTCCATGGACTTTTTGGTGATCTGGAATTT";
        tempRefFile = File.createTempFile("temp",".fasta");
        bw = new BufferedWriter(new FileWriter(tempRefFile));
        bw.write(referenceData);
        bw.close();

        String referenceIndexData = "1\t280\t3\t70\t71";
        tempRefIndexFile = new File(tempRefFile.toString() + ".fai");
        bw = new BufferedWriter(new FileWriter(tempRefIndexFile));
        bw.write(referenceIndexData);
        bw.close();*/
    }

    /*@After
    public void tearDown() {
        tempVariantFile.delete();
        tempRefFile.delete();
        tempRefIndexFile.delete();
    }*/

    @Test
    public void testConvert() throws Exception {
        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(false, 4);
        List<String> output = testConvertMain("testdata/test.vcf", "testdata/reference.fasta", converter);
        ArrayList<String> expected = new ArrayList<String>() {{
            add("   5                                  26");
            add("refSeq  CCATTTGTCA AATGTGAGTC CATTG-");
            add("acs0293 CTATTTGTCA CATGTGAGTC CA--GA");
            add("acs0292 CYATTTGTCA MAYGTGAGTC CATTGA");
            add("acs0291 CCATTTGTCA AACGTGAGTC CATTGA");
            add("acs8671 CYATTTGTCA GACGTGAGTC CATTG-");
            add("");
        }};

        assertEquals(expected, output);
    }

    @Test
    public void testConvert2() throws Exception {
        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(true, 4);
        List<String> output = testConvertMain("testdata/test.vcf", "testdata/reference.fasta", converter);
        ArrayList<String> expected = new ArrayList<String>() {{
            add("   5                                  26");
            add("refSeq  CCATTTGTCA AATGTGAGTC CATTG-");
            add("acs0293 CTATTTGTCA CATGTGAGTC CA--GA");
            add("acs0292 CYATTTGTCA MAYGTGAGTC CANNGN");
            add("acs0291 CCATTTGTCA AACGTGAGTC CANNGN");
            add("acs8671 CYATTTGTCA GACGTGAGTC CATTG-");
            add("");
        }};

        assertEquals(expected, output);
    }

    @Test
    public void testConvertOverlap() throws Exception {
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(false, 4);
        testConvertMain("testdata/test_overlap.vcf", "testdata/reference.fasta", converter);
        assertTrue("Did not get expected warning for overlapping variants", errContent.toString()
                .contains("WARNING: Skipping variant becuse it overlaps with the previous variant:"));
        System.setErr(System.err);
    }

    @Test
    public void testConvertWrongReference() throws Exception {
        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(false, 4);
        try {
            testConvertMain("testdata/test.vcf", "testdata/wrong_reference.fasta", converter);
            fail("Did not get expected ConverterException for reference sequence mismatch");
        } catch(ConverterException expectedException) {
            assertEquals("Did not get expected ConverterException for reference sequence mismatch",
                "The reference stated in the variant file does not match the reference in the reference sequence: C - A",
                expectedException.getMessage());
        }
    }

    public List<String> testConvertMain(String variantFileName,
                                String refFileName,
                                VcfToAlignmentConverter converter) {
        File variantFile = new File(variantFileName);
        File refFile = new File(refFileName);
        File outFile;
        try {
            outFile = File.createTempFile("testOutput", ".phy");
        } catch (IOException e) {
            e.printStackTrace();
            fail("Could not create temporary out file for testing");
            return new ArrayList<>();
        }

        VCFFileReader variantFileReader = new VCFFileReader(variantFile, false);
        IndexedFastaSequenceFile indexedReferenceFile = null;
        try {
            indexedReferenceFile = new IndexedFastaSequenceFile(refFile);
        } catch (FileNotFoundException e) {
            fail("ERROR: Cant open '" + refFile.toString() + "' to read.");
        }
        converter.useRelaxedPhylipFormat();
        converter.convert(variantFileReader, indexedReferenceFile, outFile);

        List<String> lines;
        try {
            lines = Files.readAllLines(outFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not read temporary out file");
            return new ArrayList<>();
        }
        return lines;
    }

    @Test
    public void testFillsHeterozygousIndelsWithN() throws Exception {
        assertFalse(converter.fillsHeterozygousIndelsWithN());
    }

    @Test
    public void testSetFillHeterozygousIndelsWithN() throws Exception {
        assertFalse(converter.fillsHeterozygousIndelsWithN());
        converter.setFillHeterozygousIndelsWithN(true);
        assertTrue(converter.fillsHeterozygousIndelsWithN());
    }

    @Test
    public void testGetSurroundingReferenceAmount() throws Exception {
        assertEquals(4, converter.getSurroundingReferenceAmount());
    }

    @Test
    public void testSetSurroundingReferenceAmount() throws Exception {
        converter.setSurroundingReferenceAmount(5);
        assertEquals(5, converter.getSurroundingReferenceAmount());
    }
}