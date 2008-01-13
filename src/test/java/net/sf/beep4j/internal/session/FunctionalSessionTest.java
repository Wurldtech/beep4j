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

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import junit.framework.TestCase;
import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.StartChannelRequest;
import net.sf.beep4j.StartSessionRequest;
import net.sf.beep4j.internal.management.ManagementMessageBuilder;
import net.sf.beep4j.internal.management.SaxMessageBuilder;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;
import net.sf.beep4j.internal.stream.BeepStream;
import net.sf.beep4j.internal.stream.MessageHandler;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.api.Action;

public class FunctionalSessionTest extends TestCase {

	private static final String PROFILE = "http://www.example.com/profiles/echo";
	
	private SessionHandler sessionHandler;
	
	private BeepStream beepStream;
	
	private Mockery context;
	
	private Sequence sequence;
	
	@Override
	protected void setUp() throws Exception {
		context = new Mockery();
		sequence = context.sequence("main-sequence");
		sessionHandler = context.mock(SessionHandler.class);
		beepStream = context.mock(BeepStream.class);
		setupTransportMapping();
	}
	
	private void assertIsSatisfied() {
		context.assertIsSatisfied();
	}
	
	private void setupTransportMapping() {
		context.checking(new Expectations() {{ 
			one(beepStream).channelStarted(0); inSequence(sequence);
		}});
	}

