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
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageStub;
import net.sf.beep4j.NullCloseChannelCallback;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.profile.BEEPError;
import net.sf.beep4j.internal.profile.ChannelManagementProfile;
import net.sf.beep4j.internal.profile.Greeting;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class SessionImplTest extends MockObjectTestCase {
	
	private Mock mappingMock;
	
	private TransportMapping mapping;
	
	private Mock sessionHandlerMock;
	
	private SessionHandler sessionHandler;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mappingMock = mock(TransportMapping.class);
		mapping = (TransportMapping) mappingMock.proxy();
		
		sessionHandlerMock = mock(SessionHandler.class);
		sessionHandler = (SessionHandler) sessionHandlerMock.proxy();
	}
	
	// --> test TransportContext methods <--
	
	public void testConnectionEstablished() throws Exception {
		// TODO: method stub
	}
	
	public void testExceptionCaught() throws Exception {
		// nothing to be checked, yet
	}
	
	public void testMessageReceived() throws Exception {
		// TODO: method stub
	}
	
	public void testConnectionClosed() throws Exception {
		// TODO: method stub
	}
	
	// --> test MessageHandler methods <--
	
	public void testReceiveANS() throws Exception {
		// TODO: method stub
	}
	
	public void testReceiveNUL() throws Exception {
		// TODO: method stub
	}
	
	public void testReceiveMSG() throws Exception {
		Mock profileMock = mock(ChannelManagementProfile.class);
		final ChannelManagementProfile profile = (ChannelManagementProfile) profileMock.proxy(); 
		Mock channelHandlerMock = mock(ChannelHandler.class);
		ChannelHandler channelHandler = (ChannelHandler) channelHandlerMock.proxy();
		Message message = new MessageStub();
		Message greeting = new MessageStub();
		
		// define expectations
		mappingMock.expects(once()).method("channelStarted").with(eq(0));
		profileMock.expects(once()).method("createChannelHandler").with(ANYTHING).will(returnValue(channelHandler));
		profileMock.expects(once()).method("receivedGreeting").with(same(greeting));
		channelHandlerMock.expects(once()).method("channelOpened").with(ANYTHING);
		channelHandlerMock.expects(once()).method("messageReceived").with(same(message), ANYTHING);
		sessionHandlerMock.expects(once()).method("sessionOpened").with(ANYTHING);

		// test
		MessageHandler session = new SessionImpl(false, sessionHandler, mapping) {
			@Override
			protected ChannelManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		session.receiveRPY(0, 0, greeting);
		session.receiveMSG(0, 0, message);
	}
	
	public void testReceiveInitialERR() throws Exception {
		Mock profileMock = mock(ChannelManagementProfile.class);
		final ChannelManagementProfile profile = (ChannelManagementProfile) profileMock.proxy(); 
		Mock channelHandlerMock = mock(ChannelHandler.class);
		ChannelHandler channelHandler = (ChannelHandler) channelHandlerMock.proxy();
		Message message = new MessageStub();
		
		// define expectations
		mappingMock.expects(once()).method("channelStarted").with(eq(0));
		mappingMock.expects(once()).method("closeTransport");
		profileMock.expects(once()).method("createChannelHandler").with(ANYTHING).will(returnValue(channelHandler));
		channelHandlerMock.expects(once()).method("channelOpened").with(ANYTHING);
		sessionHandlerMock.expects(once()).method("sessionStartDeclined")
				.with(eq(550), eq("still working"));
		profileMock.expects(once()).method("receivedError")
				.with(same(message))
				.will(returnValue(new BEEPError(550, "still working")));

		// test
		MessageHandler session = new SessionImpl(false, sessionHandler, mapping) {
			@Override
			protected ChannelManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		session.receiveERR(0, 0, message);
	}
	
	public void testReceiveInitialRPY() throws Exception {
		Mock profileMock = mock(ChannelManagementProfile.class);
		final ChannelManagementProfile profile = (ChannelManagementProfile) profileMock.proxy(); 
		Mock channelHandlerMock = mock(ChannelHandler.class);
		ChannelHandler channelHandler = (ChannelHandler) channelHandlerMock.proxy();
		Message message = new MessageStub();
		
		// define expectations
		mappingMock.expects(once()).method("channelStarted").with(eq(0));
		profileMock.expects(once()).method("createChannelHandler").with(ANYTHING).will(returnValue(channelHandler));
		channelHandlerMock.expects(once()).method("channelOpened").with(ANYTHING);
		profileMock.expects(once()).method("receivedGreeting")
				.with(same(message))
				.will(returnValue(new Greeting(new String[0], new String[0], new String[] { "abc" })));
		sessionHandlerMock.expects(once()).method("sessionOpened").with(ANYTHING);
		
		// test
		MessageHandler session = new SessionImpl(false, sessionHandler, mapping) {
			@Override
			protected ChannelManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		session.receiveRPY(0, 0, message);
	}
	
	public void testReceiveRPY() throws Exception {
		Mock profileMock = mock(ChannelManagementProfile.class);
		final ChannelManagementProfile profile = (ChannelManagementProfile) profileMock.proxy(); 
		Mock channelHandlerMock = mock(ChannelHandler.class);
		ChannelHandler channelHandler = (ChannelHandler) channelHandlerMock.proxy();
		Mock replyListenerMock = mock(ReplyHandler.class);
		ReplyHandler replyListener = (ReplyHandler) replyListenerMock.proxy();
		
		Message greeting = new MessageStub();
		Message message = new MessageStub();
		Message reply = new MessageStub();
		
		// define expectations
		sessionHandlerMock.expects(once()).method("sessionOpened").with(ANYTHING);
		mappingMock.expects(once()).method("channelStarted").with(eq(0));
		profileMock.expects(once()).method("createChannelHandler").with(ANYTHING).will(returnValue(channelHandler));
		channelHandlerMock.expects(once()).method("channelOpened").with(ANYTHING);
		profileMock.expects(once()).method("receivedGreeting")
				.with(same(greeting))
				.will(returnValue(new Greeting(new String[0], new String[0], new String[] { "abc" })));

		mappingMock.expects(once()).method("sendMSG").with(eq(0), eq(1), same(message));
		replyListenerMock.expects(once()).method("receivedRPY").with(same(reply));

		// test
		SessionImpl session = new SessionImpl(false, sessionHandler, mapping) {
			@Override
			protected ChannelManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		session.receiveRPY(0, 0, greeting);
		session.sendMessage(0, message, replyListener);
		session.receiveRPY(0, 1, reply);
	}
	
	// --> test SessionManager methods <--
	
	public void testChannelStartRequested() throws Exception {
		// TODO: method stub
	}
	
	public void testChannelCloseRequested() throws Exception {
		// TODO: method stub
	}
	
	public void testSessionCloseRequested() throws Exception {
		// TODO: method stub
	}
	
	// --> test Session methods <--
	
	// TODO: reimplement this test
//	public void testStartChannelWithUri() throws Exception {
//		Mock handlerMock = mock(ChannelHandler.class);
//		ChannelHandler handler = (ChannelHandler) handlerMock.proxy();
//		
//		String profile = "http://example.org/profile/echo";
//		
//		// define expectations
//		mappingMock.expects(once()).method("channelStarted").with(eq(0));
//		mappingMock.expects(once()).method("sendMSG")
//				.with(eq(0), eq(1), eq(createStartChannelMessage(2, new ProfileInfo(profile))));
//
//		// test
//		InternalSession session = new SessionImpl(false, sessionHandler, mapping);
//		session.startChannel(profile, handler);
//	}
	
	// TODO: reimplement this test
//	public void testStartChannelWithProfileInfo() throws Exception {
//		Mock handlerMock = mock(ChannelHandler.class);
//		ChannelHandler handler = (ChannelHandler) handlerMock.proxy();
//		
//		String profile = "http://example.org/profile/echo";
//		ProfileInfo info = new ProfileInfo(profile, "abc");
//		
//		// define expectations
//		mappingMock.expects(once()).method("channelStarted").with(eq(0));
//		mappingMock.expects(once()).method("sendMSG")
//				.with(eq(0), eq(1), eq(createStartChannelMessage(2, info)));
//
//		// test
//		InternalSession session = new SessionImpl(false, sessionHandler, mapping);
//		session.startChannel(info, handler);
//	}
	
	public void testStartChannelWithFactory() throws Exception {
		// TODO: method stub
	}
	
	public void testClose() throws Exception {
		// TODO: method stub
	}
	
	// --> test InternalSession methods <--
	
	// TODO: reimplement this test
//	public void testSendMessage() throws Exception {
//		Message m1 = new MessageStub();
//		Message m2 = new MessageStub();
//		Message m3 = new MessageStub();
//		
//		// define expectations
//		mappingMock.expects(once()).method("channelStarted").with(eq(0));
//		mappingMock.expects(once()).method("sendMSG").with(eq(0), eq(1), same(m1));
//		mappingMock.expects(once()).method("sendMSG").with(eq(0), eq(2), same(m2));
//		mappingMock.expects(once()).method("sendMSG").with(eq(0), eq(3), same(m3));
//
//		// test
//		InternalSession session = new SessionImpl(false, sessionHandler, mapping);
//		session.sendMessage(0, m1, new NullReplyListener());
//		session.sendMessage(0, m2, new NullReplyListener());
//		session.sendMessage(0, m3, new NullReplyListener());
//	}
	
	public void testRequestChannelClose() throws Exception {
		// TODO: test channel close of channel other than channel 0
		
		Mock profileMock = mock(ChannelManagementProfile.class);
		final ChannelManagementProfile profile = (ChannelManagementProfile) profileMock.proxy(); 
		Mock channelHandlerMock = mock(ChannelHandler.class);
		ChannelHandler channelHandler = (ChannelHandler) channelHandlerMock.proxy();
		
		// define expectations
		mappingMock.expects(once()).method("channelStarted").with(eq(0));
		profileMock.expects(once()).method("createChannelHandler").with(ANYTHING).will(returnValue(channelHandler));
		channelHandlerMock.expects(once()).method("channelOpened").with(ANYTHING);
		profileMock.expects(once()).method("closeChannel").with(eq(0), ANYTHING);

		// test
		InternalSession session = new SessionImpl(false, sessionHandler, mapping) {
			@Override
			protected ChannelManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		
		session.requestChannelClose(0, new NullCloseChannelCallback());
	}
	
}
