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
package net.sf.beep4j.ext;

import net.sf.beep4j.Message;
import net.sf.beep4j.ReplyHandler;

/**
 * Adapter class simplifying the implementation of ReplyHandlers. Throws an
 * UnsupportedOperationException on all method calls. Override the expected
 * method calls to implement real functionality.
 * 
 * @author Simon Raess
 */
public class ReplyHandlerAdapter implements ReplyHandler {

	public void receivedANS(Message message) {
		throw new UnsupportedOperationException();
	}

	public void receivedERR(Message message) {
		throw new UnsupportedOperationException();
	}

	public void receivedNUL() {
		throw new UnsupportedOperationException();
	}

	public void receivedRPY(Message message) {
		throw new UnsupportedOperationException();
	}

}
