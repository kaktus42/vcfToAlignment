package de.kaktus42;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.variant.vcf.VCFFileReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        File varaintFile = new File(args[0]);
        File referenceFile = new File(args[1]);
        File outFile = new File(args[2]);

        if(!(varaintFile.exists() && varaintFile.canRead())) {
            System.err.println("ERROR: Cant open '" + args[0] + "' to read.");
            return;
        }
        IndexedFastaSequenceFile indexedReferenceFile;
        try {
            indexedReferenceFile = new IndexedFastaSequenceFile(referenceFile);
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Cant open '" + args[1] + "' to read.");
            return;
        }

        /*final SamReaderFactory factory =
                SamReaderFactory.makeDefault()
                        .enable(SamReaderFactory.Option.INCLUDE_SOURCE_IN_RECORDS, SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS)
                        .validationStringency(ValidationStringency.STRICT);*/
        //SamReader samReader = factory.open(samFile)

        VCFFileReader variantFileReader = new VCFFileReader(varaintFile, false);

        /*FileWriter outFileWriter;
        try {
            outFileWriter = new FileWriter(outFile);
        } catch (IOException e) {
            System.err.println("ERROR: Cant open '" + outFile.toURI() + "' to read.");
            return;
        }*/


        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(false, 4);
        converter.useRelaxedPhylipFormat();
        converter.convert(variantFileReader, indexedReferenceFile, outFile);

        /*try {
            outFileWriter.write(converter.getAlignment());
            outFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

    }
}
