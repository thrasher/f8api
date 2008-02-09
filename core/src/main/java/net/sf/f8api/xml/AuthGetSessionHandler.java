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

import java.io.Serializable;
import java.util.Date;

import net.sf.f8api.FacebookException;
import net.sf.f8api.Session;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


/**
 * <pre>
 * <code>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 *  &lt;auth_getSession_response xmlns=&quot;http://api.facebook.com/1.0/&quot; xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xsi:schemaLocation=&quot;http://api.facebook.com/1.0/ http://api.facebook.com/1.0/facebook.xsd&quot;&gt;
 *  &lt;session_key&gt;5f34e11bfb97c762e439e6a5-8055&lt;/session_key&gt;
 *  &lt;uid&gt;8055&lt;/uid&gt;
 *  &lt;expires&gt;1173309298&lt;/expires&gt;
 *  &lt;/auth_getSession_response&gt;
 * 
 * </code>
 * </pre>
 * 
 * @author <a href="mailto:jasonthrasher@gmail.com">Jason Thrasher</a>
 * 
 */
public class AuthGetSessionHandler extends DefaultHandler {
	protected static Log log = LogFactory.getLog(AuthGetSessionHandler.class);
	public static final String auth_getSession_response = "auth_getSession_response";
	public static final String session_key = "session_key";
	public static final String uid = "uid";
	public static final String expires = "expires";
	public static final String secret = "secret";
	private final String apiKey;
	private final String apiSecret;

	private StringBuffer charContent = null; // text content of an element

	private SessionImpl session;

	private ErrorResponseHandler errorHandler = null;

	public AuthGetSessionHandler(final String apiKey, final String apiSecret) {
		super();
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	public void startElement(String uri, String name, String qName,
			Attributes atts) {

		if (errorHandler != null) {
			errorHandler.startElement(uri, name, qName, atts);
			return;
		} else if (qName.equals(ErrorResponseHandler.error_response)) {
			log.warn("facebook error response");
			errorHandler = new ErrorResponseHandler();
			errorHandler.startElement(uri, name, qName, atts);
		}

		if (qName.equals(auth_getSession_response)) {
			session = new SessionImpl();
			session.setApiKey(apiKey);
			session.setDesktop(false); // web application assumed
			session.setSecret(apiSecret); // web application assumed
		}
		charContent = new StringBuffer();
	}

	public void endElement(String uri, String name, String qName) {
		if (errorHandler != null) {
			errorHandler.endElement(uri, name, qName);
			return;
		}

		if (qName.equals(session_key)) {
			session.setKey(charContent.toString().trim());
		} else if (qName.equals(uid)) {
			session.setUserId(Long.parseLong(charContent.toString().trim()));
		} else if (qName.equals(expires)) {
			Date expires = new Date();
			expires.setTime(Long.parseLong(charContent.toString().trim()));
			session.setExpires(expires);
		} else if (qName.equals(secret)) {
			// this must be a desktop application
			session.setDesktop(true); // implied if secret is returned
			session.setSecret(charContent.toString().trim()); // override
		}
	}

	public void characters(char ch[], int start, int length) {
		if (errorHandler != null) {
			errorHandler.characters(ch, start, length);
			return;
		}

		charContent.append(ch, start, length);
	}

	public Session getSessionResponse() throws FacebookException {
		if (errorHandler != null) {
			throw new FacebookException(errorHandler.getErrorResponse());
		}
		return session;
	}

	/**
	 * A session implementation that knows how to print itself.
	 */
	protected class SessionImpl implements Session, Serializable {
		private static final long serialVersionUID = 1L;

		private String apiKey;// identifies the application
		private String secret; // only gets set for desktop sessions
		private boolean desktop = false; // true for desktop apps

		private String key; // uniquely identifies this session
		private long userId; // user that owns this session
		private Date expires; // getTime() == zero for infinite session

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		public boolean isDesktop() {
			return desktop;
		}

		public void setDesktop(boolean desktop) {
			this.desktop = desktop;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public long getUserId() {
			return userId;
		}

		public void setUserId(long userId) {
			this.userId = userId;
		}

		public Date getExpires() {
			return expires;
		}

		public void setExpires(Date expires) {
			this.expires = expires;
		}

		public boolean isInfinite() {
			return expires.getTime() == 0;
		}

		public boolean isExpired() {
			return isInfinite() ? false : expires.before(new Date());
		}

		public String toString() {
			return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
					.append("key", this.key).append("secret", secret).append(
							"userId", this.userId).append("expires", expires)
					.append("isInfinite", isInfinite()).toString();
		}

	}

}