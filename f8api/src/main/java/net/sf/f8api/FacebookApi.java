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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.f8api.xml.AuthCreateTokenHandler;
import net.sf.f8api.xml.AuthGetSessionHandler;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Stateless communication interface.
 * 
 * @author <a href="mailto:jasonthrasher@gmail.com">Jason Thrasher</a>
 */
public class FacebookApi {
	protected static final Log log = LogFactory.getLog(FacebookApi.class);
	protected static final String ENCODING = "UTF-8";

	public static final String API_VERSION = "1.0";
	public static int NUM_AUTOAPPENDED_PARAMS = 5;
	public static final String FORMAT_XML = "XML";
	public static final String FORMAT_JSON = "JSON";

	// public static final String ERROR_TAG = "error_response";
	protected static final String FB_SERVER = "api.facebook.com/restserver.php";
	protected static final String HTTP_SERVER_ADDR = "http://" + FB_SERVER;
	protected static final String HTTPS_SERVER_ADDR = "https://" + FB_SERVER;

	protected static final int BUFF_SIZE = 1024 * 8; // for network I/O

	protected static HttpClient httpClient;
	protected static MultiThreadedHttpConnectionManager connectionManager;

	static {
		connectionManager = new MultiThreadedHttpConnectionManager();
		httpClient = new HttpClient(connectionManager);
	}

	private FacebookApi() {
	}

