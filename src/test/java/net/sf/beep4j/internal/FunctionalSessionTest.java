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

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.Reply;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.StartChannelRequest;
import net.sf.beep4j.StartSessionRequest;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;
import net.sf.beep4j.internal.profile.ChannelManagementMessageBuilder;
import net.sf.beep4j.internal.profile.SaxMessageBuilder;
import net.sf.beep4j.internal.stream.BeepStream;
import net.sf.beep4j.internal.stream.MessageHandler;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

public class FunctionalSessionTest extends MockObjectTestCase {
	
	private static final String PROFILE = "http://www.example.com/profiles/echo";

	private Mock sessionHandlerMock;
	
	private SessionHandler sessionHandler;
	
	private Mock beepStreamMock;
	
	private BeepStream beepStream;
	
	@Override
	protected void setUp() throws Exception {
		sessionHandlerMock = mock(SessionHandler.class);
		sessionHandler = (SessionHandler) sessionHandlerMock.proxy();
		beepStreamMock = mock(BeepStream.class);
		beepStream = (BeepStream) beepStreamMock.proxy();
		setupTransportMapping();
	}
	
	private void setupTransportMapping() {
		beepStreamMock.expects(once()).method("channelStarted").with(eq(0));
	}

	/*
	 * Scenario: session accepted, rejected by local peer
	 */
	public void testRejectSessionStart() throws Exception {
		Stub rejector = new StartSessionRequestRejector(); 
		
		sessionHandlerMock.expects(once()).method("connectionEstablished").will(rejector);
		beepStreamMock.expects(once()).method("sendERR").with(eq(0), eq(0), eq(createErrorMessage(421, "service not available")));
		beepStreamMock.expects(once()).method("closeTransport");
		
		SessionImpl session = new SessionImpl(false, sessionHandler, beepStream);
		session.connectionEstablished(null);
	}
	
	/*
	 * Scenario: session initiated, rejected by remote peer.
	 */
	public void testSessionStartRejected() throws Exception {
		Stub acceptor = new StartSessionRequestAcceptor(new String[] { PROFILE });
		
		sessionHandlerMock.expects(once()).method("connectionEstablished").will(acceptor);
		sessionHandlerMock.expects(once()).method("sessionStartDeclined").with(eq(550), eq("listener not available"));
		beepStreamMock.expects(once()).method("sendRPY")
				.with(eq(0), eq(0), eq(createGreetingMessage(new String[] { PROFILE })));
		beepStreamMock.expects(once()).method("closeTransport");
		
		SessionImpl session = new SessionImpl(true, sessionHandler, beepStream);
		session.connectionEstablished(null);
		session.receiveERR(0, 0, createErrorMessage(550, "listener not available"));
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
		
		closeChannelRequested(session, channel, 1, 2);
		closeSessionRequested(session, 3);
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
		ChannelStruct channel = startChannelRequested(1, 1, new ProfileInfo[] { new ProfileInfo(PROFILE) }, session);
		
		Mock callbackMock = mock(CloseChannelCallback.class);
		CloseChannelCallback callback = (CloseChannelCallback) callbackMock.proxy();
		requestChannelClose(beepStreamMock, channel.channel, callback, 1, 1);
		
		// close requested and immediately accepted without calling back the application
		Message request = createCloseMessage(1);		
		Message reply = createOkMessage();
		
		callbackMock.expects(once()).method("closeAccepted");
		channel.channelHandlerMock.expects(once()).method("channelClosed");
		beepStreamMock.expects(once()).method("sendRPY")
				.with(eq(0), eq(1), eq(reply));
		beepStreamMock.expects(once()).method("channelClosed")
				.with(eq(1));
		session.receiveMSG(0, 1, request);
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
		
		beepStreamMock.expects(once()).method("sendRPY")
				.with(eq(0), eq(1), eq(createOkMessage()));
		beepStreamMock.expects(once()).method("closeTransport");
		sessionHandlerMock.expects(once()).method("sessionClosed");
		
		session.receiveMSG(0, 1, createCloseMessage(0));
	}

	private SessionImpl openSession(boolean initiator, String[] profiles, String[] remoteProfiles) {
		beepStreamMock.expects(once()).method("sendRPY")
				.with(eq(0), eq(0), eq(createGreetingMessage(profiles)));

		SessionImpl session = new SessionImpl(initiator, sessionHandler, beepStream);

		Stub acceptor = new StartSessionRequestAcceptor(profiles);
		
		sessionHandlerMock.expects(once()).method("connectionEstablished").will(acceptor);
		sessionHandlerMock.expects(once()).method("sessionOpened").with(same(session));
		
		session.connectionEstablished(null);
		session.receiveRPY(0, 0, createGreetingMessage(remoteProfiles));
		
		return session;
	}
	
