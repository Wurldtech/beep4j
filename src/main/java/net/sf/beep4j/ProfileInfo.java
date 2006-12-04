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

import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.internal.util.ByteUtil;

/**
 * Represents a profile as seen inside a start channel request.
 * 
 * @author Simon Raess
 */
public final class ProfileInfo {
	
	private final String uri;
	
	private boolean base64;
	
	private byte[] data;
	
	private String content;
	
	public ProfileInfo(String uri) {
		Assert.notNull("uri", uri);
		this.uri = uri;
	}
	
	public ProfileInfo(String uri, byte[] data) {
		this(uri);
		this.base64 = true;
		this.data = data;
	}
	
	public ProfileInfo(String uri, String content) {
		this(uri);
		this.base64 = false;
		this.content = content;
	}
	
	/**
	 * Determines whether this ProfileInfo has content.
	 * 
	 * @return true iff there is content
	 */
	public final boolean hasContent() {
		return data != null || content != null;
	}
	
	/**
	 * The profile URI, which should uniquely identify the profile.
	 * 
	 * @return the URI of the profile
	 */
	public final String getUri() {
		return uri;
	}
	
	/**
	 * Determines whether the body was (or should be) base 64 encoded.
	 * 
	 * @return true if base 64 encoded
	 */
	public final boolean isBase64Encoded() {
		return base64;
	}
	
	/**
	 * Tries to retrieve a the textual content. If no textual content
	 * is present an IllegalStateException is thrown. 
	 * 
	 * @return a textual content
	 * @throws IllegalStateException if there is no textual content
	 */
	public final String getContent() {
		if (content == null) {
			throw new IllegalStateException("ProfileInfo does not have textual content");
		}
		return content;
	}
	
	/**
	 * Tries to retrieve the binary content. If no binary content is present
	 * an IllegalStateException is thrown. The content returned is base64 decoded.
	 * 
	 * @return the base64 decoded initialization data
	 * @throws IllegalStateException if there is no binary data
	 */
	public final byte[] getBinaryContent() {
		if (data == null) {
			throw new IllegalStateException("ProfileInfo does not have binary data");
		}
		return data;
	}
		
	/**
	 * Append the ProfileInfo content to the StringBuilder.
	 * 
	 * @param appendable the StringBuilder to which the content is appended
	 */
	public final void appendTo(StringBuilder builder) {
		if (hasContent()) {
			if (isBase64Encoded()) {
				builder.append(ByteUtil.base64Encode(data));
			} else {
				builder.append(content);
			}
		}
	}
	
	@Override
	public String toString() {
		return "<profile uri='" + uri + "' />";
	}
	
}