	/**
	 * (Intended for desktop applications only.) Creates an auth_token to be
	 * passed in as a parameter to login.php and then to
	 * facebook.auth.getSession after the user has logged in. The user must log
	 * in soon after you create this token. See the authentication guide for
	 * more information.
	 * 
	 * @param application
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static String getAuthToken(String apiKey, String apiSecret)
			throws IOException, FacebookException {
		InputStream in = null;
		try {
			in = callMethod(apiKey, null, apiSecret, true,
					FacebookMethod.AUTH_CREATE_TOKEN, null,
					new ArrayList<Pair<String, CharSequence>>());

			XMLReader xr = XMLReaderFactory.createXMLReader();
			AuthCreateTokenHandler handler = new AuthCreateTokenHandler();
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);
			xr.parse(new InputSource(in));
			return handler.getAuthToken();

		} catch (SAXException saxe) {
			throw new IOException(saxe.getMessage());
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Get a session for the application's authentication token. If successful
	 * the application will be updated with session information for use in
	 * future calls.
	 * 
	 * If this is a desktop app, then steps to get a session are: <code>
	 * 1) call getAuthToken, to get a valid token
	 * 2) call Url.getDesktopLoginUrl(application.getApiKey(), authToken, ...), to generate a login URL
	 * 3) present the login URL to the user (as popup?), the user should login
	 * 4) user indicates to the app that he's logged in
	 * 5) desktop app calls getSession, using the auth_token from step 1)
	 * 6) application is updated with a live-session
	 * </code>
	 * 
	 * If this is a webapp, then steps to get session are: <code>
	 * 1) call Url.getWebappLoginUrl(application.getApiKey(), ...), to generate a login URL
	 * 2) present the login URL to the user (as redirect?), the user should login
	 * 3) if Facebook webapp uses a callback url, the user will be redirected there by facebook
	 * 4) the auth_token will be presented to the application's url (by redirect to callback or by proxy via canvas page)
	 * 5) desktop app calls getSession, using the auth_token from step 4)
	 * 6) application is updated with a live-session
	 * </code>
	 * 
	 * If this is a web app, use the token in the GET request from the facebook
	 * redirect after the user logs in. This will be a GET request from the
	 * user's web browser to our website similar to: <code>
	 * http://www.mycompany.com/some/path/callback.html?auth_token=76fa46c30a679e324e21a0b487527b40
	 * </code>
	 * 
	 * Note: this makes it hard to test api_key/secret values for a webapp using
	 * JUnit because the browser will redirect to a live url, not our test url.
	 * However this is possible to test using Jetty and by modifying the hosts
	 * file to point the live webapp url to localhost for testing.
	 * 
	 * @param apiKey
	 *            for our application
	 * @param authToken
	 *            for desktop apps the token is created by calling
	 *            getAuthToken(apiKey, apiSecret), for webapps it is posted to
	 *            our service from Facebook after the user logs in
	 * @param isDesktop
	 *            true if we are using a desktop app, false for webapps
	 * @return an active session
	 * @throws IOException
	 *             if there's a communication error
	 * @throws FacebookException
	 *             if Facebook doesn't generate the session
	 */
	public static Session getSession(String apiKey, String apiSecret,
			String authToken, boolean isDesktop) throws IOException,
			FacebookException {
		InputStream in = null;
		try {
			in = callMethod(apiKey, null, apiSecret, isDesktop,
					FacebookMethod.AUTH_GET_SESSION, null, Arrays
							.asList(new Pair<String, CharSequence>(
									"auth_token", authToken)));

			XMLReader xr = XMLReaderFactory.createXMLReader();
			AuthGetSessionHandler handler = new AuthGetSessionHandler(apiKey,
					apiSecret);
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);
			xr.parse(new InputSource(in));

			Session session = handler.getSessionResponse();

			return session;

		} catch (SAXException saxe) {
			throw new IOException(saxe.getMessage());
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Returns the identifiers of the current user's Facebook friends. The
	 * current user is determined from the session_key parameter. The values
	 * returned from this call are not storable.
	 * 
	 * Response Privacy Note : The friend ids returned are those friends visible
	 * to the Facebook Platform. If no friends are found, the method will return
	 * an empty friends_get_response element.
	 * 
	 * FQL Equivalent: This function is similar to doing the following FQL
	 * query, with the appropriate parameters filled in:<code>
	 * SELECT uid2 FROM friend WHERE uid1=loggedInUid
	 * </code>
	 * 
	 * @param application
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getFriends(Session session, String format,
			String callback) throws IOException, FacebookException {
		// optional params
		List<Pair<String, CharSequence>> params = getParams(format, callback);

		return callMethod(session, FacebookMethod.FRIENDS_GET, null, params);
	}

	/**
	 * Returns the identifiers of the current user's Facebook friends who are
	 * signed up for the specific calling application. The current user is
	 * determined from the session_key parameter. The values returned from this
	 * call are not storable.
	 * 
	 * @param application
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getAppUsers(Session session, String format,
			String callback) throws IOException, FacebookException {
		// optional params
		List<Pair<String, CharSequence>> params = getParams(format, callback);

		return callMethod(session, FacebookMethod.FRIENDS_GET_APP_USERS, null,
				params);
	}

	/**
	 * Returns information on outstanding Facebook notifications for current
	 * session user.
	 * 
	 * @param application
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getNotifications(Session session, String format,
			String callback) throws IOException, FacebookException {

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		return callMethod(session, FacebookMethod.NOTIFICATIONS_GET, null,
				params);
	}

	/**
	 * Send a notification to a set of users. You can send emails to users that
	 * have added the application without any confirmation, or you can direct a
	 * user of your application to the URL returned by this function to email
	 * users who have not yet added your application. You can also send messages
	 * to the user's notification page without needing any confirmation.
	 * 
	 * The notification and email parameters are a very stripped-down set of
	 * FBML which allows only tags that result in just text and links, and in
	 * the email, linebreaks. There is one additional tag supported within the
	 * email parameter - use fb:notif-subject around the subject of the email.
	 * 
	 * @param application
	 * @param toUserIds
	 *            These must be friends of the logged-in user or people who have
	 *            added your application.
	 * @param notificationFbml
	 *            FBML for the notifications page.
	 * @param emailFbml
	 *            Optional FBML for the email. If not passed, no email will be
	 *            sent.
	 * @return If a URL is returned, redirect the user to that URL to confirm
	 *         sending of the notification. If no URL is returned, either an
	 *         error occurred or the message went through without requiring
	 *         confirmation. We will eventually support some additional error
	 *         codes for the error case.
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream sendNotification(Session session,
			String toUserIds[], String notificationFbml, String emailFbml,
			String format, String callback) throws IOException,
			FacebookException {
		if (toUserIds == null || toUserIds.length == 0) {
			log.warn("array of user Id values is required");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"array of user Id values is required");
		}
		if (notificationFbml == null) {
			log.warn("notificationFbml is required");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"notificationFbml is required");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		params.add(new Pair<String, CharSequence>("to_ids",
				getCommaDelimited(toUserIds)));
		params.add(new Pair<String, CharSequence>("notification",
				notificationFbml));

		// optional
		if (emailFbml != null) {
			params.add(new Pair<String, CharSequence>("email", emailFbml));
		}

		return callMethod(session, FacebookMethod.NOTIFICATIONS_SEND, null,
				params);
	}

	/**
	 * Send a request or invitation to a set of users. You can send requests to
	 * users that have added the application without any confirmation, or you
	 * can direct a user of your application to the URL returned by this
	 * function to send requests to users who have not yet added your
	 * application.
	 * 
	 * @param application
	 * @param toUserIds
	 *            These must be friends of the logged-in user or people who have
	 *            added your application.
	 * @param type
	 *            The type of request/invitation - e.g. the word "event" in "1
	 *            event invitation."
	 * @param content
	 *            Content of the request/invitation. This should be FBML
	 *            containing only links and the special tag <fb:req-choice
	 *            url="" label="" /> to specify the buttons to be included in
	 *            the request.
	 * @param image
	 *            URL of an image to show beside the request. It will be resized
	 *            to be 100 pixels wide.
	 * @param isInvite
	 *            Whether to call this an "invitation" or a "request".
	 * @param format
	 * @param callback
	 * @return If a URL is returned, redirect the user to that URL to confirm
	 *         sending of the request. If no URL is returned, either an error
	 *         occurred or the request went through without requiring
	 *         confirmation. We will eventually support some additional error
	 *         codes for the error case.
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream sendNotificationRequest(Session session,
			String[] toUserIds, String type, String content, String image,
			boolean isInvite, String format, String callback)
			throws IOException, FacebookException {

		if (toUserIds == null || toUserIds.length == 0 || type == null
				|| content == null || image == null) {
			log.warn("missing arguments");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"missing arguments");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		params.add(new Pair<String, CharSequence>("to_ids",
				getCommaDelimited(toUserIds)));
		params.add(new Pair<String, CharSequence>("type", type));
		params.add(new Pair<String, CharSequence>("content", content));
		params.add(new Pair<String, CharSequence>("image", image));
		params.add(new Pair<String, CharSequence>("invite", Boolean
				.toString(isInvite)));

		return callMethod(session, FacebookMethod.NOTIFICATIONS_SEND_REQUEST,
				null, params);
	}

	/**
	 * Returns a wide array of user-specific information for each user
	 * identifier passed, limited by the view of the current user. The current
	 * user is determined from the session_key parameter. The only storable
	 * values returned from this call are the those under the affiliations
	 * element, the notes_count value, and the contents of the
	 * profile_update_time element.
	 * 
	 * @param application
	 * @param uids
	 *            List of user ids. This is a comma-separated list of user ids.
	 * @param fields
	 *            List of desired fields in return. This is a comma-separated
	 *            list of field strings.
	 * @param format
	 * @param callback
	 * @return The user info elements returned are those friends visible to the
	 *         Facebook Platform. If no visible users are found from the passed
	 *         uids argument, the method will return an empty result element.
	 * @throws IOException
	 * @throws FacebookException
	 * @see <a
	 *      href="http://developers.facebook.com/documentation.php?v=1.0&method=users.getInfo">docs</a>
	 */
	public static InputStream getUsersInfo(Session session, String[] userIds,
			String[] fields, String format, String callback)
			throws IOException, FacebookException {
		// validate
		if (userIds == null || userIds.length == 0 || fields == null
				|| fields.length == 0) {
			log.warn("missing arguments");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"missing arguments");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);
		params.add(new Pair<String, CharSequence>("uids",
				getCommaDelimited(userIds)));
		params.add(new Pair<String, CharSequence>("fields",
				getCommaDelimited(fields)));

		return callMethod(session, FacebookMethod.USERS_GET_INFO, null, params);
	}

