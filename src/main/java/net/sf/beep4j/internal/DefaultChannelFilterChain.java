package net.sf.beep4j.internal;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelFilter;
import net.sf.beep4j.Message;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.ChannelFilter.NextFilter;

public class DefaultChannelFilterChain implements InternalChannelFilterChain {
	
	private Entry head;
	
	private Entry tail;
	
	public DefaultChannelFilterChain(ChannelFilter headFilter, ChannelFilter tailFilter) {
		head = new Entry(headFilter);
		tail = new Entry(tailFilter);
		
		head.next = tail;
		tail.previous = head;
	}
	
	private void insertBetween(Entry previous, Entry next, Entry entry) {
		entry.next = next;
		entry.previous = previous;
		next.previous.next = entry;
		next.previous = entry;
	}
	
	public void addAfter(Class<? extends ChannelFilter> after, ChannelFilter filter) {
		// TODO Auto-generated method stub

	}

	public void addBefore(Class<? extends ChannelFilter> before, ChannelFilter filter) {
		// TODO Auto-generated method stub

	}

	public void addFirst(ChannelFilter filter) {
		Entry entry = new Entry(filter);
		insertBetween(head, head.next, entry);		
	}

	public void addLast(ChannelFilter filter) {
		Entry entry = new Entry(filter);
		insertBetween(tail.previous, tail, entry);
	}
	
	public void fireFilterSendMessage(Message message, ReplyHandler replyHandler) {
		callPreviousFilterSendMessage(tail, message, replyHandler);
	}
	
	private static void callPreviousFilterSendMessage(Entry entry, Message message, ReplyHandler replyHandler) {
		entry.getFilter().filterSendMessage(entry.getNextFilter(), message, replyHandler);
	}
	
	public void fireFireChannelOpened(Channel channel) {
		callNextFilterChannelOpened(head, channel);
	}
	
	private static void callNextFilterChannelOpened(Entry entry, Channel channel) {
		entry.getFilter().filterChannelOpened(entry.getNextFilter(), channel);
	}
	
	public void fireFilterMessageReceived(Message message, Reply reply) {
		callNextFilterMessageReceived(head, message, reply);
	}
	
	private static void callNextFilterMessageReceived(Entry entry, Message message, Reply reply) {
		entry.getFilter().filterMessageReceived(entry.getNextFilter(), message, reply);
	}
	
	public void fireFilterChannelClosed() {
		callNextFilterChannelClosed(head);
	}
	
	private static void callNextFilterChannelClosed(Entry entry) {
		entry.getFilter().filterChannelClosed(entry.getNextFilter());
	}
	
	public void fireFilterReceivedRPY(ReplyHandler replyHandler, Message message) {
		callNextFilterReceivedRPY(head, replyHandler, message);
	}
	
	private static void callNextFilterReceivedRPY(Entry entry, ReplyHandler replyHandler, Message message) {
		entry.getFilter().filterReceivedRPY(entry.getNextFilter(), replyHandler, message);
	}
	
	public void fireFilterReceivedERR(Message message) {
		callNextFilterReceivedERR(head, message);
	}
	
	private static void callNextFilterReceivedERR(Entry entry, Message message) {
		entry.getFilter().filterReceivedERR(entry.getNextFilter(), message);
	}
	
	public void fireFilterReceivedANS(Message message) {
		callNextFilterReceivedANS(head, message);
	}
	
	private static void callNextFilterReceivedANS(Entry entry, Message message) {
		entry.getFilter().filterReceivedANS(entry.getNextFilter(), message);
	}
	
	public void fireFilterReceivedNUL() {
		callNextFilterReceivedNUL(head);
	}
	
	private static void callNextFilterReceivedNUL(Entry entry) {
		entry.getFilter().filterReceivedNUL(entry.getNextFilter());
	}
	
	public void fireFilterSendRPY(Message message) {
		callPreviousFilterSendRPY(tail, message);
	}
	
	private static void callPreviousFilterSendRPY(Entry entry, Message message) {
		entry.getFilter().filterSendRPY(entry.getNextFilter(), message);
	}
	
	public void fireFilterSendERR(Message message) {
		callPreviousFilterSendERR(tail, message);
	}
	
	private static void callPreviousFilterSendERR(Entry entry, Message message) {
		entry.getFilter().filterSendERR(entry.getNextFilter(), message);
	}
	
	public void fireFilterSendANS(Message message) {
		callPreviousFilterSendANS(tail, message);
	}
	
	private static void callPreviousFilterSendANS(Entry entry, Message message) {
		entry.getFilter().filterSendANS(entry.getNextFilter(), message);
	}
	
	public void fireFilterSendNUL() {
		callPreviousFilterSendNUL(tail);
	}
	
	private static void callPreviousFilterSendNUL(Entry entry) {
		entry.getFilter().filterSendNUL(entry.getNextFilter());
	}
	
	private static class Entry {
		private Entry next;
		private Entry previous;
		private ChannelFilter filter;
		private NextFilter nextFilter;
		
		private Entry(ChannelFilter filter) {
			this.filter = filter;
			this.nextFilter = new NextFilter() {
				public void filterSendMessage(Message message, ReplyHandler replyHandler) {
					callPreviousFilterSendMessage(previous, message, replyHandler);
				}
				public void filterChannelOpened(Channel channel) {
					callNextFilterChannelOpened(next, channel);
				}
				public void filterMessageReceived(Message message, Reply reply) {
					callNextFilterMessageReceived(next, message, reply);
				}
				public void filterChannelClosed() {
					callNextFilterChannelClosed(next);
				}
				public void filterReceivedRPY(ReplyHandler replyHandler, Message message) {
					callNextFilterReceivedRPY(next, replyHandler, message);
				}
				public void filterReceivedERR(Message message) {
					callNextFilterReceivedERR(next, message);
				}
				public void filterReceivedANS(Message message) {
					callNextFilterReceivedANS(next, message);
				}
				public void filterReceivedNUL() {
					callNextFilterReceivedNUL(next);
				}
				public void filterSendRPY(Message message) {
					callPreviousFilterSendRPY(previous, message);
				}
				public void filterSendERR(Message message) {
					callPreviousFilterSendERR(previous, message);
				}
				public void filterSendANS(Message message) {
					callPreviousFilterSendANS(previous, message);
				}
				public void filterSendNUL() {
					callPreviousFilterSendNUL(previous);
				}
			};
		}
		
		private ChannelFilter getFilter() {
			return filter;
		}
		
		private NextFilter getNextFilter() {
			return nextFilter;
		}
	}

}
