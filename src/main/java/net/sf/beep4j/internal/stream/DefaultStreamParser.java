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
import java.util.Arrays;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of a BEEP {@link StreamParser}. Parses the stream of
 * bytes as it arrives from the remote peer and passes the resulting BEEP
 * frames to a {@link FrameHandler}. Works together with a {@link TransportMapping}
 * to update sender / receiver windows of a BEEP session.
 * 
 * @author Simon Raess
 */
public class DefaultStreamParser implements StreamParser, ParseStateContext {
	
	private static final Logger LOG = LoggerFactory.getLogger(StreamParser.class);
	
	private final TransportMapping mapping;
	
	private final FrameHandler handler;
	
	private final ParseState headerState = new HeaderState();
	
	private final ParseState trailerState = new TrailerState();
	
	// conversational state
	
	private ParseState currentState;
	
	private DataHeader header;
	
	private ByteBuffer payload;
	
	public DefaultStreamParser(FrameHandler handler, TransportMapping mapping) {
		this.handler = handler;
		this.mapping = mapping;
		this.currentState = headerState;
	}
	
	private void setCurrentState(ParseState state) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("moving from " + currentState.getName() + " to " + state.getName());
		}
		currentState = state;
	}
	
	public void process(ByteBuffer buffer) {
		while (currentState.process(buffer, this));
	}
		
	protected void forward(Frame frame) {
		handler.handleFrame(frame);
		if (frame.getHeader().getPayloadSize() > 0) {				
			mapping.frameReceived(
					frame.getChannelNumber(), frame.getSequenceNumber(), frame.getSize());
		}
	}
		
	public void handleHeader(String[] tokens) {
		LOG.debug("got header: " + Arrays.toString(tokens));
		
		if (isStandardType(tokens[0])) {			
			header = DataHeader.parseHeader(tokens);

			int channel = header.getChannel();
			int size = header.getPayloadSize();
			long seqno = header.getSequenceNumber();
			mapping.checkFrame(channel, seqno, size);
			
			setCurrentState(new PayloadState(header.getPayloadSize()));
			
		} else {
			mapping.processMappingFrame(tokens);
		}
	}
	
	private boolean isStandardType(String type) {
		return MessageType.ANS.name().equals(type)
		    || MessageType.ERR.name().equals(type)
		    || MessageType.MSG.name().equals(type)
		    || MessageType.NUL.name().equals(type)
		    || MessageType.RPY.name().equals(type);
	}
	
	public void handlePayload(ByteBuffer payload) {
		this.payload = payload;
		setCurrentState(trailerState);
	}
	
	public void handleTrailer() {
		Frame frame = new Frame(header, payload);
		forward(frame);
		header = null;
		payload = null;
		setCurrentState(headerState);
	}

}
