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
package net.sf.beep4j.internal.tcp;

import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.internal.util.ByteUtil;

/**
 * Object representation of a SEQ header as used by the TCP transport
 * mapping defined in RFC 3081.
 * 
 * @author Simon Raess
 */
public class SEQHeader {
	
	public static final String TYPE = "SEQ";
	
	private int channel;
	
	private long acknowledgeNumber;
	
	private int windowSize;
	
	public SEQHeader(int channel, long acknowledgeNumber, int windowSize) {
		this.channel = channel;
		this.acknowledgeNumber = acknowledgeNumber;
		this.windowSize = windowSize;
	}
	
	public SEQHeader(String[] tokens) {
		if (tokens.length != 4) {
			throw new ProtocolException("header must consist of 4 tokens");
		}
		if (!TYPE.equals(tokens[0])) {
			throw new ProtocolException("unkown header type: " + tokens[0]);
		}
		this.channel = ByteUtil.parseUnsignedInt("channel", tokens[1]);
		this.acknowledgeNumber = ByteUtil.parseUnsignedLong("acknowledge number", tokens[2]);
		this.windowSize = ByteUtil.parseUnsignedInt("window size", tokens[3]);
	}
	
	public final String getType() {
		return TYPE;
	}
	
	public int getPayloadSize() {
		return 0;
	}

	public long getAcknowledgeNumber() {
		return acknowledgeNumber;
	}

	public int getChannel() {
		return channel;
	}

	public int getWindowSize() {
		return windowSize;
	}
	
	public String[] getTokens() {
		return new String[] {
			"SEQ", "" + getChannel(), "" + getAcknowledgeNumber(), "" + getWindowSize()
		};
	}
	
	@Override
	public String toString() {
		return "SEQ " + channel + " " + acknowledgeNumber + " " + windowSize;
	}
	
}