	/**
	 * Gets the user id (uid) associated with the current sesssion. This value
	 * should be stored for the duration of the session, to avoid unnecessary
	 * subsequent calls to this method. The same value is also returned by
	 * facebook.auth.getSession.
	 * 
	 * @param application
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getLoggedInUser(Session session, String format,
			String callback) throws IOException, FacebookException {

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		return callMethod(session, FacebookMethod.USERS_GET_LOGGED_IN_USER,
				null, params);
	}

	/**
	 * Returns all visible events according to the filters specified. This may
	 * be used to find all events of a user, or to query specific eids.
	 * 
	 * This method returns all events satisfying the filters specified. The
	 * method can be used to return all events associated with user, or query a
	 * specific set of events by a list of eids.
	 * 
	 * If both the uid and eids parameters are provided, the method returns all
	 * events in the set of eids, with which the user is associated. If the eids
	 * parameter is omitted, the method returns all events associated with the
	 * provided user.
	 * 
	 * If, instead, the uid parameter is omitted, the method returns all events
	 * associted with the provided eids, regardless of any user relationship.
	 * 
	 * If both parameters are omitted, the method returns all events associated
	 * with the session user. start_time and end_time parameters specify a
	 * (possibly open-ended) window which all events returned will overlap.
	 * 
	 * Note that if start_time is greater than or equal to end_time, an empty
	 * top-level element is returned.
	 * 
	 * The RSVP status should be one of the following strings:
	 * 
	 * <pre><code>
	 * attending
	 * unsure
	 * declined
	 * not_replied 
	 * </code></pre>
	 * 
	 * Privacy note: Event creators will be visible to an application only if
	 * the creator has not turned off access to the Platform or used the
	 * application'; If the creator has opted out , the creator element will
	 * appear in the following format:
	 * 
	 * <pre><code>
	 * &lt;creator xsi:nil=&quot;true&quot;/&gt;
	 * </code></pre>
	 * 
	 * @param application
	 * @param userId
	 *            optional filter
	 * @param eventIds
	 *            optional filter
	 * @param startTime
	 *            optional filter
	 * @param endTime
	 *            optional filter
	 * @param rsvpStatusCode
	 *            optional filter
	 * @param format
	 * @param callback
	 * @return Events are only visible if they are not secret. If no such events
	 *         are found, the method will return an empty events_get_response
	 *         element. The nid field will be 0 for global events.
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getEvents(Session session, String userId,
			String[] eventIds, Date startTime, Date endTime,
			String rsvpStatusCode, String format, String callback)
			throws IOException, FacebookException {

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		if (userId != null)
			params.add(new Pair<String, CharSequence>("uid", userId));

		if (eventIds != null && eventIds.length != 0)
			params.add(new Pair<String, CharSequence>("eids",
					getCommaDelimited(eventIds)));

		if (startTime != null)
			params.add(new Pair<String, CharSequence>("start_time", Long
					.toString(startTime.getTime())));

		if (endTime != null)
			params.add(new Pair<String, CharSequence>("end_time", Long
					.toString(endTime.getTime())));

		if (rsvpStatusCode != null)
			params.add(new Pair<String, CharSequence>("rsvp_status",
					rsvpStatusCode));

		return callMethod(session, FacebookMethod.USERS_GET_LOGGED_IN_USER,
				null, params);
	}

	/**
	 * Returns membership list data associated with an event.
	 * 
	 * Privacy note: The lists can contain uids of users not using the calling
	 * application.
	 * 
	 * @param application
	 * @param eventId
	 *            Event id
	 * @param format
	 * @param callback
	 * @return This method returns four (possibly empty) lists of users
	 *         associated with an event, keyed on their associations. These
	 *         lists should never share any members.
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getEventMembers(Session session, String eventId,
			String format, String callback) throws IOException,
			FacebookException {
		if (eventId == null) {
			log.warn("missing eventId argument");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"missing eventId argument");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		params.add(new Pair<String, CharSequence>("eid", eventId));

		return callMethod(session, FacebookMethod.EVENTS_GET_MEMBERS, null,
				params);
	}

	/**
	 * Returns all visible groups according to the filters specified. This may
	 * be used to find all groups of which a user is as member, or to query
	 * specific gids.
	 * 
	 * This method returns all groups satisfying the filters specified. The
	 * method can be used to return all groups associated with user, or query a
	 * specific set of events by a list of gids.
	 * 
	 * If both the uid and gids parameters are provided, the method returns all
	 * groups in the set of gids, with which the user is associated. If the gids
	 * parameter is omitted, the method returns all groups associated with the
	 * provided user.
	 * 
	 * If, instead, the uid parameter is omitted, the method returns all groups
	 * associted with the provided gids, regardless of any user relationship.
	 * 
	 * If both parameters are omitted, the method returns all groups of the
	 * session user.
	 * 
	 * Privacy note: Group creators will be visible to an application only if
	 * the creator has not turned off access to the Platform or used the
	 * application'; If the creator has opted out , the creator element will
	 * appear in the following format:
	 * 
	 * <pre><code>
	 * &lt;creator xsi:nil=&quot;true&quot;/&gt;
	 * </code></pre>
	 * 
	 * @param application
	 * @param userId
	 *            Optional - Filter by groups associated with a user with this
	 *            uid
	 * @param groupIds
	 *            Optional - Filter by this list of group ids. This is a
	 *            comma-separated list of gids.
	 * @param format
	 * @param callback
	 * @return Groups are only visible if they are not secret. If no such groups
	 *         are found, the method will return an empty groups_get_response
	 *         element. The nid field will be 0 for global groups.
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getGroups(Session session, String userId,
			String[] groupIds, String format, String callback)
			throws IOException, FacebookException {

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		if (userId != null)
			params.add(new Pair<String, CharSequence>("uid", userId));

		if (groupIds != null && groupIds.length != 0)
			params.add(new Pair<String, CharSequence>("gids",
					getCommaDelimited(groupIds)));

		return callMethod(session, FacebookMethod.GROUPS_GET, null, params);
	}

	/**
	 * Returns membership list data associated with a group.
	 * 
	 * Privacy note: The lists can contain uids of users not using the calling
	 * application.
	 * 
	 * @param application
	 * @param groupId
	 *            Group id
	 * @param format
	 * @param callback
	 * @return This method returns four (possibly empty) lists of users
	 *         associated with a group, keyed on their associations. The members
	 *         list will contain the officers and admins lists, but will not
	 *         overlap with the not_replied list.
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getGroupMembers(Session session, String groupId,
			String format, String callback) throws IOException,
			FacebookException {

		List<Pair<String, CharSequence>> params = getParams(format, callback);
		params.add(new Pair<String, CharSequence>("gid", groupId));

		return callMethod(session, FacebookMethod.GROUPS_GET_MEMBERS, null,
				params);
	}

	public static InputStream getFriendsAreFriends(Session session,
			String[] uids1, String[] uids2, String format, String callback)
			throws IOException, FacebookException {

		if (uids1.length != uids2.length)
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"Invalid parameter, UID arrays must be of equal length");

		return getFriendsAreFriends(session, getCommaDelimited(uids1),
				getCommaDelimited(uids2), format, callback);
	}

	/**
	 * Returns whether or not each pair of specified users is friends with each
	 * other. The first array specifies one half of each pair, the second array
	 * the other half; therefore, they must be of equal size.
	 * 
	 * FQL Equivalent This function is similar to doing the following FQL query,
	 * with the appropriate parameters filled in: <code>
	 * SELECT uid1, uid2 FROM friend WHERE uid1=uid1 AND uid2=uid2
	 * </code>
	 * 
	 * @param application
	 * @param uids1
	 *            a comma seperated list of UIDs
	 * @param uids2
	 *            a comma seperated list of UIDs
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getFriendsAreFriends(Session session,
			String uids1, String uids2, String format, String callback)
			throws IOException, FacebookException {

		// check the input parameters
		StringTokenizer uid1Tokens = new StringTokenizer(uids1, ",");
		StringTokenizer uid2Tokens = new StringTokenizer(uids2, ",");
		if (uid1Tokens.countTokens() != uid2Tokens.countTokens())
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"Invalid parameter, UID arrays must be of equal length");

		// optional params
		List<Pair<String, CharSequence>> params = getParams(format, callback);
		params.add(new Pair<String, CharSequence>("uids1", uids1));
		params.add(new Pair<String, CharSequence>("uids2", uids2));

		return callMethod(session, FacebookMethod.FRIENDS_ARE_FRIENDS, null,
				params);
	}

	/**
	 * utility method to comma delimit a string array
	 * 
	 * @param values
	 * @return
	 */
	private static String getCommaDelimited(String[] values) {
		StringBuffer delimited = new StringBuffer(values.length * 16);
		boolean first = true;
		for (int i = 0; i < values.length; i++) {
			if (!first) {
				delimited.append(",");
				first = false;
			}
			delimited.append(values[i]);
		}
		return delimited.toString();

	}

