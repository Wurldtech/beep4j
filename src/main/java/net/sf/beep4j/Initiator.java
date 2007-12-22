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
package net.sf.beep4j;

import java.net.SocketAddress;

/**
 * An interface whose implementations allow to initiate BEEP sessions.
 * The connect methods connect to a remote peer identified by a
 * SocketAddress. The SessionHandler receives callbacks for important
 * moments in the lifecycle of the session.
 * 
 * @author Simon Raess
 */
public interface Initiator extends Peer {
	
	/**
	 * Tries to establish a session to the peer specified by the given
	 * address. The SessionHandler receives call backs as certain events
	 * in the session life cycle are reached. 
	 * 
	 * @param address the address of the peer
	 * @param handler the handler callback
	 */
	void connect(SocketAddress address, SessionHandler handler);
	
}
