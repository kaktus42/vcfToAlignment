package de.kaktus42;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.variant.vcf.VCFFileReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void printUsage() {
        System.out.print("usage: " + Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        System.out.print(" <VARIANTS> <REFERENCE> <OUT>");
        System.out.println();
        System.out.println("   <VARIANTS>  Input variant file name");
        System.out.println("   <REFERENCE> Input reference file name");
        System.out.println("   <OUT>       Output phylip file name");
    }

    public static void main(String[] args) {
        if(args.length != 3) {
            if(args.length > 0 && args[0].contains("-version")) {
                System.out.println("version: " + Main.class.getPackage().getImplementationVersion());
            }
            printUsage();
            return;
        }

        File varaintFile = new File(args[0]);
        File referenceFile = new File(args[1]);
        File outFile = new File(args[2]);

        if(!(varaintFile.exists() && varaintFile.canRead())) {
            System.err.println("ERROR: Cant open '" + args[0] + "' to read.");
            printUsage();
            return;
        }
        IndexedFastaSequenceFile indexedReferenceFile;
        try {
            indexedReferenceFile = new IndexedFastaSequenceFile(referenceFile);
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Cant open '" + args[1] + "' to read.");
            printUsage();
            return;
        }

        /*final SamReaderFactory factory =
                SamReaderFactory.makeDefault()
                        .enable(SamReaderFactory.Option.INCLUDE_SOURCE_IN_RECORDS, SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS)
                        .validationStringency(ValidationStringency.STRICT);*/
        //SamReader samReader = factory.open(samFile)

        VCFFileReader variantFileReader = new VCFFileReader(varaintFile, false);

        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(false, 4);
        converter.useRelaxedPhylipFormat();
        try {
            converter.convert(variantFileReader, indexedReferenceFile, outFile);
        } catch (IOException e) {
            System.err.println("ERROR: Could not open output file");
            Main.printUsage();
        }
    }
}
