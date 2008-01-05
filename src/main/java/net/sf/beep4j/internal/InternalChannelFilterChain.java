package net.sf.beep4j.internal;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelFilterChain;
import net.sf.beep4j.Message;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;

public interface InternalChannelFilterChain extends ChannelFilterChain {
	
	void fireFilterSendMessage(Message message, ReplyHandler replyHandler);

	void fireFireChannelOpened(Channel channel);
	
	void fireFilterMessageReceived(Message message, Reply reply);
	
	void fireFilterChannelClosed();
	
	void fireFilterReceivedRPY(ReplyHandler replyHandler, Message message);

	void fireFilterReceivedERR(Message message);
	
	void fireFilterReceivedANS(Message message);
	
	void fireFilterReceivedNUL();
	
	void fireFilterSendRPY(Message message);

	void fireFilterSendERR(Message message);

	void fireFilterSendANS(Message message);

	void fireFilterSendNUL();

}
