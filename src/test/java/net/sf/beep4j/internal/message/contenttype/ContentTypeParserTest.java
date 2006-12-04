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
package net.sf.beep4j.internal.message.contenttype;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class ContentTypeParserTest extends TestCase {
	
	public void testContentType() throws Exception {
		test("one/two", "one", "two", new HashMap<String,String>());
	}
	
    public void testContentTypeWithParameter() throws ParseException {
        test("one/two; three          =  four", 
        		"one", "two", 
        		Collections.singletonMap("three", "four"));
    }
    
    public void testContentTypeWithQuotedParameter() throws Exception {
        test("one/(foo)two; three          =  \"four\"", 
        		"one", "two", 
        		Collections.singletonMap("three", "four"));
    }
    
    public void testContentTypeWithComments() throws Exception {
        test("one(foo)/two; three          =  (foo) four", 
        		"one", "two", 
        		Collections.singletonMap("three", "four"));
	}

    private void test(String val, 
    		String expectedType, String expectedSubtype, 
    		Map<String,String> parameters) throws ParseException {
    	
        ContentTypeParser parser = new ContentTypeParser(new StringReader(val));
        parser.parseAll();

        String type = parser.getType();
        String subtype = parser.getSubType();

        assertEquals(parameters, parser.getParameters());
        
        assertEquals(expectedType, type);
        assertEquals(expectedSubtype, subtype);
    }

}
