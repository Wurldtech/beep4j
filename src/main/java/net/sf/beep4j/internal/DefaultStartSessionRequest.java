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

import java.util.HashSet;
import java.util.Set;

import net.sf.beep4j.StartSessionRequest;
import net.sf.beep4j.internal.util.Assert;

/**
 * Default implementation of the StartSessionRequest interface.
 * 
 * @author Simon Raess
 */
public class DefaultStartSessionRequest implements StartSessionRequest {
	
	private final Set<String> profiles = new HashSet<String>();
	
	private boolean cancellable;
	
	private boolean cancelled;
	
	private int code;
	
	private String message;
	
	public DefaultStartSessionRequest(boolean cancellable) {
		this.cancellable = cancellable;
	}
	
	public boolean isCancellable() {
		return cancellable;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	public int getReplyCode() {
		return code;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String[] getProfiles() {
		return profiles.toArray(new String[profiles.size()]);
	}
	
	public void cancel() {
		this.cancelled = true;
		this.code = 421;
		this.message = "service not available";
	}

	public void registerProfile(String profileUri) {
		Assert.notNull("profileUri", profileUri);
		profiles.add(profileUri);
	}

}
