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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.Session;
import net.sf.beep4j.internal.management.CloseCallback;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;
import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.internal.util.IntegerSequence;
import net.sf.beep4j.internal.util.Sequence;

class ChannelImpl implements Channel, InternalChannel {
	
	private final InternalSession session;
	
	private final String profile;
	
	private final int channelNumber;
	
	private final Sequence<Integer> messageNumberSequence = new IntegerSequence(1, 1);

	private final Map<Integer, Reply> replies = new HashMap<Integer, Reply>();
	
	private final LinkedList<ReplyHandlerHolder> replyHandlerHolders = new LinkedList<ReplyHandlerHolder>();
	
	private ChannelHandler channelHandler;
	
	private State state = new Alive();
	
	/**
	 * Counter that counts how many messages we have sent but to which we
	 * have not received a reply.
	 */
	private int outstandingReplyCount;
	
	/**
	 * Counter that counts how many messages we have received but to which
	 * we have not sent a response.
	 */
	private int outstandingResponseCount;
	
	public ChannelImpl(
			InternalSession session, 
			String profile, 
			int channelNumber) {
		this.session = session;
		this.profile = profile;
		this.channelNumber = channelNumber;
	}
	
	public void channelOpened(ChannelHandler channelHandler) {
		Assert.notNull("channelHandler", channelHandler);
		this.channelHandler = channelHandler;
		this.channelHandler.channelOpened(this);
	}
	
	// --> replies to incoming messages <--
	
	protected boolean hasReply(int messageNumber) {
		return replies.containsKey(messageNumber);
	}
	
	protected Reply createReply(InternalSession session, int messageNumber) {
		Reply reply = new DefaultReply(session, channelNumber, messageNumber);
		registerReply(messageNumber, reply);
		return reply;
	}
	
