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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrSubstitutor;

/**
 * @see <a
 *      href="http://developers.facebook.com/documentation.php?v=1.0&doc=other">Other
 *      Functionality</a>
 * 
 * @author <a href="mailto:jasonthrasher@gmail.com">Jason Thrasher</a>
 */
public class Url {
	/**
	 * Longin url ?api_key=${api_key}&v=${v}
	 */
	protected static final String LOGIN = "http://www.facebook.com/login.php?api_key=";

	protected static final String PROFILE = "http://www.facebook.com/profile.php?id=";

	protected static final String POKE = "http://www.facebook.com/poke.php?id=";

	protected static final String MESSAGE = "http://www.facebook.com/message.php?id=";

	protected static final String ADD_FRIEND = "http://www.facebook.com/addfriend.php?id=";

	protected static final String PHOTOS = "http://www.facebook.com/photos.php?id=";

	protected static final String PHOTO_SEARCH = "http://www.facebook.com/photo_search.php?id=";

	protected static final String WALL = "http://www.facebook.com/wall.php?id=";

	protected static final String NOTES = "http://www.facebook.com/notes.php?id=";

	/**
	 * Facebook page allowing user to generate an auth token that is entered
	 * into the app. This is used for Desktop apps to create infinite sessions.
	 */
	protected static final String GENERATE_AUTH_TOKEN = "http://www.facebook.com/code_gen.php?v=1.0&api_key=${api_key}";

	/**
	 * Get a login url for a webapp. The user should use the returned URL to
	 * login to this web based application.
	 * 
	 * When using next="next" and toCanvas=true, the successful login redirect
	 * URL will take the user to something like: <code>
	 * http://apps.facebook.com/appName/next?auth_token=e7e418a4cab359827ad98761b742af7b
	 * </code>
	 * 
	 * If the app's callBack page is configured for FBML, a request will be made
	 * by the Facebook server to: <code>
	 * http://www.mycompany.com/some/path/callback.htmlnext?auth_token=e7e418a4cab359827ad98761b742af7b
	 * </code>
	 * 
	 * Example: If the callback url is: <code>
	 * http://www.mycompany.com/callback/
	 * </code>
	 * And the <code>next</code> parameter is: <code>
	 * success.html
	 * </code>
	 * Then on a successful login the user will see the content of: <code>
	 * http://www.mycompany.com/callback/success.html
	 * </code>
	 * And when <code>toCanvas == false</code> they will be redirected to:
	 * <code>
	 * http://www.mycompany.com/callback/success.html?auth_token=347d2d517bcc31650b7e3d3ee830bbcd
	 * </code>
	 * Or when <code>toCanvas == true</code> they will be redirected to:
	 * <code>
	 * http://apps.facebook.com/appName/success.html?auth_token=e7e418a4cab359827ad98761b742af7b
	 * </code>
	 * Which will render FBML or iFrame content, based on how the app was
	 * configured at Facebook.
	 * 
	 * @param apiKey
	 * @param next
	 *            is usually the callback-filename the user should redirect to
	 *            after they login
	 * @param popup
	 * @param skipCookie
	 * @param hideCheckbox
	 * @param toCanvas
	 * @return
	 */
	public static String getWebappLoginUrl(String apiKey, String next,
			boolean popup, boolean skipCookie, boolean hideCheckbox,
			boolean toCanvas) {

		return getLoginUrl(FacebookApi.API_VERSION, apiKey, next, null, popup,
				skipCookie, hideCheckbox, toCanvas);
	}

	/**
	 * Get a login url for a desktop application.
	 * 
	 * @param apiKey
	 * @param authToken
	 * @param popup
	 * @param skipCookie
	 *            if true, the user will have to login again even for infinite
	 *            sessions
	 * @param hideCheckbox
	 * @return
	 */
	public static String getDesktopLoginUrl(String apiKey, String authToken,
			boolean popup, boolean skipCookie, boolean hideCheckbox) {

		return Url.getLoginUrl(FacebookApi.API_VERSION, apiKey, null,
				authToken, popup, skipCookie, hideCheckbox, false);
	}

