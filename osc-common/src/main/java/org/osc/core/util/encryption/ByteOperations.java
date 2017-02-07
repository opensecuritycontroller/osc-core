package org.osc.core.util.encryption;

class ByteOperations {
    /**
     * Method that compares two byte arrays.
     * Prevents from timing attack, comparsion time is constant
     * @param a first byte array
     * @param b second byte array
     * @return true if both byte arrays are equal, false otherwise
     */
    public static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for(int i = 0; i < a.length && i < b.length; i++)
            diff |= a[i] ^ b[i];
        return diff == 0;
    }
}