	private ChannelStruct startChannel(int channelNumber, int messageNumber, ProfileInfo[] profiles, SessionImpl session) {
		Mock channelHandlerMock = mock(ChannelHandler.class);
		ChannelHandler channelHandler = (ChannelHandler) channelHandlerMock.proxy();
		
		ParameterCaptureStub<Channel> channelExtractor = 
				new ParameterCaptureStub<Channel>(0, Channel.class, null);
		channelHandlerMock.expects(once()).method("channelOpened").with(ANYTHING).will(channelExtractor);
		
		beepStreamMock.expects(once()).method("sendMSG")
				.with(eq(0), eq(1), eq(createStartMessage(1, new ProfileInfo[] { new ProfileInfo(PROFILE) })));
		beepStreamMock.expects(once()).method("channelStarted").with(eq(1));
		session.startChannel(PROFILE, channelHandler);
		session.receiveRPY(0, 1, createProfileMessage(new ProfileInfo(PROFILE)));
		
		Channel channel = channelExtractor.getParameter();
		return new ChannelStruct(channel, channelHandlerMock);
	}
	
	private ChannelStruct startChannelRequested(int channelNumber, int messageNumber, ProfileInfo[] profiles, SessionImpl session) {		
		Mock channelHandlerMock = mock(ChannelHandler.class);
		ChannelHandler channelHandler = (ChannelHandler) channelHandlerMock.proxy();
		
		ProfileInfo profile = profiles[0];
		StartChannelRequestAcceptor acceptor = new StartChannelRequestAcceptor(profile, channelHandler);
		
		sessionHandlerMock.expects(once()).method("channelStartRequested")
				.with(ANYTHING)
				.will(acceptor);
		
		beepStreamMock.expects(once()).method("channelStarted")
				.with(eq(channelNumber));
		beepStreamMock.expects(once()).method("sendRPY")
				.with(eq(0), eq(messageNumber), eq(createProfileMessage(profile)));
		
		ParameterCaptureStub<Channel> channelExtractor = 
			new ParameterCaptureStub<Channel>(0, Channel.class, null);
		channelHandlerMock.expects(once()).method("channelOpened").with(ANYTHING).will(channelExtractor);
		
		session.receiveMSG(0, messageNumber, createStartMessage(channelNumber, profiles));		
		
		Channel channel = channelExtractor.getParameter();
		return new ChannelStruct(channel, channelHandlerMock);
	}
	
	private void startChannelRequestedReject(SessionImpl session, int channelNumber, int messageNumber, ProfileInfo[] profiles) {
		Stub stub = new StartChannelRequestRejector(550, "no profiles supported");
		
		sessionHandlerMock.expects(once()).method("channelStartRequested")
				.with(ANYTHING).will(stub);
		beepStreamMock.expects(once()).method("sendERR")
				.with(eq(0), eq(messageNumber), eq(createErrorMessage(550, "no profiles supported")));
		session.receiveMSG(0, messageNumber, createStartMessage(channelNumber, profiles));		
	}
	
	private void startChannelRejected(SessionImpl session, int channelNumber, int messageNumber, ProfileInfo[] profiles) {
		Mock channelHandlerMock = mock(ChannelHandler.class);
		ChannelHandler channelHandler = (ChannelHandler) channelHandlerMock.proxy();
		
		beepStreamMock.expects(once()).method("sendMSG")
				.with(eq(0), eq(messageNumber), eq(createStartMessage(channelNumber, profiles)));
		channelHandlerMock.expects(once()).method("channelStartFailed")
				.with(eq(550), eq("no profiles supported"));
		
		session.startChannel(profiles[0], channelHandler);
		session.receiveERR(0, messageNumber, createErrorMessage(550, "no profiles supported"));
	}

	private void closeChannel(SessionImpl session, ChannelStruct channel, 
			int channelNumber, int messageNumber) {
		Mock closeChannelCallbackMock = mock(CloseChannelCallback.class);
		CloseChannelCallback closeChannelCallback = (CloseChannelCallback) closeChannelCallbackMock.proxy();
		
		beepStreamMock.expects(once()).method("sendMSG")
				.with(eq(0), eq(messageNumber), eq(createCloseMessage(channelNumber)));
		channel.channel.close(closeChannelCallback);
		
		channel.channelHandlerMock.expects(once()).method("channelClosed");
		beepStreamMock.expects(once()).method("channelClosed").with(eq(channelNumber));
		closeChannelCallbackMock.expects(once()).method("closeAccepted");
		session.receiveRPY(0, messageNumber, createOkMessage());
	}
	
