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

import java.io.IOException;
import java.net.SocketAddress;

/**
 * An interface whose implementations allow to listen for BEEP sessions.
 * 
 * @author Simon Raess
 */
public interface Listener {
	
	/**
	 * Binds the given SocketAddress. Whenever a new SessionHandler
	 * is created with the SessionHandlerFactory whenever a new
	 * session is created.
	 * 
	 * @param address the address
	 * @param factory the factory used to create SessionHandlers
	 * @throws IOException if binding the address fails
	 */
	void bind(SocketAddress address, SessionHandlerFactory factory) throws IOException;
	
}
