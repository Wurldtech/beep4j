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
package net.sf.beep4j.internal.management;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.Reply;
import net.sf.beep4j.internal.util.Assert;

/**
 * {@link ChannelHandler} for the BEEP management profile. Implements only
 * {@link #messageReceived(Message, Reply)} and dispatches directly to the
 * {@link ChannelManagementProfile}.
 * 
 * @author Simon Raess
 */
final class ManagementChannelHandler implements ChannelHandler {
	
	private final ChannelManagementProfile profile;
	
	private final ChannelManagementMessageParser parser;
	
	ManagementChannelHandler(ChannelManagementProfile profile, ChannelManagementMessageParser parser) {
		Assert.notNull("profile", profile);
		Assert.notNull("parser", parser);
		this.profile = profile;
		this.parser = parser;
	}
	
	/**
	 * This method is only called on channels created through the
	 * startChannel methods of the Session. Thus, this method can
	 * safely throw an UnsupportedOperationException because it
	 * is never called. The channel management profile is created
	 * by the session itself when it starts up.
	 * 
	 * @throws UnsupportedOperationException unconditionally
	 */
	public void channelStartFailed(int code, String message) {
		throw new UnsupportedOperationException();
	}
	
	public void channelOpened(Channel c) {
		// ignored, channel management profile is not interested in channel
	}
	
	public void messageReceived(Message message, Reply reply) {
		ChannelManagementRequest r = parser.parseRequest(message);
		
		if (r instanceof StartChannelMessage) {
			StartChannelMessage request = (StartChannelMessage) r;
			int channelNumber = request.getChannelNumber();
			ProfileInfo[] profiles = request.getProfiles();
			profile.startChannelRequested(channelNumber, profiles, reply);
			
		} else if (r instanceof CloseChannelMessage) {
			int channelNumber = ((CloseChannelMessage) r).getChannelNumber();
			
			if (channelNumber == 0) {
				profile.closeSessionRequested(reply);
			} else {
				profile.closeChannelRequested(channelNumber, reply);
			}
			
		} else {
			throw new RuntimeException("unexpected code path");
		}
	}
	
	public void channelCloseRequested(CloseChannelRequest request) {
		throw new UnsupportedOperationException("unexpected code path");
	}
	
	public void channelClosed() {
		// ignored
	}
	
}
