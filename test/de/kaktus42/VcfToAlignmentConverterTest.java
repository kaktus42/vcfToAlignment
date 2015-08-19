package de.kaktus42;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.variant.vcf.VCFFileReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

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
        testConvertMain("testdata/test.vcf", "testdata/reference.fasta", converter);
        String expected =
                "refSeq \tCCATTTGTCAAATGTGAGTCCATTG-\n" +
                "acs0293\tCTATTTGTCACATGTGAGTCCA--GA\n" +
                "acs0292\tCYATTTGTCAMAYGTGAGTCCATTGA\n" +
                "acs0291\tCCATTTGTCAAACGTGAGTCCATTGA\n" +
                "acs8671\tCYATTTGTCAGACGTGAGTCCATTG-\n";

        assertEquals(expected, converter.getAlignment());
    }

    @Test
    public void testConvert2() throws Exception {
        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(true, 4);
        testConvertMain("testdata/test.vcf", "testdata/reference.fasta", converter);
        String expected =
                "refSeq \tCCATTTGTCAAATGTGAGTCCATTG-\n" +
                "acs0293\tCTATTTGTCACATGTGAGTCCA--GA\n" +
                "acs0292\tCYATTTGTCAMAYGTGAGTCCANNGN\n" +
                "acs0291\tCCATTTGTCAAACGTGAGTCCANNGN\n" +
                "acs8671\tCYATTTGTCAGACGTGAGTCCATTG-\n";

        assertEquals(expected, converter.getAlignment());
    }

    @Test
    public void testConvertOverlap() throws Exception {
        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(false, 4);
        try {
            testConvertMain("testdata/test_overlap.vcf", "testdata/reference.fasta", converter);
            fail("Did not get expected ConverterException for overlapping variants");
        } catch(ConverterException expectedException) {
            assertTrue("Did not get expected ConverterException for overlapping variants",
                    expectedException.getMessage()
                            .startsWith("ERROR: Following variant overlaps the previous variant:"));
        }
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

    public void testConvertMain(String variantFileName,
                                String refFileName,
                                VcfToAlignmentConverter converter) {
        File variantFile = new File(variantFileName);
        File refFile = new File(refFileName);

        VCFFileReader variantFileReader = new VCFFileReader(variantFile, false);
        IndexedFastaSequenceFile indexedReferenceFile = null;
        try {
            indexedReferenceFile = new IndexedFastaSequenceFile(refFile);
        } catch (FileNotFoundException e) {
            fail("ERROR: Cant open '" + refFile.toString() + "' to read.");
        }
        converter.convert(variantFileReader, indexedReferenceFile);
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