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
package net.sf.beep4j.transport.mina;

import java.net.SocketAddress;

import net.sf.beep4j.Initiator;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.tcp.TCPMapping;
import net.sf.beep4j.internal.util.Assert;

import org.apache.mina.common.IoConnector;

public class MinaInitiator implements Initiator {

	private final IoConnector connector;
	private int receiveBufferSize;
	
        public MinaInitiator(IoConnector connector) {
            this(connector, TCPMapping.DEFAULT_BUFFER_SIZE);
        }
        
	public MinaInitiator(IoConnector connector, int receiveBufferSize) {
		Assert.notNull("connector", connector);
		this.connector = connector;
		this.receiveBufferSize = receiveBufferSize;
	}
	
	public void connect(SocketAddress address, SessionHandler handler) {
		MinaTransport transport = new MinaTransport(true, handler, receiveBufferSize);
		// TODO this returns a connect future... we should be doing something like:
		//   future.addListener(handler)
		// on it!
		connector.connect(address, transport);
	}

}
