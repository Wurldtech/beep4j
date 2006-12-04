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
package net.sf.beep4j.internal.profile;

import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;

public interface ChannelManagementMessageBuilder {
	
	Message createGreeting(MessageBuilder builder, String[] profiles);

	Message createProfile(MessageBuilder builder, ProfileInfo profile);

	Message createOk(MessageBuilder builder);

	Message createError(MessageBuilder builder, int code, String message);

	Message createStart(MessageBuilder builder, int channelNumber, ProfileInfo[] infos);

	Message createClose(MessageBuilder builder, int channelNumber, int code);
	
}
