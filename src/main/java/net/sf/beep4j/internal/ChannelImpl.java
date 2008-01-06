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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelFilterChainBuilder;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.Session;
import net.sf.beep4j.ext.ChannelFilterAdapter;
import net.sf.beep4j.internal.management.CloseCallback;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;
import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.internal.util.IntegerSequence;
import net.sf.beep4j.internal.util.Sequence;

class ChannelImpl implements Channel, InternalChannel {
	
	private final InternalSession session;
	
	private final String profile;
	
	private final int channelNumber;
	
	private final InternalChannelFilterChain filterChain;
	
	private final ReentrantLock sessionLock;
	
	private final Sequence<Integer> messageNumberSequence = new IntegerSequence(1, 1);

	/**
	 * Maps from message number to Reply objects. Replies are registered when they are
	 * created and removed from this map as soon as they are completed by the
	 * application.
	 */
	private final Map<Integer, Reply> replies = Collections.synchronizedMap(new HashMap<Integer, Reply>());
	
	private final LinkedList<ReplyHandlerHolder> replyHandlerHolders = new LinkedList<ReplyHandlerHolder>();
	
	private ChannelHandler channelHandler;
	
	private State state = new Alive();
	
	/**
	 * Counter that counts how many messages we have sent but to which we
	 * have not received a reply.
	 */
	private int openOutgoingReplies;
	
	/**
	 * Counter that counts how many messages we have received but to which
	 * we have not sent a response.
	 */
	private int openIncomingReplies;
	
	public ChannelImpl(
			InternalSession session, 
			String profile, 
			int channelNumber,
			ChannelFilterChainBuilder filterChainBuilder,
			ReentrantLock sessionLock) {
		this.session = session;
		this.profile = profile;
		this.channelNumber = channelNumber;
		this.sessionLock = sessionLock;
		this.filterChain = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		filterChainBuilder.buildFilterChain(filterChain);
	}
	
	protected void setState(State state) {
		this.state = state;
		this.state.checkCondition();
	}
	
	// --> replies to incoming messages <--
	
	protected boolean hasReply(int messageNumber) {
		return replies.containsKey(messageNumber);
	}
	
	protected Reply createReply(InternalSession session, int messageNumber) {
		Reply reply = wrapReply(new DefaultReply(session, channelNumber, messageNumber));
		registerReply(messageNumber, reply);
		return reply;
	}
	
	protected void registerReply(int messageNumber, Reply reply) {
		if (replies.put(messageNumber, reply) != null) {
			throw new ProtocolException("there is already a reply registered for " 
					+ messageNumber
					+ " on channel " + channelNumber);
		}
	}
	
	protected void replyCompleted(int channelNumber, int messageNumber) {
		Reply reply = replies.remove(messageNumber);
		if (reply == null) {
			throw new IllegalStateException(
					"completed reply that does no longer exist (channel="
					+ channelNumber + ",message=" + messageNumber + ")");
		}
	}
	
	// --> replies to outgoing messages <--
	
	/**
	 * Gets the next ReplyHandlerHolder. The given <var>messageNumber</var>
	 * must match the message number of the ReplyHandlerHolder. Otherwise
	 * a protocol exception is thrown and the session terminated.
	 */
	private ReplyHandlerHolder getReplyHandlerHolder(final int messageNumber) {
		synchronized (replyHandlerHolders) {
			if (replyHandlerHolders.isEmpty()) {
				throw new ProtocolException("received a reply (message=" + messageNumber + ") "
						+ " on channel " + channelNumber + " but expects no outstanding replies");
			}
			ReplyHandlerHolder holder = replyHandlerHolders.getFirst();
			if (holder.getMessageNumber() != messageNumber) {
				throw new ProtocolException("next expected reply on channel "
						+ channelNumber + " must have message number "
						+ holder.getMessageNumber() + " but was "
						+ messageNumber);
			}
			return holder;
		}
	}
	
	/**
	 * Removes the next ReplyHandlerHolder. The given <var>messageNumber</var>
	 * must match the message number of the returned ReplyHandlerHolder. If that
	 * is not the case a protocol exception is thrown and the session is
	 * terminated.
	 * 
	 * @param messageNumber the expected message number
	 */
	private ReplyHandlerHolder unregisterReplyHandlerHolder(final int messageNumber) {
		synchronized (replyHandlerHolders) {
			ReplyHandlerHolder holder = replyHandlerHolders.removeFirst();
			if (messageNumber != holder.getMessageNumber()) {
				throw new ProtocolException("next expected reply has message number " 
						+ holder.getMessageNumber()
						+ "; received reply had message number " + messageNumber);
			}
			return holder;
		}
	}
	
