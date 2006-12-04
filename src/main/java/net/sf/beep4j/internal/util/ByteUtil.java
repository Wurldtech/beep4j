/*
 *  Copyright 2006 Simon Raess
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.beep4j.internal.util;

import java.io.UnsupportedEncodingException;

import net.sf.beep4j.ProtocolException;

/**
 * Utility class for parsing stuff.
 * 
 * @author Simon Raess
 */
public final class ByteUtil {
	
	private ByteUtil() { }
	
	public static final byte[] toASCII(String s) {
		try {
			return s.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static final String base64Encode(byte[] data) {
		Base64Encoder encoder = new Base64Encoder();
		return encoder.encode(data);
	}
	
	/**
	 * Parses a String field that must be in the range 0..2147483647.
	 * 
	 * @param field the name of the field
	 * @param s the String to parse
	 * @return the value returned as an int
	 * @throws ProtocolException if the parsed value does not conform to the 
	 *                           expected format and range
	 */
	public static final int parseUnsignedInt(String field, String s) {
		try {
			int result = Integer.parseInt(s);
			if (result < 0 || result > 2147483647) {
				throw new ProtocolException(field + " must be in range 0..2147483647");
			}
			return result;
		} catch (NumberFormatException e) {
			throw new ProtocolException(field + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * Parses a String field that must be in the range 0..4294967295.
	 * 
	 * @param field the name of the field
	 * @param s the String to parse
	 * @return the value returned as a long
	 * @throws ProtocolException if the parsed value does not conform to the 
	 *                           expected format and range
	 */
	public static final long parseUnsignedLong(String field, String s) {
		try {
			long result = Long.parseLong(s);
			if (result < 0 || result > 4294967295L) {
				throw new ProtocolException(field + " must be in range 0..4294967295");
			}
			return result;
		} catch (NumberFormatException e) {
			throw new ProtocolException(field + ": " + e.getMessage(), e);
		}
	}
	
}
