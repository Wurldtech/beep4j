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

/**
 * A SessionHandlerFactory creates SessionHandler objects.
 * It is used by Listener implementations to create a new
 * SessionHandler whenever a new session was initiated.
 * 
 * @author Simon Raess
 */
public interface SessionHandlerFactory {
	
	/**
	 * Creates a new SessionHandler object.
	 * 
	 * @return the new SessionHandler
	 */
	SessionHandler createSessionHandler();
	
}
