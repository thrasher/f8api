/**
 * Copyright 2007 Jason Thrasher
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.f8api.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handle error messages.
 * 
 * <pre>
 * <code>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 * &lt;result method=&quot;&quot; type=&quot;struct&quot;&gt;
 *   &lt;fb_error type=&quot;struct&quot;&gt;
 *     &lt;code&gt;101&lt;/code&gt;
 *     &lt;msg&gt;Invalid API key&lt;/msg&gt;
 *     &lt;your_request/&gt;
 *   &lt;/fb_error&gt;
 *  &lt;/result&gt;
 * </code>
 * </pre>
 * 
 * @author <a href="mailto:jasonthrasher@gmail.com">Jason Thrasher</a>
 * 
 */
public class FacebookErrorHandler extends DefaultHandler {
	protected static Log log = LogFactory.getLog(AuthCreateTokenHandler.class);
	private static final String fb_error = "fb_error";
	private static final String type = "type";
	private static final String code = "code";
	private static final String msg = "msg";
	private static final String your_request = "your_request";

	private StringBuffer charContent = null; // text content of an element

	private String authToken; // the current carrier ID

	public FacebookErrorHandler() {
		super();
	}

	public void startDocument() {
		log.debug("Start document");
	}

	public void endDocument() {
		log.debug("End document");
	}

	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		log.debug("Start element: " + qName);
		if (qName.equals(fb_error)) {
			charContent = new StringBuffer();
		}
	}

	public void endElement(String uri, String name, String qName) {
		log.debug("End element: " + qName);
		if (qName.equals("auth_createToken_response")) {
			authToken = charContent.toString();
		}
	}

	public void characters(char ch[], int start, int length) {
		charContent.append(ch, start, length);
		log.debug("Characters: " + charContent);
	}
}