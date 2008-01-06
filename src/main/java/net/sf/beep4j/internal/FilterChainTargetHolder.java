package net.sf.beep4j.internal;

import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.internal.management.CloseCallback;

public class FilterChainTargetHolder {
	
	private static final ThreadLocal<ChannelHandler> channelHandlerHolder = new ThreadLocal<ChannelHandler>();
	
	private static final ThreadLocal<Reply> replyHolder = new ThreadLocal<Reply>();
	
	private static final ThreadLocal<ReplyHandler> replyHandlerHolder = new ThreadLocal<ReplyHandler>();
	
	private static final ThreadLocal<CloseCallback> closeCallbackHolder = new ThreadLocal<CloseCallback>();
	
	public static void setReply(Reply reply) {
		replyHolder.set(reply);
	}
	
	public static Reply getReply() {
		return replyHolder.get();
	}
	
	public static void setReplyHandler(ReplyHandler target) {
		replyHandlerHolder.set(target);
	}
	
	public static ReplyHandler getReplyHandler() {
		return replyHandlerHolder.get();
	}
	
	public static void setCloseCallback(CloseCallback target) {
		closeCallbackHolder.set(target);
	}
	
	public static CloseCallback getCloseCallback() {
		return closeCallbackHolder.get();
	}

	public static void setChannelHandler(ChannelHandler target) {
		channelHandlerHolder.set(target);
	}
	
	public static ChannelHandler getChannelHandler() {
		return channelHandlerHolder.get();
	}
	
}
