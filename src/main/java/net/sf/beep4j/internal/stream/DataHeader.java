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
package net.sf.beep4j.internal.stream;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.internal.util.ByteUtil;

/**
 * Represents a data header as specified by the BEEP specification.
 * 
 * @author Simon Raess
 */
public class DataHeader {
	
	private static final String EOL = "\r\n";

	private static final char FINAL = '.';

	private static final char INTERMEDIATE = '*';

	private static final String SPACE = " ";

	private static final Charset charset = Charset.forName("US-ASCII");
	
	/**
	 * The message type of the frame.
	 */
	protected MessageType type;
	
	/**
	 * The size of the frame's payload.
	 */
	protected int payloadSize;
	
	/**
	 * The channel number of the frame.
	 */
	protected int channel;
	
	/**
	 * The message number of the frame.
	 */
	protected int messageNumber;
	
	/**
	 * Whether this is an intermediate frame.
	 */
	protected boolean intermediate;
	
	/**
	 * The sequence number of the first byte in the frame's payload.
	 */
	protected long sequenceNumber;
	
	/**
	 * Creates a new DataHeader.
	 * 
	 * @param type the message type of the frame
	 * @param channel the channel number, must be greater or equal than zero
	 * @param messageNumber the message number of the frame
	 * @param intermediate whether this is an intermediate frame
	 * @param sequenceNumber the sequence number of the frame
	 * @param size the payload size of the frame
	 */
	public DataHeader(MessageType type, int channel, int messageNumber, boolean intermediate, long sequenceNumber, int size) {
		Assert.notNull("type", type);
		this.type = type;
		this.channel = channel;
		this.messageNumber = messageNumber;
		this.intermediate = intermediate;
		this.sequenceNumber = sequenceNumber;
		this.payloadSize = size;
	}
	
	/**
	 * Parses the passed in tokenized header line into a DataHeader.
	 * 
	 * @param tokens the tokenized header line
	 * @return the parsed header
	 */
	public static final DataHeader parseHeader(String[] tokens) {
		if (tokens.length == 0) {
			throw new ProtocolException("header has 0 tokens");
		}
		
		MessageType type = parseMessageType(tokens[0]);
		
		if (type == MessageType.ANS && tokens.length != 7) {
			throw new ProtocolException("expecting 7 tokens in ANS header, was " + tokens.length);
		} else if (type != MessageType.ANS && tokens.length != 6) {
			throw new ProtocolException("expecting 6 tokens in header, was " + tokens.length);
		}
		
		int channel = ByteUtil.parseUnsignedInt("channel number", tokens[1]);
		int messageNumber = ByteUtil.parseUnsignedInt("message number", tokens[2]);
		boolean intermediate = parseIntermediate(tokens[3]);
		long sequenceNumber = ByteUtil.parseUnsignedLong("sequence number", tokens[4]);
		int payloadSize = ByteUtil.parseUnsignedInt("size", tokens[5]);
		
		if (MessageType.ANS == type) {
			int answerNumber = ByteUtil.parseUnsignedInt("answer number", tokens[6]);
			return new ANSHeader(channel, messageNumber, intermediate, sequenceNumber, payloadSize, answerNumber);
		} else {
			return new DataHeader(type, channel, messageNumber, intermediate, sequenceNumber, payloadSize);
		}
	}
	
	private static MessageType parseMessageType(String s) {
		try {
			return MessageType.valueOf(s);
		} catch (IllegalArgumentException e) {
			throw new ProtocolException("'" + s + "' is an invalid message type");
		}
	}
		
	private static boolean parseIntermediate(String s) {
		if ("*".equals(s)) {
			return true;
		} else if (".".equals(s)) {
			return false;
		} else {
			throw new ProtocolException("'" + s + "' is an invalid intermediate indicator");
		}
	}
	
	public MessageType getType() {
		return type;
	}

	public int getChannel() {
		return channel;
	}

	public int getMessageNumber() {
		return messageNumber;
	}

	public boolean isIntermediate() {
		return intermediate;
	}

	public int getPayloadSize() {
		return payloadSize;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}
	
