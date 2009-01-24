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

import java.util.HashMap;
import java.util.Map;

import net.sf.beep4j.Message;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.internal.TransportMapping;
import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.transport.Transport;

public class TCPMapping implements TransportMapping, ChannelControllerFactory {

	public static final int DEFAULT_BUFFER_SIZE = 4096;
	
	private final Transport transport;
	
	private final ChannelControllerFactory factory;
	
	private final int sendBufferSize;
        private final int receiveBufferSize;
	
	private final Map<Integer, ChannelController> channels = 
			new HashMap<Integer, ChannelController>();

	
	public TCPMapping(Transport transport) {
		this(transport, null);
	}
	
	public TCPMapping(Transport transport, ChannelControllerFactory factory) {
		this(transport, factory, DEFAULT_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
	}
	
	public TCPMapping(Transport transport, ChannelControllerFactory factory, int receiveBufferSize) {
	    this(transport, factory, DEFAULT_BUFFER_SIZE, receiveBufferSize );
	}
	
	public TCPMapping(Transport transport, ChannelControllerFactory factory, int sendBufferSize, int receiveBufferSize) {
		Assert.notNull("transport", transport);
		this.transport = transport;
		this.factory = factory != null ? factory : this;
		this.sendBufferSize = sendBufferSize;
		this.receiveBufferSize = receiveBufferSize;
	}
	
	
	// --> start of SessionListener methods <--
	
	public void channelStarted(int channelNumber) {
		if (channels.containsKey(channelNumber)) {
			throw new IllegalArgumentException("there is already a channel for channel number: " 
					+ channelNumber);
		}
		ChannelController controller = factory.createChannelController(channelNumber, transport);
		channels.put(channelNumber, controller);
	}
	
	public void channelClosed(int channelNumber) {
		channels.remove(channelNumber);
	}
	
	// --> end of SessionListener methods <--

	
	// --> start of ChannelControllerFactory methods <--
	
	public DefaultChannelController createChannelController(int channelNumber, Transport transport) {
		return new DefaultChannelController(transport, channelNumber, sendBufferSize, receiveBufferSize);
	}
	
	// --> end of ChannelControllerFactory methods <--
	
	
	// --> start of TransportMapping methods <--
	
	public void checkFrame(int channel, long seqno, int size) {
		getChannelController(channel).checkFrame(seqno, size);
	}
	
	public void frameReceived(int channel, long seqno, int size) {
		getChannelController(channel).frameReceived(seqno, size);
	}

	public void processMappingFrame(String[] tokens) {
		if (!tokens[0].equals(SEQHeader.TYPE)) {
			throw new ProtocolException("unsupported frame type: " + tokens[0]);
		}
		
		SEQHeader header = new SEQHeader(tokens);
		int channel = header.getChannel();
		long ackno = header.getAcknowledgeNumber();
		int size = header.getWindowSize();
			
		// adapt the local view of the other peers window			
		getChannelController(channel).updateSendWindow(ackno, size);
	}
	
	public void sendANS(int channel, int messageNumber, int answerNumber, Message message) {
		getChannelController(channel).sendANS(messageNumber, answerNumber, message);
	}
	
	public void sendERR(int channel, int messageNumber, Message message) {
		getChannelController(channel).sendERR(messageNumber, message);
	}
	
	public void sendMSG(int channel, int messageNumber, Message message) {
		getChannelController(channel).sendMSG(messageNumber, message);		
	}
	
	public void sendNUL(int channel, int messageNumber) {
		getChannelController(channel).sendNUL(messageNumber);
	}
	
	public void sendRPY(int channel, int messageNumber, Message message) {
		getChannelController(channel).sendRPY(messageNumber, message);
	}
	
	public void closeTransport() {
		transport.closeTransport();
	}
	
	// --> end of TransportMapping methods <--
	
				
	private ChannelController getChannelController(int channel) {
		ChannelController controller = channels.get(new Integer(channel));
		if (controller == null) {
			throw new ProtocolException("unkown channel: " + channel);
		}
		return controller;
	}

}
