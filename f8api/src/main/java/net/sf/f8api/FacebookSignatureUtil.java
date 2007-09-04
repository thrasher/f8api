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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FacebookSignatureUtil {
	private FacebookSignatureUtil() {
	}

	/**
	 * Out of the passed in <code>reqParams</code>, extracts the parameters
	 * that are in the FacebookParam namespace and returns them.
	 * 
	 * @param reqParams
	 *            A map of request parameters to their values. Values are arrays
	 *            of strings, as returned by ServletRequest.getParameterMap().
	 *            Only the first element in a given array is significant.
	 * @return a boolean indicating whether the calculated signature matched the
	 *         expected signature
	 */
	public static Map<String, CharSequence> extractFacebookParamsFromArray(
			Map<CharSequence, CharSequence[]> reqParams) {
		if (null == reqParams)
			return null;
		Map<String, CharSequence> result = new HashMap<String, CharSequence>(
				reqParams.size());
		for (Map.Entry<CharSequence, CharSequence[]> entry : reqParams
				.entrySet()) {
			String key = entry.getKey().toString();
			if (FacebookParam.isInNamespace(key)) {
				CharSequence[] value = entry.getValue();
				if (value.length > 0)
					result.put(key, value[0]);
			}
		}
		return result;
	}

	/**
	 * Out of the passed in <code>reqParams</code>, extracts the parameters
	 * that are in the FacebookParam namespace and returns them.
	 * 
	 * @param reqParams
	 *            a map of request parameters to their values
	 * @return a boolean indicating whether the calculated signature matched the
	 *         expected signature
	 */
	public static Map<String, CharSequence> extractFacebookNamespaceParams(
			Map<CharSequence, CharSequence> reqParams) {
		if (null == reqParams)
			return null;
		Map<String, CharSequence> result = new HashMap<String, CharSequence>(
				reqParams.size());
		for (Map.Entry<CharSequence, CharSequence> entry : reqParams.entrySet()) {
			String key = entry.getKey().toString();
			if (FacebookParam.isInNamespace(key))
				result.put(key, entry.getValue());
		}
		return result;
	}

	/**
	 * Out of the passed in <code>reqParams</code>, extracts the parameters
	 * that are known FacebookParams and returns them.
	 * 
	 * @param reqParams
	 *            a map of request parameters to their values
	 * @return a map suitable for being passed to verify signature
	 */
	public static EnumMap<FacebookParam, CharSequence> extractFacebookParams(
			Map<CharSequence, CharSequence> reqParams) {
		if (null == reqParams)
			return null;

		EnumMap<FacebookParam, CharSequence> result = new EnumMap<FacebookParam, CharSequence>(
				FacebookParam.class);
		for (Map.Entry<CharSequence, CharSequence> entry : reqParams.entrySet()) {
			FacebookParam matchingFacebookParam = FacebookParam.get(entry
					.getKey().toString());
			if (null != matchingFacebookParam) {
				result.put(matchingFacebookParam, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Verifies that a signature received matches the expected value. Removes
	 * FacebookParam.SIGNATURE from params if present.
	 * 
	 * @param params
	 *            a map of parameters and their values, such as one obtained
	 *            from extractFacebookParams; expected to the expected signature
	 *            as the FacebookParam.SIGNATURE parameter
	 * @param secret
	 * @return a boolean indicating whether the calculated signature matched the
	 *         expected signature
	 */
	public static boolean verifySignature(
			EnumMap<FacebookParam, CharSequence> params, String secret) {
		if (null == params || params.isEmpty())
			return false;
		CharSequence sigParam = params.remove(FacebookParam.SIGNATURE);
		return (null == sigParam) ? false : verifySignature(params, secret,
				sigParam.toString());
	}

	/**
	 * Verifies that a signature received matches the expected value.
	 * 
	 * @param params
	 *            a map of parameters and their values, such as one obtained
	 *            from extractFacebookParams
	 * @return a boolean indicating whether the calculated signature matched the
	 *         expected signature
	 */
	public static boolean verifySignature(
			EnumMap<FacebookParam, CharSequence> params, String secret,
			String expected) {
		assert !(null == secret || "".equals(secret));
		if (null == params || params.isEmpty())
			return false;
		if (null == expected || "".equals(expected)) {
			return false;
		}
		params.remove(FacebookParam.SIGNATURE);
		List<String> sigParams = convertFacebookParams(params.entrySet());
		return verifySignature(sigParams, secret, expected);
	}

	/**
	 * Verifies that a signature received matches the expected value. Removes
	 * FacebookParam.SIGNATURE from params if present.
	 * 
	 * @param params
	 *            a map of parameters and their values, such as one obtained
	 *            from extractFacebookNamespaceParams; expected to contain the
	 *            signature as the FacebookParam.SIGNATURE parameter
	 * @param secret
	 * @return a boolean indicating whether the calculated signature matched the
	 *         expected signature
	 */
	public static boolean verifySignature(Map<String, CharSequence> params,
			String secret) {
		if (null == params || params.isEmpty())
			return false;
		CharSequence sigParam = params.remove(FacebookParam.SIGNATURE
				.toString());
		return (null == sigParam) ? false : verifySignature(params, secret,
				sigParam.toString());
	}

	/**
	 * Verifies that a signature received matches the expected value.
	 * 
	 * @param params
	 *            a map of parameters and their values, such as one obtained
	 *            from extractFacebookNamespaceParams
	 * @return a boolean indicating whether the calculated signature matched the
	 *         expected signature
	 */
	public static boolean verifySignature(Map<String, CharSequence> params,
			String secret, String expected) {
		assert !(null == secret || "".equals(secret));
		if (null == params || params.isEmpty())
			return false;
		if (null == expected || "".equals(expected)) {
			return false;
		}
		params.remove(FacebookParam.SIGNATURE.toString());
		List<String> sigParams = convert(params.entrySet());
		return verifySignature(sigParams, secret, expected);
	}

	private static boolean verifySignature(List<String> sigParams,
			String secret, String expected) {
		if (null == expected || "".equals(expected))
			return false;
		String signature = generateSignature(sigParams, secret);
		return expected.equals(signature);
	}

	/**
	 * Converts a Map of key-value pairs into the form expected by
	 * generateSignature
	 * 
	 * @param entries
	 *            a collection of Map.Entry's, such as can be obtained using
	 *            myMap.entrySet()
	 * @return a List suitable for being passed to generateSignature
	 */
	public static List<String> convert(
			Collection<Map.Entry<String, CharSequence>> entries) {
		List<String> result = new ArrayList<String>(entries.size());
		for (Map.Entry<String, CharSequence> entry : entries)
			result.add(FacebookParam.stripSignaturePrefix(entry.getKey()) + "="
					+ entry.getValue());
		return result;
	}

	/**
	 * Converts a Map of key-value pairs into the form expected by
	 * generateSignature
	 * 
	 * @param entries
	 *            a collection of Map.Entry's, such as can be obtained using
	 *            myMap.entrySet()
	 * @return a List suitable for being passed to generateSignature
	 */
	public static List<String> convertFacebookParams(
			Collection<Map.Entry<FacebookParam, CharSequence>> entries) {
		List<String> result = new ArrayList<String>(entries.size());
		for (Map.Entry<FacebookParam, CharSequence> entry : entries)
			result.add(entry.getKey().getSignatureName() + "="
					+ entry.getValue());
		return result;
	}

	public static String generateSignature(
			Set<Map.Entry<String, CharSequence>> entries, String secret) {
		return FacebookSignatureUtil.generateSignature(FacebookSignatureUtil
				.convert(entries), secret);
	}

	/**
	 * Calculates the signature for the given set of params using the supplied
	 * secret
	 * 
	 * @param params
	 *            Strings of the form "key=value"
	 * @param secret
	 * @return the signature
	 */
	public static String generateSignature(List<String> params, String secret) {
		// first sort the params
		Collections.sort(params);

		StringBuffer buffer = new StringBuffer();

		// concatenate them
		for (String param : params) {
			buffer.append(param);
		}

		// add the secret (this is different for webapps vs desktop)
		buffer.append(secret);

		if (FacebookApi.log.isDebugEnabled())
			FacebookApi.log.debug("signing: " + buffer);

		try {
			java.security.MessageDigest md = java.security.MessageDigest
					.getInstance("MD5");
			StringBuffer result = new StringBuffer();
			for (byte b : md.digest(buffer.toString().getBytes())) {
				result.append(Integer.toHexString((b & 0xf0) >>> 4));
				result.append(Integer.toHexString(b & 0x0f));
			}

			if (FacebookApi.log.isDebugEnabled())
				FacebookApi.log.debug("signature: " + result);

			return result.toString();
		} catch (java.security.NoSuchAlgorithmException ex) {
			FacebookApi.log.debug("MD5 does not appear to be supported" + ex);
			return "";
		}
	}
}
