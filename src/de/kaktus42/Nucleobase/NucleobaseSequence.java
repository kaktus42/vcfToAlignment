package de.kaktus42.Nucleobase;

import java.util.ArrayList;

/**
 * G	G				Guanin
 * A	A				Adenin
 * C	C				Cytosin
 * T	T				Thymin
 * R	A oder G		Purine
 * Y	C oder T		Pyrimidine
 * W	A oder T		engl. weak (schwache) Wasserstoffbrückenbindungen
 * S	G oder C		engl. strong (starke) Wasserstoffbrückenbindungen
 * M	A oder C		Aminogruppe
 * K	G oder T		Ketogruppe
 * H	A, C, oder T	nicht G, (H folgt im Alphabet auf G)
 * B	G, C, oder T	nicht A, (B folgt im Alphabet auf A)
 * V	G, A, oder C	nicht T (U), (V folgt im Alphabet auf U)
 * D	G, A, oder T	nicht C, (D folgt im Alphabet auf C)
 * N	G, A, C oder T	engl. any (alle Nukleotide möglich)
 */

public class NucleobaseSequence implements Cloneable
{
    protected ArrayList<Nucleobase> sequence;
    private String sequenceStringRepresentation;

    public NucleobaseSequence(String sequenceString) {
        sequenceString = sequenceString.toUpperCase();
        if(!sequenceString.matches("[" + Nucleobase.translationMatrixIdx + "]*")) {
            throw new NucelobaseException("NucleobaseSequence contains not only [" + Nucleobase.translationMatrixIdx + "]");
        }
        this.sequence = new ArrayList<>();
        for(char base : sequenceString.toCharArray()) {
            this.sequence.add(new Nucleobase(base));
        }
        updateStringRepresentation();
    }

    public NucleobaseSequence(NucleobaseSequence nucleobaseSequence) {
        this.sequence = nucleobaseSequence.sequence;
        updateStringRepresentation();
    }

    public NucleobaseSequence(ArrayList<Nucleobase> sequence) {
        this.sequence = sequence;
        updateStringRepresentation();
    }

    public void appendFront(String sequenceString) {
        this.appendFront(new NucleobaseSequence(sequenceString), true);
    }

    public void appendFront(NucleobaseSequence nucleobaseSequence) {
        this.appendFront(nucleobaseSequence, true);
    }

    private void appendFront(NucleobaseSequence nucleobaseSequence, boolean update) {
        int index = 0;
        for(Nucleobase nucleobase : nucleobaseSequence.sequence) {
            this.sequence.add(index++, nucleobase);
        }
        if(update) updateStringRepresentation();
    }

    public void appendBack(String sequenceString) {
        this.appendBack(new NucleobaseSequence(sequenceString), true);
    }

    public void appendBack(NucleobaseSequence nucleobaseSequence) {
        this.appendBack(nucleobaseSequence, true);
    }

    private void appendBack(NucleobaseSequence nucleobaseSequence, boolean update) {
        for(Nucleobase nucleobase : nucleobaseSequence.sequence) {
            this.sequence.add(nucleobase);
        }
        if(update) updateStringRepresentation();
    }

    public void combineWith(String sequenceString) {
        combineWith(new NucleobaseSequence(sequenceString), 0);
    }

    public void combineWith(String sequenceString, int offset) {
        combineWith(new NucleobaseSequence(sequenceString), offset);
    }

    public void combineWith(NucleobaseSequence nucleobaseSequence) {
        combineWith(nucleobaseSequence, 0);
    }

    public void combineWith(NucleobaseSequence nucleobaseSequence, int offset) {
        int overlapFront = offset < 0 ? -offset : 0;
        int overlapBack = nucleobaseSequence.length() - this.length() + offset;
        if(overlapBack < 0) overlapBack = 0;
        if(overlapFront > nucleobaseSequence.length()) {
            throw new NucelobaseException(nucleobaseSequence + ".combineWith(" + this + ", " + offset + ") would create gaps in the front.");
        }
        if(overlapBack > nucleobaseSequence.length()){
            throw new NucelobaseException(nucleobaseSequence + ".combineWith(" + this + ", " + offset + ") would create gaps in the back.");
        }
        NucleobaseSequence mergeSeq = nucleobaseSequence.subSequence(overlapFront, overlapBack > 0 ? nucleobaseSequence.length() - overlapBack : nucleobaseSequence.length());

        int shift = offset > 0 ? offset : 0;
        for(int i = 0; i < mergeSeq.length(); ++i) {
            this.sequence.get(i + shift).merge(mergeSeq.sequence.get(i));
        }

        if(overlapFront > 0) {
            this.appendFront(nucleobaseSequence.subSequence(0, overlapFront), false);
        }
        if(overlapBack > 0) {
            this.appendBack(nucleobaseSequence.subSequence(nucleobaseSequence.length() - overlapBack), false);
        }

        updateStringRepresentation();
    }

    private void updateStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        for(Nucleobase n : this.sequence) {
            sb.append(n);
        }
        this.sequenceStringRepresentation = sb.toString();
    }

    public NucleobaseSequence subSequence(int begin) {
        return subSequence(begin, this.sequenceStringRepresentation.length());
    }

    public NucleobaseSequence subSequence(int begin, int end) {
        return new NucleobaseSequence(this.sequenceStringRepresentation.substring(begin, end));
    }

    public String toString() {
        return this.sequenceStringRepresentation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NucleobaseSequence || o instanceof String)) return false;

        if(o instanceof String) {
            return this.sequenceStringRepresentation.equals(o);
        }

        NucleobaseSequence that = (NucleobaseSequence) o;
        return this.sequenceStringRepresentation.equals(that.sequenceStringRepresentation);
    }

    @Override
    protected Object clone() {
        return new NucleobaseSequence(this.sequence);
    }

    public int length() {
        return sequence.size();
    }
}
