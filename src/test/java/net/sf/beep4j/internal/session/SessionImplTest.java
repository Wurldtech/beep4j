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
package net.sf.beep4j.internal.session;

import junit.framework.TestCase;
import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageStub;
import net.sf.beep4j.NullCloseCallback;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.Session;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.StartSessionRequest;
import net.sf.beep4j.internal.SessionManager;
import net.sf.beep4j.internal.management.BEEPError;
import net.sf.beep4j.internal.management.CloseCallback;
import net.sf.beep4j.internal.management.Greeting;
import net.sf.beep4j.internal.management.ManagementProfile;
import net.sf.beep4j.internal.stream.BeepStream;
import net.sf.beep4j.internal.stream.MessageHandler;

import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;

public class SessionImplTest extends TestCase {
	
	private BeepStream beepStream;
	
	private SessionHandler sessionHandler;
	
	private Mockery context;
	
	private Sequence sequence;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		context = new Mockery();
		sequence = context.sequence("sequence");
		beepStream = context.mock(BeepStream.class);
		sessionHandler = context.mock(SessionHandler.class);
	}
	
	private void assertIsSatisfied() {
		context.assertIsSatisfied();
	}
		
	// --> test TransportContext methods <--
	
	public void testConnectionEstablished() throws Exception {
		VmPipeAddress address = new VmPipeAddress(1);
		
		context.checking(new Expectations() {{
			one(beepStream).channelStarted(0); inSequence(sequence);
			
			one(sessionHandler).connectionEstablished(with(any(StartSessionRequest.class)));
			inSequence(sequence);
			
			one(beepStream).sendRPY(with(equal(0)), with(equal(0)), with(any(Message.class)));
			inSequence(sequence);
		}});

		SessionImpl session = new SessionImpl(true, sessionHandler, beepStream);
		session.connectionEstablished(address);
		
		// verify
		assertIsSatisfied();
	}
	
	public void testExceptionCaught() throws Exception {
		// nothing to be checked, yet
	}
	
	public void testConnectionClosed() throws Exception {
		VmPipeAddress address = new VmPipeAddress(1);
		
		context.checking(new Expectations() {{
			one(beepStream).channelStarted(0); 
			inSequence(sequence);
			
			one(sessionHandler).connectionEstablished(with(any(StartSessionRequest.class)));
			inSequence(sequence);
			
			one(sessionHandler).sessionClosed();
			inSequence(sequence);
			
			one(beepStream).sendRPY(with(equal(0)), with(equal(0)), with(any(Message.class)));
		}});

		SessionImpl session = new SessionImpl(true, sessionHandler, beepStream);
		session.connectionEstablished(address);
		session.connectionClosed();
		
		// verify
		assertIsSatisfied();
	}
	
	// --> test MessageHandler methods <--
	
	public void testReceiveANS() throws Exception {
		// TODO: method stub
	}
	
	public void testReceiveNUL() throws Exception {
		// TODO: method stub
	}
	
	public void testReceiveMSG() throws Exception {
		final ManagementProfile profile = context.mock(ManagementProfile.class); 
		final ChannelHandler channelHandler = context.mock(ChannelHandler.class);

		final Message message = new MessageStub();
		final Message greeting = new MessageStub();
		
		// define expectations
		context.checking(new Expectations() {{			
			one(profile).createChannelHandler(with(any(SessionManager.class)), with(any(InternalChannel.class)));
			will(returnValue(channelHandler));
			inSequence(sequence);
			
			one(beepStream).channelStarted(0);
			inSequence(sequence);
			
			one(channelHandler).channelOpened(with(any(Channel.class)));
			inSequence(sequence);
			
			one(sessionHandler).connectionEstablished(with(any(StartSessionRequest.class)));
			inSequence(sequence);
			
			one(profile).createGreeting(with(any(String[].class)));
			will(returnValue(new MessageStub()));
			inSequence(sequence);
			
			one(beepStream).sendRPY(with(equal(0)), with(equal(0)), with(any(Message.class)));
			inSequence(sequence);
			
			one(profile).receivedGreeting(with(any(Message.class)));
			will(returnValue(new Greeting(new String[0], new String[0], new String[0]))); 
			inSequence(sequence);
			
			one(sessionHandler).sessionOpened(with(any(Session.class)));
			inSequence(sequence);
			
			one(channelHandler).messageReceived(with(same(message)), with(any(Reply.class)));
			inSequence(sequence);
		}});

		// test
		SessionImpl session = new SessionImpl(false, sessionHandler, beepStream) {
			@Override
			protected ManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		session.connectionEstablished(null);
		session.receiveRPY(0, 0, greeting);
		session.receiveMSG(0, 0, message);
		
		// verify
		assertIsSatisfied();
	}
	
	public void testReceiveInitialERR() throws Exception {
		final ManagementProfile profile = context.mock(ManagementProfile.class); 
		final ChannelHandler channelHandler = context.mock(ChannelHandler.class);
		final Message message = new MessageStub();
		
		// define expectations
		context.checking(new Expectations() {{
			one(profile).createChannelHandler(with(any(SessionManager.class)), with(any(InternalChannel.class)));
			will(returnValue(channelHandler));
			inSequence(sequence);
			
			one(beepStream).channelStarted(0);
			inSequence(sequence);
			
			one(channelHandler).channelOpened(with(any(Channel.class)));
			inSequence(sequence);
			
			one(profile).receivedError(with(same(message)));
			will(returnValue(new BEEPError(550, "still working")));
			inSequence(sequence);
			
			one(sessionHandler).sessionStartDeclined(550, "still working");
			inSequence(sequence);
			
			one(beepStream).closeTransport();
			inSequence(sequence);
		}});

		// test
		MessageHandler session = new SessionImpl(false, sessionHandler, beepStream) {
			@Override
			protected ManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		session.receiveERR(0, 0, message);
		
		// verify
		assertIsSatisfied();
	}
	
	public void testReceiveInitialRPY() throws Exception {
		final ManagementProfile profile = context.mock(ManagementProfile.class); 
		final ChannelHandler channelHandler = context.mock(ChannelHandler.class);
		final Message message = new MessageStub();
		
		// define expectations
		context.checking(new Expectations() {{
			one(profile).createChannelHandler(with(any(SessionManager.class)), with(any(InternalChannel.class)));
			will(returnValue(channelHandler));
			inSequence(sequence);
			
			one(beepStream).channelStarted(0);
			inSequence(sequence);
			
			one(channelHandler).channelOpened(with(any(Channel.class)));
			inSequence(sequence);
			
			one(sessionHandler).connectionEstablished(with(any(StartSessionRequest.class)));
			inSequence(sequence);
			
			one(profile).createGreeting(with(any(String[].class)));
			will(returnValue(new MessageStub()));
			inSequence(sequence);
			
			one(beepStream).sendRPY(with(equal(0)), with(equal(0)), with(any(Message.class)));
			inSequence(sequence);
			
			one(profile).receivedGreeting(with(same(message)));
			will(returnValue(new Greeting(new String[0], new String[0], new String[] { "abc" })));
			inSequence(sequence);
			
			one(sessionHandler).sessionOpened(with(any(Session.class)));
			inSequence(sequence);
		}});
		
		// test
		SessionImpl session = new SessionImpl(false, sessionHandler, beepStream) {
			@Override
			protected ManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		session.connectionEstablished(null);
		session.receiveRPY(0, 0, message);
		
		// verify
		assertIsSatisfied();
	}
	
	public void testReceiveRPY() throws Exception {
		final ManagementProfile profile = context.mock(ManagementProfile.class); 
		final ChannelHandler channelHandler = context.mock(ChannelHandler.class);
		final ReplyHandler replyHandler = context.mock(ReplyHandler.class);
		
		final Message greeting = new MessageStub();
		final Message sentGreeting = new MessageStub();
		final Message message = new MessageStub();
		final Message reply = new MessageStub();
		
		final ParameterCaptureAction<InternalChannel> extractChannel = new ParameterCaptureAction<InternalChannel>(0, InternalChannel.class, null);
		
		// define expectations
		context.checking(new Expectations() {{
			one(profile).createChannelHandler(with(any(SessionManager.class)), with(any(InternalChannel.class)));
			will(returnValue(channelHandler));
			inSequence(sequence);
			
			one(beepStream).channelStarted(0);
			inSequence(sequence);
			
			one(channelHandler).channelOpened(with(any(Channel.class)));
			will(extractChannel);
			inSequence(sequence);
			
			one(sessionHandler).connectionEstablished(with(any(StartSessionRequest.class)));
			inSequence(sequence);
			
			one(profile).createGreeting(with(any(String[].class)));
			will(returnValue(sentGreeting));
			inSequence(sequence);
			
			one(beepStream).sendRPY(0, 0, sentGreeting);
			inSequence(sequence);
			
			one(profile).receivedGreeting(with(same(greeting)));
			will(returnValue(new Greeting(new String[0], new String[0], new String[] { "abc" })));
			inSequence(sequence);
			
			one(sessionHandler).sessionOpened(with(any(Session.class)));
			inSequence(sequence);
			
			one(beepStream).sendMSG(0, 1, message);
			inSequence(sequence);
			
			one(replyHandler).receivedRPY(reply);
			inSequence(sequence);
		}});

		// test
		SessionImpl session = new SessionImpl(false, sessionHandler, beepStream) {
			@Override
			protected ManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		session.connectionEstablished(null);
		session.receiveRPY(0, 0, greeting);
		extractChannel.getParameter().sendMessage(message, replyHandler);
		session.receiveRPY(0, 1, reply);
		
		// verify
		assertIsSatisfied();
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
		final ManagementProfile profile = context.mock(ManagementProfile.class); 
		final ChannelHandler channelHandler = context.mock(ChannelHandler.class);
		
		// define expectations
		context.checking(new Expectations() {{
			one(profile).createChannelHandler(with(any(SessionManager.class)), with(any(InternalChannel.class)));
			will(returnValue(channelHandler));
			inSequence(sequence);
			
			one(beepStream).channelStarted(0);
			inSequence(sequence);
			
			one(channelHandler).channelOpened(with(any(Channel.class)));
			inSequence(sequence);
			
			one(profile).closeChannel(with(equal(0)), with(any(CloseCallback.class)));
			inSequence(sequence);
		}});

		// test
		InternalSession session = new SessionImpl(false, sessionHandler, beepStream) {
			@Override
			protected ManagementProfile createChannelManagementProfile(boolean initiator) {
				return profile;
			}
		};
		
		session.requestChannelClose(0, new NullCloseCallback());
		
		// verify
		assertIsSatisfied();
	}
	
}
