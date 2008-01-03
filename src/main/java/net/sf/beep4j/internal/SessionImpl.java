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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.ChannelHandlerFactory;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.management.BEEPError;
import net.sf.beep4j.internal.management.ChannelManagementProfile;
import net.sf.beep4j.internal.management.ChannelManagementProfileImpl;
import net.sf.beep4j.internal.management.CloseCallback;
import net.sf.beep4j.internal.management.Greeting;
import net.sf.beep4j.internal.management.StartChannelCallback;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;
import net.sf.beep4j.internal.stream.BeepStream;
import net.sf.beep4j.internal.stream.MessageHandler;
import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.internal.util.IntegerSequence;
import net.sf.beep4j.internal.util.Sequence;

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
		implements MessageHandler, SessionManager, InternalSession, TransportHandler {
	
	private final Logger LOG = LoggerFactory.getLogger(SessionImpl.class);
	
	private final boolean initiator;
	
	private final Map<Integer,LinkedList<ReplyHandlerHolder>> replyHandlerHolders = new HashMap<Integer,LinkedList<ReplyHandlerHolder>>();
	
	private final Map<String,Reply> replies = new HashMap<String,Reply>();
	
	private final Map<Integer,Channel> channels = new HashMap<Integer,Channel>();
	
	private final Map<Integer,ChannelHandler> channelHandlers = new HashMap<Integer,ChannelHandler>(); 
	
	private final ChannelManagementProfile channelManagementProfile;
	
	private final BeepStream beepStream;
	
	private final SessionHandler sessionHandler;
	
	private final Sequence<Integer> channelNumberSequence;
	
	private final List<SessionListener> listeners = Collections.synchronizedList(new ArrayList<SessionListener>());
	
	private final ReentrantLock sessionLock = new ReentrantLock();
	
	private SessionState currentState;
	
	private SessionState initialState;
	
	private SessionState aliveState;
	
	private SessionState closeInitiatedState;
	
	private SessionState deadState;

	/**
	 * The greeting received from the other peer.
	 */
	private Greeting greeting;
	
	/**
	 * Whether the greeting has already been sent.
	 */
	private boolean greetingSent;
	
	public SessionImpl(boolean initiator, SessionHandler sessionHandler, BeepStream beepStream) {
		Assert.notNull("sessionHandler", sessionHandler);
		Assert.notNull("beepStream", beepStream);
		
		this.initiator = initiator;
		this.sessionHandler = new SessionHandlerWrapper(sessionHandler, sessionLock);
		this.beepStream = beepStream;
		
		addSessionListener(beepStream);
				
		this.channelManagementProfile = createChannelManagementProfile(initiator);
		initChannelManagementProfile();
		
		this.channelNumberSequence = new IntegerSequence(initiator ? 1 : 2, 2);
		
		initialState = new InitialState();
		aliveState = new AliveState();
		closeInitiatedState = new CloseInitiatedState();
		deadState = new DeadState();
		currentState = initialState;
	}
	
	protected ChannelManagementProfile createChannelManagementProfile(boolean initiator) {
		return new ChannelManagementProfileImpl(initiator);
	}

	protected void initChannelManagementProfile() {
		ChannelHandler channelHandler = channelManagementProfile.createChannelHandler(this);
		
		// we are never using the Channel object for the management profile
		// so we'll just pass null as this won't be a problem
		registerChannel(0, null, channelHandler);
	}
		
	protected InternalChannel createChannel(InternalSession session, String profileUri, int channelNumber) {
		return new ChannelImpl(session, profileUri, channelNumber);
	}
	
	private ChannelHandler initChannel(InternalChannel channel, ChannelHandler handler) {
		return new ChannelHandlerWrapper(channel.initChannel(handler), sessionLock);
	}
	
	protected void lock() {
		sessionLock.lock();
	}
	
	protected void unlock() {
		sessionLock.unlock();
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
		if (LOG.isInfoEnabled()) {
			LOG.info(traceInfo() + message);
		}
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

	public void addSessionListener(SessionListener l) {
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
	
	protected boolean hasOpenChannels() {
		return channels.size() > 1;
	}

	protected void registerChannel(int channelNumber, Channel channel, ChannelHandler handler) {
		channels.put(channelNumber, channel);
		channelHandlers.put(channelNumber, handler);
		replyHandlerHolders.put(channelNumber, new LinkedList<ReplyHandlerHolder>());
		fireChannelStarted(channelNumber);
	}

	protected void unregisterChannel(int channelNumber) {
		channels.remove(channelNumber);
		channelHandlers.remove(channelNumber);
		replyHandlerHolders.remove(channelNumber);
		fireChannelClosed(channelNumber);
	}
	
	// --> start of reply handler related methods (incoming) <--
	
	protected ReplyHandlerHolder getReplyHandler(int channelNumber, int messageNumber) {
		LinkedList<ReplyHandlerHolder> listeners = replyHandlerHolders.get(channelNumber);
		if (listeners.isEmpty()) {
			throw new ProtocolException("received a reply but expects no outstanding replies");
		}
		return listeners.getFirst();
	}

	protected void registerReplyHandler(int channelNumber, int messageNumber, ReplyHandler handler) {
		LinkedList<ReplyHandlerHolder> expectedReplies = replyHandlerHolders.get(channelNumber);
		if (channelNumber == 0) {
			// channel 0 is managed by the channel management profile
			// that profile must be executed while holding the session lock
			// thus no unlocking / locking should be done while caling the ReplyHandler
			expectedReplies.addLast(new ReplyHandlerHolder(messageNumber, handler));
		} else {
			// ensures that when the application is called, no locks are held
			expectedReplies.addLast(new ReplyHandlerHolder(messageNumber, handler, sessionLock));			
		}
	}
	
	protected ReplyHandlerHolder unregisterReplyHandler(final int channelNumber, final int messageNumber) {
		LinkedList<ReplyHandlerHolder> holders = replyHandlerHolders.get(channelNumber);
		ReplyHandlerHolder holder = holders.removeFirst();
		if (messageNumber != holder.getMessageNumber()) {
			throw new ProtocolException("next expected reply has message number " 
					+ holder.getMessageNumber()
					+ "; received reply had message number " + messageNumber);
		}
		return holder;
	}

	// --> end of reply handler related methods (incoming) <--
	
	// --> start of methods related to replies (outgoing) <--
	
	protected Reply createReply(BeepStream mapping, int channelNumber, int messageNumber) {
		Reply reply = new DefaultReply(mapping, channelNumber, messageNumber);
		if (channelNumber > 0) {
			reply = new LockingReply(reply, sessionLock);
		}
		registerReply(channelNumber, messageNumber, reply);
		return reply;
	}
	
	protected Reply getReply(int channelNumber, int messageNumber) {
		return replies.get(key(channelNumber, messageNumber));
	}
	
	protected void registerReply(int channelNumber, int messageNumber, Reply reply) {
		replies.put(key(channelNumber, messageNumber), reply);
	}
	
	protected void replyCompleted(int channelNumber, int messageNumber) {
		Reply reply = replies.remove(key(channelNumber, messageNumber));
		if (reply == null) {
			throw new IllegalStateException(
					"completed reply that does no longer exist (channel="
					+ channelNumber + ",message=" + messageNumber + ")");
		}
	}
	
	// --> end of methods related to replies (outgoing) <--
	
	private ChannelHandler getChannelHandler(int channelNumber) {
		return channelHandlers.get(channelNumber);
	}
	
	private String key(int channelNumber, int messageNumber) {
		return channelNumber + ":" + messageNumber;
	}
	
	protected void checkInitialAliveTransition() {
		if (greeting != null && greetingSent) {
			setCurrentState(aliveState);
			sessionHandler.sessionOpened(SessionImpl.this);
		}
	}
	
	// --> start of Session methods <--
	
	public String[] getProfiles() {
		if (greeting == null) {
			throw new IllegalStateException("greeting has not yet been received");
		}
		return greeting.getProfiles();
	}
	
	public void startChannel(String profileUri, ChannelHandler handler) {
		startChannel(new ProfileInfo(profileUri), handler);
	}
	
	public void startChannel(final ProfileInfo profile, final ChannelHandler handler) {
		startChannel(new ProfileInfo[] { profile }, new ChannelHandlerFactory() {
			public ChannelHandler createChannelHandler(ProfileInfo info) {
				if (!profile.getUri().equals(info.getUri())) {
					throw new IllegalArgumentException("profile URIs do not match: "
							+ profile.getUri() + " | " + info.getUri());
				}
				return handler;
			}
			public void startChannelFailed(int code, String message) {
				unlock();
				try {
					handler.channelStartFailed(code, message);
				} finally {
					lock();
				}
			}
		});
	}
	
	public void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory) {
		lock();
		try {
			getCurrentState().startChannel(profiles, factory);
		} finally {
			unlock();
		}
	}
	
	public void close() {
		lock();
		try {
			getCurrentState().closeSession();
		} finally {
			unlock();
		}
	}
	
	// --> end of Session methods <--
	
	
	// --> start of InternalSession methods <--
	
	/*
	 * This method is called by the channel implementation to send a message on
	 * a particular channel to the other peer.
	 */	
	public void sendMessage(int channelNumber, int messageNumber, Message message, ReplyHandler reply) {
		lock();
		try {
			getCurrentState().sendMessage(channelNumber, messageNumber, message, reply);
		} finally {
			unlock();
		}
	}

	/*
	 * This method is called by the channel implementation to send a close channel
	 * request to the other peer.
	 */
	public void requestChannelClose(final int channelNumber, final CloseChannelCallback callback) {
		Assert.notNull("callback", callback);
		lock();
		try {
			channelManagementProfile.closeChannel(channelNumber, new CloseCallback() {
				public void closeDeclined(int code, String message) {
					callback.closeDeclined(code, message);
				}
				public void closeAccepted() {
					unregisterChannel(channelNumber);
					callback.closeAccepted();
				}
			});
		} finally {
			unlock();
		}
	}
	
	// --> end of InternalSession methods <--
		
	
	// --> start of SessionManager methods <--
	//
	//   - these methods are invoked while holding the session lock
	//   - they are called by the ChannelManagementProfile
	//   - thus, it is not necessary to lock / unlock the session lock here
	
	/*
	 * This method is invoked by the ChannelManagementProfile when the other
	 * peer requests creating a new channel.
	 */
	public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
		Assert.holdsLock("session", sessionLock);
		return getCurrentState().channelStartRequested(channelNumber, profiles);
	}
	
	/*
	 * This method is invoked by the ChannelManagement profile when a channel
	 * close request is received. This request is passed on to the ChannelHandler,
	 * that is the application, which decides what to do with the request to
	 * close the channel.
	 */
	public void channelCloseRequested(final int channelNumber, final CloseChannelRequest request) {
		Assert.holdsLock("session", sessionLock);
		getCurrentState().channelCloseRequested(channelNumber, request);
	}
	
	/*
	 * This method is invoked by the ChannelManagement profile when a session
	 * close request is received.
	 */
	public void sessionCloseRequested(CloseCallback callback) {
		Assert.holdsLock("session", sessionLock);
		getCurrentState().sessionCloseRequested(callback);
	}
	
	/*
	 * Sends a message on channel 0. Used by the ChannelManagementProfile.
	 */
	public void sendChannelManagementMessage(int messageNumber, Message message, ReplyHandler reply) {
		Assert.holdsLock("session", sessionLock);
		getCurrentState().sendMessage(0, messageNumber, message, reply);
	}
	
	// --> end of SessionManager methods <--
	
	
	// --> start of MessageHandler methods <-- 

	public final void receiveMSG(int channelNumber, int messageNumber, Message message) {
		lock();
		try {
			getCurrentState().receiveMSG(channelNumber, messageNumber, message);
		} finally {
			unlock();
		}
	}

	public final void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
		lock();
		try {
			getCurrentState().receiveANS(channelNumber, messageNumber, answerNumber, message);
		} finally {
			unlock();
		}
	}
	
	public final void receiveNUL(int channelNumber, int messageNumber) {
		lock();
		try {
			getCurrentState().receiveNUL(channelNumber, messageNumber);
		} finally {
			unlock();
		}
	}

	public final void receiveERR(int channelNumber, int messageNumber, Message message) {
		lock();
		try {
			getCurrentState().receiveERR(channelNumber, messageNumber, message);
		} finally {
			unlock();
		}
	}
		
	public final void receiveRPY(int channelNumber, int messageNumber, Message message) {
		lock();
		try {
			getCurrentState().receiveRPY(channelNumber, messageNumber, message);
		} finally {
			unlock();
		}
	}
	
	// --> end of MessageHandler methods <--
	
	/*
	 * Notifies the ChannelManagementProfile about this event. The
	 * ChannelManagementProfile then asks the application (SessionHandler)
	 * whether to accept the connection and sends the appropriate response.
	 */
	public void connectionEstablished(SocketAddress address) {
		lock();
		try {
			getCurrentState().connectionEstablished(address);
		} finally {
			unlock();
		}
	}
	
	public void exceptionCaught(Throwable cause) {
		lock();
		try {
			getCurrentState().exceptionCaught(cause);
		} finally {
			unlock();
		}
	}
	
	public void connectionClosed() {
		lock();
		try {
			getCurrentState().connectionClosed();
		} finally {
			unlock();
		}
	}
	
	// --> end of TransportHandler methods <--
	
	/**
	 * Interface of session states. The whole implementation of the SessionImpl
	 * class is based on the state pattern. All the important methods are
	 * delegated to an implementation of SessionState. A BEEP session is
	 * inherently state dependent. Some actions are not supported in
	 * certain states.
	 */
	protected static interface SessionState extends MessageHandler {
		
		void connectionEstablished(SocketAddress address);
		
		void exceptionCaught(Throwable cause);

		void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory);
		
		void sendMessage(int channelNumber, int messageNumber, Message message, ReplyHandler listener);
		
		void closeSession();
		
		StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles);
		
		void channelCloseRequested(int channelNumber, CloseChannelRequest request);
		
		void sessionCloseRequested(CloseCallback callback);
		
		void connectionClosed();
		
	}
	
	protected abstract class AbstractSessionState implements SessionState {
		
		public abstract String getName();
		
		public void exceptionCaught(Throwable cause) {
			// TODO: how to handle other exceptions?
			if (cause instanceof ProtocolException) {
				handleProtocolException((ProtocolException) cause);
			}			
		}

		private void handleProtocolException(ProtocolException cause) {
			warn("dropping connection because of a protocol exception", cause);
			try {
				sessionHandler.sessionClosed();
			} finally {
				setCurrentState(deadState);
				beepStream.closeTransport();
			}
		}
		
		public void connectionEstablished(SocketAddress address) {
			throw new IllegalStateException("connection already established, state=<" 
					+ getName() + ">");
		}
		
		public void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory) {
			throw new IllegalStateException("" +
					"cannot start channel in state <" + getName() + ">");
		}
		
		public void sendMessage(int channelNumber, int messageNumber, Message message, ReplyHandler listener) {
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
		
		public void channelCloseRequested(int channelNumber, CloseChannelRequest request) {
			throw new IllegalStateException("cannot close channel");
		}
		
		public void sessionCloseRequested(CloseCallback callback) {
			throw new IllegalStateException("cannot close session");
		}
		
		public void connectionClosed() {
			try {
				sessionHandler.sessionClosed();
			} finally {
				setCurrentState(deadState);
			}
		}

	}

	protected class InitialState extends AbstractSessionState {
		
		@Override
		public String getName() {
			return "initial";
		}
		
		public void connectionEstablished(SocketAddress address) {
			Reply reply = new InitialReply(beepStream);
			registerReply(0, 0, reply);
			
			DefaultStartSessionRequest request = new DefaultStartSessionRequest(!initiator);
			sessionHandler.connectionEstablished(request);
			
			if (request.isCancelled()) {
				channelManagementProfile.sendSessionStartDeclined(request.getReplyCode(), request.getMessage(), reply);
				setCurrentState(deadState);
				beepStream.closeTransport();
			} else {
				channelManagementProfile.sendGreeting(request.getProfiles(), reply);
				greetingSent = true;
				checkInitialAliveTransition();
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
		
		public void receiveRPY(final int channelNumber, final int messageNumber, final Message message) {
			validateMessage(channelNumber, messageNumber);
			greeting = channelManagementProfile.receivedGreeting(message);
			checkInitialAliveTransition();
		}
		
		public void receiveERR(int channelNumber, int messageNumber, Message message) {
			validateMessage(channelNumber, messageNumber);
			BEEPError error = channelManagementProfile.receivedError(message);
			
			info("received error, session start failed: " + error.getCode() + ":" + error.getMessage());
			
			sessionHandler.sessionStartDeclined(error.getCode(), error.getMessage());
			setCurrentState(deadState);
			beepStream.closeTransport();
		}

		private void validateMessage(int channelNumber, int messageNumber) {
			if (channelNumber != 0 || messageNumber != 0) {
				throw new ProtocolException("first message in session must be sent on "
						+ "channel 0 with message number 0: was channel " + channelNumber
						+ ", message=" + messageNumber);
			}
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
					lock();
					try {
						ChannelHandler handler = factory.createChannelHandler(info);
						InternalChannel channel = createChannel(
								SessionImpl.this, info.getUri(), channelNumber);
						ChannelHandler channelHandler = initChannel(channel, handler);
						registerChannel(channelNumber, channel, channelHandler);
						channelHandler.channelOpened(channel);
					} finally {
						unlock();
					}
				}
				public void channelFailed(int code, String message) {
					lock();
					try {
						factory.startChannelFailed(code, message);
					} finally {
						unlock();
					}
				}
			});
		}
		
		@Override
		public void sendMessage(int channelNumber, int messageNumber, Message message, ReplyHandler listener) {
			debug("send message: channel=", channelNumber, ",message=", messageNumber);
			registerReplyHandler(channelNumber, messageNumber, listener);
			beepStream.sendMSG(channelNumber, messageNumber, message);
		}
		
		@Override
		public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
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
			ChannelHandler handler = initChannel(channel, response.getChannelHandler());
			handler.channelOpened(channel);
			registerChannel(channelNumber, channel, handler);
			
			return response;
		}
		
		@Override
		public void receiveMSG(int channelNumber, int messageNumber, Message message) {
			Reply reply = getReply(channelNumber, messageNumber);
			if (reply != null) {
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
			reply = createReply(beepStream, channelNumber, messageNumber);
			
			getChannelHandler(channelNumber).messageReceived(message, reply);
		}

		@Override
		public void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
			ReplyHandlerHolder holder = getReplyHandler(channelNumber, messageNumber);
			holder.receivedANS(channelNumber, messageNumber, message);
		}
		
		@Override
		public void receiveNUL(int channelNumber, int messageNumber) {
			ReplyHandlerHolder holder = getReplyHandler(channelNumber, messageNumber);
			try {
				holder.receivedNUL(channelNumber, messageNumber);
			} finally {
				unregisterReplyHandler(channelNumber, messageNumber);
			}
		}

		@Override
		public void receiveERR(int channelNumber, int messageNumber, Message message) {
			ReplyHandlerHolder holder = getReplyHandler(channelNumber, messageNumber);
			try {
				holder.receivedERR(channelNumber, messageNumber, message);
			} finally {
				unregisterReplyHandler(channelNumber, messageNumber);
			}
		}

		@Override
		public void receiveRPY(int channelNumber, int messageNumber, Message message) {
			ReplyHandlerHolder holder = getReplyHandler(channelNumber, messageNumber);
			try {
				holder.receivedRPY(channelNumber, messageNumber, message);
			} finally {
				unregisterReplyHandler(channelNumber, messageNumber);
			}
		}
		
		@Override
		public void closeSession() {
			// TODO: do not allow session close if there are still open channels
			setCurrentState(closeInitiatedState);
			channelManagementProfile.closeSession(new CloseCallback() {
				public void closeDeclined(int code, String message) {
					Assert.holdsLock("session", sessionLock);
					performClose();
				}
			
				public void closeAccepted() {
					Assert.holdsLock("session", sessionLock);
					performClose();
				}
				
				private void performClose() {
					try {
						sessionHandler.sessionClosed();
					} finally {
						setCurrentState(deadState);
						beepStream.closeTransport();
					}
				}
			});
		}
		
		@Override
		public void channelCloseRequested(int channelNumber, CloseChannelRequest request) {
			DefaultCloseChannelRequest closeRequest = new DefaultCloseChannelRequest();
			ChannelHandler handler = getChannelHandler(channelNumber);
			handler.channelCloseRequested(closeRequest);
			if (closeRequest.isAccepted()) {
				request.accept();
				unregisterChannel(channelNumber);
			} else {
				request.reject();
			}
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
					beepStream.closeTransport();
				}
			}
		}
		
		@Override
		public String toString() {
			return "<alive>";
		}
		
	}
	
	protected class CloseInitiatedState extends AliveState {
		
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
				beepStream.closeTransport();
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
	
	protected class DefaultReply implements Reply {
		
		private final BeepStream mapping;
		
		private final int channel;
		
		private final int messageNumber;
		
		private int answerNumber = 0;
		
		private boolean complete;
		
		public DefaultReply(BeepStream mapping, int channel, int messageNumber) {
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
			mapping.sendANS(channel, messageNumber, answerNumber++, message);
		}
		
		public void sendERR(Message message) {
			Assert.notNull("message", message);
			checkCompletion();
			mapping.sendERR(channel, messageNumber, message);
			complete();
		}
		
		public void sendNUL() {
			checkCompletion();
			mapping.sendNUL(channel, messageNumber);
			complete();
		}
		
		public void sendRPY(Message message) {
			Assert.notNull("message", message);
			checkCompletion();
			mapping.sendRPY(channel, messageNumber, message);
			complete();
		}
		
	}
	
	protected class InitialReply extends DefaultReply {
		
		public InitialReply(BeepStream mapping) {
			super(mapping, 0, 0);
		}
		
		@Override
		public void sendANS(Message message) {
			throw new InternalException("ANS is not a valid initial response");
		}
		
		@Override
		public void sendNUL() {
			throw new InternalException("NUL is not a valid initial response");
		}
		
	}
	
}