	private void closeChannelRejected(SessionImpl session, ChannelStruct channel,
			int channelNumber, int messageNumber) {
		Mock closeChannelCallbackMock = mock(CloseChannelCallback.class);
		CloseChannelCallback closeChannelCallback = (CloseChannelCallback) closeChannelCallbackMock.proxy();
		
		beepStreamMock.expects(once()).method("sendMSG")
				.with(eq(0), eq(messageNumber), eq(createCloseMessage(channelNumber)));
		channel.channel.close(closeChannelCallback);
		
		closeChannelCallbackMock.expects(once()).method("closeDeclined")
				.with(eq(550), eq("still working"));
		session.receiveERR(0, messageNumber, createErrorMessage(550, "still working"));
	}
	
	private void requestChannelClose(
			Mock transportMappingMock,
			Channel channel,
			CloseChannelCallback callback,
			int channelNumber,
			int messageNumber) {
		Message request = createCloseMessage(channelNumber);
		
		transportMappingMock.expects(once()).method("sendMSG")
				.with(eq(0), eq(messageNumber), eq(request));
		channel.close(callback);
	}
	
	private void closeChannelRequested(SessionImpl session, ChannelStruct channel, 
			int channelNumber, int messageNumber) {
		Stub closeChannelAcceptor = new CloseChannelAcceptor();
		
		Message request = createCloseMessage(1);		
		Message reply = createOkMessage();
		
		channel.channelHandlerMock.expects(once()).method("channelCloseRequested")
				.with(ANYTHING).will(closeChannelAcceptor);
		channel.channelHandlerMock.expects(once()).method("channelClosed");
		beepStreamMock.expects(once()).method("sendRPY")
				.with(eq(0), eq(messageNumber), eq(reply));
		beepStreamMock.expects(once()).method("channelClosed")
				.with(eq(1));
		session.receiveMSG(0, messageNumber, request);
	}

	private void closeSession(SessionImpl session, int messageNumber) {
		initiateCloseSession(session, messageNumber);
		sessionHandlerMock.expects(once()).method("sessionClosed");
		beepStreamMock.expects(once()).method("closeTransport");
		session.receiveRPY(0, messageNumber, createOkMessage());
	}
	
	private void initiateCloseSession(SessionImpl session, int messageNumber) {
		beepStreamMock.expects(once()).method("sendMSG")
				.with(eq(0), eq(messageNumber), eq(createCloseMessage(0)));
		session.close();
	}
	
	private void closeSessionRequested(SessionImpl session, int messageNumber) {
		Message request = createCloseMessage(0);
		beepStreamMock.expects(once()).method("sendRPY")
		        .with(eq(0), eq(messageNumber), eq(createOkMessage()));
		beepStreamMock.expects(once()).method("closeTransport");
		sessionHandlerMock.expects(once()).method("sessionClosed");
		session.receiveMSG(0, messageNumber, request);
	}
	
	private void closeSessionRequestedReject(SessionImpl session, int messageNumber) {
		Message request = createCloseMessage(0);
		beepStreamMock.expects(once()).method("sendERR")
		        .with(eq(0), eq(messageNumber), eq(createErrorMessage(550, "still working")));
		session.receiveMSG(0, messageNumber, request);
	}
	
	private Mock sendEcho(Channel channel, int channelNumber, int messageNumber, String content) throws IOException {
		Mock replyListenerMock = mock(ReplyHandler.class);
		ReplyHandler replyListener = (ReplyHandler) replyListenerMock.proxy();
		
		Message request = createEchoMessage(content);		
		beepStreamMock.expects(once()).method("sendMSG")
				.with(eq(channelNumber), eq(messageNumber), same(request));
		channel.sendMessage(request, replyListener);
		
		return replyListenerMock;
	}
	
	private void receiveEcho(MessageHandler messageHandler, Mock listenerMock, int channelNumber, int messageNumber, String content) throws IOException {
		Message reply = createEchoMessage(content);
		listenerMock.expects(once()).method("receivedRPY").with(same(reply));
		messageHandler.receiveRPY(channelNumber, messageNumber, reply);
	}
	
