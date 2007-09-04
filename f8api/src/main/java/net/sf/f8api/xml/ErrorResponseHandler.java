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

import net.sf.f8api.model.ErrorResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


/**
 * <pre>
 * <code>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 *  &lt;error_response xmlns=&quot;http://api.facebook.com/1.0/&quot; xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xsi:schemaLocation=&quot;http://api.facebook.com/1.0/ http://api.facebook.com/1.0/facebook.xsd&quot;&gt;
 *  &lt;error_code&gt;101&lt;/error_code&gt;
 *  &lt;error_msg&gt;Invalid API key&lt;/error_msg&gt;
 *  &lt;request_args list=&quot;true&quot;&gt;
 *  &lt;arg&gt;
 *  &lt;key&gt;v&lt;/key&gt;
 *  &lt;value&gt;1.0&lt;/value&gt;
 *  &lt;/arg&gt;
 *  &lt;arg&gt;
 *  &lt;key&gt;method&lt;/key&gt;
 *  &lt;value&gt;facebook.auth.createToken&lt;/value&gt;
 *  &lt;/arg&gt;
 *  &lt;arg&gt;
 *  &lt;key&gt;api_key&lt;/key&gt;
 *  &lt;value&gt;a35034ff659e77d704e8b732c8238368q&lt;/value&gt;
 *  &lt;/arg&gt;
 *  &lt;arg&gt;
 *  &lt;key&gt;sig&lt;/key&gt;
 *  &lt;value&gt;b0cbb177a043dde4751d5884f403eea3&lt;/value&gt;
 *  &lt;/arg&gt;
 *  &lt;/request_args&gt;
 *  &lt;/error_response&gt;
 * 
 * </code>
 * </pre>
 * 
 * 
 * @author <a href="mailto:jasonthrasher@gmail.com">Jason Thrasher</a>
 */
public class ErrorResponseHandler extends DefaultHandler {
	protected static Log log = LogFactory.getLog(ErrorResponseHandler.class);

	public static final String error_response = "error_response";
	public static final String error_code = "error_code";
	public static final String error_msg = "error_msg";
	public static final String request_args = "request_args";
	public static final String arg = "arg";
	public static final String key = "key";
	public static final String value = "value";

	private StringBuffer charContent = null; // text content of an element

	private ErrorResponse errorResponse;
	private String keyS;
	private String valueS;

	public ErrorResponseHandler() {
		super();
	}

	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		if (qName.equals(error_response)) {
			errorResponse = new ErrorResponse();
			charContent = new StringBuffer();
		}
	}

	public void endElement(String uri, String name, String qName) {
		if (qName.equals(error_code)) {
			errorResponse.setCode(Integer.parseInt(charContent.toString().trim()));
		} else if (qName.equals(error_msg)) {
			errorResponse.setMessage(charContent.toString().trim());
		} else if (qName.equals(key)) {
			keyS = charContent.toString().trim();
		} else if (qName.equals(value)) {
			valueS = charContent.toString().trim();
		} else if (qName.equals(arg)) {
			errorResponse.addParam(keyS, valueS);
		}
		charContent = new StringBuffer();
	}

	public void characters(char ch[], int start, int length) {
		charContent.append(ch, start, length);
	}

	public ErrorResponse getErrorResponse() {
		return errorResponse;
	}
}