	/**
	 * Gets the FBML that is currently set for a user's profile. See the FBML
	 * documentation for a description of the markup and its role in various
	 * contexts.
	 * 
	 * @param application
	 * @param userId
	 *            Optional, the user whose profile FBML is to be fetched. Not
	 *            allowed for desktop applications (since the application secret
	 *            is essentially public). Use "-1" to exclude, or for desktop
	 *            apps.
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getProfileFBML(Session session, long userId,
			String format, String callback) throws IOException,
			FacebookException {

		// help the caller out, give peace a chance
		if (session.isDesktop() && userId != -1) {
			log
					.warn("desktop apps must use -1 for the PROFILE_GET_FBML userId parameter");
			userId = -1;
		}

		// optional params
		List<Pair<String, CharSequence>> params = getParams(format, callback);

		// desktop apps don't use the "uid" parameter
		if (userId != -1) {
			params.add(new Pair<String, CharSequence>("uid", Long
					.toString(userId)));
		}

		return callMethod(session, FacebookMethod.PROFILE_GET_FBML, null,
				params);
	}

	/**
	 * Publishes a News Feed story to the user corresponding to the session_key
	 * 
	 * parameter. The function returns 1 on success, 0 on permissions error, or
	 * otherwise an error response.Publishing to News Feed requires
	 * understanding rules of its operation.
	 * 
	 * Applications are limited to calling this function once every 12 hours for
	 * each user. The story may or may not show up in the user's News Feed,
	 * depending on the number and quality of competing stories.
	 * 
	 * Developer Note: If an app developer calls feed.publishStoryToUser for his
	 * own user id, the story is always published. This is to allow for testing
	 * and display tweaks.
	 * 
	 * @param application
	 *            with a valid session representing the app user
	 * @param titleFbml
	 *            The title is required, and is limited to 60 displayed
	 *            characters (excluding tags). The a tag is allowed, and there
	 *            can be zero or one instance in the title. No other tags are
	 *            allowed.
	 * @param bodyFbml
	 *            optional, limited to 200 displayed characters (excluding
	 *            tags), and can include the tags a, b, and i.
	 * @param imageUrlLinkMap
	 *            optional, Up to 4 images can be displayed, which will be
	 *            shrunk to fit within 75x75, cached, and formatted by Facebook.
	 *            Images can either be a URL, or a facebook PID. If it is a URL,
	 *            you must own the image and grant Facebook the permission to
	 *            cache it. Each image must have a link associated with it,
	 *            which must start with http://
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream feedPublishStoryToUser(Session session,
			String titleFbml, String bodyFbml,
			Map<String, String> imageUrlLinkMap, String format, String callback)
			throws IOException, FacebookException {

		List<Pair<String, CharSequence>> params = getFeedParams(titleFbml,
				bodyFbml, imageUrlLinkMap, format, callback);

		return callMethod(session, FacebookMethod.FEED_PUBLISH_STORY_TO_USER,
				null, params);
	}

	/**
	 * Publishes a Mini-Feed story to the user corresponding to the session_key
	 * parameter, and publishes News Feed stories to the friends of that user.
	 * 
	 * Applications are limited to calling this function ten (10) times for each
	 * user in a rolling 48-hour window.
	 * 
	 * The story may or may not show up in the user's friends' News Feeds,
	 * depending on the number and quality of competing stories.
	 * 
	 * Developer Note: Unlike feed_publishStoryToUser, there is no unlimited
	 * rule for feed_publishActionOfUser, since this method affects all Facebook
	 * friends of the developer.
	 * 
	 * 
	 * @param application
	 *            with a valid session representing the app user
	 * @param titleFbml
	 *            The title is required, and is limited to 60 displayed
	 *            characters (excluding tags). The a tag is allowed, and there
	 *            can be zero or one instance in the title. One fb:userlink tag
	 *            is allowed, and the uid parameter must be populated with the
	 *            user id on whose behalf the action is being published. If
	 *            there is no such fb:userlink tag found, then one is
	 *            automatically prepended to the title. The fb:name tag is
	 *            allowed, and there may be multiple instances of this tag. No
	 *            other tags are allowed.
	 * 
	 * @param bodyFbml
	 *            The body is optional, is limited to 200 displayed characters
	 *            (excluding tags), and can include the tags fb:userlink,
	 *            fb:name, a, b, and i.
	 * 
	 * @param imageUrlLinkMap
	 *            Up to 4 images can be displayed, which will be shrunk to fit
	 *            within 75x75, cached, and formatted by Facebook. Images can
	 *            either be a URL, or a facebook PID. If it is a URL, you must
	 *            own the image and grant Facebook the permission to cache it.
	 *            Each image must have a link associated with it, which must
	 *            start with http://
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream feedPublishActionOfUser(Session session,
			String titleFbml, String bodyFbml,
			Map<String, String> imageUrlLinkMap, String format, String callback)
			throws IOException, FacebookException {

		List<Pair<String, CharSequence>> params = getFeedParams(titleFbml,
				bodyFbml, imageUrlLinkMap, format, callback);

		// get the response
		return callMethod(session, FacebookMethod.FEED_PUBLISH_ACTION_OF_USER,
				null, params);
	}

	/**
	 * builds paramter sets for feed publishing using feedPublishActionOfUser
	 * and feedPublishStoryToUser
	 * 
	 * @param titleFbml
	 * @param bodyFbml
	 * @param imageUrlLinkMap
	 * @param format
	 * @param callback
	 * @return
	 * @throws FacebookException
	 */
	private static List<Pair<String, CharSequence>> getFeedParams(
			String titleFbml, String bodyFbml,
			Map<String, String> imageUrlLinkMap, String format, String callback)
			throws FacebookException {
		// validation
		if (titleFbml == null) {
			log.warn("title parameter is required");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"title is a required parameter");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		// required parameter
		params.add(new Pair<String, CharSequence>("title", titleFbml));

		// optional body
		if (bodyFbml != null) {
			params.add(new Pair<String, CharSequence>("body", bodyFbml));
		}

		// optional, add up to 4 images to the request
		if (imageUrlLinkMap != null && imageUrlLinkMap.size() > 0) {
			// validation
			if (imageUrlLinkMap.size() > 4) {
				log.warn("only the first 4 images will be published");
			}

			int count = 1;
			Iterator<String> imageIter = imageUrlLinkMap.keySet().iterator();
			while (imageIter.hasNext() && count < 5) {
				String imageUrl = imageIter.next();
				params.add(new Pair<String, CharSequence>("image_" + count,
						imageUrl));
				params.add(new Pair<String, CharSequence>("image_" + count
						+ "_link", imageUrlLinkMap.get(imageUrl)));
				count++;
			}
		}

		return params;
	}

