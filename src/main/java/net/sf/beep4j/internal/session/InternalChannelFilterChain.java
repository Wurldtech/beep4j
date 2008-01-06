package net.sf.beep4j.internal.session;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelFilterChain;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;

public interface InternalChannelFilterChain extends ChannelFilterChain {
	
	// --> filtering Channel methods <--
	
	void fireFilterSendMessage(Message message, ReplyHandler replyHandler);

	void fireFilterClose(CloseChannelCallback callback);
	
	// --> filtering ChannelHandler methods <--
	
	void fireFilterChannelOpened(Channel channel);
	
	void fireFilterMessageReceived(Message message, Reply reply);
	
	void fireFilterChannelCloseRequested(CloseChannelRequest request);
	
	void fireFilterChannelClosed();
	
	// --> filtering ReplyHandler methods <--
	
	void fireFilterReceivedRPY(Message message);

	void fireFilterReceivedERR(Message message);
	
	void fireFilterReceivedANS(Message message);
	
	void fireFilterReceivedNUL();
	
	// --> filtering Reply methods <--
	
	void fireFilterSendRPY(Message message);

	void fireFilterSendERR(Message message);

	void fireFilterSendANS(Message message);

	void fireFilterSendNUL();

}
