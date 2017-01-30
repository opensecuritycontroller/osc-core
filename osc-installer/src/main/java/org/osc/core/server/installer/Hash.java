package org.osc.core.server.installer;

import java.util.Arrays;

public final class Hash {

	private final static char[] hexArray = "0123456789abcdef".toCharArray();

	private final String algo;
	private final byte[] bytes;

	public Hash(String algo, byte[] bytes) {
		this.algo = algo;
		this.bytes = bytes;
	}

	public String getAlgo() {
		return this.algo;
	}

	public byte[] getBytes() {
		return this.bytes;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append(this.algo)
				.append(':')
				.append(bytesToHex(this.bytes))
				.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.algo == null) ? 0 : this.algo.hashCode());
		result = prime * result + Arrays.hashCode(this.bytes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
            return true;
        }
		if (obj == null) {
            return false;
        }
		if (getClass() != obj.getClass()) {
            return false;
        }
		Hash other = (Hash) obj;
		if (this.algo == null) {
			if (other.algo != null) {
                return false;
            }
		} else if (!this.algo.equals(other.algo)) {
            return false;
        }
		if (!Arrays.equals(this.bytes, other.bytes)) {
            return false;
        }
		return true;
	}

	private static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
