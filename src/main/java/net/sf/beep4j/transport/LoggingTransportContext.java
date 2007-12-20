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
package net.sf.beep4j.transport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.internal.util.HexDump;

/**
 * Logging interceptor for methods of TransportContext.
 * 
 * @author Simon Raess
 */
public class LoggingTransportContext implements TransportContext {

	private static final Logger LOG = LoggerFactory.getLogger("net.sf.beep4j.transport");
	
	private static final Logger DATA_LOG = LoggerFactory.getLogger("net.sf.beep4j.transport.DATA");
	
	private final TransportContext target;
		
	public LoggingTransportContext(TransportContext target) {
		Assert.notNull("target", target);
		this.target = target;
	}

	public void connectionEstablished(SocketAddress address) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("connection established to " + address);
		}
		target.connectionEstablished(address);
	}

	public void messageReceived(ByteBuffer buffer) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("message received");
			DATA_LOG.debug(HexDump.dump(buffer));
		}
		target.messageReceived(buffer);
	}

	public void exceptionCaught(Throwable cause) {
		LOG.warn("exception caught from transport:");
		StringWriter writer = new StringWriter();
		cause.printStackTrace(new PrintWriter(writer));
		LOG.warn(writer.toString());
		target.exceptionCaught(cause);
	}

	public void connectionClosed() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("connection closed");
		}
		target.connectionClosed();
	}

}
