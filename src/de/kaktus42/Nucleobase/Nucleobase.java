package de.kaktus42.Nucleobase;

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

public class Nucleobase
{
    private char base;
    private String groupRepresentation;

    static final String translationMatrixIdx = "GACTRYWSMKHBVDN";
    static final char[][] translationMatrix =
       {{'G','R','S','K','R','B','D','S','V','K','N','B','V','D','N'},
        {'R','A','M','W','R','H','W','V','M','D','H','N','V','D','N'},
        {'S','M','C','Y','V','Y','H','S','M','B','H','B','V','N','N'},
        {'K','W','Y','T','D','Y','W','B','H','K','H','B','N','D','N'},
        {'R','R','V','D','R','N','D','V','V','D','N','N','V','D','N'},
        {'B','H','Y','Y','N','Y','H','B','H','B','H','B','N','N','N'},
        {'D','W','H','W','D','H','W','N','H','D','H','N','N','D','N'},
        {'S','V','S','B','V','B','N','S','V','B','N','B','V','N','N'},
        {'V','M','M','H','V','H','H','V','M','N','H','N','V','N','N'},
        {'K','D','B','K','D','B','D','B','N','K','N','B','N','D','N'},
        {'N','H','H','H','N','H','H','N','H','N','H','N','N','N','N'},
        {'B','N','B','B','N','B','N','B','N','B','N','B','N','N','N'},
        {'V','V','V','N','V','N','N','V','V','N','N','N','V','N','N'},
        {'D','D','N','D','D','N','D','N','N','D','N','N','N','D','N'},
        {'N','N','N','N','N','N','N','N','N','N','N','N','N','N','N'}};

    static String createGroupRepresentation(Nucleobase a) {
        return createGroupRepresentation(a.base);
    }

    static String createGroupRepresentation(char a) {
        a = String.valueOf(a).toUpperCase().charAt(0);
        StringBuilder groupRepresentationBuilder = new StringBuilder();
        if("ARWMHVDN".indexOf(a) != -1) {
            groupRepresentationBuilder.append('A');
        }
        if("CYSMHBVN".indexOf(a) != -1) {
            groupRepresentationBuilder.append('C');
        }
        if("GRSKBVDN".indexOf(a) != -1) {
            groupRepresentationBuilder.append('G');
        }
        if("TYWKHBDN".indexOf(a) != -1) {
            groupRepresentationBuilder.append('T');
        }
        return groupRepresentationBuilder.toString();
    }

    public Nucleobase(char base) throws NucelobaseException {
        base = String.valueOf(base).toUpperCase().charAt(0);
        if(translationMatrixIdx.indexOf(base) == -1) {
            throw new NucelobaseException("Nucleobase is not one of [" + translationMatrixIdx + "]");
        }
        this.base = base;
        this.groupRepresentation = createGroupRepresentation(this.base);
    }

    public Nucleobase(Character base) throws NucelobaseException {
        this(base.charValue());
    }


    /**
     * merges the nucleobase with another nucleobase according to the unspecific nucleobase alphabet
     *
     * @param otherBase Nucleobase to combineWith with
     */
    public void merge(Nucleobase otherBase) {
        if(otherBase == null) return;
        this.base =
                translationMatrix[translationMatrixIdx.indexOf(this.base)]
                                 [translationMatrixIdx.indexOf(otherBase.base)];
        this.groupRepresentation = createGroupRepresentation(this.base);
    }

    public boolean represents(char base) {
        return this.represents(new Nucleobase(base));
    }

    public boolean represents(Character base) {
        return this.represents(new Nucleobase(base));
    }

    public boolean represents(Nucleobase otherBase) {
        if(otherBase == null) return false;
        if(this.base == otherBase.base) {
            return true;
        }

        for(char charBase : otherBase.groupRepresentation.toCharArray()) {
            if(this.groupRepresentation.indexOf(charBase) == -1) {
                return false;
            }
        }

        return true;
    }

    public boolean equals(char o) {
        return this.equals(new Nucleobase(o));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nucleobase || o instanceof Character)) return false;

        if(o instanceof Character) {
            return this.equals(((Character) o).charValue());
        }

        Nucleobase that = (Nucleobase) o;

        return this.base == that.base;
    }

    @Override
    public int hashCode() {
        return this.base;
    }

    public String toString() {
        return String.valueOf(this.base);
    }

/*
     *
     * merges the nucleobase with another nucleobase according to the unspecific nucleobase alphabet
     *
     * @param otherBase Nucleobase to combineWith with
     *
    public void combineWith(Nucleobase otherBase) {
        if(this.equals(otherBase)) {
            return;
        }
        for(char c : otherBase.groupRepresentation.toCharArray()) {
            try {
                this.mergeSingle(new Nucleobase(c));
            } catch (NucelobaseException e) {
                e.printStackTrace();
            }
        }
    }

     *
     * only takes A C G T
     * @param otherBase A, C, G or T
     *
    public void mergeSingle(Nucleobase otherBase) {
        if(otherBase.groupRepresentation.length() != 1) {
            throw new RuntimeException();
        }
        if(this.groupRepresentation.indexOf(otherBase.base) != -1) {
            return;
        }

        ArrayList<Character> cArr = new ArrayList<Character>();
        for(char c : this.groupRepresentation.toCharArray()) {
            cArr.add(c);
        }
        cArr.add(otherBase.base.charValue());
        Collections.sort(cArr);
        StringBuilder sb = new StringBuilder();
        for(Character c : cArr) sb.append(c);

        this.groupRepresentation = sb.toString();

        if(this.groupRepresentation.equals("AG")) this.base = 'R';
        if(this.groupRepresentation.equals("CT")) this.base = 'Y';
        if(this.groupRepresentation.equals("AT")) this.base = 'W';
        if(this.groupRepresentation.equals("CG")) this.base = 'S';
        if(this.groupRepresentation.equals("AC")) this.base = 'M';
        if(this.groupRepresentation.equals("GT")) this.base = 'K';

        if(this.groupRepresentation.equals("ACT")) this.base = 'H';
        if(this.groupRepresentation.equals("CGT")) this.base = 'B';
        if(this.groupRepresentation.equals("ACG")) this.base = 'V';
        if(this.groupRepresentation.equals("AGT")) this.base = 'D';

        if(this.groupRepresentation.equals("ACGT")) this.base = 'N';

        //System.out.println(sb.toString() + " " + this.base);
    }

    public static void createTable() {
        try {
            String allBases = "GACTRYWSMKHBVDN";
            for(int i = 0; i < allBases.length(); ++i) {
                Nucleobase nbA = new Nucleobase(allBases.charAt(i));
                for(int j = 0; j <= i; ++j) {
                    Nucleobase nbB = new Nucleobase(allBases.charAt(j));
                    nbB.combineWith(nbA);
                    System.out.print(nbB.toString());
                    System.out.print(" ");
                }
                System.out.println();
            }
        } catch (NucelobaseException e) {
            e.printStackTrace();
        }
    }

*/
}
