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

import java.util.EnumSet;

public enum FacebookMethod implements CharSequence {
	// Authentication
	AUTH_CREATE_TOKEN("facebook.auth.createToken"), AUTH_GET_SESSION(
			"facebook.auth.getSession", 1),
	// FQL Query
	FQL_QUERY("facebook.fql.query", 1),
	// Events
	EVENTS_GET("facebook.events.get", 5), EVENTS_GET_MEMBERS(
			"facebook.events.getMembers", 1),
	// Friends
	FRIENDS_GET("facebook.friends.get"), FRIENDS_GET_APP_USERS(
			"facebook.friends.getAppUsers"), FRIENDS_GET_REQUESTS(
			"facebook.friends.getRequests"), FRIENDS_ARE_FRIENDS(
			"facebook.friends.areFriends", 2),
	// Users
	USERS_GET_INFO("facebook.users.getInfo", 2), USERS_GET_LOGGED_IN_USER(
			"facebook.users.getLoggedInUser"), USERS_IS_APP_ADDED(
			"facebook.users.isAppAdded"),
	// Photos
	PHOTOS_GET("facebook.photos.get", 2), PHOTOS_GET_ALBUMS(
			"facebook.photos.getAlbums", 1), PHOTOS_GET_TAGS(
			"facebook.photos.getTags", 1),
	// PhotoUploads
	PHOTOS_CREATE_ALBUM("facebook.photos.createAlbum", 3), PHOTOS_ADD_TAG(
			"facebook.photos.addTag", 5), PHOTOS_UPLOAD(
			"facebook.photos.upload", 3, true),
	// Notifications
	NOTIFICATIONS_GET("facebook.notifications.get"), NOTIFICATIONS_SEND(
			"facebook.notifications.send", 5), NOTIFICATIONS_SEND_REQUEST(
			"facebook.notifications.sendRequest", 5),
	// Groups
	GROUPS_GET("facebook.groups.get", 1), GROUPS_GET_MEMBERS(
			"facebook.groups.getMembers", 1),
	// FBML
	PROFILE_SET_FBML("facebook.profile.setFBML", 2), PROFILE_GET_FBML(
			"facebook.profile.getFBML", 1), FBML_REFRESH_REF_URL(
			"facebook.fbml.refreshRefUrl", 1), FBML_REFRESH_IMG_SRC(
			"facebook.fbml.refreshImgSrc", 1),
	// Feed
	FEED_PUBLISH_ACTION_OF_USER("facebook.feed.publishActionOfUser", 11), FEED_PUBLISH_STORY_TO_USER(
			"facebook.feed.publishStoryToUser", 11), ;

	private String methodName;
	private int numParams;
	private int maxParamsWithSession;
	private boolean takesFile;

	private static EnumSet<FacebookMethod> preAuth = null;
	private static EnumSet<FacebookMethod> postAuth = null;

	public static EnumSet<FacebookMethod> preAuthMethods() {
		if (null == preAuth)
			preAuth = EnumSet.of(AUTH_CREATE_TOKEN, AUTH_GET_SESSION);
		return preAuth;
	}

	public static EnumSet<FacebookMethod> postAuthMethods() {
		if (null == postAuth)
			postAuth = EnumSet.complementOf(preAuthMethods());
		return postAuth;
	}

	FacebookMethod(String name) {
		this(name, 0, false);
	}

	FacebookMethod(String name, int maxParams) {
		this(name, maxParams, false);
	}

	FacebookMethod(String name, int maxParams, boolean takesFile) {
		assert (name != null && 0 != name.length());
		this.methodName = name;
		this.numParams = maxParams;
		this.maxParamsWithSession = maxParams
				+ FacebookApi.NUM_AUTOAPPENDED_PARAMS;
		this.takesFile = takesFile;
	}

	public String methodName() {
		return this.methodName;
	}

	public int numParams() {
		return this.numParams;
	}

	public boolean requiresSession() {
		return postAuthMethods().contains(this);
	}

	public int numTotalParams() {
		return requiresSession() ? this.maxParamsWithSession : this.numParams;
	}

	public boolean takesFile() {
		return this.takesFile;
	}

	/* Implementing CharSequence */
	public char charAt(int index) {
		return this.methodName.charAt(index);
	}

	public int length() {
		return this.methodName.length();
	}

	public CharSequence subSequence(int start, int end) {
		return this.methodName.subSequence(start, end);
	}

	public String toString() {
		return this.methodName;
	}
}