	/**
	 * Splits the header into two parts. The first part's size is set
	 * to the passed in parameter. It has the intermediate flag set to
	 * true. The second part has the remaining
	 * size plus its sequence number adapted.
	 *  
	 * @param size the size of the first part
	 * @return an array of two elements
	 */
	public DataHeader[] split(int size) {
		MessageType type = getType();
		
		DataHeader[] result = new DataHeader[2];
		result[0] = new DataHeader(type, channel, messageNumber, true, sequenceNumber, size);
		result[1] = new DataHeader(type, channel, messageNumber, false, sequenceNumber + size, payloadSize - size);
		
		return result;
	}
	
	/**
	 * Converts the header into a ByteBuffer.
	 * 
	 * @return the converted ByteBuffer 
	 */
	public ByteBuffer asByteBuffer() {
		StringBuilder builder = new StringBuilder(type.name());
		
		builder.append(SPACE);
		builder.append(channel);
		builder.append(SPACE);
		builder.append(messageNumber);
		builder.append(SPACE);
		builder.append(intermediate ? INTERMEDIATE : FINAL);
		builder.append(SPACE);
		builder.append(sequenceNumber);
		builder.append(SPACE);
		builder.append(payloadSize);
		builder.append(EOL);
		
		return charset.encode(builder.toString());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (getClass() == obj.getClass()) {
			DataHeader header = (DataHeader) obj;
			return type == header.type
			    && channel == header.channel
			    && messageNumber == header.messageNumber
			    && intermediate == header.intermediate
			    && sequenceNumber == header.sequenceNumber
			    && payloadSize == header.payloadSize;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = result * 23 + type.hashCode();
		result = result * 23 + channel;
		result = result * 23 + messageNumber; 
		result = result * 23 + (intermediate ? 1 : 0);
		result = result * 23 + payloadSize;
		result = result * 23 + new Long(sequenceNumber).hashCode();
		return result;
	}
	
	@Override
	public String toString() {
		return type.name() + " " 
			 + channel + " "
			 + messageNumber + " " 
		     + (intermediate ? "*" : ".") + " " 
		     + sequenceNumber + " "
		     + payloadSize;
	}
	
	/**
	 * Header for messages of type ANS. This header type has an additional
	 * property <code>answerNumber</code>.
	 * 
	 * @author Simon Raess
	 */
	public static class ANSHeader extends DataHeader {
		
		private final int answerNumber;
		
		public ANSHeader(int channel, int messageNumber, boolean intermediate, long sequenceNumber, int payloadSize, int answerNumber) {
			super(MessageType.ANS, channel, messageNumber, intermediate, sequenceNumber, payloadSize);
			this.answerNumber = answerNumber;
		}
		
		public int getAnswerNumber() {
			return answerNumber;
		}
		
		@Override
		public DataHeader[] split(int size) {
			DataHeader[] result = new DataHeader[2];
			result[0] = new ANSHeader(channel, messageNumber, true, sequenceNumber, size, answerNumber);
			result[1] = new ANSHeader(channel, messageNumber, false, sequenceNumber + size, payloadSize - size, answerNumber);
			return result;
		}
		
		@Override
		public ByteBuffer asByteBuffer() {
			StringBuilder builder = new StringBuilder();
			
			builder.append(type.name());
			builder.append(SPACE);
			builder.append(channel);
			builder.append(SPACE);
			builder.append(messageNumber);
			builder.append(SPACE);
			builder.append(intermediate ? INTERMEDIATE : FINAL);
			builder.append(SPACE);
			builder.append(sequenceNumber);
			builder.append(SPACE);
			builder.append(payloadSize);
			builder.append(SPACE);
			builder.append(answerNumber);
			builder.append(EOL);
			
			return charset.encode(builder.toString());
		}		

		@Override
		public boolean equals(Object obj) {
			if (!super.equals(obj)) {
				return false;
			} else if (obj.getClass().equals(obj)) {
				ANSHeader header = (ANSHeader) obj;
				return answerNumber == header.answerNumber;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = result * 23 + answerNumber;
			return result;
		}
		
		 @Override
		public String toString() {
			 return super.toString() + " " + answerNumber;
		}
		 
	}
		
}
