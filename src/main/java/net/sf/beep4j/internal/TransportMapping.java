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


/**
 * A TransportMapping implements a transport mapping as described by
 * section 2.5 of the BEEP specification (RFC 3080). The interface is meant to
 * be generally applicable. But it could be that it needs some 
 * rework for other transport mappings as those specified by RFC 3081.
 * 
 * <p>General responsibilities of implementors are:</p>
 * <ul>
 *  <li>ensure that sent frames fit into the receivers advertised buffer window</li>
 *  <li>fragment messages into several frames as necessary</li>
 *  <li>validate that the other peer respects the local advertised buffer window</li>
 *  <li>process any transport specific mapping frame</li>
 * </ul>
 * 
 * @author Simon Raess
 */
public interface TransportMapping extends BeepStream {
	
	/**
	 * Process a mapping frame. This method receives the header tokens.
	 * As the TCP mapping is currently the only existing mapping, this
	 * is sufficient. If there ever exists another mapping that defines
	 * a payload for mapping frames, some changes would have to be
	 * implemented.
	 * 
	 * @param token the header tokens (i.e. header is split on spaces)
	 */
	void processMappingFrame(String[] token);
	
	/**
	 * Checks that an incoming frame is valid. This method is called
	 * after the header has been parsed, but before the parsing of
	 * the body has started.
	 * 
	 * @param channel the channel number of the message
	 * @param seqno the sequence number of the message
	 * @param size the payload size of the message
	 */
	void checkFrame(int channel, long seqno, int size);
	
	/**
	 * Invoked by the framework to notify the mapping that the parsing
	 * of the message has completed.
	 * 
	 * @param channel the channel number of the message
	 * @param seqno the sequence number of the message
	 * @param size the size of the message
	 */
	void frameReceived(int channel, long seqno, int size);
		
}