	/**
	 * Be sure to close the result or you'll leak connections.
	 * 
	 * @see <a
	 *      href="http://developers.facebook.com/documentation.php?v=1.0&doc=fql">FQL
	 *      Reference</a>
	 * @param application
	 * @param query
	 * @param format
	 *            optional, XML or JSON
	 * @param callback
	 *            optional, for XSS
	 * @return result set, (be sure to close the stream or you'll leak
	 *         connections)
	 * @throws IOException
	 * @throws FacebookException
	 */
	public static InputStream getFqlResultSet(Session session, String fqlQuery,
			String format, String callback) throws IOException,
			FacebookException {

		// this request requires an application session
		if (session.getKey() == null) {
			throw new FacebookException(ErrorCodes.FB_SESSION_TIMEOUT,
					"Empty Session Key");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);
		params.add(new Pair<String, CharSequence>("query", fqlQuery));

		// do it!
		return callMethod(session, FacebookMethod.FQL_QUERY, null, params);
	}

	/**
	 * Returns metadata about all of the photo albums uploaded by the specified
	 * user. The values returned from this call are not storable.
	 * 
	 * This method returns all visible photos satisfying the filters specified.
	 * The method can be used to return all photo albums created by a user,
	 * query a specific set of albums by a list of aids, or filter on any
	 * combination of these two.
	 * 
	 * It is an error to omit both of the uid and aids parameters. They have no
	 * defaults.
	 * 
	 * @param application
	 * @param userId
	 *            Optional - Return albums created by this user.
	 * @param photoIds
	 *            Optional - Return albums with aids in this list. This is a
	 *            comma-separated list of pids.
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 * @see <a
	 *      href="http://developers.facebook.com/documentation.php?v=1.0&method=photos.getAlbums">facebook.photos.getAlbums</a>
	 */
	public static InputStream getPhotoAlbums(Session session, String userId,
			String[] photoIds, String format, String callback)
			throws IOException, FacebookException {

		if (userId == null && (photoIds == null || photoIds.length == 0)) {
			log.warn("userId or photoIds required");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"userId or photoIds required");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		if (userId != null)
			params.add(new Pair<String, CharSequence>("uid", userId));

		if (photoIds != null && photoIds.length > 0)
			params.add(new Pair<String, CharSequence>("pids",
					getCommaDelimited(photoIds)));

		// do it!
		return callMethod(session, FacebookMethod.PHOTOS_GET_ALBUMS, null,
				params);
	}

