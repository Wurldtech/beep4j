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

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Interface exposing all necessary methods a Message must have.
 * 
 * @author Simon Raess
 */
public interface Message {
	
	/**
	 * Canonical name of the Content-Type header.
	 */
	String CONTENT_TYPE = "content-type";
	
	/**
	 * Canonical name of the Content-Transfer-Encoding header.
	 */
	String CONTENT_TRANSFER_ENCODING = "content-transfer-encoding";
	
	/**
	 * Value for binary transfer encoding.
	 */
	String BINARY_TRANSFER_ENCODING = "binary";
	
	/**
	 * Value for base64 transfer encoding.
	 */
	String BASE64_TRANSFER_ENCODING = "base64";
	
	/**
	 * Gets the content type of the message. The format of the
	 * content type is defined in RFC 2045. The returned
	 * value does not contain parameters.
	 * 
	 * @return the content type of the message
	 */
	String getContentType();
	
	/**
	 * Gets the list of all defined header names. The content type
	 * header is not returned from this method.
	 * 
	 * @return the list of header names
	 */
	Iterator<String> getHeaderNames();
	
	/**
	 * Gets the value of the header with the given name.
	 * If no such header exists, null is returned. The content type
	 * header is not returned from this method.
	 * 
	 * @param name the name of the header
	 * @return the value of the header
	 */
	String getHeader(String name);
	
	/**
	 * Gets an InputStream to read the content of the message.
	 * Use this method if you receive binary messages. Otherwise
	 * we recommend using the {@link #getReader()} method, which
	 * returns a fully configured Reader.
	 * 
	 * @return the raw InputStream
	 */
	InputStream getInputStream();
	
	/**
	 * Gets a Reader to read the content of the message.
	 * Use this method to process textual messages. The
	 * reader is fully configured, e.g. the correct charset is
	 * used to decode the binary content.
	 * 
	 * @return a fully configured Reader
	 */
	Reader getReader();
	
	/**
	 * Gets a Reader to read the content of the message.
	 * Use this method if the charset has not been specified as
	 * part of the content-type and there is no default charset
	 * for the content type.
	 * 
	 * @param charset the name of the charset
	 * @return the fully configured Reader
	 */
	Reader getReader(String charset);
	
	/**
	 * Gets the content as a ByteBuffer.
	 * 
	 * @return the ByteBuffer of the content
	 */
	ByteBuffer getContentBuffer();
	
	/**
	 * Writes the complete message (headers and content) into a
	 * ByteBuffer, which can then be used to send the message over
	 * the network.
	 * 
	 * @return the Message written into a ByteBuffer
	 */
	ByteBuffer asByteBuffer();
			
}