	/*
	 * Scenario: session accepted, rejected by local peer
	 */
	public void testRejectSessionStart() throws Exception {
		// define expectations
		context.checking(new Expectations() {{ 
			one(sessionHandler).connectionEstablished(with(any(StartSessionRequest.class)));
			will(rejectSessionStart()); inSequence(sequence);
			
			one(beepStream).sendERR(with(equal(0)), with(equal(0)), with(equal(createErrorMessage(421, "service not available"))));
			inSequence(sequence);
			
			one(beepStream).closeTransport(); inSequence(sequence);
		}});
		
		// test
		SessionImpl session = new SessionImpl(false, sessionHandler, beepStream);
		session.connectionEstablished(null);
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Scenario: session initiated, rejected by remote peer.
	 */
	public void testSessionStartRejected() throws Exception {
		context.checking(new Expectations() {{ 
			one(sessionHandler).connectionEstablished(with(any(StartSessionRequest.class)));
			will(acceptSessionStart(new String[] { PROFILE })); inSequence(sequence);
			
			one(beepStream).sendRPY(with(equal(0)), with(equal(0)), with(equal(createGreetingMessage(new String[] { PROFILE }))));
			inSequence(sequence);
			
			one(sessionHandler).sessionStartDeclined(550, "listener not available");
			inSequence(sequence);
			
			one(beepStream).closeTransport(); inSequence(sequence);
		}});
		
		SessionImpl session = new SessionImpl(true, sessionHandler, beepStream);
		session.connectionEstablished(null);
		session.receiveERR(0, 0, createErrorMessage(550, "listener not available"));
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 1: open session, start channel, send message, close channel and session
	 * - open session
	 * - initiator successfully initiates channel
	 * - send message and receive response (twice)
	 * - close channel
	 * - close session
	 */
	public void testNormalOperationChannelStartedByLocalPeer() throws Exception {
		SessionImpl session = openSession(true, new String[0], new String[] { PROFILE });
		
		ChannelStruct channel = startChannel(1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);
		
		sendAndReceiveEcho(session, channel.channel, 1, 1, "abcdefghijklmnopqrstuvwxyz");
		sendAndReceiveEcho(session, channel.channel, 1, 2, "abcdefghijk");
		
		closeChannel(session, channel, 1, 2);
		closeSession(session, 3);
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 2: open session, start channel, send message, close channel and session
	 * - open session (listener)
	 * - remote peer (initiator) successfully initiate channel
	 * - receive message and send response (twice)
	 * - close channel requested and accepted
	 * - close session requested and accepted
	 */
	public void testNormalOperationChannelStartedByRemotePeer() throws Exception {
		SessionImpl session = openSession(false, new String[] { PROFILE }, new String[0]);
		
		ChannelStruct channel = startChannelRequested(1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);
		
		receiveAndReplyEcho(session, channel, 1, 1, "abcdefghijklmnopqrstuvwxyz");
		receiveAndReplyEcho(session, channel, 1, 2, "abcdefghijk");
		
		expectAndDoCloseChannelRequested(session, channel, 1, 2);
		closeSessionRequested(session, 3);
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 3: reject session/channel close and closing transport
	 * - open session (listener)
	 * - remote peer (initiator) successfully initiates channel
	 * - receive message and send response
	 * - request close channel, rejected
	 * - receive message and send response
	 * - close session requested, rejected (there are still open channels)
	 * - drop connection
	 */
	public void testRejectSessionAndChannelCloseAndCloseTransport() throws Exception {
		SessionImpl session = openSession(false, new String[] { PROFILE }, new String[0]);
		
		ChannelStruct channel = startChannelRequested(1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);
		
		receiveAndReplyEcho(session, channel, 1, 1, "abcdefghijklmnopqrstuvwxyz");
		closeChannelRejected(session, channel, 1, 1);
		receiveAndReplyEcho(session, channel, 1, 2, "abcdefghijk");
		
		closeSessionRequestedReject(session, 2);
		
		connectionClosed(session);
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 4: abort session when receiving out of sequence response
	 * - open session (listener)
	 * - remote peer (initiator) successfully initiates channel
	 * - send message 1
	 * - send message 2
	 * - receive reply to message 2 (results in ProtocolException)
	 */
	public void testAbortSessionWhenReceivingOutOfSequenceResponse() throws Exception {
		SessionImpl session = openSession(false, new String[] { PROFILE }, new String[0]);
		
		ChannelStruct channel = startChannelRequested(1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);
		
		sendEcho(channel.channel, 1, 1, "abcdefghijklmnopqrstuvwxyz");
		sendEcho(channel.channel, 1, 2, "abcdefghijk");
		
		try {
			session.receiveRPY(1, 2, createEchoMessage("abcdefghijk"));
			fail("expects receiving message 1 on channel 1, must result in ProtocolException");
		} catch (ProtocolException e) {
			// expected
		}
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 5: receive close channel request when close already initiated
	 * - open session (listener)
	 * - remote peer (initiator) successfully initiates channel
	 * - local peer initiates channel close
	 * - receive close reqest from remote peer
	 * -> results in immediate ok message
	 * -> results in callback from initial request beeing called back
	 */
	public void testReceiveCloseChannelRequestWhenCloseAlreadyInitiated() throws Exception {
		SessionImpl session = openSession(false, new String[] { PROFILE }, new String[0]);
		final ChannelStruct channel = startChannelRequested(1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);
		
		final CloseChannelCallback callback = context.mock(CloseChannelCallback.class);
		requestChannelClose(beepStream, channel.channel, callback, 1, 1);
		
		// close requested and immediately accepted without calling back the application
		Message request = createCloseMessage(1);		
		final Message reply = createOkMessage();
		
		// expectations
		context.checking(new Expectations() {{
			one(callback).closeAccepted(); inSequence(sequence);
			
			one(channel.handler).channelClosed();
			inSequence(sequence);
			
			one(beepStream).sendRPY(0, 1, reply);
			inSequence(sequence);
			
			one(beepStream).channelClosed(1);
			inSequence(sequence);
		}});
		
		// send close channel request
		session.receiveMSG(0, 1, request);
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 6: local peer rejects a channel start request
	 * - open session (listener)
	 * - remote peer (initiator) tries to initiate channel
	 * - local peer rejects channel creation
	 */
	public void testLocalPeerRejectsChannelStart() throws Exception {
		SessionImpl session = openSession(false, new String[] { PROFILE }, new String[0]);
		startChannelRequestedReject(session, 1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) });
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 7: remote peer rejects a channel start request
	 * - open session (initiator)
	 * - local peer tries to start channel
	 * - remote peer rejects channel creation
	 */
	public void testRemotePeerRejectsChannelStart() throws Exception {
		SessionImpl session = openSession(true, new String[0], new String[] { PROFILE });
		assertTrue(Arrays.equals(new String[] { PROFILE }, session.getProfiles()));
		startChannelRejected(session, 1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) });
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 8: receive close session request when close session already initiated
	 * - open session
	 * - local peer tries to close session
	 * - receive close session request from remote peer
	 * -> immediate ok message
	 * -> close transport
	 * -> callback? notified
	 */
	public void testReceiveCloseSessionRequestWhenCloseAlreadyInitiated() throws Exception {
		SessionImpl session = openSession(false, new String[] { PROFILE }, new String[0]);
		initiateCloseSession(session, 1);
		
		context.checking(new Expectations() {{			
			one(sessionHandler).sessionClosed(); inSequence(sequence);
			
			one(beepStream).sendRPY(0, 1, createOkMessage()); inSequence(sequence);
			
			one(beepStream).closeTransport(); inSequence(sequence);
		}});
		
		session.receiveMSG(0, 1, createCloseMessage(0));
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 9:
	 * - open session
	 * - remote peer creates channel 1
	 * - remote peer again creates channel 1
	 * - session is terminated because of a protocol exception
	 */
	public void testStartRequestOfOpenChannelTerminatesSession() throws Exception {
		String[] profiles = new String[] { PROFILE };		
		SessionImpl session = openSession(true, profiles, new String[0]);
		
		startChannelRequested(2, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);

		try {
			session.receiveMSG(0, 2, createStartMessage(2, new ProfileInfo[] { new ProfileInfo(PROFILE) }));
			fail("starting an open channel must fail");
		} catch (ProtocolException e) {
			// expected
		}
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 10:
	 * - open session
	 * - start channel
	 * - local peer sends a message
	 * - remote peer requests channel close
	 * - local peer does not accept request right away
	 * - remote peer sends reply
	 * - local peer closes channel and sends positive reply
	 */
	public void testDelayedChannelCloseWhenRequested() throws Exception {
		String[] profiles = new String[] { PROFILE };		
		SessionImpl session = openSession(true, profiles, new String[0]);
		ChannelStruct channel = startChannel(1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);
		ReplyHandler replyHandler = sendEcho(channel.channel, 1, 1, "Hello World");
		
		final Message message = createEchoMessage("Hello World");
		expectReceiveEcho(replyHandler, message);
		expectCloseChannelRequested(channel, 1, 2);
		doCloseChannelRequested(session, 1, 2);
		doReceiveEcho(session, 1, 1, message);
		
		// verify
		assertIsSatisfied();
	}
	
	/*
	 * Test Scenario 11:
	 * - open session
	 * - start channel
	 * - local peer initiates channel close
	 * - remote peer sends message
	 * - local peer replies to this message
	 * - local peer receives positive reply to channel close
	 */
	public void testReplyingToMessagesWhenCloseInitiated() throws Exception {
		String[] profiles = new String[] { PROFILE };		
		SessionImpl session = openSession(true, profiles, new String[0]);
		ChannelStruct channel = startChannel(1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);
		
		expectSendCloseChannelMessage(1, 2);
		CloseChannelCallback callback = performChannelClose(channel.channel);
		receiveAndReplyEcho(session, channel, 1, 1, "Hello World");
		
		expectCloseChannelAccepted(channel, callback, 1);
		receiveOkMessage(session, 2);
		
		// verify
		assertIsSatisfied();
	}
	
	private SessionImpl openSession(boolean initiator, final String[] profiles, String[] remoteProfiles) {
		context.checking(new Expectations() {{
			one(beepStream).sendRPY(0, 0, createGreetingMessage(profiles)); inSequence(sequence);
			
			one(sessionHandler).connectionEstablished(with(any(StartSessionRequest.class)));
			will(acceptSessionStart(profiles));
		}});

		final SessionImpl session = new SessionImpl(initiator, sessionHandler, beepStream);
		
		context.checking(new Expectations() {{
			one(sessionHandler).sessionOpened(with(same(session)));
			inSequence(sequence);
		}});
		
		session.connectionEstablished(null);
		session.receiveRPY(0, 0, createGreetingMessage(remoteProfiles));
		
		return session;
	}
	
	private ChannelStruct startChannel(int channelNumber, int messageNumber, ProfileInfo[] profiles, SessionImpl session) {
		final ChannelHandler channelHandler = context.mock(ChannelHandler.class);
		final ParameterCaptureAction<Channel> channelExtractor = 
				new ParameterCaptureAction<Channel>(0, Channel.class, null);
		
		// expectations
		context.checking(new Expectations() {{
			one(beepStream).sendMSG(with(equal(0)), with(equal(1)), with(equal(createStartMessage(1, new ProfileInfo[] { new ProfileInfo(PROFILE) }))));
			inSequence(sequence);
			
			one(beepStream).channelStarted(1); inSequence(sequence);

			one(channelHandler).channelOpened(with(any(Channel.class)));
			will(channelExtractor); inSequence(sequence);			
		}});
		
		// start channel
		session.startChannel(PROFILE, channelHandler);
		session.receiveRPY(0, 1, createProfileMessage(new ProfileInfo(PROFILE)));
		
		Channel channel = channelExtractor.getParameter();
		return new ChannelStruct(channel, channelHandler);
	}
	
	private ChannelStruct startChannelRequested(
			final int channelNumber, 
			final int messageNumber, 
			ProfileInfo[] profiles, 
			SessionImpl session) {
		
		final ChannelHandler channelHandler = context.mock(ChannelHandler.class, "channel-" + System.currentTimeMillis());
		final ProfileInfo profile = profiles[0];
		
		final ParameterCaptureAction<Channel> channelExtractor = 
			new ParameterCaptureAction<Channel>(0, Channel.class, null);
		
		context.checking(new Expectations() {{ 
			one(sessionHandler).channelStartRequested(with(any(StartChannelRequest.class)));
			will(acceptStartChannel(profile, channelHandler)); inSequence(sequence);
			
			one(beepStream).channelStarted(channelNumber); inSequence(sequence);
			
			one(channelHandler).channelOpened(with(any(Channel.class))); 
			will(channelExtractor); inSequence(sequence);
			
			one(beepStream).sendRPY(0, messageNumber, createProfileMessage(profile)); inSequence(sequence);
		}});
		
		session.receiveMSG(0, messageNumber, createStartMessage(channelNumber, profiles));		
		
		Channel channel = channelExtractor.getParameter();
		return new ChannelStruct(channel, channelHandler);
	}
	
	private void startChannelRequestedReject(
			SessionImpl session, 
			int channelNumber, 
			final int messageNumber, 
			ProfileInfo[] profiles) {
		
		context.checking(new Expectations() {{
			one(sessionHandler).channelStartRequested(with(any(StartChannelRequest.class)));
			will(rejectStartChannel(550, "no profiles supported")); inSequence(sequence);
			
			one(beepStream).sendERR(0, messageNumber, createErrorMessage(550, "no profiles supported"));
			inSequence(sequence);
		}});

		session.receiveMSG(0, messageNumber, createStartMessage(channelNumber, profiles));		
	}
	
	private void startChannelRejected(
			final SessionImpl session, 
			final int channelNumber, 
			final int messageNumber, 
			final ProfileInfo[] profiles) {
		
		final ChannelHandler channelHandler = context.mock(ChannelHandler.class);
		final int errorCode = 550;
		final String errorMessage = "no profiles supported";
		
		context.checking(new Expectations() {{
			one(beepStream).sendMSG(0, messageNumber, createStartMessage(channelNumber, profiles));
			inSequence(sequence);
			
			one(sessionHandler).channelStartFailed(profiles[0].getUri(), channelHandler, errorCode, errorMessage);
			inSequence(sequence);
		}});
		
		session.startChannel(profiles[0], channelHandler);
		session.receiveERR(0, messageNumber, createErrorMessage(errorCode, errorMessage));
	}

	private void closeChannel(
			final SessionImpl session, 
			final ChannelStruct channel, 
			final int channelNumber, 
			final int messageNumber) {
		
		expectSendCloseChannelMessage(channelNumber, messageNumber);
		CloseChannelCallback callback = performChannelClose(channel.channel);
		expectCloseChannelAccepted(channel, callback, channelNumber);
		receiveOkMessage(session, messageNumber);
	}
	
	private CloseChannelCallback performChannelClose(final Channel channel) {
		final CloseChannelCallback callback = context.mock(CloseChannelCallback.class);
		channel.close(callback);
		return callback;
	}
	
	private void expectSendCloseChannelMessage(
			final int channelNumber,
			final int messageNumber) {
		
		context.checking(new Expectations() {{
			one(beepStream).sendMSG(0, messageNumber, createCloseMessage(channelNumber));
			inSequence(sequence);
		}});
	}
	
	private void expectCloseChannelAccepted(
			final ChannelStruct channel, 
			final CloseChannelCallback callback,
			final int channelNumber) {
		
		context.checking(new Expectations() {{
			one(callback).closeAccepted(); inSequence(sequence);
			
			one(channel.handler).channelClosed(); inSequence(sequence);
			
			one(beepStream).channelClosed(channelNumber); inSequence(sequence);
		}});
	}
	
	private void receiveOkMessage(
			final SessionImpl session,
			final int messageNumber) {
		
		session.receiveRPY(0, messageNumber, createOkMessage());
	}
	
	private void closeChannelRejected(
			final SessionImpl session, 
			final ChannelStruct channel,
			final int channelNumber, 
			final int messageNumber) {
		
		final CloseChannelCallback callback = context.mock(CloseChannelCallback.class);
		
		context.checking(new Expectations() {{
			one(beepStream).sendMSG(0, messageNumber, createCloseMessage(channelNumber)); 
			inSequence(sequence);
		}});

		channel.channel.close(callback);

		final int errorCode = 550;
		final String errorMessage = "still working";
		
		context.checking(new Expectations() {{
			one(callback).closeDeclined(errorCode, errorMessage);  
			inSequence(sequence);
		}});

		session.receiveERR(0, messageNumber, createErrorMessage(550, errorMessage));
	}
	
	private void requestChannelClose(
			final BeepStream beepStream,
			Channel channel,
			CloseChannelCallback callback,
			int channelNumber,
			final int messageNumber) {
		final Message request = createCloseMessage(channelNumber);
		
		// define expectation
		context.checking(new Expectations() {{ 
			one(beepStream).sendMSG(0, messageNumber, request);
		}});

		// request channel close
		channel.close(callback);
	}
	
	private void expectAndDoCloseChannelRequested(
			SessionImpl session, 
			final ChannelStruct channel, 
			final int channelNumber, 
			final int messageNumber) {
		expectCloseChannelRequested(channel, channelNumber, messageNumber);
		doCloseChannelRequested(session, channelNumber, messageNumber);
	}

	private void doCloseChannelRequested(
			final SessionImpl session,
			final int channelNumber,
			final int messageNumber) {
		session.receiveMSG(0, messageNumber, createCloseMessage(channelNumber));
	}

	private void expectCloseChannelRequested(
			final ChannelStruct channel,
			final int channelNumber,
			final int messageNumber) {
		context.checking(new Expectations() {{
			one(channel.handler).channelCloseRequested(with(any(CloseChannelRequest.class)));
			will(acceptCloseChannel()); inSequence(sequence);
			
			one(channel.handler).channelClosed(); inSequence(sequence);
			
			one(beepStream).sendRPY(0, messageNumber, createOkMessage()); inSequence(sequence);
			
			one(beepStream).channelClosed(channelNumber); inSequence(sequence);			
		}});
	}

	private void closeSession(SessionImpl session, int messageNumber) {
		initiateCloseSession(session, messageNumber);
		
		context.checking(new Expectations() {{
			one(sessionHandler).sessionClosed(); inSequence(sequence);
			
			one(beepStream).closeTransport(); inSequence(sequence);
		}});

		session.receiveRPY(0, messageNumber, createOkMessage());
	}
	
	private void initiateCloseSession(SessionImpl session, final int messageNumber) {
		context.checking(new Expectations() {{
			one(beepStream).sendMSG(0, messageNumber, createCloseMessage(0));
		}});
		session.close();
	}
	
	private void closeSessionRequested(SessionImpl session, final int messageNumber) {
		Message request = createCloseMessage(0);
		
		context.checking(new Expectations() {{			
			one(beepStream).sendRPY(0, messageNumber, createOkMessage()); inSequence(sequence);
			
			one(sessionHandler).sessionClosed(); inSequence(sequence);
			
			one(beepStream).closeTransport(); inSequence(sequence);
		}});

		session.receiveMSG(0, messageNumber, request);
	}
	
	private void closeSessionRequestedReject(SessionImpl session, final int messageNumber) {
		Message request = createCloseMessage(0);
		
		context.checking(new Expectations() {{
			one(beepStream).sendERR(0, messageNumber, createErrorMessage(550, "still working"));
		}});

		session.receiveMSG(0, messageNumber, request);
	}
	
	private ReplyHandler sendEcho(Channel channel, final int channelNumber, final int messageNumber, String content) throws IOException {
		final ReplyHandler replyHandler = context.mock(ReplyHandler.class, "echo-" + channelNumber + "-" + messageNumber);
		final Message request = createEchoMessage(content);
		
		// define expectations
		context.checking(new Expectations() {{ 
			one(beepStream).sendMSG(channelNumber, messageNumber, request);
		}});
		
		// send message
		channel.sendMessage(request, replyHandler);
		
		return replyHandler;
	}
	
	private void receiveEcho(
			final MessageHandler messageHandler, 
			final ReplyHandler handler, 
			final int channelNumber, 
			final int messageNumber, 
			final String content) throws IOException {
		
		final Message reply = createEchoMessage(content);
		expectReceiveEcho(handler, reply);
		doReceiveEcho(messageHandler, channelNumber, messageNumber, reply);
	}

	private void doReceiveEcho(
			final MessageHandler messageHandler,
			final int channelNumber, final int messageNumber,
			final Message reply) {
		messageHandler.receiveRPY(channelNumber, messageNumber, reply);
	}

	private void expectReceiveEcho(
			final ReplyHandler handler,
			final Message reply) {
		context.checking(new Expectations() {{ 
			one(handler).receivedRPY(reply); inSequence(sequence);
		}});
	}
	
	private void sendAndReceiveEcho(
			MessageHandler messageHandler, 
			Channel channel, 
			int channelNumber, 
			int messageNumber, 
			String content) throws IOException {
		
		ReplyHandler handler = sendEcho(channel, channelNumber, messageNumber, content);
		receiveEcho(messageHandler, handler, channelNumber, messageNumber, content);
	}
	
	private void receiveAndReplyEcho(
			final MessageHandler messageHandler, 
			final ChannelStruct channel,
			final int channelNumber, 
			final int messageNumber, 
			final String content) throws IOException {
		
		final ParameterCaptureAction<Reply> extractReply = 
				new ParameterCaptureAction<Reply>(1, Reply.class, null);
		
		final Message request = createEchoMessage(content);
		final Message reply = createEchoMessage(content);
		
		// expectations
		context.checking(new Expectations() {{
			one(channel.handler).messageReceived(with(equal(request)), with(any(Reply.class)));
			will(extractReply); inSequence(sequence);
		}});
		
		messageHandler.receiveMSG(channelNumber, messageNumber, request);
		
		Reply responseHandler = extractReply.getParameter();
		
		// expectations
		context.checking(new Expectations() {{
			one(beepStream).sendRPY(channelNumber, messageNumber, reply); inSequence(sequence);
		}});

		responseHandler.sendRPY(reply);
	}
	
	private void connectionClosed(SessionImpl session) {
		context.checking(new Expectations() {{
			one(sessionHandler).sessionClosed(); inSequence(sequence);
		}});

		session.connectionClosed();
	}
	
	private static Message createEchoMessage(String content) throws IOException {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("text", "plain");
		Writer writer = messageBuilder.getWriter();
		writer.write(content);
		writer.close();
		return messageBuilder.getMessage();
	}
	
	private static Message createErrorMessage(int code, String message) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createError(messageBuilder, code, message);
	}
	
	private static Message createOkMessage() {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createOk(messageBuilder);
	}
	
	private static Message createGreetingMessage(String[] profiles) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createGreeting(messageBuilder, profiles);
	}
	
	private static Message createProfileMessage(ProfileInfo profile) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createProfile(messageBuilder, profile);
	}
	
	private static Message createStartMessage(int channelNumber, ProfileInfo[] profiles) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createStart(messageBuilder, channelNumber, profiles);
	}
	
	private static Message createCloseMessage(int channelNumber) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createClose(messageBuilder, channelNumber, 200);
	}
	
	private static class ChannelStruct {
		private final ChannelHandler handler;
		private final Channel channel;
		private ChannelStruct(Channel channel, ChannelHandler handler) {
			this.channel = channel;
			this.handler = handler;
		}
	}
	
	private static Action rejectSessionStart() {
		return new StartSessionRequestRejector();
	}

	private static class StartSessionRequestRejector implements Action {
		public void describeTo(Description description) {
			description.appendText("rejects session start");
		}
		public Object invoke(org.jmock.api.Invocation invocation) throws Throwable {
			StartSessionRequest request = (StartSessionRequest) invocation.getParameter(0);
			request.cancel();
			return null;
		}
	}
	
	private static Action acceptSessionStart(String[] profileUris) {
		return new StartSessionRequestAcceptor(profileUris);
	}
	
	private static class StartSessionRequestAcceptor implements Action {
		private final String[] profileUris;
		public StartSessionRequestAcceptor(String[] profileUris) {
			this.profileUris = profileUris;
		}
		public void describeTo(Description description) {
			description.appendText("accepts session start");
		}
		public Object invoke(org.jmock.api.Invocation invocation) throws Throwable {
			StartSessionRequest request = (StartSessionRequest) invocation.getParameter(0);
			for (int i = 0; i < profileUris.length; i++) {
				request.registerProfile(profileUris[i]);
			}
			return null;
		}
	}
	
	private static Action acceptStartChannel(ProfileInfo profile, ChannelHandler channelHandler) {
		return new StartChannelRequestAcceptor(profile, channelHandler);
	}
	
	private static class StartChannelRequestAcceptor implements Action {
		private final ProfileInfo profile;
		private final ChannelHandler channelHandler;
		private StartChannelRequestAcceptor(ProfileInfo profile, ChannelHandler channelHandler) {
			this.profile = profile;
			this.channelHandler = channelHandler;
		}
		public void describeTo(Description description) {
			description.appendText("accepts channel start");
		}
		public Object invoke(org.jmock.api.Invocation invocation) throws Throwable {
			StartChannelRequest request = (StartChannelRequest) invocation.getParameter(0);
			request.selectProfile(profile, channelHandler);
			return null;
		}
	}
	
	private static Action rejectStartChannel(int code, String message) {
		return new StartChannelRequestRejector(code, message);
	}
	
	private static class StartChannelRequestRejector implements Action {
		private final int code;
		private final String message;
		private StartChannelRequestRejector(int code, String message) {
			this.code = code;
			this.message = message;
		}
		public void describeTo(Description description) {
			description.appendText("rejects channel start");
		}
		public Object invoke(org.jmock.api.Invocation invocation) throws Throwable {
			StartChannelRequest request = (StartChannelRequest) invocation.getParameter(0);
			request.cancel(code, message);
			return null;
		}
	}
	
	private static Action acceptCloseChannel() {
		return new CloseChannelAcceptor();
	}
	
	private static class CloseChannelAcceptor implements Action {
		public void describeTo(Description description) {
			description.appendText("accepts close channel");
		}
		public Object invoke(org.jmock.api.Invocation invocation) throws Throwable {
			CloseChannelRequest callback = (CloseChannelRequest) invocation.getParameter(0);
			callback.accept();
			return null;
		}
	}
	
}
