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
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageStub;
import net.sf.beep4j.NullReplyListener;
import net.sf.beep4j.ReplyHandler;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

public class ChannelImplTest extends MockObjectTestCase {
	
	private static final String PROFILE = "http://www.example.org/profiles/echo";
	
	private static final int CHANNEL = 1;
	
	private Mock sessionMock;
	
	private InternalSession session;

	private Mock callbackMock;

	private CloseChannelCallback callback;

	private Mock channelHandlerMock;

	private ChannelHandler channelHandler;
		
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		sessionMock = mock(InternalSession.class);
		session = (InternalSession) sessionMock.proxy();
		callbackMock = mock(CloseChannelCallback.class);
		callback = (CloseChannelCallback) callbackMock.proxy();
		channelHandlerMock = mock(ChannelHandler.class);
		channelHandler = (ChannelHandler) channelHandlerMock.proxy();
	}
	
	private void assertIsAlive(InternalChannel channel) {
		assertTrue(channel.isAlive());
		assertFalse(channel.isShuttingDown());
		assertFalse(channel.isDead());
	}

	private void assertIsShuttingDown(InternalChannel channel) {
		assertFalse(channel.isAlive());
		assertTrue(channel.isShuttingDown());
		assertFalse(channel.isDead());
	}

	private void assertIsDead(InternalChannel channel) {
		assertFalse(channel.isAlive());
		assertFalse(channel.isShuttingDown());
		assertTrue(channel.isDead());
	}

	public void testAcceptedCloseRequest() throws Exception {
		InternalChannel channel = new ChannelImpl(session, PROFILE, CHANNEL);
		channel.initChannel(channelHandler);
		
		// define expectations
		// TODO setup ordering constraints
		sessionMock.expects(once()).method("requestChannelClose")
				.with(eq(1), ANYTHING)
				.will(new CloseAcceptingStub(1));
		
		callbackMock.expects(once()).method("closeAccepted");
		
		channelHandlerMock.expects(once()).method("channelClosed");
		
		// test
		channel.close(callback);
		assertIsDead(channel);
		try {
			channel.sendMessage(new MessageStub(), new NullReplyListener());
			fail("sending messages in dead state must fail");
		} catch (IllegalStateException e) {
			// expected
		}
		try {
			channel.close(callback);
			fail("closing a dead channel must fail");
		} catch (IllegalStateException e) {
			// expected
		}
		
		verify();
	}
	
	public void testDelayedAcceptedCloseRequest() throws Exception {
		InternalChannel channel = new ChannelImpl(session, PROFILE, CHANNEL);
		channel.initChannel(channelHandler);
		
		Message message = new MessageStub();
		
		// define expectations
		// TODO setup ordering constraints
		ParameterCaptureStub<ReplyHandler> capture = 
			new ParameterCaptureStub<ReplyHandler>(2, ReplyHandler.class, null);
		
		sessionMock.expects(once()).method("sendMessage")
				.with(eq(1), same(message), ANYTHING)
				.will(capture);
		
		sessionMock.expects(once()).method("requestChannelClose")
				.with(eq(1), ANYTHING)
				.will(new CloseAcceptingStub(1));
		
		callbackMock.expects(once()).method("closeAccepted");
		
		channelHandlerMock.expects(once()).method("channelClosed");
		
		// test
		channel.sendMessage(message, new NullReplyListener());
		channel.close(callback);
		assertIsShuttingDown(channel);
		
		ReplyHandler listener = capture.getParameter();
		listener.receivedNUL();
		assertIsDead(channel);
	}
	
	public void testDeclinedCloseRequest() throws Exception {		
		InternalChannel channel = new ChannelImpl(session, PROFILE, CHANNEL);
		channel.initChannel(channelHandler);
		
		Message message = new MessageStub();
		
		// define expectations
		// TODO: define ordering constraints
		sessionMock.expects(once()).method("requestChannelClose")
				.with(eq(1), ANYTHING)
				.will(new CloseDecliningStub(1, 550, "still working"));
		
		callbackMock.expects(once()).method("closeDeclined")
				.with(eq(550), eq("still working"));
		
		sessionMock.expects(once()).method("sendMessage")
				.with(eq(1), same(message), ANYTHING);
		
		// test
		channel.close(callback);
		assertIsAlive(channel);
		channel.sendMessage(message, new NullReplyListener());
	}
	
	public void testDelayedDeclinedCloseRequest() throws Exception {
		InternalChannel channel = new ChannelImpl(session, PROFILE, CHANNEL);
		channel.initChannel(channelHandler);
		
		Message m1 = new MessageStub();
		Message m2 = new MessageStub();
		
		// define expectations
		// TODO: define ordering constraints
		ParameterCaptureStub<ReplyHandler> capture = 
			new ParameterCaptureStub<ReplyHandler>(2, ReplyHandler.class, null);
		
		sessionMock.expects(once()).method("sendMessage")
				.with(eq(1), same(m1), ANYTHING)
				.will(capture);
		
		sessionMock.expects(once()).method("requestChannelClose")
				.with(eq(1), ANYTHING)
				.will(new CloseDecliningStub(1, 550, "still working"));
		
		callbackMock.expects(once()).method("closeDeclined")
				.with(eq(550), eq("still working"));
		
		sessionMock.expects(once()).method("sendMessage")
				.with(eq(1), same(m2), ANYTHING);
		
		// test
		channel.sendMessage(m1, new NullReplyListener());
		channel.close(callback);
		assertIsShuttingDown(channel);
		
		ReplyHandler listener = capture.getParameter();
		listener.receivedNUL();
		assertIsAlive(channel);
		
		channel.sendMessage(m2, new NullReplyListener());
	}
	
	public void testCloseRequestedAccepted() throws Exception {
		InternalChannel channel = new ChannelImpl(session, PROFILE, CHANNEL);
		ChannelHandler handler = channel.initChannel(channelHandler);
		
		// define expectations
		channelHandlerMock.expects(once()).method("channelCloseRequested")
				.with(ANYTHING)
				.will(new CloseAcceptingRequest(0));
		
		channelHandlerMock.expects(once()).method("channelClosed");
		
		Mock mock = mock(CloseChannelRequest.class);
		mock.expects(once()).method("accept");
		CloseChannelRequest request = (CloseChannelRequest) mock.proxy();
		
		// test
		handler.channelCloseRequested(request);
		assertIsDead(channel);
	}
	
	public void testDelayedCloseRequestedAccepted() throws Exception {
		InternalChannel channel = new ChannelImpl(session, PROFILE, CHANNEL);
		ChannelHandler handler = channel.initChannel(channelHandler);
		
		Message message = new MessageStub();
		
		ParameterCaptureStub<ReplyHandler> capture =
			new ParameterCaptureStub<ReplyHandler>(2, ReplyHandler.class, null);
		
		// define expectations
		sessionMock.expects(once()).method("sendMessage")
				.with(eq(1), same(message), ANYTHING)
				.will(capture);
		
		channelHandlerMock.expects(once()).method("channelCloseRequested")
				.with(ANYTHING)
				.will(new CloseAcceptingRequest(0));
		
		channelHandlerMock.expects(once()).method("channelClosed");
		
		Mock mock = mock(CloseChannelRequest.class);
		mock.expects(once()).method("accept");
		CloseChannelRequest request = (CloseChannelRequest) mock.proxy();

		// test
		channel.sendMessage(message, new NullReplyListener());
		handler.channelCloseRequested(request);
		assertIsShuttingDown(channel);
		
		ReplyHandler listener = capture.getParameter();
		listener.receivedNUL();
		assertIsDead(channel);
	}
	
	public void testCloseRequestedDeclined() throws Exception {
		InternalChannel channel = new ChannelImpl(session, PROFILE, CHANNEL);
		ChannelHandler handler = channel.initChannel(channelHandler);
		
		// define expectations
		channelHandlerMock.expects(once()).method("channelCloseRequested")
				.with(ANYTHING)
				.will(new CloseRejectingRequest(0));
		
		Mock mock = mock(CloseChannelRequest.class);
		mock.expects(once()).method("reject");
		CloseChannelRequest request = (CloseChannelRequest) mock.proxy();
		
		// test
		handler.channelCloseRequested(request);
		assertIsAlive(channel);
	}
	
	public void testDelayedCloseRequestedDeclined() throws Exception {
		InternalChannel channel = new ChannelImpl(session, PROFILE, CHANNEL);
		ChannelHandler handler = channel.initChannel(channelHandler);
		
		Message m1 = new MessageStub();
		Message m2 = new MessageStub();
		
		ParameterCaptureStub<ReplyHandler> capture =
			new ParameterCaptureStub<ReplyHandler>(2, ReplyHandler.class, null);
		
		// define expectations
		sessionMock.expects(once()).method("sendMessage")
				.with(eq(1), same(m1), ANYTHING)
				.will(capture);
		
		channelHandlerMock.expects(once()).method("channelCloseRequested")
				.with(ANYTHING)
				.will(new CloseRejectingRequest(0));
		
		sessionMock.expects(once()).method("sendMessage")
				.with(eq(1), same(m2), ANYTHING);
		
		Mock mock = mock(CloseChannelRequest.class);
		mock.expects(once()).method("reject");
		CloseChannelRequest request = (CloseChannelRequest) mock.proxy();

		// test
		channel.sendMessage(m1, new NullReplyListener());
		handler.channelCloseRequested(request);
		assertIsShuttingDown(channel);
		
		ReplyHandler listener = capture.getParameter();
		listener.receivedNUL();
		assertIsAlive(channel);
		
		channel.sendMessage(m2, new NullReplyListener());
	}
	
	private static class CloseAcceptingStub implements Stub {
		private final int index;
		private CloseAcceptingStub(int index) {
			this.index = index;
		}
		public StringBuffer describeTo(StringBuffer buf) {
			buf.append("stub[accept close request]");
			return buf;
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseChannelCallback callback = (CloseChannelCallback) invocation.parameterValues.get(index);
			callback.closeAccepted();
			return null;
		}
	}
	
	private static class CloseAcceptingRequest implements Stub {
		private final int index;
		private CloseAcceptingRequest(int index) {
			this.index = index;
		}
		public StringBuffer describeTo(StringBuffer buf) {
			buf.append("stub[accept close request]");
			return buf;
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseChannelRequest callback = (CloseChannelRequest) invocation.parameterValues.get(index);
			callback.accept();
			return null;
		}
	}
	
	private static class CloseDecliningStub implements Stub {
		private final int code;
		private final String message;
		private final int index;
		private CloseDecliningStub(int index, int code, String message) {
			this.code = code;
			this.message = message;
			this.index = index;
		}
		public StringBuffer describeTo(StringBuffer buf) {
			buf.append("stub[decline close request]");
			return buf;
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseChannelCallback callback = (CloseChannelCallback) invocation.parameterValues.get(index);
			callback.closeDeclined(code, message);
			return null;
		}
	}
	
	private static class CloseRejectingRequest implements Stub {
		private final int index;
		private CloseRejectingRequest(int index) {
			this.index = index;
		}
		public StringBuffer describeTo(StringBuffer buf) {
			buf.append("stub[decline close request]");
			return buf;
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseChannelRequest callback = (CloseChannelRequest) invocation.parameterValues.get(index);
			callback.reject();
			return null;
		}
	}
	
}
