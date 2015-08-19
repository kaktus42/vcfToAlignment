package de.kaktus42;

import de.kaktus42.Nucleobase.NucleobaseSequence;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;

import java.util.HashMap;
import java.util.Map;

public class VcfToAlignmentConverter
{
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
     *
     */
    private String refSeqName = "refSeq";

    private HashMap<String, StringBuilder> alignmentBuilders;

    public VcfToAlignmentConverter() {
        this(false, 4);
    }

    public VcfToAlignmentConverter(boolean fillHeterozygousIndelsWithN, int surroundingReferenceAmount) {
        this.fillHeterozygousIndelsWithN = fillHeterozygousIndelsWithN;
        this.surroundingReferenceAmount = surroundingReferenceAmount;
    }

    public void convert(VCFFileReader varaintFileReader,
                        IndexedFastaSequenceFile indexedReferenceFile){
        VCFHeader vcfHeader = varaintFileReader.getFileHeader();

        // Determine longest sample name for formatting
        // Initialize alignmentBuilders
        alignmentBuilders = new HashMap<>();
        int longestSampleNameLength = refSeqName.length();
        alignmentBuilders.put(refSeqName, new StringBuilder());
        for(String sampleName : vcfHeader.getGenotypeSamples()) {
            if(sampleName.length() > longestSampleNameLength) {
                longestSampleNameLength = sampleName.length();
            }
            alignmentBuilders.put(sampleName, new StringBuilder());
        }
        for(Map.Entry<String, StringBuilder> entry : alignmentBuilders.entrySet()) {
            entry.getValue().append(Util.paddedString(entry.getKey(), longestSampleNameLength, ' ')).append('\t');
        }

        String basesAfter = "";
        String lastContig = null;
        int lastPosition = -1;
        int lastSurroundingReferenceAmountAfter = 0;
        for(VariantContext variantContext : varaintFileReader) {

            // When changing the chromosome, reset everything and safely append rest of sequence
            if(lastContig == null || !lastContig.equals(variantContext.getContig())) {
                lastContig = variantContext.getContig();
                lastPosition = -1;
                lastSurroundingReferenceAmountAfter = 0;
            }

            // When two varaints overlap - is that allowed in VCF format?
            if(lastPosition >= variantContext.getStart()) {
                throw new ConverterException("ERROR: Following variant overlaps the previous variant:\n" + variantContext);
            }

            int surroundingReferenceAmountBefore = variantContext.getStart() - surroundingReferenceAmount < 0 ? variantContext.getStart() - 1 : surroundingReferenceAmount;
            if(lastPosition + lastSurroundingReferenceAmountAfter >= variantContext.getStart()) {
                // When two consecutive varaints overlap respective their surroundings
                // ignore basesAfter and calculate new surroundingReferenceAmountBefore
                surroundingReferenceAmountBefore = variantContext.getStart() - lastPosition - 1;
            } else {
                // safely append surrounding reference sequence after
                appendToAllSequences(basesAfter);
            }

            // get parts of the reference sequence around the variant site
            // take care of edge cases
            ReferenceSequence contigSequence = indexedReferenceFile.getSequence(variantContext.getContig());
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


            System.out.println(variantContext);
            System.out.println(variantLength);
            System.out.format("%s\t%s%s%s\n",
                    Util.paddedString("", longestSampleNameLength, ' '),
                    Util.paddedString("", basesBefore.length() + alignmentBuilders.get(refSeqName).length() - (longestSampleNameLength+1), ' '),
                    Util.paddedString("", multipleAlignmentWindowSize, '_'),
                    Util.paddedString("", basesAfter.length(), ' '));

            alignmentBuilders.get(refSeqName).append(basesBefore)
                    .append(Util.paddedBaseString(referenceBase, multipleAlignmentWindowSize));

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
                        .append(Util.paddedBaseString(ns.toString(), multipleAlignmentWindowSize));
            }

            System.out.println(this.getAlignment());
        }
        appendToAllSequences(basesAfter);
    }

    public String getAlignment() {
        StringBuilder alignmentBuilder = new StringBuilder();
        for(StringBuilder stringBuilder : alignmentBuilders.values()) {
            alignmentBuilder.append(stringBuilder).append('\n');
        }
        return alignmentBuilder.toString();
    }

    private void appendToAllSequences(String string) {
        for(StringBuilder stringBuilder : alignmentBuilders.values()) {
            stringBuilder.append(string);
        }
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
