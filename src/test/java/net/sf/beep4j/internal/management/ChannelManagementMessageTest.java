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

import java.util.Arrays;

import junit.framework.TestCase;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.internal.management.BEEPError;
import net.sf.beep4j.internal.management.ChannelManagementMessageBuilder;
import net.sf.beep4j.internal.management.ChannelManagementMessageParser;
import net.sf.beep4j.internal.management.CloseChannelMessage;
import net.sf.beep4j.internal.management.Greeting;
import net.sf.beep4j.internal.management.SaxMessageBuilder;
import net.sf.beep4j.internal.management.SaxMessageParser;
import net.sf.beep4j.internal.management.StartChannelMessage;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;

public class ChannelManagementMessageTest extends TestCase {
	
	private static final String PROFILE = "http://examples.org/profile/echo";

	private static final String PROFILE_2 = "http://examples.org/profile/reverse";

	private static final String PROFILE_3 = "http://examples.org/profile/crypto";

	private ChannelManagementMessageBuilder builder;
	
	private ChannelManagementMessageParser parser;

	private MessageBuilder messageBuilder;
	
	@Override
	protected void setUp() throws Exception {
		builder = new SaxMessageBuilder();
		parser = new SaxMessageParser();
		messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
	}
	
	public void testGreetingEmpty() throws Exception {
		Message message = builder.createGreeting(messageBuilder, new String[0]);
		Greeting greeting = parser.parseGreeting(message);
		assertEquals(0, greeting.getProfiles().length);
	}
	
	public void testGreetingTwoProfiles() throws Exception {
		Message message = builder.createGreeting(messageBuilder, new String[] {
			PROFILE, PROFILE_2	
		});
		Greeting greeting = parser.parseGreeting(message);
		assertEquals(2, greeting.getProfiles().length);
		assertEquals(PROFILE, greeting.getProfiles()[0]);
		assertEquals(PROFILE_2, greeting.getProfiles()[1]);
	}
	
	public void testError() throws Exception {
		Message message = builder.createError(messageBuilder, 550, "still working");
		BEEPError error = parser.parseError(message);
		assertEquals(550, error.getCode());
		assertEquals("still working", error.getMessage());
	}
	
	public void testOk() throws Exception {
		Message message = builder.createOk(messageBuilder);
		parser.parseOk(message);
	}

	public void testStartWithOneProfile() throws Exception {
		Message message = builder.createStart(messageBuilder, 2, new ProfileInfo[] {
			new ProfileInfo(PROFILE)
		});
		assertEquals("application/beep+xml", message.getContentType());
		
		StartChannelMessage result = (StartChannelMessage) parser.parseRequest(message);
		assertEquals(2, result.getChannelNumber());
		assertEquals(1, result.getProfiles().length);
		assertEquals(PROFILE, result.getProfiles()[0].getUri());
	}

	public void testStartWithThreeProfiles() throws Exception {
		Message message = builder.createStart(messageBuilder, 2, new ProfileInfo[] {
			new ProfileInfo(PROFILE), new ProfileInfo(PROFILE_2), new ProfileInfo(PROFILE_3)
		});
		assertEquals("application/beep+xml", message.getContentType());
		
		StartChannelMessage result = (StartChannelMessage) parser.parseRequest(message);
		assertEquals(2, result.getChannelNumber());
		assertEquals(3, result.getProfiles().length);
		assertEquals(PROFILE, result.getProfiles()[0].getUri());
		assertEquals(PROFILE_2, result.getProfiles()[1].getUri());
		assertEquals(PROFILE_3, result.getProfiles()[2].getUri());
	}
	
	public void testStartWithContent() throws Exception {
		Message message = builder.createStart(messageBuilder, 2, new ProfileInfo[] {
			new ProfileInfo(PROFILE, "abcdefg")	
		});
		
		StartChannelMessage result = (StartChannelMessage) parser.parseRequest(message);
		assertEquals(2, result.getChannelNumber());
		assertEquals(1, result.getProfiles().length);
		assertEquals(PROFILE, result.getProfiles()[0].getUri());
		assertEquals("abcdefg", result.getProfiles()[0].getContent());
	}
	
	public void testStartWithBase64EncodedContent() throws Exception {
		byte[] expected = new byte[] { 0x30, 0x31, 0x32, 0x33, 0x34 };
		Message message = builder.createStart(messageBuilder, 2, new ProfileInfo[] {
				new ProfileInfo(PROFILE, expected)	
		});
			
		StartChannelMessage result = (StartChannelMessage) parser.parseRequest(message);
		assertEquals(2, result.getChannelNumber());
		assertEquals(1, result.getProfiles().length);
		assertEquals(PROFILE, result.getProfiles()[0].getUri());
		assertArrayEquals(expected, result.getProfiles()[0].getBinaryContent());
	}
	
	public void testProfileWithoutContent() throws Exception {
		Message message = builder.createProfile(messageBuilder, new ProfileInfo(PROFILE));
		ProfileInfo profile = parser.parseProfile(message);
		assertEquals(PROFILE, profile.getUri());
		assertFalse(profile.hasContent());
	}
	
	public void testProfileWithContent() throws Exception {
		Message message = builder.createProfile(messageBuilder, new ProfileInfo(PROFILE, "abcdefg"));
		ProfileInfo profile = parser.parseProfile(message);
		assertEquals(PROFILE, profile.getUri());
		assertTrue(profile.hasContent());
		assertFalse(profile.isBase64Encoded());
		assertEquals("abcdefg", profile.getContent());
	}
	
	public void testClose() throws Exception {
		Message message = builder.createClose(messageBuilder, 2, 200);
		CloseChannelMessage request = (CloseChannelMessage) parser.parseRequest(message);
		assertEquals(2, request.getChannelNumber());
		assertEquals(200, request.getCode());
	}
	
	private void assertArrayEquals(byte[] a, byte[] b) {
		assertEquals("lengths do not match", a.length, b.length);
		assertTrue("content does not match", Arrays.equals(a, b));
	}

}
