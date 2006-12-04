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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public final class HexDump {
	
	public static final String dump(ByteBuffer buffer) {
		StringWriter writer = new StringWriter();
		dump(buffer, writer);
		return writer.getBuffer().toString();
	}
	
	public static final void dump(ByteBuffer buffer, OutputStream out) {
		Writer writer = new OutputStreamWriter(out, Charset.forName("US-ASCII"));
		dump(buffer, writer);
	}
	
	public static final void dump(ByteBuffer buffer, Writer w) {
		PrintWriter writer = new PrintWriter(w);
		buffer = buffer.asReadOnlyBuffer();
		
		int remaining = buffer.remaining();
		byte[] lineBuffer = new byte[16];
		
		for (int i = 0; i < remaining; i += 16) {
			int length = Math.min(16, buffer.remaining());
			buffer.get(lineBuffer, 0, length);
			
			for (int j = 0; j < length; j++) {
				String hex = Integer.toHexString(lineBuffer[j]);
				hex = hex.length() == 1 ? "0" + hex : hex;
				writer.write(hex + " ");
			}
			
			for (int j = length; j < 16; j++) {
				writer.write("   ");
			}
			
			writer.write("   ");
			
			for (int j = 0; j < length; j++) {
				writer.write(asChar(lineBuffer[j]) + " ");
			}
			
			writer.write("\r\n");
		}
		
		writer.flush();
	}
	
	private static final char asChar(byte b) {
		if (b > 0x20) {
			return (char) b;
		} else {
			return '.';
		}
	}
	
}
