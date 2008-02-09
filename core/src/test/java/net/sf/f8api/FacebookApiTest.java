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
package net.sf.f8api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import net.sf.f8api.Application;
import net.sf.f8api.FacebookApi;
import net.sf.f8api.FacebookException;
import net.sf.f8api.Session;
import net.sf.f8api.Url;
import net.sf.f8api.model.ErrorResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class FacebookApiTest extends TestCase {
	protected final Log log = LogFactory.getLog(getClass());
	public static final String CONFIG_FILE = "/facebook.properties";
	public static final String REPLACE_MESSAGE = "Please enter both your API key and secret in the "
			+ CONFIG_FILE
			+ " configuration file. Your API key and secret can be found at "
			+ "http://developers.facebook.com/account.php";

	private static int sec = 5;
	Properties props;
	Application app;
	CallbackServer callbackServer;
	String authToken;

	public FacebookApiTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		// load the test properties (should not be bundled in a JAR!)
		props = new Properties();
		InputStream in = this.getClass().getResourceAsStream(CONFIG_FILE);
		props.load(in);

		String api_key = props.getProperty("api_key");
		String secret = props.getProperty("secret");
		boolean isDesktop = Boolean.parseBoolean(props.getProperty("desktop"));

		assertNotNull(api_key);
		assertNotNull(secret);

		if ("set_your_api_key_here".equals(api_key)
				|| "set_your_secret_here".equals(secret)) {
			log.info(REPLACE_MESSAGE);
			assertFalse(true); // fail the test
		}

		app = new Application();
		app.setApiKey(api_key);
		app.setApiSecret(secret);
		app.setDesktop(isDesktop);
		// use hosts file to spoof any address for production testing!
		app.setCallbackUrl(props.getProperty("callback"));

		// for desktop login, start test server for callbacks
		if (!app.isDesktop() && app.getCallbackUrl() != null) {
			// start the callback server
			try {
				URL callbackUrl = new URL(app.getCallbackUrl());
				int port = callbackUrl.getPort() > 0 ? callbackUrl.getPort()
						: 80;
				callbackServer = new CallbackServer(port,
						new CallbackServlet(), callbackUrl.getPath());
				callbackServer.start();

				Thread.sleep(1000); // wait to startup
			} catch (Exception e) {
				log.fatal(e);
				assertNull(e);
			}
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		// try {
		// FacebookApi.shutdown();
		// } catch (Throwable t) {
		// t.printStackTrace();
		// }
		if (!app.isDesktop() && callbackServer != null) {
			// stop server
			callbackServer.stop();
			Thread.sleep(1000); // wait to startup
		}
	}

	public void testGetAuthToken() throws Exception {
		String authToken = FacebookApi.getAuthToken(app.getApiKey(), app
				.getApiSecret());
		assertNotNull(authToken);
	}

	public void testErrorResponse() throws Exception {
		log.info("testing invalid API key");
		// set invalid api key
		app.setApiKey("");

		FacebookException exeception = null;
		try {
			// this should throw an exception since api_key is empty
			FacebookApi.getAuthToken(app.getApiKey(), app.getApiSecret());
		} catch (FacebookException fe) {
			exeception = fe;
		}

		assertNotNull(exeception);

		ErrorResponse er = exeception.getErrorResponse();

		assertNotNull(er);

		log.info(er);
	}

	/**
	 * Login success is defined by having a valid session.
	 * 
	 * @throws Exception
	 */
	public void testLogin() throws Exception {
		getSession();
	}

	private Session getSession() throws Exception {
		authToken = FacebookApi.getAuthToken(app.getApiKey(), app
				.getApiSecret());

		assertNotNull(authToken);

		String loginUrl;
		if (app.isDesktop()) {
			loginUrl = Url.getDesktopLoginUrl(app.getApiKey(), authToken, true,
					false, false);
		} else {
			loginUrl = Url.getWebappLoginUrl(app.getApiKey(), null, false,
					false, false, false);
		}

		assertNotNull(loginUrl);

		log.info("login to the Facebook app to test the session process:\n"
				+ loginUrl);
		launchBrowser(loginUrl);

		log.info("Please login within " + sec + " seconds");
		Thread.sleep(sec * 1000); // in ms

		try {
			Session session = FacebookApi.getSession(app.getApiKey(), app
					.getApiSecret(), authToken, app.isDesktop());

			log.info("session: " + session);
			assertNotNull(session);
			return session;
		} catch (FacebookException fe) {
			log.fatal(fe.getErrorResponse());
			assertNull(fe);// will fail
		}
		return null;
	}

	/**
	 * This test exists so we don't have to continuously get a new session for
	 * each test.
	 * 
	 * @throws Exception
	 */
	public void testRequestsRequiringSessions() throws Exception {
		Session session = getSession();

		assertNotNull(session.getKey()); // check for valid session
		if (app.isDesktop()) {
			assertNotNull(session.getSecret());
			assertTrue(session.isDesktop());
		}

		getFriends(session);
		getFqlRequest(session, "SELECT name FROM user where uid = "
				+ session.getUserId());
		getFriendsAreFriends(session, Long.toString(session.getUserId())
				+ ",1237823", "1110445,1237823");
		getAppUsers(session);
		getProfileFBML(session, session.getUserId());
		getNotifications(session);

	}

	private void getNotifications(Session session) throws Exception {
		log.info("getNotifications");

		InputStream in = FacebookApi.getNotifications(session, null, null);

		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) != -1) {
			System.out.print(new String(buff, 0, len));
		}
		assertTrue(len == -1);
	}

	private void getProfileFBML(Session session, long userId) throws Exception {
		log.info("getProfileFBML");

		InputStream in;
		in = FacebookApi.getProfileFBML(session, userId, null, null);

		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) != -1) {
			System.out.print(new String(buff, 0, len));
		}
		assertTrue(len == -1);
	}

	private void getFriends(Session session) throws Exception {
		log.info("getFriends");

		InputStream in = FacebookApi.getFriends(session, null, null);

		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) != -1) {
			System.out.print(new String(buff, 0, len));
		}
		assertTrue(len == -1);
	}

	/**
	 * Session is set in previous test!
	 * 
	 * @throws Exception
	 */
	private void getFqlRequest(Session session, String fqlQuery)
			throws Exception {
		log.info("getFqlRequest");

		log.info("FQL: " + fqlQuery);

		InputStream in = FacebookApi.getFqlResultSet(session, fqlQuery, null,
				null);
		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) != -1) {
			System.out.print(new String(buff, 0, len));
		}
		assertTrue(len == -1);
	}

	private void getFriendsAreFriends(Session session, String uids1,
			String uids2) throws Exception {
		log.info("getFriendsAreFriends");

		InputStream in = FacebookApi.getFriendsAreFriends(session, uids1,
				uids2, null, null);
		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) != -1) {
			System.out.print(new String(buff, 0, len));
		}
		assertTrue(len == -1);
	}

	private void getAppUsers(Session session) throws Exception {
		log.info("getAppUsers");

		InputStream in = FacebookApi.getAppUsers(session, null, null);

		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) != -1) {
			System.out.print(new String(buff, 0, len));
		}
		assertTrue(len == -1);
	}

	private void getdAppUsers(Session session) throws Exception {
		log.info("getAppUsers");

		InputStream in = FacebookApi.getAppUsers(session, null, null);

		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) != -1) {
			System.out.print(new String(buff, 0, len));
		}
		assertTrue(len == -1);
	}

	private void launchBrowser(String url) throws IOException {
		try {
			BrowserLauncher launcher = new BrowserLauncher();
			launcher.openURLinBrowser(url);
		} catch (BrowserLaunchingInitializingException e) {
			log.warn(e.getMessage());
			throw new IOException(e.getMessage());
		} catch (UnsupportedOperatingSystemException e) {
			log.warn(e.getMessage());
			throw new IOException(e.getMessage());
		}

	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(FacebookApiTest.class);
	}

	/**
	 * This lets us test webapps on our local machine using Jetty for the
	 * callback url. Must set callback url on Facebook to
	 * http://127.0.0.1:8080/callback (or to match the facebook.callback
	 * parameter, or use HOSTS file.
	 * 
	 */
	private class CallbackServlet extends HttpServlet {
		public void init() throws ServletException {
		}

		/**
		 * Handle requests.
		 */
		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException,
				IOException {

			authToken = request.getParameter("auth_token");

			log.info("got local callback authToken: " + authToken);
			response.getWriter().write("Login Success!!!<br/>");
			response.getWriter().write(
					"got local callback authToken: " + authToken);
			response.flushBuffer();

		}
	}
}