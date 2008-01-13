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
package net.sf.beep4j.internal.management;

import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;

/**
 * A ManagementMessageBuilder is used to build {@link Message} objects for
 * all the messages sent by the channel management profile.
 * 
 * @author Simon Raess
 */
interface ManagementMessageBuilder {
	
	/**
	 * Creates a BEEP greeting message that contains the specified greetings.
	 * See section 2.3.1.1 of RFC 3080.
	 * 
	 * @param builder the {@link MessageBuilder} used to build a {@link Message}
	 * @param profiles the profiles supported by the peer
	 * @return a {@link Message} object containing a well-formed greeting
	 */
	Message createGreeting(MessageBuilder builder, String[] profiles);
	
	/**
	 * Creates a BEEP ok message. See section 2.3.1.4 of RFC 3080.
	 * 
	 * @param builder the {@link MessageBuilder} used to build a {@link Message}
	 * @return the {@link Message} object containing a well-formed ok response
	 */
	Message createOk(MessageBuilder builder);

	/**
	 * Creates a BEEP error message using the given code and message. See section
	 * 2.3.1.5 of RFC 3080.
	 * 
	 * @param builder the {@link MessageBuilder} used to build a {@link Message}
	 * @param code the error code
	 * @param message the error diagnostic message
	 * @return a {@link Message} object containing a well-formed error response
	 */
	Message createError(MessageBuilder builder, int code, String message);
	
	/**
	 * Creates a BEEP start channel message with the given channel number and
	 * requested profiles. See section 2.3.1.2 of RFC 3080.
	 * 
	 * @param builder the {@link MessageBuilder} used to build a {@link Message}
	 * @param channelNumber the channel number of the newly created channel
	 * @param profiles the set of profiles that are acceptable for the channel
	 * @return a {@link Message} containing a well-formed start message
	 */
	Message createStart(MessageBuilder builder, int channelNumber, ProfileInfo[] profiles);

	/**
	 * Creates a BEEP profile message using the given {@link ProfileInfo} object.
	 * This message is the response to the start channel message. See section
	 * 2.3.1.2 of RFC 3080.
	 * 
	 * @param builder the {@link MessageBuilder} used to build a {@link Message}
	 * @param profile the profile used by the newly created channel
	 * @return a {@link Message} object containing a well-formed profile response
	 */
	Message createProfile(MessageBuilder builder, ProfileInfo profile);
	
	/**
	 * Creates a BEEP close channel message requesting to close the specified
	 * channel. See section 2.3.1.3 of RFC 3080.
	 * 
	 * @param builder the {@link MessageBuilder} used to build a {@link Message}
	 * @param channelNumber the channel number of the channel to be closed
	 * @param code a diagnostic code specifying why the channel should be closed
	 * @return a {@link Message} object containing a well-formed close message
	 */
	Message createClose(MessageBuilder builder, int channelNumber, int code);
	
}
