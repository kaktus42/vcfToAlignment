package de.kaktus42;

import de.kaktus42.Nucleobase.NucleobaseSequence;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VcfToAlignmentConverter
{
    public static final int PHYLIP_FORMAT_STRICT = 0;
    public static final int PHYLIP_FORMAT_RELAXED = 1;
    public static final int PHYLIP_FORMAT_OTHER = 10;

    /**
     * true:  AA/A -> AN
     * false: AA/A -> AA
     */
    private boolean fillHeterozygousIndelsWithN = false;

    /**
     * amount of reference sequence to add on both sides of the varaint
     */
    private int surroundingReferenceAmount = 4;

    /**
     * amount of blocks written in interleaved mode. If 0, file is written in sequential mode
     */
    private int interleavedBlockCount = 6;

    /**
     * size of blocks written in interleaved mode
     */
    private int interleavedBlockSize = 10;

    /**
     * detected phylip format to use
     */
    private int phylipFormat = this.PHYLIP_FORMAT_STRICT;

    /**
     *
     */
    private String refSeqName = "refSeq";

    /**
     * length of taxa names
     */
    private int longestSampleNameLength = refSeqName.length();

    /**
     * to write the output
     */
    private PrintWriter outWriter;

    /**
     * holds current size of alignmentBuilders
     */
    private int bufferSize = 0;

    /**
     * how many bases have been written to the alignment file
     */
    private int alignmentLength = 0;

    /**
     * how much space to leave for header.
     *  -> 4 for taxa count, 36 for alignment length
     */
    private static final int headerSizeTaxaCount = 4;
    private static final int headerSizeAlignmentLength = 36;
    private static int headerSize = headerSizeTaxaCount + headerSizeAlignmentLength;

    /**
     * states if the outfile writer is still on the first section that includes the taxa names
     */
    private boolean hasWrittenFirstSection;
    private int preBlockCharacterCount;
    private int blockCharacterCount;

    private HashMap<String, StringBuilder> alignmentBuilders;

    public VcfToAlignmentConverter() {
        this(false, 4);
    }

    public VcfToAlignmentConverter(boolean fillHeterozygousIndelsWithN, int surroundingReferenceAmount) {
        this.fillHeterozygousIndelsWithN = fillHeterozygousIndelsWithN;
        this.surroundingReferenceAmount = surroundingReferenceAmount;
    }

    public void convert(VCFFileReader varaintFileReader,
                        IndexedFastaSequenceFile indexedReferenceFile,
                        File outFile) throws IOException {
        VCFHeader vcfHeader = varaintFileReader.getFileHeader();

        // Prepare output file in buffered write mode
        outWriter = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));

        // Determine longest sample name for formatting
        // Initialize alignmentBuilders
        alignmentBuilders = new HashMap<>();
        alignmentBuilders.put(refSeqName, new StringBuilder());
        for(String sampleName : vcfHeader.getGenotypeSamples()) {
            if(sampleName.length() > longestSampleNameLength) {
                longestSampleNameLength = sampleName.length();
            }
            alignmentBuilders.put(sampleName, new StringBuilder());
        }
        if(longestSampleNameLength > 250 && phylipFormat != this.PHYLIP_FORMAT_OTHER) {
            System.err.println("WARNING: one or more samples have names longer than 250 characters. This violates relaxed phylip format.");
            phylipFormat = this.PHYLIP_FORMAT_OTHER;
        } else if(longestSampleNameLength > 10 && phylipFormat != this.PHYLIP_FORMAT_RELAXED) {
            System.err.println("WARNING: one or more samples have names longer than 10 characters. This violates strict phylip format.");
            phylipFormat = this.PHYLIP_FORMAT_RELAXED;
        }

        // write header to file and sample names to buffers
        outWriter.append(Util.rightPaddedString("", headerSize, ' ')).append('\n');  // 20 spaces for header data, written later
        hasWrittenFirstSection = false;
        preBlockCharacterCount = longestSampleNameLength + (phylipFormat != PHYLIP_FORMAT_STRICT ? 1 : 0);
        blockCharacterCount = interleavedBlockCount * interleavedBlockSize;


        // loop over variants
        String basesAfter = "";
        String lastContig = null;
        int lastPosition = -1;
        int lastSurroundingReferenceAmountAfter = 0;
        bufferSize = 0;
        ReferenceSequence contigSequence = null;
        for(VariantContext variantContext : varaintFileReader) {

            // When changing the chromosome, reset everything
            if(lastContig == null || !lastContig.equals(variantContext.getContig())) {
                lastContig = variantContext.getContig();
                lastPosition = -1;
                lastSurroundingReferenceAmountAfter = 0;
                System.err.println("LOG: Entering contig " + lastContig);
                contigSequence = indexedReferenceFile.getSequence(variantContext.getContig());
            }

            // When two varaints overlap - is that allowed in VCF format?
            if(lastPosition >= variantContext.getStart()) {
                System.err.println("WARNING: Skipping variant becuse it overlaps with the previous variant:");
                System.err.println(variantContext);
                continue;
            }

            // decide what to do with the basesAfter from the last variant
            int surroundingReferenceAmountBefore = variantContext.getStart() - surroundingReferenceAmount < 0 ? variantContext.getStart() - 1 : surroundingReferenceAmount;
            if(lastPosition + lastSurroundingReferenceAmountAfter >= variantContext.getStart()) {
                // When two consecutive varaints overlap respective their surroundings
                // ignore basesAfter and calculate new surroundingReferenceAmountBefore
                surroundingReferenceAmountBefore = variantContext.getStart() - lastPosition - 1;
            } else {
                // safely append surrounding reference sequence after
                appendToAllSequences(basesAfter);
                bufferSize += basesAfter.length();
            }

            // get parts of the reference sequence around the variant site
            // take care of edge cases
            int surroundingReferenceAmountAfter = variantContext.getEnd() + surroundingReferenceAmount > contigSequence.length() ? contigSequence.length() - variantContext.getEnd() : surroundingReferenceAmount;
            lastSurroundingReferenceAmountAfter = surroundingReferenceAmountAfter;
            ReferenceSequence referenceSequence =
                    indexedReferenceFile.getSubsequenceAt(
                            variantContext.getContig(),
                            variantContext.getStart() - surroundingReferenceAmountBefore,
                            variantContext.getEnd() + surroundingReferenceAmountAfter);
            String reference = new String(referenceSequence.getBases());
            lastPosition = variantContext.getEnd();

            int variantLength = variantContext.getEnd() - variantContext.getStart() + 1;
            String basesBefore = reference.substring(0, surroundingReferenceAmountBefore);
            String referenceBase = new String(indexedReferenceFile.getSubsequenceAt(
                    variantContext.getContig(), variantContext.getStart(), variantContext.getEnd()).getBases());
            basesAfter = reference.substring(surroundingReferenceAmountBefore + variantLength);

            if(!referenceBase.equals(variantContext.getReference().getDisplayString()))
                throw new ConverterException("The reference stated in the variant file does not match the reference " +
                        "in the reference sequence: " + referenceBase + " - " + variantContext.getReference().getBaseString());

            int multipleAlignmentWindowSize = variantLength;

            // If not SNP, find longest alternate allele
            if(variantContext.getType() != VariantContext.Type.SNP) {
                for(Allele currentAllele : variantContext.getAlleles()) {
                    if(!currentAllele.isCalled()) continue;
                    if(currentAllele.length() > multipleAlignmentWindowSize) {
                        multipleAlignmentWindowSize = currentAllele.length();
                    }
                }
            }

            alignmentBuilders.get(refSeqName).append(basesBefore)
                    .append(Util.rightPaddedBaseString(referenceBase, multipleAlignmentWindowSize));
            bufferSize += basesBefore.length() + multipleAlignmentWindowSize;

            // go through all samples
            for(Genotype genotype : variantContext.getGenotypesOrderedByName()) {
                NucleobaseSequence ns = new NucleobaseSequence(genotype.getAllele(0).getDisplayString());
                for(Allele currentAllele : genotype.getAlleles()) {
                    if(currentAllele.isNoCall()) continue;

                    if(fillHeterozygousIndelsWithN) {
                        if (currentAllele.length() > ns.length()) {
                            ns.appendBack(new String(new char[currentAllele.length() - ns.length()]).replace('\0', 'N'));
                        }
                        if (currentAllele.length() < ns.length()) {
                            ns.combineWith(new String(new char[ns.length() - currentAllele.length()]).replace('\0', 'N'), currentAllele.length());
                        }
                    }
                    ns.combineWith(currentAllele.getDisplayString());
                }

                alignmentBuilders.get(genotype.getSampleName())
                        .append(basesBefore)
                        .append(Util.rightPaddedBaseString(ns.toString(), multipleAlignmentWindowSize));
            }

            writeAlignmentBuffer(false);
        }
        appendToAllSequences(basesAfter);
        writeAlignmentBuffer(true);
        outWriter.close();

        // overwrite header
        byte[] header = getHeader();
        System.err.println("LOG: Writing header:" + new String(header, StandardCharsets.UTF_8).replaceAll(" +", " "));
        try {
            RandomAccessFile raf = new RandomAccessFile(outFile, "rw");
            raf.seek(0);
            raf.write(header);
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR: Could not write header information:" + new String(header, StandardCharsets.UTF_8).replaceAll(" +", " "));
        }
    }

    /**
     * Write alignment buffers to file
     * If interleaved format, only write when buffer is full enough for block line break
     *
     * @param force write also if buffer is not full
     */
    private void writeAlignmentBuffer(boolean force) {
        if(!force && (interleavedBlockCount < 1 || bufferSize < blockCharacterCount))
            return;

        StringBuilder alignmentBuilder = new StringBuilder();
        alignmentBuilder.ensureCapacity(preBlockCharacterCount + blockCharacterCount + interleavedBlockCount + 1);
        while(bufferSize >= blockCharacterCount) {
            for (Map.Entry<String, StringBuilder> entry : alignmentBuilders.entrySet()) {
                StringBuilder currentSequenceBuilder = entry.getValue();
                if(hasWrittenFirstSection) {
                    alignmentBuilder.append(Util.rightPaddedString("", preBlockCharacterCount, ' '));
                } else {
                    String currentTaxaName = entry.getKey();
                    if(phylipFormat == this.PHYLIP_FORMAT_STRICT) {
                        alignmentBuilder.append(Util.rightPaddedString(currentTaxaName, longestSampleNameLength, ' '));
                    } else {
                        alignmentBuilder.append(Util.rightPaddedString(currentTaxaName, longestSampleNameLength, ' ')).append(' ');
                    }
                }
                int i = 0;
                for(; i + 10 <= blockCharacterCount; i += 10) {
                    alignmentBuilder.append(currentSequenceBuilder.substring(i, i + 10)).append(' ');
                }
                currentSequenceBuilder.delete(0, i);
                alignmentBuilder.append('\n');
            }
            outWriter.append(alignmentBuilder.append('\n'));
            hasWrittenFirstSection = true;
            bufferSize -= blockCharacterCount;
            alignmentLength += blockCharacterCount;
            alignmentBuilder.setLength(0);
        }
        if(force) {
            int basesToWrite = alignmentBuilders.values().iterator().next().length();
            for(Map.Entry<String, StringBuilder> entry : alignmentBuilders.entrySet()) {
                StringBuilder currentSequenceBuilder = entry.getValue();
                if(hasWrittenFirstSection) {
                    alignmentBuilder.append(Util.rightPaddedString("", preBlockCharacterCount, ' '));
                } else {
                    String currentTaxaName = entry.getKey();
                    if(phylipFormat == this.PHYLIP_FORMAT_STRICT) {
                        alignmentBuilder.append(Util.rightPaddedString(currentTaxaName, longestSampleNameLength, ' '));
                    } else {
                        alignmentBuilder.append(Util.rightPaddedString(currentTaxaName, longestSampleNameLength, ' ')).append(' ');
                    }
                }
                int i = 0;
                for(; i + 10 <= currentSequenceBuilder.length(); i += 10) {
                    alignmentBuilder.append(currentSequenceBuilder.substring(i, i + 10)).append(' ');
                }
                if(currentSequenceBuilder.length() > i)
                    alignmentBuilder.append(currentSequenceBuilder.substring(i, currentSequenceBuilder.length()));
                else
                    alignmentBuilder.setLength(currentSequenceBuilder.length() - 1);
                alignmentBuilder.append('\n');
            }
            outWriter.append(alignmentBuilder.append('\n'));
            hasWrittenFirstSection = true;
            bufferSize -= basesToWrite;
            alignmentLength += basesToWrite;
            alignmentBuilder.setLength(0);
        }
    }

    private byte[] getHeader() {
        int taxaCount = alignmentBuilders.size();
        return Util.leftPaddedString(String.valueOf(taxaCount), headerSizeTaxaCount, ' ')
                .concat(Util.leftPaddedString(String.valueOf(alignmentLength), headerSizeAlignmentLength, ' '))
                .getBytes(StandardCharsets.UTF_8);
    }

    private void appendToAllSequences(String string) {
        for(StringBuilder stringBuilder : alignmentBuilders.values()) {
            stringBuilder.append(string);
        }
    }

    public void useStrictPhylipFormat() {
        this.phylipFormat = this.PHYLIP_FORMAT_STRICT;
    }

    public void useRelaxedPhylipFormat() {
        this.phylipFormat = this.PHYLIP_FORMAT_RELAXED;
    }

    public boolean fillsHeterozygousIndelsWithN() {
        return fillHeterozygousIndelsWithN;
    }

    public void setFillHeterozygousIndelsWithN(boolean fillHeterozygousIndelsWithN) {
        this.fillHeterozygousIndelsWithN = fillHeterozygousIndelsWithN;
    }

    public int getSurroundingReferenceAmount() {
        return surroundingReferenceAmount;
    }

    public void setSurroundingReferenceAmount(int surroundingReferenceAmount) {
        this.surroundingReferenceAmount = surroundingReferenceAmount;
    }

}
