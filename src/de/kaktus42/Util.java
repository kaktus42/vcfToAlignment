package de.kaktus42;

public class Util
{
    public static String rightPaddedBaseString(String bases, int length)
    {
        return rightPaddedString(bases, length, '-');
    }

    public static String rightPaddedString(String string, int length, char paddingChar) {
        if(string.length() > length) {
            System.err.println("WARNING: String to pad is longer than expected length. Return original string.");
            return string;
        }
        if(string.length() == length) return string;
        return string.concat(String.format("%0" + (length - string.length()) + "d", 0).replace('0', paddingChar));
    }

    public static String leftPaddedString(String string, int length, char paddingChar) {
        if(string.length() > length) {
            System.err.println("WARNING: String to pad is longer than expected length. Return original string.");
            return string;
        }
        if(string.length() == length) return string;
        return String.format("%0" + (length - string.length()) + "d", 0).replace('0', paddingChar).concat(string);
    }
}
