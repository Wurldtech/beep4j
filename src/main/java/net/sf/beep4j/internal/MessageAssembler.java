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
package net.sf.beep4j.internal;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.beep4j.Message;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.internal.DataHeader.ANSHeader;
import net.sf.beep4j.internal.message.DefaultMessageParser;
import net.sf.beep4j.internal.message.MessageParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageAssembler assembles fragmented frames into a Message.
 * The assembled Messages are passed to a MessageHandler.
 * 
 * @author Simon Raess
 */
public class MessageAssembler implements FrameHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(MessageAssembler.class);
	
	private final MessageHandler handler;
	
	private State state;

	public MessageAssembler(MessageHandler handler) {
		this.handler = handler;
	}

	
	// --> start of FrameHandler methods <--
	
	public void handleFrame(Frame frame) {
		LOG.info("got frame: " + frame.getHeader());
		
		if (state == null) {
			MessageType type = frame.getHeader().getType();
			if (MessageType.ANS == type || MessageType.NUL == type) {
				LOG.info("moving to ANS state");
				state = new AnsState();
			} else {
				LOG.info("moving to normal state");
				state = new NormalState();
			}
		}
		
		// pass on to the state
		state.append(frame, handler);
	}
	
	// --> end of FrameHandler methods <--
	
	
	protected Message createMessage(List<Frame> frames) {
		if (frames.size() == 0) {
			throw new IllegalArgumentException("cannot create message from 0 fragments");
		}
		
		LOG.info("creating message from " + frames.size() + " frames");
		
		int total = 0;
		for (Frame frame : frames) {
			long check = total + frame.getSize();
			if (check > Integer.MAX_VALUE) {
				throw new ProtocolException("total message length is longer "
						+ "than supported: " + check);
			}
			total += frame.getPayload().remaining();
		}
		
		LOG.info("total payload size is " + total);
		
		ByteBuffer buffer = ByteBuffer.allocate(total);
		for (Frame frame : frames) {
			buffer.put(frame.getPayload());
		}
		buffer.flip();
		
		MessageParser parser = new DefaultMessageParser();
		return parser.parse(buffer);
	}

	protected void receive(MessageType type, int channelNumber, int messageNumber, Message message) {
		if (MessageType.ERR == type) {
			handler.receiveERR(channelNumber, messageNumber, message);
		} else if (MessageType.MSG == type) {
			handler.receiveMSG(channelNumber, messageNumber, message);
		} else if (MessageType.RPY == type) {
			handler.receiveRPY(channelNumber, messageNumber, message);
		} else {
			throw new IllegalArgumentException("unkown type: " + type);
		}
	}
	
	protected void receive(int channelNumber, int messageNumber, int answerNumber, Message message) {
		handler.receiveANS(channelNumber, messageNumber, answerNumber, message);
	}
	
	private static interface State {
		void append(Frame frame, MessageHandler handler);
	}
	
	private class NormalState implements State {
		private List<Frame> fragments;
		private DataHeader last;
		
		private NormalState() { 
			this.fragments = new LinkedList<Frame>();
		}
		
		private boolean hasPreviousFrame() {
			return last != null;
		}
		
		public void append(Frame frame, MessageHandler handler) {
			DataHeader header = (DataHeader) frame.getHeader();
			MessageType type = header.getType();
			
			if (hasPreviousFrame()) {
				validateMessageNumber(header);
				validateMatchingFragmentTypes(last.getType(), type);
			}
			
			fragments.add(frame);
			
			if (header.isIntermediate()) {
				last = (DataHeader) frame.getHeader();
			} else {
				LOG.info("got complete message with " + fragments.size() + " fragments");
				last = null;
				List<Frame> copy = new LinkedList<Frame>(fragments);
				fragments.clear();
				state = null;
				receive(type, frame.getChannelNumber(), frame.getMessageNumber(), createMessage(copy));
			}
		}

		/*
		 * Validation of sequencing according to the BEEP specification section
		 * 2.2.1.1.
		 * 
		 * A frame is poorly formed, if the continuation indicator of the 
		 * previous frame received on the same channel 
		 * was intermediate ("*"), and its message number isn't identical to this frame's 
		 * message number.
		 */
		private void validateMessageNumber(DataHeader header) {
			if (last.getMessageNumber() != header.getMessageNumber()) {
				throw new ProtocolException("message number for fragments does not match: was "
						+ header.getMessageNumber() + ", should be " 
						+ last.getMessageNumber());
			}
		}
		
		/*
		 * Validation of sequencing according to the BEEP specification section
		 * 2.2.1.1.
		 * 
		 * A frame is poorly formed if the header starts with "MSG", "RPY", "ERR", 
		 * or "ANS", and refers to a message number for which at least one other 
		 * frame has been received, and the three-character keyword starting this 
		 * frame and the immediately-previous received frame for this message 
		 * number are not identical
		 */
		private void validateMatchingFragmentTypes(MessageType last, MessageType current) {
			if (MessageType.ERR == current
					|| MessageType.MSG == current
					|| MessageType.RPY == current) {
				if (!last.equals(current)) {
					throw new ProtocolException("header type does not match: expected "
							+ last + " but was " + current);
				}
			}
		}
	}
	
	private class AnsState implements State {
		private Map<Integer, List<Frame>> fragments;
		private int messageNumber = -1;
		
		private AnsState() {
			this.fragments = new HashMap<Integer, List<Frame>>();
		}
		
		public void append(Frame frame, MessageHandler handler) {
			MessageType type = frame.getType();
			
			if (messageNumber == -1) {
				messageNumber = frame.getMessageNumber();
			} else {
				validateMessageNumber(frame.getHeader());
			}
			
			if (MessageType.ANS == type) {
				ANSHeader header = (ANSHeader) frame.getHeader();
				List<Frame> frames = fragments.get(header.getAnswerNumber());
				if (frames == null) {
					frames = new LinkedList<Frame>();
					fragments.put(header.getAnswerNumber(), frames);
				}
				frames.add(frame);
				if (!header.isIntermediate()) {
					fragments.remove(header.getAnswerNumber());
					receive(frame.getChannelNumber(), 
							frame.getMessageNumber(), 
							header.getAnswerNumber(),
							createMessage(frames));				
				}
				
			} else if (MessageType.NUL == type) {
				if (hasUnfinishedAnsMessages()) {
					// Validation of sequencing according to the BEEP specification section
					// 2.2.1.1.
					//  
					// A frame is poorly formed if the header starts with "NUL", and refers to 
					// a message number for which at least one other frame has been received, 
					// and the keyword of of the immediately-previous received frame for 
					// this reply isn't "ANS".
					
					// TODO: use proper exceptions
					throw new ProtocolException("unfinished ANS messages");
				} else if (frame.isIntermediate()) {
					throw new ProtocolException("NUL reply's continuation indicator is '*'");
				} else if (frame.getSize() != 0) {
					throw new ProtocolException("NUL reply's payload size is non-zero ("
							+ frame.getSize() + ")");
				}
				
				fragments.clear();
				state = null;
				handler.receiveNUL(frame.getChannelNumber(), frame.getMessageNumber());
				
			} else {
				throw new ProtocolException("expected ANS or NUL message, was " + type.name());
			}			
		}

		/*
		 * Validation of sequencing according to the BEEP specification section
		 * 2.2.1.1.
		 * 
		 * A frame is poorly formed, if the continuation indicator of the 
		 * previous frame received on the same channel 
		 * was intermediate ("*"), and its message number isn't identical to this frame's 
		 * message number.
		 */
		private void validateMessageNumber(DataHeader current) {
			if (messageNumber != current.getMessageNumber()) {
				throw new ProtocolException("message number for fragments does not match: was "
						+ current.getMessageNumber() + ", should be " 
						+ messageNumber);
			}
		}
				
		private boolean hasUnfinishedAnsMessages() {
			return fragments.size() > 0;
		}
		
	}
	
}