	/**
	 * Registers a ReplyHandlerHolder. A ReplyHandlerHolder represents a reply that
	 * must be received later.
	 * 
	 * @param messageNumber the message number of the incoming reply
	 * @param handler the ReplyHandler that will process the reply
	 */
	private void registerReplyHandler(final int messageNumber, final ReplyHandler handler) {
		synchronized (replyHandlerHolders) {
			replyHandlerHolders.addLast(new ReplyHandlerHolder(handler, messageNumber));
		}
	}
	
	// --> start of InternalChannel methods <--
	
	public void channelOpened(ChannelHandler channelHandler) {
		Assert.notNull("channelHandler", wrappChannelHandler(channelHandler));
		this.channelHandler = wrappChannelHandler(channelHandler);
		this.channelHandler.channelOpened(this);
	}

	private ChannelHandler wrappChannelHandler(ChannelHandler channelHandler) {
		return new FilterChannelHandler(filterChain, channelHandler);
	}
	
	public void receiveMSG(final int messageNumber, final Message message) {
		Assert.holdsLock("session", sessionLock);
		
		if (hasReply(messageNumber)) {
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
		
		Reply reply = createReply(session, messageNumber);
		state.receiveMSG(message, reply);
	}
	
	public void receiveRPY(final int messageNumber, final Message message) {
		Assert.holdsLock("session", sessionLock);
		ReplyHandlerHolder holder = unregisterReplyHandlerHolder(messageNumber);
		state.receiveRPY(holder, message);
	}
	
	public void receiveERR(final int messageNumber, final Message message) {
		Assert.holdsLock("session", sessionLock);
		ReplyHandlerHolder holder = unregisterReplyHandlerHolder(messageNumber);
		state.receiveERR(holder, message);
	}
	
	public void receiveANS(final int messageNumber, final int answerNumber, final Message message) {
		Assert.holdsLock("session", sessionLock);
		ReplyHandlerHolder holder = getReplyHandlerHolder(messageNumber);
		state.receiveANS(holder, message);
	}
	
	public void receiveNUL(final int messageNumber) {
		Assert.holdsLock("session", sessionLock);
		ReplyHandlerHolder holder = unregisterReplyHandlerHolder(messageNumber);
		state.receiveNUL(holder);
	}
	
	public boolean isAlive() {
		return state instanceof Alive;
	}
	
	public boolean isDead() {
		return state instanceof Dead;
	}
	
	public boolean isShuttingDown() {
		return !isAlive() && !isDead();
	}
	
	// --> end of InternalChannel methods <--
	
	// --> start of Channel methods <--
	
	public String getProfile() {
		return profile;
	}

	public Session getSession() {
		return session;
	}
	
	public MessageBuilder createMessageBuilder() {
		return new DefaultMessageBuilder();
	}
	
	public void sendMessage(Message message, ReplyHandler reply) {
		Assert.notNull("message", message);
		Assert.notNull("listener", reply);
		incrementOpenOutgoingReplies();
		state.sendMessage(message, wrapReplyHandler(reply));
	}
	
	private void lock() {
		if (sessionLock != null) {
			sessionLock.lock();
		}
	}
	
	private void unlock() {
		if (sessionLock != null) {
			sessionLock.unlock();
		}
	}
	
	private void doSendMessage(Message message, ReplyHandler replyHandler) {
		lock();
		try {
			int messageNumber = messageNumberSequence.next();
			registerReplyHandler(messageNumber, replyHandler);
			session.sendMSG(channelNumber, messageNumber, message, replyHandler);
		} finally {
			unlock();
		}
	}

	/*
	 * The passed in ReplyHandler is decorated by the the following 
	 * decorators:
	 * 
	 * 1. ReplyHandlerWrapper:   bookkeeping
	 * 2. UnlockingReplyHandler: unlock / lock session lock
	 * 3. FilterReplyHandler:    passes request through filters
	 * 4. target:                after the filters are processed, this method is called
	 */
	protected ReplyHandler wrapReplyHandler(ReplyHandler replyHandler) {
		replyHandler = new FilterReplyHandler(filterChain, replyHandler);
		replyHandler = new UnlockingReplyHandler(replyHandler, sessionLock);
		replyHandler = new ReplyHandlerWrapper(replyHandler);
		return replyHandler;
	}
	
	public void close(CloseChannelCallback callback) {
		Assert.notNull("callback", callback);
		state.closeInitiated(new UnlockingCloseChannelCallback(callback, sessionLock));
	}

	protected Reply wrapReply(Reply reply) {
		incrementOpenIncomingReplies();
		reply = new ReplyWrapper(reply);
		reply = new LockingReply(reply, sessionLock);
		return new FilterReply(filterChain, reply);
	}
	
	public void channelCloseRequested(CloseCallback callback) {
		state.closeRequested(callback);
	}
	
	private void doChannelCloseRequested(CloseChannelRequest r) {
		DefaultCloseChannelRequest request = (DefaultCloseChannelRequest) r;
		CloseCallback callback = FilterChainTargetHolder.getCloseCallback();
		FilterChainTargetHolder.getChannelHandler().channelCloseRequested(request);
		if (request.isAccepted()) {
			channelHandler.channelClosed();
			callback.closeAccepted();
		} else {
			callback.closeDeclined(550, "still working");
			setState(new Alive());
		}
	}
	
	// --> end of Channel methods <--
	
	protected synchronized void incrementOpenOutgoingReplies() {
		openOutgoingReplies++;
	}
	
	protected synchronized void outgoingReplyCompleted() {
		openOutgoingReplies--;
		state.checkCondition();
	}
	
	protected synchronized boolean hasOpenOutgoingReplies() {
		return openOutgoingReplies > 0;
	}
	
	protected synchronized void incrementOpenIncomingReplies() {
		openIncomingReplies++;
	}
	
	protected synchronized void incomingReplyCompleted() {
		openIncomingReplies--;
		state.checkCondition();
	}
	
	protected synchronized boolean hasOpenIncomingReplies() {
		return openIncomingReplies > 0;
	}
	
	protected synchronized boolean isReadyToShutdown() {
		return !hasOpenOutgoingReplies() && !hasOpenIncomingReplies();
	}

	/**
	 * Filter used by the {@link DefaultChannelFilterChain} at the head of
	 * the chain. Depending on the kind of operation either delegates
	 * to the next filter (incoming operations on {@link ChannelHandler} and
	 * {@link ReplyHandler}) or performs the requested operation (on outgoing
	 * operations, {@link Channel} and {@link Reply}).
	 */
	private final class HeadFilter extends ChannelFilterAdapter {
		@Override
		public void filterSendMessage(NextFilter next, Message message, ReplyHandler replyHandler) {
			doSendMessage(message, replyHandler);
		}
		
		@Override
		public void filterClose(NextFilter next, CloseChannelCallback callback) {
			// TODO: perform close
		}

		@Override
		public void filterSendRPY(NextFilter next, Message message) {
			FilterChainTargetHolder.getReply().sendRPY(message);
		}

		@Override
		public void filterSendERR(NextFilter next, Message message) {
			FilterChainTargetHolder.getReply().sendERR(message);
		}

		@Override
		public void filterSendANS(NextFilter next, Message message) {
			FilterChainTargetHolder.getReply().sendANS(message);
		}
		
		@Override
		public void filterSendNUL(NextFilter next) {
			FilterChainTargetHolder.getReply().sendNUL();
		}
	}

	/**
	 * Filter used by the {@link DefaultChannelFilterChain} at the tail of
	 * the chain. Depending on the kind of operation either delegates
	 * to the next filter (outgoing operations on {@link Channel} and
	 * {@link Reply}) or performs the requested operation (on incoming
	 * operations, {@link ChannelHandler} and {@link ReplyHandler}).
	 */
	private final class TailFilter extends ChannelFilterAdapter {
		@Override
		public void filterChannelOpened(NextFilter next, Channel channel) {
			FilterChainTargetHolder.getChannelHandler().channelOpened(ChannelImpl.this);
		}

		@Override
		public void filterMessageReceived(NextFilter next, Message message, Reply reply) {
			FilterChainTargetHolder.getChannelHandler().messageReceived(message, reply);
		}
		
		@Override
		public void filterChannelCloseRequested(NextFilter next, CloseChannelRequest request) {
			doChannelCloseRequested(request);
		}
		
		@Override
		public void filterChannelClosed(NextFilter next) {
			FilterChainTargetHolder.getChannelHandler().channelClosed();
			setState(new Dead());
		}

		@Override
		public void filterReceivedRPY(NextFilter next, Message message) {
			FilterChainTargetHolder.getReplyHandler().receivedRPY(message);
		}
		
		@Override
		public void filterReceivedERR(NextFilter next, Message message) {
			FilterChainTargetHolder.getReplyHandler().receivedERR(message);
		}
		
		@Override
		public void filterReceivedANS(NextFilter next, Message message) {
			FilterChainTargetHolder.getReplyHandler().receivedANS(message);
		}
		
		@Override
		public void filterReceivedNUL(NextFilter next) {
			FilterChainTargetHolder.getReplyHandler().receivedNUL();
		}
	}

	/*
	 * Wrapper for ReplyHandler that decrements a counter whenever
	 * a complete message has been received. Intercepts calls to 
	 * the real ReplyHandler from the application to make this
	 * book-keeping possible.
	 */
	private class ReplyHandlerWrapper implements ReplyHandler {

		private final ReplyHandler target;
		
		private ReplyHandlerWrapper(ReplyHandler target) {
			Assert.notNull("target", target);
			this.target = target;
		}
		
		public void receivedANS(Message message) {
			target.receivedANS(message);			
		}
		
		public void receivedNUL() {
			outgoingReplyCompleted();
			target.receivedNUL();
		}
		
		public void receivedERR(Message message) {
			outgoingReplyCompleted();
			target.receivedERR(message);
		}
		
		public void receivedRPY(Message message) {
			outgoingReplyCompleted();
			target.receivedRPY(message);
		}
	}
	
	/*
	 * The ReplyWrapper is used to count outstanding replies. This information
	 * is needed to know when a channel close can be accepted.
	 */
	private class ReplyWrapper implements Reply {
		
		private final Reply target;
		
		private ReplyWrapper(Reply target) {
			Assert.notNull("target", target);
			this.target = target;
		}
		
		public void sendANS(Message message) {
			target.sendANS(message);			
		}
		
		public void sendNUL() {
			incomingReplyCompleted();
			target.sendNUL();
		}
		
		public void sendERR(Message message) {
			incomingReplyCompleted();
			target.sendERR(message);
		}
		
		public void sendRPY(Message message) {
			incomingReplyCompleted();
			target.sendRPY(message);
		}
	}
	
	private static class ReplyHandlerHolder implements ReplyHandler {
		private final ReplyHandler target;
		private final int messageNumber;
		private ReplyHandlerHolder(ReplyHandler target, int messageNumber) {
			this.target = target;
			this.messageNumber = messageNumber;
		}
		int getMessageNumber() {
			return messageNumber;
		}
		public void receivedANS(Message message) {
			target.receivedANS(message);
		}
		public void receivedNUL() {
			target.receivedNUL();
		}
		public void receivedERR(Message message) {
			target.receivedERR(message);
		}
		public void receivedRPY(Message message) {
			target.receivedRPY(message);
		}
	}
	
	private static interface State {
		
		void checkCondition();
		
		void sendMessage(Message message, ReplyHandler replyHandler);
		
		void closeInitiated(CloseChannelCallback callback);
		
		void closeRequested(CloseCallback callback);
		
		void receiveMSG(Message message, Reply reply);
		
		void receiveRPY(ReplyHandler replyHandler, Message message);
		
		void receiveERR(ReplyHandler replyHandler, Message message);
		
		void receiveANS(ReplyHandler replyHandler, Message message);
		
		void receiveNUL(ReplyHandler replyHandler);
		
	}
	
	private static abstract class AbstractState implements State {
		
		public void checkCondition() {
			// nothing to check
		}
		
		public void sendMessage(Message message, ReplyHandler replyHandler) {
			throw new IllegalStateException();
		}
		
		public void closeInitiated(CloseChannelCallback callback) {
			throw new IllegalStateException();
		}
		
		public void closeRequested(CloseCallback callback) {
			throw new IllegalStateException();
		}
		
		public void receiveMSG(Message message, Reply reply) {
			throw new IllegalStateException();
		}
		
		public void receiveANS(ReplyHandler replyHandler, Message message) {
			throw new IllegalStateException();
		}
		
		public void receiveNUL(ReplyHandler replyHandler) {
			throw new IllegalStateException();
		}
		
		public void receiveERR(ReplyHandler replyHandler, Message message) {
			throw new IllegalStateException();
		}
		
		public void receiveRPY(ReplyHandler replyHandler, Message message) {
			throw new IllegalStateException();
		}
	}
	
	private abstract class AbstractReceivingState extends AbstractState {
		
		@Override
		public void receiveANS(ReplyHandler replyHandler, Message message) {
			replyHandler.receivedANS(message);
		}
		
		@Override
		public void receiveNUL(ReplyHandler replyHandler) {
			replyHandler.receivedNUL();
		}
		
		@Override
		public void receiveERR(ReplyHandler replyHandler, Message message) {
			replyHandler.receivedERR(message);
		}
		
		@Override
		public void receiveRPY(ReplyHandler replyHandler, Message message) {
			replyHandler.receivedRPY(message);
		}
	}
	
	private class Alive extends AbstractReceivingState {
		
		@Override
		public void sendMessage(final Message message, final ReplyHandler replyHandler) {
			filterChain.fireFilterSendMessage(message, replyHandler);
		}
		
		@Override
		public void receiveMSG(Message message, Reply reply) {
			channelHandler.messageReceived(message, reply);
		}
		
		@Override
		public void closeInitiated(CloseChannelCallback callback) {
			setState(new CloseInitiated(callback));
		}
		
		@Override
		public void closeRequested(CloseCallback callback) {
			setState(new CloseRequested(callback));
		}
		
	}
	
	private class CloseInitiated extends AbstractReceivingState {
		
		private final CloseChannelCallback callback;
		
		private CloseInitiated(CloseChannelCallback callback) {
			this.callback = callback;
		}
		
		@Override
		public void receiveMSG(Message message, Reply reply) {
			channelHandler.messageReceived(message, reply);
		}
		
		@Override
		public void checkCondition() {
			if (isReadyToShutdown()) {
				final CloseCallback closeCallback = new CloseCallback() {
					public void closeDeclined(int code, String message) {
						callback.closeDeclined(code, message);
						setState(new Alive());
					}
					public void closeAccepted() {
						callback.closeAccepted();
						channelHandler.channelClosed();
					}
				};
				session.requestChannelClose(channelNumber, closeCallback);
			}
		}
		
		/*
		 * If we receive a close request in this state, we accept the close
		 * request immediately without consulting the application. The
		 * reasoning is that the application already requested to close
		 * the channel, so it makes no sense to let it change that 
		 * decision.
		 */
		@Override
		public void closeRequested(CloseCallback closeCallback) {
			callback.closeAccepted();
			channelHandler.channelClosed();
			closeCallback.closeAccepted();
		}
		
	}
	
	private class CloseRequested extends AbstractReceivingState {
		
		private final CloseCallback callback;
		
		private CloseRequested(CloseCallback callback) {
			this.callback = callback;
		}
		
		@Override
		public void receiveMSG(Message message, Reply handler) {
			throw new ProtocolException("the remote peer is not allowed to send "
					+ "further messages on a channel after sending a channel close request");
		}
		
		@Override
		public void checkCondition() {
			if (isReadyToShutdown()) {
				DefaultCloseChannelRequest request = new DefaultCloseChannelRequest();
				FilterChainTargetHolder.setCloseCallback(callback);
				try {
					channelHandler.channelCloseRequested(request);
				} finally {
					FilterChainTargetHolder.setCloseCallback(null);
				}
			}
		}
	}
	
	private class Dead extends AbstractState {
		// dead is dead ;)
	}
	
	
	protected class DefaultReply implements Reply {
		
		private final InternalSession session;
		
		private final int channel;
		
		private final int messageNumber;
		
		private int answerNumber = 0;
		
		private boolean complete;
		
		public DefaultReply(InternalSession session, int channel, int messageNumber) {
			Assert.notNull("session", session);
			this.session = session;
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
			session.sendANS(channel, messageNumber, answerNumber++, message);
		}
		
		public void sendERR(Message message) {
			Assert.notNull("message", message);
			checkCompletion();
			session.sendERR(channel, messageNumber, message);
			complete();
		}
		
		public void sendNUL() {
			checkCompletion();
			session.sendNUL(channel, messageNumber);
			complete();
		}
		
		public void sendRPY(Message message) {
			Assert.notNull("message", message);
			checkCompletion();
			session.sendRPY(channel, messageNumber, message);
			complete();
		}
		
	}

}