	/**
	 * Returns all visible photos according to the filters specified. This may
	 * be used to find all photos in which a user is tagged, return photos in a
	 * specific album, or to query specific pids.
	 * 
	 * This method returns all visible photos satisfying the filters specified.
	 * The method can be used to return all photos tagged with user, in an
	 * album, query a specific set of photos by a list of pids, or filter on any
	 * combination of these three.
	 * 
	 * It is an error to omit all three of the subj_id, aid, and pids
	 * parameters. They have no defaults.
	 * 
	 * Privacy note: Photos will be visible on the Facebook Platform only if the
	 * photo owner has not turned off access to the Platform
	 * 
	 * @param application
	 * @param subjectId
	 *            Optional - Filter by photos tagged with this user
	 * @param albumId
	 *            Optional - Filter by photos in this album
	 * @param photoIds
	 *            Optional - Filter by photos in this list. This is a
	 *            comma-separated list of pids.
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 * @see <a
	 *      href="http://developers.facebook.com/documentation.php?v=1.0&method=photos.get">facebook.photos.get</a>
	 */
	public static InputStream getPhotos(Session session, String subjectId,
			String albumId, String[] photoIds, String format, String callback)
			throws IOException, FacebookException {

		if (subjectId == null && albumId == null
				&& (photoIds == null || photoIds.length == 0)) {
			log.warn("subjectId, albumId, or photoIds required");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"subjectId, albumId, or photoIds required");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		if (subjectId != null)
			params.add(new Pair<String, CharSequence>("subj_id", subjectId));

		if (albumId != null)
			params.add(new Pair<String, CharSequence>("aid", albumId));

		if (photoIds != null && photoIds.length > 0)
			params.add(new Pair<String, CharSequence>("pids",
					getCommaDelimited(photoIds)));

		// do it!
		return callMethod(session, FacebookMethod.PHOTOS_GET, null, params);
	}

	/**
	 * Returns the set of user tags on all photos specified.
	 * 
	 * Privacy note: A tag of a user will be visible to an application only if
	 * that user has not turned off access to the Facebook Platform.
	 * 
	 * @param application
	 * @param photoIds
	 *            The list of photos from which to extract photo tags. This is a
	 *            comma-separated list of pids.
	 * @param format
	 * @param callback
	 * @return If no photo tags are found, the method will return an empty
	 *         photos_getTags_response element. Text tags not corresponding to a
	 *         user are not currently returned.
	 * @throws IOException
	 * @throws FacebookException
	 * @see <a
	 *      href="http://developers.facebook.com/documentation.php?v=1.0&method=photos.getTags">facebook.photos.getTags</a>
	 */
	public static InputStream getPhotoTags(Session session, String[] photoIds,
			String format, String callback) throws IOException,
			FacebookException {

		if (photoIds == null || photoIds.length == 0) {
			log.warn("photoIds required");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"photoIds required");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		if (photoIds != null && photoIds.length > 0)
			params.add(new Pair<String, CharSequence>("pids",
					getCommaDelimited(photoIds)));

		// do it!
		return callMethod(session, FacebookMethod.PHOTOS_GET_TAGS, null, params);
	}

