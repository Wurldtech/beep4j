package net.sf.beep4j.internal;

import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;

public class FilterChainTargetHolder {
	
	private static final ThreadLocal<Reply> replyHolder = new ThreadLocal<Reply>();
	
	private static final ThreadLocal<ReplyHandler> replyHandlerHolder = new ThreadLocal<ReplyHandler>();
	
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
	
}