	private void sendAndReceiveEcho(MessageHandler messageHandler, Channel channel, 
			int channelNumber, int messageNumber, String content) throws IOException {
		Mock replyListenerMock = sendEcho(channel, channelNumber, messageNumber, content);
		receiveEcho(messageHandler, replyListenerMock, channelNumber, messageNumber, content);
	}
	
	private void receiveAndReplyEcho(MessageHandler messageHandler, ChannelStruct channel,
			int channelNumber, int messageNumber, String content) throws IOException {
		ParameterCaptureStub<Reply> extractor = 
				new ParameterCaptureStub<Reply>(1, Reply.class, null);
		
		Message request = createEchoMessage(content);
		Message reply = createEchoMessage(content);
		
		channel.channelHandlerMock.expects(once()).method("messageReceived")
				.with(same(request), ANYTHING).will(extractor);
		
		messageHandler.receiveMSG(channelNumber, messageNumber, request);
		
		Reply responseHandler = extractor.getParameter();		
		beepStreamMock.expects(once()).method("sendRPY").with(eq(channelNumber), eq(messageNumber), same(reply));
		responseHandler.sendRPY(reply);
	}
	
	private void connectionClosed(SessionImpl session) {
		sessionHandlerMock.expects(once()).method("sessionClosed");
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
		ChannelManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createError(messageBuilder, code, message);
	}
	
	private static Message createOkMessage() {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ChannelManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createOk(messageBuilder);
	}
	
	private static Message createGreetingMessage(String[] profiles) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ChannelManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createGreeting(messageBuilder, profiles);
	}
	
	private static Message createProfileMessage(ProfileInfo profile) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ChannelManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createProfile(messageBuilder, profile);
	}
	
	private static Message createStartMessage(int channelNumber, ProfileInfo[] profiles) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ChannelManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createStart(messageBuilder, channelNumber, profiles);
	}
	
	private static Message createCloseMessage(int channelNumber) {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");
		ChannelManagementMessageBuilder builder = new SaxMessageBuilder();
		return builder.createClose(messageBuilder, channelNumber, 200);
	}
	
	private static class ChannelStruct {
		private final Mock channelHandlerMock;
		private final Channel channel;
		private ChannelStruct(Channel channel, Mock channelHandlerMock) {
			this.channel = channel;
			this.channelHandlerMock = channelHandlerMock;
		}
	}
	
	private static class StartSessionRequestRejector implements Stub {
		public StringBuffer describeTo(StringBuffer buffer) {
			return buffer.append("rejects session start");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			StartSessionRequest request = (StartSessionRequest) invocation.parameterValues.get(0);
			request.cancel();
			return null;
		}
	}
	
	private static class StartSessionRequestAcceptor implements Stub {
		private final String[] profileUris;
		public StartSessionRequestAcceptor(String[] profileUris) {
			this.profileUris = profileUris;
		}
		public StringBuffer describeTo(StringBuffer buffer) {
			return buffer.append("accepts session start");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			StartSessionRequest request = (StartSessionRequest) invocation.parameterValues.get(0);
			for (int i = 0; i < profileUris.length; i++) {
				request.registerProfile(profileUris[i]);
			}
			return null;
		}
	}
	
	private static class StartChannelRequestAcceptor implements Stub {
		private final ProfileInfo profile;
		private final ChannelHandler channelHandler;
		private StartChannelRequestAcceptor(ProfileInfo profile, ChannelHandler channelHandler) {
			this.profile = profile;
			this.channelHandler = channelHandler;
		}
		public StringBuffer describeTo(StringBuffer buffer) {
			return buffer.append("accepts channel start");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			StartChannelRequest request = (StartChannelRequest) invocation.parameterValues.get(0);
			request.selectProfile(profile, channelHandler);
			return null;
		}
	}
	
	private static class StartChannelRequestRejector implements Stub {
		private final int code;
		private final String message;
		private StartChannelRequestRejector(int code, String message) {
			this.code = code;
			this.message = message;
		}
		public StringBuffer describeTo(StringBuffer buffer) {
			return buffer.append("rejects channel start");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			StartChannelRequest request = (StartChannelRequest) invocation.parameterValues.get(0);
			request.cancel(code, message);
			return null;
		}
	}
	
	private static class CloseChannelAcceptor implements Stub {
		public StringBuffer describeTo(StringBuffer buffer) {
			return buffer.append("accepts close channel");
		}
		public Object invoke(Invocation invocation) throws Throwable {
			CloseChannelRequest callback = (CloseChannelRequest) invocation.parameterValues.get(0);
			callback.accept();
			return null;
		}
	}
	
}