	/**
	 * 
	 * @param application
	 * @param photoId
	 *            The photo id of the photo to be tagged.
	 * @param taggedUserId
	 *            The user id of the user being tagged. Either tag_uid or
	 *            tag_text must be specified.
	 * @param tagText
	 *            Some text identifying the person being tagged. Either tag_uid
	 *            or tag_text must be specified. This parameter is ignored if
	 *            tag_uid is specified.
	 * @param x
	 *            The horizontal position of the tag, as a percentage from 0 to
	 *            100, from the left of the photo.
	 * @param y
	 *            The vertical position of the tag, as a percentage from 0 to
	 *            100, from the top of the photo.
	 * @param tags
	 *            A JSON-serialized array representing a list of tags to be
	 *            added to the photo. If the tags parameter is specified, the x,
	 *            y, tag_uid, and tag_text parameters are ignored. Each tag in
	 *            the list must specify: "x", "y", and either the user id
	 *            "tag_uid" or free-form "tag_text" identifying the person being
	 *            tagged. An example of this is the string
	 *            <code>[{"x":"30.0","y":"30.0","tag_uid":1234567890},
	 *            {"x":"70.0","y":"70.0","tag_text":"some person"}]</code>.
	 * @param format
	 * @param callback
	 * @return
	 * @throws IOException
	 * @throws FacebookException
	 * @see <a
	 *      href="http://developers.facebook.com/documentation.php?v=1.0&method=photos.addTag">facebook.photos.addTag</a>
	 */
	public static InputStream addPhotoTag(Session session, String photoId,
			String taggedUserId, String tagText, float x, float y, String tags,
			String format, String callback) throws IOException,
			FacebookException {

		if (photoId == null) {
			log.warn("photoId parameter missing");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"photoId parameter missing");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);
		params.add(new Pair<String, CharSequence>("pid", photoId));

		if (tags != null) {
			// tags wins if specified
			params.add(new Pair<String, CharSequence>("tags", tags));
		} else {
			// use other parameters
			if (taggedUserId != null) {
				// uid wins
				params.add(new Pair<String, CharSequence>("tag_uid",
						taggedUserId));
			} else {
				params.add(new Pair<String, CharSequence>("tag_text", tagText));
			}

			params.add(new Pair<String, CharSequence>("x", Float.toString(x)));
			params.add(new Pair<String, CharSequence>("y", Float.toString(y)));
		}