	protected void registerReply(int messageNumber, Reply reply) {
		replies.put(messageNumber, reply);
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
	
	private ReplyHandlerHolder getReplyHandlerHolder(final int messageNumber) {
		if (replyHandlerHolders.isEmpty()) {
			throw new ProtocolException("received a reply (message=" + messageNumber + ") "
					+ " on channel " + channelNumber + " but expects no outstanding replies");
		}
		ReplyHandlerHolder holder = replyHandlerHolders.getFirst();
		if (holder.getMessageNumber() != messageNumber) {
			throw new ProtocolException("next expected reply on channel "
					+ "must have message number "
					+ holder.getMessageNumber() + " but was "
					+ messageNumber);
		}
		return holder;
	}
	
	private ReplyHandlerHolder unregisterReplyHandlerHolder(final int messageNumber) {
		ReplyHandlerHolder holder = replyHandlerHolders.removeFirst();
		if (messageNumber != holder.getMessageNumber()) {
			throw new ProtocolException("next expected reply has message number " 
					+ holder.getMessageNumber()
					+ "; received reply had message number " + messageNumber);
		}
		return holder;
	}

	private void registerReplyHandler(final int messageNumber, final ReplyHandler handler) {
		replyHandlerHolders.addLast(new ReplyHandlerHolder(messageNumber, handler));
	}
	
	public void receiveMSG(int messageNumber, Message message) {
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
		state.messageReceived(message, reply);
	}

	public void receiveRPY(int messageNumber, Message message) {
		ReplyHandlerHolder holder = getReplyHandlerHolder(messageNumber);
		state.receiveRPY(holder, message);
	}
	
	public void receiveERR(int messageNumber, Message message) {
		ReplyHandlerHolder holder = getReplyHandlerHolder(messageNumber);
		state.receiveERR(holder, message);
	}
	
	public void receiveANS(int messageNumber, int answerNumber, Message message) {
		ReplyHandlerHolder holder = getReplyHandlerHolder(messageNumber);
		state.receiveANS(holder, message);
	}
	
	public void receiveNUL(int messageNumber) {
		ReplyHandlerHolder holder = getReplyHandlerHolder(messageNumber);
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
	
	public String getProfile() {
		return profile;
	}

	public Session getSession() {
		return session;
	}
	
	public MessageBuilder createMessageBuilder() {
		return new DefaultMessageBuilder();
	}
	
	protected void setState(State state) {
		this.state = state;
		this.state.checkCondition();
	}
	
	public void sendMessage(Message message, ReplyHandler reply) {
		Assert.notNull("message", message);
		Assert.notNull("listener", reply);
		state.sendMessage(message, wrapReplyHandler(reply));
	}

	private ReplyHandler wrapReplyHandler(ReplyHandler reply) {
		return new ReplyHandlerWrapper(reply);
	}
	
	public void close(CloseChannelCallback callback) {
		Assert.notNull("callback", callback);
		state.closeInitiated(callback);
	}

	protected Reply wrapReply(Reply reply) {
		incrementOutstandingResponseCount();
		return new ReplyWrapper(reply);
	}
	
	public void channelCloseRequested(CloseCallback callback) {
		state.closeRequested(callback);
	}
	
	protected void doClose() {
		channelHandler.channelClosed();
		setState(new Dead());
	}
	
	private synchronized void incrementOutstandingReplyCount() {
		outstandingReplyCount++;
	}
	
	private synchronized void decrementOutstandingReplyCount() {
		outstandingReplyCount--;
		state.checkCondition();
	}
	
	private synchronized boolean hasOutstandingReplies() {
		return outstandingReplyCount > 0;
	}
	
	private synchronized void incrementOutstandingResponseCount() {
		outstandingResponseCount++;
	}
	
	private synchronized void decrementOutstandingResponseCount() {
		outstandingResponseCount--;
		state.checkCondition();
	}
	
	private synchronized boolean hasOutstandingResponses() {
		return outstandingResponseCount > 0;
	}
	
	private synchronized boolean isReadyToShutdown() {
		return !hasOutstandingReplies() && !hasOutstandingResponses();
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
			incrementOutstandingReplyCount();
		}
		
		public void receivedANS(Message message) {
			target.receivedANS(message);			
		}
		
		public void receivedNUL() {
			decrementOutstandingReplyCount();
			target.receivedNUL();
		}
		
		public void receivedERR(Message message) {
			decrementOutstandingReplyCount();
			target.receivedERR(message);
		}
		
		public void receivedRPY(Message message) {
			decrementOutstandingReplyCount();
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
		
		public MessageBuilder createMessageBuilder() {
			return target.createMessageBuilder();
		}
		
		public void sendANS(Message message) {
			target.sendANS(message);			
		}
		
		public void sendNUL() {
			decrementOutstandingResponseCount();
			target.sendNUL();
		}
		
		public void sendERR(Message message) {
			decrementOutstandingResponseCount();
			target.sendERR(message);
		}
		
		public void sendRPY(Message message) {
			decrementOutstandingResponseCount();
			target.sendRPY(message);
		}
	}
	
	private static interface State {
		
		void checkCondition();
		
		void sendMessage(Message message, ReplyHandler replyHandler);
		
		void closeInitiated(CloseChannelCallback callback);
		
		void closeRequested(CloseCallback callback);
		
		void messageReceived(Message message, Reply reply);
		
		void receiveRPY(ReplyHandlerHolder holder, Message message);
		
		void receiveERR(ReplyHandlerHolder holder, Message message);
		
		void receiveANS(ReplyHandlerHolder holder, Message message);
		
		void receiveNUL(ReplyHandlerHolder holder);
		
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
		
		public void messageReceived(Message message, Reply reply) {
			throw new IllegalStateException();
		}
		
		public void receiveANS(ReplyHandlerHolder holder, Message message) {
			throw new IllegalStateException();
		}
		
		public void receiveNUL(ReplyHandlerHolder holder) {
			throw new IllegalStateException();
		}
		
		public void receiveERR(ReplyHandlerHolder holder, Message message) {
			throw new IllegalStateException();
		}
		
		public void receiveRPY(ReplyHandlerHolder holder, Message message) {
			throw new IllegalStateException();
		}
	}
	
	private abstract class AbstractReceivingState extends AbstractState {
		
		@Override
		public void receiveANS(ReplyHandlerHolder holder, Message message) {
			holder.receivedANS(message);
		}
		
		@Override
		public void receiveNUL(ReplyHandlerHolder holder) {
			try {
				holder.receivedNUL();
			} finally {
				unregisterReplyHandlerHolder(holder.getMessageNumber());
			}
		}
		
		@Override
		public void receiveERR(ReplyHandlerHolder holder, Message message) {
			try {
				holder.receivedERR(message);
			} finally {
				unregisterReplyHandlerHolder(holder.getMessageNumber());
			}
		}
		
		@Override
		public void receiveRPY(ReplyHandlerHolder holder, Message message) {
			try {
				holder.receivedRPY(message);
			} finally {
				unregisterReplyHandlerHolder(holder.getMessageNumber());
			}
		}
	}
	
	private class Alive extends AbstractReceivingState {
		
		@Override
		public void sendMessage(final Message message, final ReplyHandler replyHandler) {
			int messageNumber = messageNumberSequence.next();
			registerReplyHandler(messageNumber, replyHandler);
			session.sendMSG(channelNumber, messageNumber, message, replyHandler);
		}
		
		@Override
		public void messageReceived(Message message, Reply reply) {
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
		public void messageReceived(Message message, Reply reply) {
			channelHandler.messageReceived(message, reply);
		}
		
		@Override
		public void checkCondition() {
			if (isReadyToShutdown()) {
				session.requestChannelClose(channelNumber, new CloseCallback() {
					public void closeDeclined(int code, String message) {
						callback.closeDeclined(code, message);
						setState(new Alive());
					}
					public void closeAccepted() {
						callback.closeAccepted();
						doClose();
					}
				});
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
			doClose();
			closeCallback.closeAccepted();
		}
		
	}
	
	private class CloseRequested extends AbstractReceivingState {
		
		private final CloseCallback callback;
		
		private CloseRequested(CloseCallback callback) {
			this.callback = callback;
		}
		
		@Override
		public void messageReceived(Message message, Reply handler) {
			throw new ProtocolException("the remote peer is not allowed to send "
					+ "further messages on a channel after sending a channel close request");
		}
		
		@Override
		public void checkCondition() {
			if (isReadyToShutdown()) {
				DefaultCloseChannelRequest request = new DefaultCloseChannelRequest();
				channelHandler.channelCloseRequested(request);
				if (request.isAccepted()) {
					doClose();
					this.callback.closeAccepted();
				} else {
					this.callback.closeDeclined(550, "still working");
					setState(new Alive());
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
