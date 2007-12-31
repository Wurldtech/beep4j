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
package net.sf.beep4j.internal.management;

/**
 * Constants of all Strings used by the channel management profile
 * messages.
 * 
 * @author Simon Raess
 */
interface XMLConstants {
	
	String E_GREETING = "greeting";
	
	String E_ERROR = "error";
	
	String E_OK = "ok";
	
	String E_CLOSE = "close";
	
	String E_START = "start";
	
	String E_PROFILE = "profile";
	
	String ENCODING_BASE64 = "base64";
	
	String A_CODE = "code";
	
	String A_ENCODING = "encoding";
	
	String A_FEATURES = "features";
	
	String A_LOCALIZE = "localize";
	
	String A_NUMBER = "number";
	
	String A_URI = "uri";

}
