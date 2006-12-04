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
package net.sf.beep4j;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

/**
 * A MessageBuilder allows to easily create Message objects. It has methods
 * to set MIME headers. The content type and character encoding can be
 * set with convenience methods.
 * 
 * <p>To write something into the message you can either get the output
 * stream or a writer. If you want to write binary data you should use
 * the output stream. Otherwise, use the writer as it uses the correct
 * charset encoder.</p>
 * 
 * <p>Finally, you can retrieve the completed message with the
 * {@link #getMessage()} method.</p>
 * 
 * <p>An example of a common use case is given below:</p>
 * 
 * <pre>
 *   MessageBuilder builder = ...;
 *   builder.setContentType("application", "beep+xml");
 *   builder.setCharacterEncoding("UTF-8");
 *   PrintWriter writer = new PrintWriter(builder.getWriter());
 *   writer.println("<ok />");
 *   Message message = writer.getMessage();
 * </pre>
 * 
 * @author Simon Raess
 */
public interface MessageBuilder {
	
	/**
	 * Sets the content type of the message.
	 * 
	 * @param type the type
	 * @param subtype the subtype
	 */
	void setContentType(String type, String subtype);
	
	/**
	 * Sets the character encoding used by the message.
	 * 
	 * @param charset the charset
	 */
	void setCharsetName(String charset);
	
	/**
	 * Adds an arbitrary header field.
	 * 
	 * @param name the name of the header
	 * @param value the value
	 */
	void addHeader(String name, String value);
		
	/**
	 * Gets the underlying OutputStream that can be used to write
	 * binary messages.
	 * 
	 * @return the OutputStream
	 */
	OutputStream getOutputStream();
	
	/**
	 * Gets the underlying Writer that can be used to write textual
	 * messages.
	 * 
	 * @return the Writer
	 */
	Writer getWriter();
	
	/**
	 * Allocates a ByteBuffer into which the message content can be
	 * written.
	 * 
	 * @param size the size of the ByteBuffer
	 * @return an allocated ByteBuffer
	 */
	ByteBuffer getContentBuffer(int size);
	
	/**
	 * Retrieves the resulting message object.
	 * 
	 * @return the Message object
	 */
	Message getMessage();
	
}