		// do it!
		return callMethod(session, FacebookMethod.PHOTOS_ADD_TAG, null, params);
	}

	/**
	 * 
	 * @param application
	 * @param name
	 *            The album name.
	 * @param location
	 *            Optional - The album location.
	 * @param description
	 *            Optional - The album description.
	 * @param format
	 * @param callback
	 * @return The returned cover_pid is always 0.
	 * @throws IOException
	 * @throws FacebookException
	 * @see <a
	 *      href="http://developers.facebook.com/documentation.php?v=1.0&method=photos.createAlbum">facebook.photos.createAlbum</a>
	 */
	public static InputStream createPhotoAlbum(Session session, String name,
			String location, String description, String format, String callback)
			throws IOException, FacebookException {

		if (name == null) {
			log.warn("name parameter missing");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"name parameter missing");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);
		params.add(new Pair<String, CharSequence>("name", name));

		if (location != null)
			params.add(new Pair<String, CharSequence>("location", location));
		if (description != null)
			params.add(new Pair<String, CharSequence>("description",
					description));

		// do it!
		return callMethod(session, FacebookMethod.PHOTOS_CREATE_ALBUM, null,
				params);
	}

	public static InputStream uploadPhoto(Session session, String albumId,
			String caption, File photo, String format, String callback)
			throws IOException, FacebookException {

		if (photo == null || !photo.exists()) {
			log.warn("photo parameter missing");
			throw new FacebookException(ErrorCodes.FB_BAD_PARAMETER,
					"photo parameter missing");
		}

		List<Pair<String, CharSequence>> params = getParams(format, callback);

		if (albumId != null)
			params.add(new Pair<String, CharSequence>("aid", albumId));
		if (caption != null)
			params.add(new Pair<String, CharSequence>("caption", caption));

		// do it!
		return callMethod(session, FacebookMethod.PHOTOS_UPLOAD, photo, params);
	}

	/**
	 * Get params used for session calls that allow format and callback.
	 * 
	 * @param format
	 * @param callback
	 * @return
	 */
	private static List<Pair<String, CharSequence>> getParams(String format,
			String callback) {
		// optional params
		List<Pair<String, CharSequence>> params = new ArrayList<Pair<String, CharSequence>>();
		if (format != null)
			params.add(new Pair<String, CharSequence>("format", format));
		if (callback != null)
			params.add(new Pair<String, CharSequence>("callback", callback));
		return params;
	}

	/**
	 * 
	 * @param application
	 * @param method
	 * @param paramPairs
	 * @return
	 * @throws FacebookException
	 * @throws IOException
	 */
	private static InputStream callMethod(Session session,
			FacebookMethod method, Pair<String, CharSequence>... paramPairs)
			throws FacebookException, IOException {
		return callMethod(session, method, null, Arrays.asList(paramPairs));
	}

	private static InputStream callMethod(Session session,
			FacebookMethod method, File file,
			Collection<Pair<String, CharSequence>> paramPairs)
			throws IOException, FacebookException {
		return callMethod(session.getApiKey(), session.getKey(), session
				.getSecret(), false, method, null, paramPairs);
	}

	/**
	 * Call the specified method, with the given parameters, and return a DOM
	 * tree with the results.
	 * 
	 * @param apiKey
	 *            used by the calling application
	 * @param sessionKey
	 *            key for the current session, which also represents a unique
	 *            user. It is only required if the method requires a session
	 *            (usually only null for methods that don't require a session).
	 * @param secret
	 *            used to sign the request. For webapps this is always the
	 *            application secret. For Desktop apps the secret will depend on
	 *            the state of the session: for an unauthenticated session
	 *            calling either getAuthToken or getSession the secret will be
	 *            the API secret, otherwise it is the secret returned by the
	 *            call to getSession.
	 * @param doHttps
	 *            true to use HTTPS for the request. Should only be true for
	 *            desktops apps using the getAuthToken method.
	 * @param method
	 *            the fieldName of the method
	 * @param file
	 * @param paramPairs
	 *            a list of arguments to the method, not includeing the
	 *            signature
	 * @return the server's response stream (usually XML data)
	 * @throws IOException
	 * @throws Exception
	 *             with a description of any errors given to us by the server.
	 */
	private static InputStream callMethod(String apiKey, String sessionKey,
			String secret, boolean doHttps, FacebookMethod method, File file,
			Collection<Pair<String, CharSequence>> paramPairs)
			throws IOException, FacebookException {

		HashMap<String, CharSequence> params = new HashMap<String, CharSequence>(
				2 * method.numTotalParams());

		params.put("method", method.methodName());
		params.put("api_key", apiKey);
		params.put("v", API_VERSION);

		if (method.requiresSession()) {
			params.put("call_id", Long.toString(System.currentTimeMillis()));
			params.put("session_key", sessionKey);
		}
		CharSequence oldVal;
		for (Pair<String, CharSequence> p : paramPairs) {
			// check for oldvalue
			oldVal = params.put(p.name, p.value);
			if (oldVal != null && log.isDebugEnabled())
				log.debug("For parameter " + p.name + ", overwrote old value "
						+ oldVal + " with new value " + p.value + ".");
		}

		assert (!params.containsKey("sig"));

		// String signature = generateSignature(application,
		// FacebookSignatureUtil
		// .convert(params.entrySet()), method.requiresSession());

		String signature = FacebookSignatureUtil.generateSignature(params
				.entrySet(), secret);

		params.put("sig", signature);

		return method.takesFile() ? postFileRequest(method.methodName(),
				params, file) : postRequest(method.methodName(), params,
				doHttps, true);
	}

	private static InputStream postFileRequest(String methodName,
			Map<String, CharSequence> params, File uploadFile)
			throws IOException {
		assert (null != uploadFile);
		try {
			PostMethod multipartPost = new PostMethod(HTTP_SERVER_ADDR);
			multipartPost.getParams().setBooleanParameter(
					HttpMethodParams.USE_EXPECT_CONTINUE, true);

			Part[] parts = new Part[params.size() + 1];
			parts[0] = new FilePart(uploadFile.getName(), uploadFile);

			int partNo = 1;
			for (Map.Entry<String, CharSequence> entry : params.entrySet()) {
				parts[partNo] = new StringPart(entry.getKey(), entry.getValue()
						.toString(), ENCODING);
				partNo++;
			}
			multipartPost.setRequestEntity(new MultipartRequestEntity(parts,
					multipartPost.getParams()));

			int iGetResultCode = httpClient.executeMethod(multipartPost);

			if (iGetResultCode >= HttpStatus.SC_BAD_REQUEST) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ multipartPost.getStatusText());
			}

			InputStream in = new HttpClientInputStream(multipartPost
					.getResponseBodyAsStream(), multipartPost);
			return in;
		} catch (Exception e) {
			log.warn("exception: " + e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Do the POST request.
	 * 
	 * @param method
	 * @param params
	 * @param doHttps
	 * @param doEncode
	 * @return
	 * @throws IOException
	 */
	private static InputStream postRequest(CharSequence method,
			Map<String, CharSequence> params, boolean doHttps, boolean doEncode)
			throws IOException {

		// CharSequence buffer = (null == params) ? "" : delimit(
		// params.entrySet(), "&", "=", doEncode);

		PostMethod post = null;
		try {
			post = (doHttps) ? new PostMethod(HTTPS_SERVER_ADDR)
					: new PostMethod(HTTP_SERVER_ADDR);

			for (Map.Entry<String, CharSequence> entry : params.entrySet()) {
				// NOTE: the value will be URLEncoded by PostMethod for us!
				post.setParameter(entry.getKey(), entry.getValue().toString());
			}

			// debug the request as a GET (it's really a POST)
			if (log.isDebugEnabled()) {
				StringBuffer msg = new StringBuffer();
				msg.append("POST: ");
				msg.append(post.getURI());
				msg.append("?");
				if (post.getQueryString() != null) {
					msg.append(post.getQueryString());
					msg.append("&");
				}
				ByteArrayOutputStream reqBody = new ByteArrayOutputStream(
						(int) post.getRequestEntity().getContentLength());
				post.getRequestEntity().writeRequest(reqBody);
				msg.append(reqBody.toString(ENCODING));
				log.debug(msg);
			}

			// check response code
			int iGetResultCode = httpClient.executeMethod(post);

			if (iGetResultCode >= HttpStatus.SC_BAD_REQUEST) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ post.getStatusText());
			}

			// read the XML
			InputStream in = new HttpClientInputStream(post
					.getResponseBodyAsStream(), post);
			return in;
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		}
	}

	public void finalize() throws Throwable {
		super.finalize();
		shutdown();
	}

	/**
	 * This is the nice way to shut down, and should only be done on system
	 * exit.
	 */
	public static void shutdown() {
		connectionManager.shutdown(); // close all connections
	}

	/**
	 * Manage name-value pairs.
	 * 
	 * @param <N>
	 * @param <V>
	 */
	protected static class Pair<N, V> {
		public N name;
		public V value;

		public Pair(N name, V value) {
			this.name = name;
			this.value = value;
		}
	}

	/**
	 * Manage connection releases without memory leaks. This will release the
	 * connection immediately after the InputStream is closed.
	 * 
	 * @see <a href="http://commons.apache.org/httpclient/threading.html">Apache
	 *      HttpClient Threading</a>
	 */
	private static class HttpClientInputStream extends BufferedInputStream {
		private HttpMethod httpMethod;

		public HttpClientInputStream(InputStream in, HttpMethod httpMethod) {
			super(in, BUFF_SIZE);
			this.httpMethod = httpMethod;
		}

		public void close() throws IOException {
			super.close();
			httpMethod.releaseConnection(); // clean up
		}
	}
}
