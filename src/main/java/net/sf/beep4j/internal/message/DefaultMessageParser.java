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
package net.sf.beep4j.internal.message;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;

import net.sf.beep4j.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMessageParser implements MessageParser {

	private static final Logger LOG = LoggerFactory.getLogger(MessageParser.class);
	
	private static final BitSet fieldChars;
	
    static {
        fieldChars = new BitSet();
        for (int i = 0x21; i <= 0x39; i++) {
            fieldChars.set(i);
        }
        for (int i = 0x3b; i <= 0x7e; i++) {
            fieldChars.set(i);
        }
    }

	public Message parse(ByteBuffer buffer) {
		buffer.mark();
		int pos = 0;
		byte prev = 0;
		boolean armed = true;
		while (buffer.hasRemaining()) {
			byte current = buffer.get();
			if (prev == (byte) '\r' && current == (byte) '\n') {
				if (armed) {
					pos = buffer.position();
					break;
				}
				armed = true;
			} else if (current != '\r') {
				armed = false;
			}
			prev = current;
		}
		
		LOG.info("message body starts at offset " + buffer.position());
		
		ByteBuffer content = buffer.slice();
		
		buffer.reset();
		buffer.limit(pos);

		MessageHeader header = parseHeader(buffer);
		
		return new DefaultMessage(header, content);
	}
	
    private MessageHeader parseHeader(ByteBuffer buffer) {
		StringBuffer sb = new StringBuffer();
		
		Charset charset = Charset.forName("US-ASCII");
		CharBuffer chars = charset.decode(buffer);
		sb.append(chars);

		int start = 0;
		int pos = 0;

		MessageHeader header = new MessageHeader();
		
		while (pos < sb.length()) {
			while (pos < sb.length() && sb.charAt(pos) != '\r') {
				pos++;
			}
			if (pos < sb.length() - 1 && sb.charAt(pos + 1) != '\n') {
				pos++;
				continue;
			}

			if (pos >= sb.length() - 2 || fieldChars.get(sb.charAt(pos + 2))) {

				/*
				 * field should be the complete field data excluding the 
				 * trailing \r\n.
				 */
				String field = sb.substring(start, pos);
				start = pos + 2;

				/*
				 * Check for a valid field.
				 */
				int index = field.indexOf(':');
				boolean valid = false;
				if (index != -1 && fieldChars.get(field.charAt(0))) {
					valid = true;
					String fieldName = field.substring(0, index).trim();
					for (int i = 0; i < fieldName.length(); i++) {
						if (!fieldChars.get(fieldName.charAt(i))) {
							valid = false;
							break;
						}
					}

					if (valid) {
						header.addHeader(fieldName, field.substring(index + 1));
					}
				}
			}

			pos += 2;
		}

		return header;
	}

}
