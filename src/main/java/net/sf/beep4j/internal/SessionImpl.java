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

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.ChannelHandlerFactory;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.ReplyListener;
import net.sf.beep4j.ResponseHandler;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;
import net.sf.beep4j.internal.profile.BEEPError;
import net.sf.beep4j.internal.profile.ChannelManagementProfile;
import net.sf.beep4j.internal.profile.ChannelManagementProfileImpl;
import net.sf.beep4j.internal.profile.Greeting;
import net.sf.beep4j.internal.profile.StartChannelCallback;
import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.internal.util.IntegerSequence;
import net.sf.beep4j.internal.util.Sequence;
import net.sf.beep4j.transport.TransportContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the Session interface. Objects of this class are
 * the central object in a BEEP session.
 * 
 * <ul>
 *  <li>dispatch incoming messages</li>
 *  <li>send outgoing messages</li>
 *  <li>manage channel start and close</li>
 * </ul>
 * 
 * @author Simon Raess
 */
public class SessionImpl 
		implements MessageHandler, SessionManager, InternalSession, TransportContext, FrameHandlerFactory {
	
	private final Logger LOG = LoggerFactory.getLogger(SessionImpl.class);
	
	private final boolean initiator;
	
	private final Map<Integer,Sequence<Integer>> messageNumberSequences = new HashMap<Integer,Sequence<Integer>>();
	
	private final Map<Integer,LinkedList<ReplyListenerHolder>> replyListeners = new HashMap<Integer,LinkedList<ReplyListenerHolder>>();
	
	private final Map<String,ResponseHandler> responseHandlers = new HashMap<String,ResponseHandler>();
	
	private final Map<Integer,Channel> channels = new HashMap<Integer,Channel>();
	
	private final Map<Integer,ChannelHandler> channelHandlers = new HashMap<Integer,ChannelHandler>(); 
	
	private final ChannelManagementProfile channelManagementProfile;
	
	private final TransportMapping mapping;
	
	private final SessionHandler sessionHandler;
	
	private final Sequence<Integer> channelNumberSequence;
	
	private final List<SessionListener> listeners = Collections.synchronizedList(new ArrayList<SessionListener>());
	
	private final StreamParser parser;
	
	private SessionState currentState;
	
	private SessionState initialState;
	
	private SessionState aliveState;
	
	private SessionState waitForResponseState;
	
	private SessionState deadState;

	/**
	 * The greeting received from the other peer.
	 */
	private Greeting greeting;
	
	public SessionImpl(boolean initiator, SessionHandler sessionHandler, TransportMapping mapping) {
		Assert.notNull("sessionHandler", sessionHandler);
		Assert.notNull("mapping", mapping);
		
		this.initiator = initiator;
		this.sessionHandler = sessionHandler;
		this.mapping = mapping;
		addSessionListener(mapping);
		
		DelegatingFrameHandler frameHandler = new DelegatingFrameHandler(this);
		addSessionListener(frameHandler);
		
		this.channelManagementProfile = createChannelManagementProfile(initiator);
		initChannelManagementProfile();
		
		this.channelNumberSequence = new IntegerSequence(initiator ? 1 : 2, 2);
		this.parser = createStreamParser(frameHandler, mapping);
		
		initialState = new InitialState();
		aliveState = new AliveState();
		waitForResponseState = new WaitForResponseState();
		deadState = new DeadState();
		currentState = initialState;
	}

	protected StreamParser createStreamParser(FrameHandler frameHandler, TransportMapping mapping) {
		return new DefaultStreamParser(frameHandler, mapping);
	}
	
	protected ChannelManagementProfile createChannelManagementProfile(boolean initiator) {
		return new ChannelManagementProfileImpl(initiator);
	}

	protected void initChannelManagementProfile() {
		ChannelHandler channelHandler = channelManagementProfile.createChannelHandler(this);
		InternalChannel channel = createChannel(this, "", 0);
		channelHandler = channel.initChannel(channelHandler);
		channelHandler.channelOpened(channel);
		registerChannel(0, channel, channelHandler);
	}
		
	protected InternalChannel createChannel(InternalSession session, String profileUri, int channelNumber) {
		return new ChannelImpl(session, profileUri, channelNumber);
	}

	protected ResponseHandler createResponseHandler(TransportMapping mapping, int channelNumber, int messageNumber) {
		ResponseHandler responseHandler = new DefaultResponseHandler(mapping, channelNumber, messageNumber);
		setResponseHandler(channelNumber, messageNumber, responseHandler);
		return responseHandler;
	}
	
	private String traceInfo() {
		StringBuilder builder = new StringBuilder();
		builder.append("[").append(System.identityHashCode(this));
		builder.append("|").append(currentState);
		builder.append("|").append(initiator).append("] ");
		return builder.toString();
	}
	
	private void debug(Object... messages) {
		if (LOG.isDebugEnabled()) {
			StringBuffer buffer = new StringBuffer();
			for (Object message : messages) {
				buffer.append(message);
			}
			LOG.debug(buffer.toString());
		}
	}
	
	private void info(String message) {
		LOG.info(traceInfo() + message);
	}
	
	private void warn(String message, Exception e) {
		LOG.warn(traceInfo() + message, e);
	}
	
	private void setCurrentState(SessionState currentState) {
		debug("setting session state from ", this.currentState, " to ", currentState);
		this.currentState = currentState;
	}

	private SessionState getCurrentState() {
		return currentState;
	}

	protected void addSessionListener(SessionListener l) {
		listeners.add(l);
	}
	
	protected void fireChannelStarted(int channel) {
		SessionListener[] list = listeners.toArray(new SessionListener[listeners.size()]);
		for (int i = 0; i < list.length; i++) {
			SessionListener listener = list[i];
			listener.channelStarted(channel);
		}
		
	}

	protected void fireChannelClosed(int channel) {
		SessionListener[] list = listeners.toArray(new SessionListener[listeners.size()]);
		for (int i = 0; i < list.length; i++) {
			SessionListener listener = list[i];
			listener.channelClosed(channel);
		}
		
	}
	
	private int getNextChannelNumber() {
		return channelNumberSequence.next();
	}
	
	private void validateChannelNumber(int number) {
		if (number <= 0) {
			throw new ProtocolException(number + " is an illegal channel number");
		}
		if (initiator && number % 2 != 0) {
			throw new ProtocolException("channel numbers of listener peer "
					+ "must be even numbered");
		} else if (!initiator && number % 2 != 1) {
			throw new ProtocolException("channel numbers of initator peer "
					+ "must be odd numbered");
		}
	}
	
	private boolean hasOpenChannels() {
		return channels.size() > 1;
	}

	private void registerChannel(int channelNumber, Channel channel, ChannelHandler handler) {
		channels.put(channelNumber, channel);
		channelHandlers.put(channelNumber, handler);
		messageNumberSequences.put(channelNumber, new IntegerSequence(1, 1));
		replyListeners.put(channelNumber, new LinkedList<ReplyListenerHolder>());
		fireChannelStarted(channelNumber);
	}

	private void unregisterChannel(int channelNumber) {
		channels.remove(channelNumber);
		channelHandlers.remove(channelNumber);
		messageNumberSequences.remove(channelNumber);
		replyListeners.remove(channelNumber);
		fireChannelClosed(channelNumber);
	}

	private void registerReplyListener(int channelNumber, int messageNumber, ReplyListener listener) {
		LinkedList<ReplyListenerHolder> expectedReplies = replyListeners.get(channelNumber);
		expectedReplies.addLast(new ReplyListenerHolder(messageNumber, listener));
	}
	
	private static class ReplyListenerHolder {
		private final int messageNumber;
		private final ReplyListener replyListener;
		protected ReplyListenerHolder(int messageNumber, ReplyListener listener) {
			this.messageNumber = messageNumber;
			this.replyListener = listener;
		}
		protected void receiveANS(int channelNumber, int messageNumber, Message message) {
			validateMessageNumber(channelNumber, messageNumber);
			replyListener.receiveANS(message);
		}
		protected void receiveNUL(int channelNumber, int messageNumber) {
			validateMessageNumber(channelNumber, messageNumber);
			replyListener.receiveNUL();
		}
		protected void receiveERR(int channelNumber, int messageNumber, Message message) {
			validateMessageNumber(channelNumber, messageNumber);
			replyListener.receiveERR(message);
		}
		protected void receiveRPY(int channelNumber, int messageNumber, Message message) {
			validateMessageNumber(channelNumber, messageNumber);
			replyListener.receiveRPY(message);
		}
		private void validateMessageNumber(int channelNumber, int messageNumber) {
			if (this.messageNumber != messageNumber) {
				throw new ProtocolException("next expected reply on channel "
						+ channelNumber + " must have message number "
						+ this.messageNumber + " but was "
						+ messageNumber);
			}
		}
	}
	
	private ReplyListenerHolder unregisterReplyListener(int channelNumber) {
		LinkedList<ReplyListenerHolder> listeners = replyListeners.get(channelNumber);
		return listeners.removeFirst();
	}

	private int getNextMessageNumber(int channelNumber) {
		Sequence<Integer> sequence = getMessageNumberSequence(channelNumber);
		Integer next = sequence.next();
		return next;
	}

	private Sequence<Integer> getMessageNumberSequence(int channelNumber) {
		Sequence<Integer> result = messageNumberSequences.get(channelNumber);
		if (result == null) {
			throw new InternalException("no open channel with channel number " + channelNumber);
		}
		return result;
	}
	
	private ReplyListenerHolder getReplyListener(int channelNumber, int messageNumber) {
		LinkedList<ReplyListenerHolder> listeners = replyListeners.get(channelNumber);
		if (listeners.isEmpty()) {
			throw new ProtocolException("received a reply but expects no outstanding replies");
		}
		return listeners.getFirst();
	}
	
	private ResponseHandler getResponseHandler(int channelNumber, int messageNumber) {
		ResponseHandler handler = responseHandlers.get(key(channelNumber, messageNumber));
		return handler;
	}
	
	private void setResponseHandler(int channelNumber, int messageNumber, ResponseHandler responseHandler) {
		responseHandlers.put(key(channelNumber, messageNumber), responseHandler);
	}
	
	private ChannelHandler getChannelHandler(int channelNumber) {
		return channelHandlers.get(channelNumber);
	}
	
	private String key(int channelNumber, int messageNumber) {
		return channelNumber + ":" + messageNumber;
	}
	
	private void replyCompleted(int channelNumber, int messageNumber) {
		responseHandlers.remove(key(channelNumber, messageNumber));
	}

	
	// --> start of Session methods <--
	
	public String[] getProfiles() {
		if (greeting == null) {
			throw new IllegalStateException("greeting has not yet been received");
		}
		return greeting.getProfiles();
	}
	
	public synchronized void startChannel(String profileUri, ChannelHandler handler) {
		startChannel(new ProfileInfo(profileUri), handler);
	}
	
	public synchronized void startChannel(final ProfileInfo profile, final ChannelHandler handler) {
		startChannel(new ProfileInfo[] { profile }, new ChannelHandlerFactory() {
			public ChannelHandler createChannelHandler(ProfileInfo info) {
				if (!profile.getUri().equals(info.getUri())) {
					throw new IllegalArgumentException("profile URIs do not match: "
							+ profile.getUri() + " | " + info.getUri());
				}
				return handler;
			}
			public void startChannelFailed(int code, String message) {
				handler.channelStartFailed(code, message);
			}
		});
	}
	
	public synchronized void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory) {
		getCurrentState().startChannel(profiles, factory);
	}
	
	public synchronized void close() {
		getCurrentState().closeSession();
	}
	
	// --> end of Session methods <--
	
	
	// --> start of InternalSession methods <--
	
	/*
	 * This method is called by the channel implementation to send a message on
	 * a particular channel to the other peer. It takes care to:
	 * - generate a message number
	 * - register the reply listener under that number
	 * - pass the message to the underlying transport mapping
	 */	
	public synchronized void sendMessage(int channelNumber, Message message, ReplyListener listener) {
		getCurrentState().sendMessage(channelNumber, message, listener);
	}

	/*
	 * This method is called by the channel implementation to send a close channel
	 * request to the other peer.
	 */
	public synchronized void requestChannelClose(final int channelNumber, final CloseChannelCallback callback) {
		Assert.notNull("callback", callback);
		channelManagementProfile.closeChannel(channelNumber, new CloseChannelCallback() {
			public void closeDeclined(int code, String message) {
				callback.closeDeclined(code, message);
			}
			public void closeAccepted() {
				unregisterChannel(channelNumber);
				callback.closeAccepted();
			}
		});
	}
	
	// --> end of InternalSession methods <--
	
	
	// --> start of FrameHandlerFactory methods <--
	
	public FrameHandler createFrameHandler() {
		return new MessageAssembler(this);
	}
	
	// --> end of FrameHandlerFactory methods <--
	
	
	// --> start of SessionManager methods <--
	
	/*
	 * This method is invoked by the ChannelManagementProfile when the other
	 * peer requests creating a new channel.
	 */
	public synchronized StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
		return getCurrentState().channelStartRequested(channelNumber, profiles);
	}
	
	/*
	 * This method is invoked by the ChannelManagement profile when a channel
	 * close request is received. This request is passed on to the ChannelHandler,
	 * that is the application, which decides what to do with the request to
	 * close the channel.
	 */
	public synchronized void channelCloseRequested(final int channelNumber, final CloseChannelRequest request) {
		ChannelHandler handler = getChannelHandler(channelNumber);
		handler.closeRequested(new CloseChannelRequest() {
			public void reject() {
				request.reject();
			}		
			public void accept() {
				request.accept();
				unregisterChannel(channelNumber);
			}
		});
	}
	
	public synchronized void sessionCloseRequested(CloseCallback callback) {
		getCurrentState().sessionCloseRequested(callback);
	}
	
	// --> end of SessionManager methods <--
	
	
	// --> start of MessageHandler methods <-- 

	public synchronized void receiveMSG(int channelNumber, int messageNumber, Message message) {
		debug("received MSG: channel=", channelNumber, ",message=",  messageNumber);
		getCurrentState().receiveMSG(channelNumber, messageNumber, message);
	}

	public synchronized void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
		debug("received ANS: channel=", channelNumber, ",message=", messageNumber, ",answer=", answerNumber);
		getCurrentState().receiveANS(channelNumber, messageNumber, answerNumber, message);
	}
	
	public synchronized void receiveNUL(int channelNumber, int messageNumber) {
		debug("received NUL: channel=", channelNumber, ",message=", messageNumber);
		getCurrentState().receiveNUL(channelNumber, messageNumber);
	}

	public synchronized void receiveERR(int channelNumber, int messageNumber, Message message) {
		debug("received ERR: channel=", channelNumber, ",message=", messageNumber);
		getCurrentState().receiveERR(channelNumber, messageNumber, message);
	}
		
	public synchronized void receiveRPY(int channelNumber, int messageNumber, Message message) {
		debug("received RPY: channel=", channelNumber, ",message=", messageNumber);
		getCurrentState().receiveRPY(channelNumber, messageNumber, message);
	}
	
	// --> end of MessageHandler methods <--
	
	
	// --> start of TransportContext methods <--
	
	/*
	 * Notifies the ChannelManagementProfile about this event. The
	 * ChannelManagementProfile then asks the application (SessionHandler)
	 * whether to accept the connection and sends the appropriate response.
	 */
	public synchronized void connectionEstablished(SocketAddress address) {
		getCurrentState().connectionEstablished(address);
	}
	
	public synchronized void exceptionCaught(Throwable cause) {
		// TODO: implement this method
		LOG.warn("exception caught by transport", cause);
	}
	
	public synchronized void messageReceived(ByteBuffer buffer) {		
		try {
			parser.process(buffer);
		} catch (ProtocolException e) {
			warn("dropping connection because of a protocol exception", e);
			try {
				sessionHandler.sessionClosed();
			} finally {
				setCurrentState(deadState);
				mapping.closeTransport();
			}
		}
	}
	
	public synchronized void connectionClosed() {
		getCurrentState().connectionClosed();
	}
	
	// --> end of TransportContext methods <--
	
	protected static interface SessionState extends MessageHandler {
		
		void connectionEstablished(SocketAddress address);
		
		void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory);
		
		void sendMessage(int channelNumber, Message message, ReplyListener listener);
		
		StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles);
		
		void closeSession();
		
		void sessionCloseRequested(CloseCallback callback);
		
		void connectionClosed();
		
	}
	
	protected abstract class AbstractSessionState implements SessionState {
		
		public abstract String getName();
		
		public void connectionEstablished(SocketAddress address) {
			throw new IllegalStateException("connection already established, state=<" 
					+ getName() + ">");
		}
		
		public void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory) {
			throw new IllegalStateException("" +
					"cannot start channel in state <" + getName() + ">");
		}
		
		public void sendMessage(int channelNumber, Message message, ReplyListener listener) {
			throw new IllegalStateException(
					"cannot send messages in state <" + getName() + ">: channel="
					+ channelNumber);
		}
		
		public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
			return StartChannelResponse.createCancelledResponse(550, "cannot start channel");
		}

		public void receiveANS(int channelNumber, int messageNumber,
				int answerNumber, Message message) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message ANS, channel=" + channelNumber 
					+ ",message=" + messageNumber
					+ ",answerNumber=" + answerNumber);
		}

		public void receiveERR(int channelNumber, int messageNumber, Message message) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message ERR, channel=" + channelNumber + ",message=" + messageNumber);
		}

		public void receiveMSG(int channelNumber, int messageNumber, Message message) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message MSG, channel=" + channelNumber + ",message=" + messageNumber);
		}

		public void receiveNUL(int channelNumber, int messageNumber) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message NUL, channel=" + channelNumber + ",message=" + messageNumber);
		}

		public void receiveRPY(int channelNumber, int messageNumber, Message message) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message RPY, channel=" + channelNumber + ",message=" + messageNumber);
		}
		
		public void closeSession() {
			throw new IllegalStateException("cannot close session");
		}
		
		public void sessionCloseRequested(CloseCallback callback) {
			throw new IllegalStateException("cannot close session");
		}

	}

	protected class InitialState extends AbstractSessionState {
		
		@Override
		public String getName() {
			return "initial";
		}
		
		public void connectionEstablished(SocketAddress address) {
			ResponseHandler responseHandler = new InitialResponseHandler(mapping);
			setResponseHandler(0, 0, responseHandler);
			if (!channelManagementProfile.connectionEstablished(address, sessionHandler, responseHandler)) {
				setCurrentState(deadState);
				mapping.closeTransport();
			}
		}
		
		public void receiveMSG(int channelNumber, int messageNumber, Message message) {
			throw new ProtocolException(
					"first message in a session must be RPY or ERR on channel 0: "
					+ "was MSG channel=" + channelNumber + ",message=" + messageNumber);
		}
		
		public void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
			throw new ProtocolException(
					"first message in a session must be RPY or ERR on channel 0: "
					+ "was ANS channel=" + channelNumber + ",message=" + messageNumber);
		}
		
		public void receiveNUL(int channelNumber, int messageNumber) {
			throw new ProtocolException(
					"first message in a session must be RPY or ERR on channel 0: "
					+ "was NUL channel=" + channelNumber + ",message=" + messageNumber);
		}
		
		public void receiveRPY(int channelNumber, int messageNumber, Message message) {
			validateMessage(channelNumber, messageNumber);
			greeting = channelManagementProfile.receivedGreeting(message);
			setCurrentState(aliveState);
			sessionHandler.sessionOpened(SessionImpl.this);
		}
		
		public void receiveERR(int channelNumber, int messageNumber, Message message) {
			validateMessage(channelNumber, messageNumber);
			BEEPError error = channelManagementProfile.receivedError(message);
			
			info("received error, session start failed: " + error.getCode() + ":"
					+ error.getMessage());
			
			sessionHandler.sessionStartDeclined(error.getCode(), error.getMessage());
			setCurrentState(deadState);
			mapping.closeTransport();
		}

		private void validateMessage(int channelNumber, int messageNumber) {
			if (channelNumber != 0 || messageNumber != 0) {
				throw new ProtocolException("first message in session must be sent on "
						+ "channel 0 with message number 0: was channel " + channelNumber
						+ ",message=" + messageNumber);
			}
		}
		
		public void connectionClosed() {
			setCurrentState(deadState);
		}
		
		@Override
		public String toString() {
			return "<initial>";
		}

	}
	
	protected class AliveState extends AbstractSessionState {
		
		@Override
		public String getName() {
			return "alive";
		}
		
		@Override
		public void startChannel(final ProfileInfo[] profiles, final ChannelHandlerFactory factory) {
			final int channelNumber = getNextChannelNumber();
			channelManagementProfile.startChannel(channelNumber, profiles, new StartChannelCallback() {
				public void channelCreated(ProfileInfo info) {
					ChannelHandler handler = factory.createChannelHandler(info);
					InternalChannel channel = createChannel(
							SessionImpl.this, info.getUri(), channelNumber);
					ChannelHandler channelHandler = channel.initChannel(handler);
					registerChannel(channelNumber, channel, channelHandler);
					channelHandler.channelOpened(channel);
				}
				public void channelFailed(int code, String message) {
					factory.startChannelFailed(code, message);
				}
			});
		}
		
		@Override
		public void sendMessage(int channelNumber, Message message, ReplyListener listener) {
			int messageNumber = getNextMessageNumber(channelNumber);
			debug("send message: channel=", channelNumber, ",message=", messageNumber);
			registerReplyListener(channelNumber, messageNumber, listener);
			mapping.sendMSG(channelNumber, messageNumber, message);
		}
		
		@Override
		public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
			validateChannelNumber(channelNumber);

			debug("start of channel ", channelNumber, " requested by remote peer: ", Arrays.toString(profiles));
			DefaultStartChannelRequest request = new DefaultStartChannelRequest(profiles);
			sessionHandler.channelStartRequested(request);
			
			StartChannelResponse response = request.getResponse();
			
			if (response.isCancelled()) {
				debug("start of channel ", channelNumber, " is cancelled by application: ", response.getCode(), 
						",'", response.getMessage(), "'");
				return response;
			}
			
			ProfileInfo info = response.getProfile();
			if (info == null) {
				throw new ProtocolException("StartChannelRequest must be either cancelled or a profile must be selected");
			}
			
			debug("start of channel ", channelNumber, " is accepted by application: ", info.getUri());
			
			InternalChannel channel = createChannel(SessionImpl.this, info.getUri(), channelNumber);
			ChannelHandler handler = channel.initChannel(response.getChannelHandler());
			handler.channelOpened(channel);
			registerChannel(channelNumber, channel, handler);
			
			return response;
		}
		
		@Override
		public void receiveMSG(int channelNumber, int messageNumber, Message message) {
			ResponseHandler responseHandler = getResponseHandler(channelNumber, messageNumber);
			if (responseHandler != null) {
				// Validation of frames according to the BEEP specification section 2.2.1.1.
				//
				// A frame is poorly formed if the header starts with "MSG", and 
				// the message number refers to a "MSG" message that has been 
				// completely received but for which a reply has not been completely sent.
				throw new ProtocolException("Message number " + messageNumber
						+ " on channel " + channelNumber + " refers to a MSG message "
						+ "that has been received but for which a reply has not been "
						+ "completely sent.");
			}
			responseHandler = createResponseHandler(mapping, channelNumber, messageNumber);
			
			ChannelHandler handler = getChannelHandler(channelNumber);
			handler.messageReceived(message, responseHandler);
		}

		@Override
		public void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
			ReplyListenerHolder listener = getReplyListener(channelNumber, messageNumber);
			listener.receiveANS(channelNumber, messageNumber, message);
		}
		
		@Override
		public void receiveNUL(int channelNumber, int messageNumber) {
			ReplyListenerHolder listener = getReplyListener(channelNumber, messageNumber);
			try {
				listener.receiveNUL(channelNumber, messageNumber);
			} finally {
				unregisterReplyListener(channelNumber);
			}
		}

		@Override
		public void receiveERR(int channelNumber, int messageNumber, Message message) {
			ReplyListenerHolder listener = getReplyListener(channelNumber, messageNumber);
			try {
				listener.receiveERR(channelNumber, messageNumber, message);
			} finally {
				unregisterReplyListener(channelNumber);
			}
		}

		@Override
		public void receiveRPY(int channelNumber, int messageNumber, Message message) {
			ReplyListenerHolder listener = getReplyListener(channelNumber, messageNumber);
			try {
				listener.receiveRPY(channelNumber, messageNumber, message);
			} finally {
				unregisterReplyListener(channelNumber);
			}
		}
		
		@Override
		public void closeSession() {
			setCurrentState(waitForResponseState);
			channelManagementProfile.closeSession(new CloseCallback() {
				public void closeDeclined(int code, String message) {
					info("close session declined by remote peer: " + code + ":" + message);
					performClose();
				}
			
				public void closeAccepted() {
					info("close session accepted by remote peer");
					performClose();
				}
				
				private void performClose() {
					try {
						sessionHandler.sessionClosed();
					} finally {
						setCurrentState(deadState);
						mapping.closeTransport();
					}
				}
			});
		}
		
		@Override
		public void sessionCloseRequested(CloseCallback callback) {
			if (hasOpenChannels()) {
				callback.closeDeclined(550, "still working");
			} else {
				callback.closeAccepted();
				try {
					sessionHandler.sessionClosed();
				} finally {
					setCurrentState(deadState);
					mapping.closeTransport();
				}
			}
		}
		
		public void connectionClosed() {
			try {
				sessionHandler.sessionClosed();
			} finally {
				setCurrentState(deadState);
			}
		}
		
		@Override
		public String toString() {
			return "<alive>";
		}
		
	}
	
	protected class WaitForResponseState extends AliveState {
		
		@Override
		public String getName() {
			return "close-initiated";
		}
		
		@Override
		public void sessionCloseRequested(CloseCallback callback) {
			debug("received session close request while close is already in progress");
			try {
				sessionHandler.sessionClosed();
			} finally {
				callback.closeAccepted();
				mapping.closeTransport();
				setCurrentState(deadState);
			}
		}
		
		@Override
		public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
			return StartChannelResponse.createCancelledResponse(550, "session release in progress");
		}
		
		@Override
		public String toString() {
			return "<wait-for-response>";
		}
		
	}

	protected class DeadState extends AbstractSessionState {
		
		@Override
		public String getName() {
			return "dead";
		}
		
		public void connectionClosed() {
			// ignore this one
		}
		
		@Override
		public String toString() {
			return "<dead>";
		}
				
	}
	
	protected class DefaultResponseHandler implements ResponseHandler {
		
		private final TransportMapping mapping;
		
		private final int channel;
		
		private final int messageNumber;
		
		private int answerNumber = 0;
		
		private boolean complete;
		
		public DefaultResponseHandler(TransportMapping mapping, int channel, int messageNumber) {
			Assert.notNull("mapping", mapping);
			this.mapping = mapping;
			this.channel = channel;
			this.messageNumber = messageNumber;
		}
		
		private void checkCompletion() {
			if (complete) {
				throw new IllegalStateException("a complete reply has already been sent");
			}
		}

		private void complete() {
			complete = true;
			replyCompleted(channel, messageNumber);
		}
		
		public MessageBuilder createMessageBuilder() {
			return new DefaultMessageBuilder();
		}

		public void sendANS(Message message) {
			Assert.notNull("message", message);
			checkCompletion();
			debug("sendANS on channel ", channel, " to message ", messageNumber, " (", answerNumber, ")");
			mapping.sendANS(channel, messageNumber, answerNumber++, message);
		}
		
		public void sendERR(Message message) {
			Assert.notNull("message", message);
			checkCompletion();
			debug("sendERR on channel ", channel, " to message ", messageNumber);
			mapping.sendERR(channel, messageNumber, message);
			complete();
		}
		
		public void sendNUL() {
			checkCompletion();
			debug("sendNUL on channel ", channel, " to message ", messageNumber);
			mapping.sendNUL(channel, messageNumber);
			complete();
		}
		
		public void sendRPY(Message message) {
			Assert.notNull("message", message);
			checkCompletion();
			debug("sendRPY on channel ", channel, " to message ", messageNumber);
			mapping.sendRPY(channel, messageNumber, message);
			complete();
		}
		
	}
	
	protected class InitialResponseHandler extends DefaultResponseHandler {
		
		public InitialResponseHandler(TransportMapping mapping) {
			super(mapping, 0, 0);
		}
		
		@Override
		public void sendANS(Message message) {
			throw new InternalException("ANS is not a valid initial response");
		}
		
		@Override
		public void sendNUL() {
			throw new InternalException("ANS is not a valid initial response");
		}
		
	}
	
}
