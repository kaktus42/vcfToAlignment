package de.kaktus42;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.variant.vcf.VCFFileReader;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) {

        File varaintFile = new File(args[0]);
        File referenceFile = new File(args[1]);

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

        VcfToAlignmentConverter converter = new VcfToAlignmentConverter(false, 4);
        converter.convert(variantFileReader, indexedReferenceFile);

    }
}
