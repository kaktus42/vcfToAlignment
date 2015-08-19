package de.kaktus42;

public class Util
{
    public static String paddedBaseString(String bases, int length)
    {
        return paddedString(bases, length, '-');
    }

    public static String paddedString(String string, int length, char paddingChar) {
        if(string.length() > length) {
            System.err.println("WARNING: String to pad is longer than expected length. Return original string.");
            return string;
        }
        if(string.length() == length) return string;
        return String.format("%s%0" + (length - string.length()) + "d", string, 0).replace('0', paddingChar);
    }
}
