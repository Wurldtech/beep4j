/*
 *  Copyright 2007 Simon Raess
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

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.internal.TransportHandler;
import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.transport.TransportContext;

public class DefaultTransportContext implements TransportContext {
	
	private final TransportHandler handler;
	
	private final StreamParser parser;
	
	public DefaultTransportContext(TransportHandler handler, StreamParser parser) {
		Assert.notNull("handler", handler);
		Assert.notNull("parser", parser);
		this.handler = handler;
		this.parser = parser;
	}
	
	public void connectionEstablished(SocketAddress address) {
		handler.connectionEstablished(address);
	}

	public void exceptionCaught(Throwable cause) {
		handler.exceptionCaught(cause);
	}

	public void messageReceived(ByteBuffer buffer) {
		try {
			parser.process(buffer);
		} catch (ProtocolException e) {
			exceptionCaught(e);
		}
	}

	public void connectionClosed() {
		handler.connectionClosed();
	}

}
