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

import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.ProfileInfo;

public abstract class StartChannelResponse {

	private StartChannelResponse() {
		// hidden constructor
	}
	
	public abstract boolean isCancelled();
	
	public int getCode() {
		throw new IllegalStateException("StartChannelRequest is not cancelled");
	}
	
	public String getMessage() {
		throw new IllegalStateException("StartChannelRequest is not cancelled");
	}
	
	public ProfileInfo getProfile() {
		throw new IllegalStateException("StartChannelRequest is cancelled");
	}
	
	public ChannelHandler getChannelHandler() {
		throw new IllegalStateException("StartChannelRequest is cancelled");
	}

	public static final StartChannelResponse createCancelledResponse(
			final int code, final String message) {
		return new StartChannelResponse() {
			@Override
			public boolean isCancelled() {
				return true;
			}
			@Override
			public int getCode() {
				return code;
			}
			@Override
			public String getMessage() {
				return message;
			}
		};
	}
	
	public static final StartChannelResponse createSuccessResponse(
			final ProfileInfo profile, final ChannelHandler channelHandler) {
		return new StartChannelResponse() {
			@Override
			public boolean isCancelled() {
				return false;
			}
			@Override
			public ChannelHandler getChannelHandler() {
				return channelHandler;
			}
			@Override
			public ProfileInfo getProfile() {
				return profile;
			}
		};
	}
	
}