	/**
	 * Get a general login url. Supports all parameters, even though
	 * combinations of desktop and webapp parameters are not allowed.
	 * 
	 * @param apiVersion
	 *            The version of the API you are using. Should be set to "1.0".
	 *            Required.
	 * @param apiKey
	 *            Uniquely assigned to the vendor, and identifies, among other
	 *            things, the list of acceptable source IPs for this call.
	 *            Required.
	 * @param next
	 *            A way for web based applications to preserve some state for
	 *            this login - this will get appended to their callback_url
	 *            after the user logs in as described below. Optional.
	 * @param authToken
	 *            Before generating the login URL, the desktop application
	 *            should call the facebook.auth.createToken API function, and
	 *            then use the auth_token returned by that function here.
	 *            Required for desktop apps.
	 * @param popup
	 *            set true to use an alternative style for the login page that
	 *            does not contain any Facebook navigational elements. For the
	 *            best results, the pop-up should ideally have the following
	 *            dimensions: width=646 pixels, height=436 pixels. Optional
	 *            (default is false).
	 * @param skipCookie
	 *            Pass this in to allow a user to re-enter their login
	 *            information. This may be useful if another Facebook user
	 *            previously forgot to logout. Optional(default is false).
	 * @param hideCheckbox
	 *            Pass this in to hide the "Save my login info" checkbox from
	 *            the user. This may be useful if your application does not wish
	 *            to persist the user's session information. See the "Infinite
	 *            Sessions" section below for more info. Optional (default is
	 *            false).
	 * @param toCanvas
	 *            If you pass this parameter, we will use your canvas page URL
	 *            instead of your callback URL as the base for the URL we
	 *            redirect the user to. Optional (default is false).
	 * @return The string url with CGI parameters.
	 */
	protected static String getLoginUrl(String apiVersion, String apiKey,
			String next, String authToken, boolean popup, boolean skipCookie,
			boolean hideCheckbox, boolean toCanvas) {

		// start with a webapp login
		// Map<String, String> map = new HashMap<String, String>();
		// map.put(new String("api_key"), apiKey);
		// map.put(new String("v"), apiVersion);
		// String rep = StrSubstitutor.replace(LOGIN, map);

		StringBuffer url = new StringBuffer(LOGIN);

		url.append(apiKey);
		url.append("&v=");
		url.append(apiVersion);

		if (next != null) {
			url.append("&next=");
			url.append(next);
		}

		if (authToken != null) {
			url.append("&auth_token=");
			url.append(authToken);
		}

		if (popup == true) {
			url.append("&popup=true");
		}

		if (skipCookie == true) {
			url.append("&skipcookie=true");
		}

		if (hideCheckbox == true) {
			url.append("&hide_checkbox=true");
		}

		if (toCanvas == true) {
			url.append("&canvas=true");
		}

		return url.toString();
	}

	/**
	 * Upon successful authentication, if the user has never logged in to this
	 * application before, he will be asked to accept the terms of service for
	 * using the application. Finally, for web-based applications, the user is
	 * redirected to the site <code>
	 * callback_url + next + separator + 'auth_token=______'
	 * </code>
	 * with these values.
	 * 
	 * @param app
	 * @param toCanvas
	 *            Use the same value as for getLoginUrl. If true, redirect will
	 *            point to the canvas page you set up (which will then
	 *            indirectly result in a request going to your callback_url).
	 * @param next
	 *            Use the same value as for getLoginUrl.
	 * @param authToken
	 * @return
	 */
	public static String getWebappLoginRedirectUrl(Application app,
			boolean toCanvas, String next, String authToken) {
		StringBuffer url;

		if (toCanvas) {
			url = new StringBuffer(app.getCanvasPageUrl());
		} else {
			url = new StringBuffer(app.getCallbackUrl());
		}

		if (next != null) {
			url.append(next);
		}

		if (url.indexOf("?") > 0) {
			url.append("&");
		} else {
			url.append("?");
		}

		url.append("auth_token=");
		url.append(authToken);

		return url.toString();
	}

	/**
	 * Use the Facebook Token Generator to let the user create a token that is
	 * then set in our app manually by the user.
	 * 
	 * @param apiKey
	 * @return
	 */
	public static String getLoginAuthTokenGenerationUrl(String apiKey) {
		Map<String, String> map = new HashMap<String, String>();
		map.put(new String("api_key"), apiKey);
		return StrSubstitutor.replace(GENERATE_AUTH_TOKEN, map);
	}

	/**
	 * view the specified user's profile
	 * 
	 * @param userId
	 * @return
	 */
	public static String getProfileUrl(long userId) {
		return PROFILE + userId;
	}

	/**
	 * poke the specified user
	 * 
	 * @param userId
	 * @return
	 */
	public static String getPokeUrl(long userId) {
		return POKE + userId;
	}

	/**
	 * send a message to the specified user. subject and msg parameters are
	 * optional ways to pre-fill the contents of the message (the user will
	 * still be able to edit the message before sending).
	 * ?id=${id}&subject=${subject}&msg=${msg}
	 * 
	 * @param userId
	 *            the user ID to send the message to
	 * @param subject
	 *            the pre-loaded message subject, or null for none
	 * @param body
	 *            the pre-loaded message body, or null for none
	 * @return
	 */
	public static String getMessageUrl(long userId, String subject,
			String message) {
		StringBuffer msg = new StringBuffer(MESSAGE);
		msg.append(userId);
		if (subject != null) {
			msg.append("&subject=");
			msg.append(subject);
		}
		if (message != null) {
			msg.append("&msg=");
			msg.append(message);
		}

		return msg.toString();
	}

	/**
	 * add the specified user as a friend
	 * 
	 * @param userId
	 * @return
	 */
	public static String getAddFriendUrl(String userId) {
		return ADD_FRIEND + userId;
	}

	/**
	 * See photos taken by the specified user.
	 * 
	 * @param userId
	 * @return
	 */
	public static String getPhotosUrl(long userId) {
		return PHOTOS + userId;
	}

	/**
	 * see photos of the specified user
	 * 
	 * @param userId
	 * @return
	 */
	public static String getPhotosOfUrl(long userId) {
		return PHOTO_SEARCH + userId;
	}

	/**
	 * read or post on the specified user's wall
	 * 
	 * @param userId
	 * @return
	 */
	public static String getWallUrl(long userId) {
		return WALL + userId;
	}

	/**
	 * read the specified user's notes
	 * 
	 * @param userId
	 * @return
	 */
	public static String getNotesUrl(long userId) {
		return NOTES + userId;
	}
}
