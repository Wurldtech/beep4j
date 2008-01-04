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

import junit.framework.TestCase;
import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageStub;
import net.sf.beep4j.NullReplyHandler;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.internal.management.CloseCallback;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

public class ChannelImplTest extends TestCase {
	
	private static final String PROFILE = "http://www.example.org/profiles/echo";
	
	private static final int CHANNEL = 1;
	
	private InternalSession session;

	private ChannelHandler channelHandler;

	private Mockery context;

	private Sequence sequence;

	private InternalChannel channel;
		
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		context = new Mockery();
		session = context.mock(InternalSession.class);
		channelHandler = context.mock(ChannelHandler.class);
		
		sequence = context.sequence("main-sequence");
		
		// opening the channel causes channelOpened to be called
		context.checking(new Expectations() {{
			one(channelHandler).channelOpened(with(any(Channel.class))); inSequence(sequence);
		}});
		
		channel = new ChannelImpl(session, PROFILE, CHANNEL, null);
		channel.channelOpened(channelHandler);
	}
	
	private void assertIsSatisfied() {
		context.assertIsSatisfied();
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

	/*
	 * - application requests channel close
	 * - channel is in state that allows sending the request immediately
	 * - remote peer accepts request
	 * - channel is closed and moves to state dead
	 */
	public void testAcceptedCloseRequest() throws Exception {		
		final CloseChannelCallback callback = context.mock(CloseChannelCallback.class);
		
		// define expectations
		context.checking(new Expectations() {{
			one(session).requestChannelClose(with(equal(1)), with(any(CloseCallback.class))); 
			will(acceptCloseChannel(1)); inSequence(sequence);
			
			one(callback).closeAccepted(); inSequence(sequence);
			one(channelHandler).channelClosed(); inSequence(sequence);
		}});
		
		// test
		channel.close(callback);
		assertIsDead(channel);
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * - application requests channel close
	 * - channel is in state that does not allow sending the request immediately
	 * - the outstanding message is replied to
	 * - remote peer accepts request
	 * - channel is closed and moves to state dead
	 */
	public void testDelayedAcceptedCloseRequest() throws Exception {		
		final CloseChannelCallback callback = context.mock(CloseChannelCallback.class);
		final Message message = new MessageStub();
		
		// used to capture the ReplyHandler
		final ParameterCaptureAction<ReplyHandler> capture = 
			new ParameterCaptureAction<ReplyHandler>(3, ReplyHandler.class, null);
		
		// define expectations
		context.checking(new Expectations() {{
			one(session).sendMSG(with(equal(1)), with(equal(1)), with(same(message)), with(any(ReplyHandler.class)));
			will(capture); inSequence(sequence);
			
			one(session).requestChannelClose(with(equal(1)), with(any(CloseCallback.class)));
			will(acceptCloseChannel(1)); inSequence(sequence);
			
			one(callback).closeAccepted(); inSequence(sequence);
			one(channelHandler).channelClosed(); inSequence(sequence);
		}});
		
		// test
		channel.sendMessage(message, new NullReplyHandler());
		channel.close(callback);
		assertIsShuttingDown(channel);
		
		// reply to the message so that the channel close request can be sent
		capture.getParameter().receivedNUL();
		assertIsDead(channel);
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * - application tries to close a channel
	 * - close request is denied by remote peer
	 * - application then sends another message to show that channel still works
	 */
	public void testDeclinedCloseRequest() throws Exception {
		final CloseChannelCallback callback = context.mock(CloseChannelCallback.class);
		final Message message = new MessageStub();
		
		// define expectations
		context.checking(new Expectations() {{
			one(session).requestChannelClose(with(equal(1)), with(any(CloseCallback.class)));
			will(declineCloseChannel(1, 550, "still working")); inSequence(sequence);
			
			one(callback).closeDeclined(550, "still working"); inSequence(sequence);
			
			one(session).sendMSG(with(equal(1)), with(equal(1)), with(same(message)), with(any(ReplyHandler.class)));
			inSequence(sequence);
		}});
		
		// test
		channel.close(callback);
		assertIsAlive(channel);
		channel.sendMessage(message, new NullReplyHandler());
		
		// verify
		context.assertIsSatisfied();
	}
	
	public void testDelayedDeclinedCloseRequest() throws Exception {
		final CloseChannelCallback callback = context.mock(CloseChannelCallback.class);
		final Message m1 = new MessageStub();
		final Message m2 = new MessageStub();
		
		final ParameterCaptureAction<ReplyHandler> capture = 
			new ParameterCaptureAction<ReplyHandler>(3, ReplyHandler.class, null);
		
		// define expectations
		context.checking(new Expectations() {{
			one(session).sendMSG(with(equal(1)), with(equal(1)), with(same(m1)), with(any(ReplyHandler.class)));
			will(capture); inSequence(sequence);
			
			one(session).requestChannelClose(with(equal(1)), with(any(CloseCallback.class)));
			will(declineCloseChannel(1, 550, "still working")); inSequence(sequence);
			
			one(callback).closeDeclined(550, "still working"); inSequence(sequence);
			
			one(session).sendMSG(with(equal(1)), with(equal(2)), with(same(m2)), with(any(ReplyHandler.class)));
			inSequence(sequence);
		}});
		
		// test
		channel.sendMessage(m1, new NullReplyHandler());
		channel.close(callback);
		assertIsShuttingDown(channel);
		
		// complete the reply by sending a NUL message
		capture.getParameter().receivedNUL();
		
		// channel must be alive again
		assertIsAlive(channel);
		
		// try that the channel works by sending an arbitrary message
		channel.sendMessage(m2, new NullReplyHandler());
		
		// verify
		assertIsSatisfied();
	}
	
	public void testCloseRequestedAccepted() throws Exception {	
		final CloseCallback callback = context.mock(CloseCallback.class);
		
		// define expectations
		context.checking(new Expectations() {{
			one(channelHandler).channelCloseRequested(with(any(CloseChannelRequest.class)));
			will(acceptCloseChannelRequest(0)); inSequence(sequence);
			
			one(channelHandler).channelClosed(); inSequence(sequence);
			one(callback).closeAccepted(); inSequence(sequence);
		}});
		
		// test
		channel.channelCloseRequested(callback);
		assertIsDead(channel);
		
		// verify
		assertIsSatisfied();
	}
	
	public void testDelayedCloseRequestedAccepted() throws Exception {
		final Message message = new MessageStub();
		
		final ParameterCaptureAction<ReplyHandler> capture =
			new ParameterCaptureAction<ReplyHandler>(3, ReplyHandler.class, null);
		
		final CloseCallback callback = context.mock(CloseCallback.class);
		
		// define expectations
		context.checking(new Expectations() {{
			one(session).sendMSG(with(equal(1)), with(equal(1)), with(same(message)), with(any(ReplyHandler.class)));
			will(capture); inSequence(sequence);
			
			one(channelHandler).channelCloseRequested(with(any(CloseChannelRequest.class)));
			will(acceptCloseChannelRequest(0)); inSequence(sequence);
			
			one(channelHandler).channelClosed(); inSequence(sequence);
			one(callback).closeAccepted(); inSequence(sequence);
		}});

		// test
		channel.sendMessage(message, new NullReplyHandler());
		channel.channelCloseRequested(callback);
		assertIsShuttingDown(channel);
		
		capture.getParameter().receivedNUL();
		assertIsDead(channel);
		
		// verify
		assertIsSatisfied();
	}
	
	public void testCloseRequestedDeclined() throws Exception {
		final CloseCallback callback = context.mock(CloseCallback.class);
		
		// define expectations
		context.checking(new Expectations() {{
			one(channelHandler).channelCloseRequested(with(any(CloseChannelRequest.class)));
			will(rejectCloseChannelRequest(0)); inSequence(sequence);
			
			one(callback).closeDeclined(550, "still working"); inSequence(sequence);
		}});
		
		// test
		channel.channelCloseRequested(callback);
		assertIsAlive(channel);
		
		// verify
		assertIsSatisfied();
	}
	
	public void testDelayedCloseRequestedDeclined() throws Exception {
		final Message m1 = new MessageStub();
		final Message m2 = new MessageStub();
		
		final CloseCallback callback = context.mock(CloseCallback.class);
		
		final ParameterCaptureAction<ReplyHandler> capture =
			new ParameterCaptureAction<ReplyHandler>(3, ReplyHandler.class, null);
		
		// define expectations
		context.checking(new Expectations() {{
			one(session).sendMSG(with(equal(1)), with(equal(1)), with(same(m1)), with(any(ReplyHandler.class)));
			will(capture); inSequence(sequence);
			
			one(channelHandler).channelCloseRequested(with(any(CloseChannelRequest.class)));
			will(rejectCloseChannelRequest(0)); inSequence(sequence);
			
			one(callback).closeDeclined(550, "still working"); inSequence(sequence);
			
			one(session).sendMSG(with(equal(1)), with(equal(2)), with(same(m2)), with(any(ReplyHandler.class)));
			inSequence(sequence);
		}});

		// test
		channel.sendMessage(m1, new NullReplyHandler());
		channel.channelCloseRequested(callback);
		assertIsShuttingDown(channel);
		
		capture.getParameter().receivedNUL();
		assertIsAlive(channel);
		
		channel.sendMessage(m2, new NullReplyHandler());
		
		// verify
		assertIsSatisfied();
	}
	
	private static Action acceptCloseChannel(int index) {
		return new CloseAcceptingAction(index);
	}
	
	private static class CloseAcceptingAction implements Action {
		private final int index;
		private CloseAcceptingAction(int index) {
			this.index = index;
		}
		public void describeTo(Description description) {
			description.appendText("stub[accept close request]");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseCallback callback = (CloseCallback) invocation.getParameter(index);
			callback.closeAccepted();
			return null;
		}
	}
	
	private static Action declineCloseChannel(int index, int code, String message) {
		return new CloseDecliningAction(index, code, message);
	}
	
	private static class CloseDecliningAction implements Action {
		private final int code;
		private final String message;
		private final int index;
		private CloseDecliningAction(int index, int code, String message) {
			this.code = code;
			this.message = message;
			this.index = index;
		}
		public void describeTo(Description description) {
			description.appendText("stub[decline close request]");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseCallback callback = (CloseCallback) invocation.getParameter(index);
			callback.closeDeclined(code, message);
			return null;
		}
	}
	
	private static Action acceptCloseChannelRequest(int index) {
		return new CloseRequestAcceptingAction(index);
	}
	
	private static class CloseRequestAcceptingAction implements Action {
		private final int index;
		private CloseRequestAcceptingAction(int index) {
			this.index = index;
		}
		public void describeTo(Description description) {
			description.appendText("stub[accept close request]");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseChannelRequest request = (CloseChannelRequest) invocation.getParameter(index);
			request.accept();
			return null;
		}
	}
	
	private static Action rejectCloseChannelRequest(int index) {
		return new CloseRequestRejectingAction(index);
	}
	
	private static class CloseRequestRejectingAction implements Action {
		private final int index;
		private CloseRequestRejectingAction(int index) {
			this.index = index;
		}
		public void describeTo(Description description) {
			description.appendText("stub[reject close request]");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseChannelRequest request = (CloseChannelRequest) invocation.getParameter(index);
			request.reject();
			return null;
		}
	}
	
}
