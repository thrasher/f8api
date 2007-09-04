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

public enum FacebookParam implements CharSequence {
	SIGNATURE,
	USER("user"),
	SESSION_KEY("session_key"),
	EXPIRES("expires"),
	IN_CANVAS("in_canvas"),
	IN_IFRAME("in_iframe"),
	IN_PROFILE("profile"),
	TIME("time"),
	FRIENDS("friends"),
	ADDED("added"),
	PROFILE_UPDATE_TIME("profile_udpate_time"),
	API_KEY("api_key");
	//,AUTH_TOKEN("auth_token");

	private static Map<String, FacebookParam> _lookupTable = new HashMap<String, FacebookParam>(
			FacebookParam.values().length);
	static {
		for (FacebookParam param : FacebookParam.values()) {
			_lookupTable.put(param.toString(), param);
		}
	}

	/**
	 * Retrieves the FacebookParam corresponding to the supplied String key.
	 * 
	 * @param key
	 *            a possible FacebookParam
	 * @return the matching FacebookParam or null if there's no match
	 */
	public static FacebookParam get(String key) {
		return isInNamespace(key) ? _lookupTable.get(key) : null;
	}

	/**
	 * Indicates whether a given key is in the FacebookParam namespace
	 * 
	 * @param key
	 * @return boolean
	 */
	public static boolean isInNamespace(String key) {
		return null != key
				&& key.startsWith(FacebookParam.SIGNATURE.toString());
	}

	public static boolean isSignature(String key) {
		return SIGNATURE.equals(get(key));
	}

	private String _paramName;
	private String _signatureName;

	private FacebookParam() {
		this._paramName = "fb_sig";
	}

	private FacebookParam(String name) {
		this._signatureName = name;
		this._paramName = "fb_sig_" + name;
	}

	/* Implementing CharSequence */
	public char charAt(int index) {
		return this._paramName.charAt(index);
	}

	public int length() {
		return this._paramName.length();
	}

	public CharSequence subSequence(int start, int end) {
		return this._paramName.subSequence(start, end);
	}

	public String toString() {
		return this._paramName;
	}

	public String getSignatureName() {
		return this._signatureName;
	}

	public static String stripSignaturePrefix(String paramName) {
		if (paramName != null && paramName.startsWith("fb_sig_")) {
			return paramName.substring(7);
		}
		return paramName;
	}

	public static void main(String[] args) {
		System.out.println(isSignature("fb_sig"));
		System.out.println(!isSignature("fb_sig_something"));

		assert false;
	}
}
