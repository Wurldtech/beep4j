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

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * The TransportContext defines the methods that a Transport implementation
 * can use to notify the BEEP implementation about important events. 
 * 
 * @author Simon Raess
 */
public interface TransportContext {

	/**
	 * Notifies the context that the physical connection has
	 * been established. This allows the BEEP peer to start initializing the
	 * BEEP session by sending the greeting.
	 * 
	 * @param address the SocketAddress of the remote peer
	 */
	void connectionEstablished(SocketAddress address);

	/**
	 * Invoked by the Transport to notify the context about an exception
	 * that has been caught while sending or processing a message.
	 * 
	 * @param cause the causing exception
	 */
	void exceptionCaught(Throwable cause);

	/**
	 * Invoked by the Transport whenever new content has been received.
	 * This method is invoked whenever a new chunk of bytes arrives.
	 * There is no guarantee whatsoever that the received buffer contains
	 * a full message. Only the application knows what constitutes a 
	 * message.
	 * 
	 * @param buffer the received bytes
	 */
	void messageReceived(ByteBuffer buffer);
	
	/**
	 * Invoked by the Transport when the underlying physical connection
	 * has been closed.
	 */
	void connectionClosed();

}
