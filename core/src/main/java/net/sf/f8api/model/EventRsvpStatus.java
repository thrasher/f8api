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
package net.sf.f8api.model;

/**
 * 
 * @author Jason Thrasher
 * @see <a
 *      href="http://developers.facebook.com/documentation.php?v=1.0&method=events.get">facebook.events.get</a>
 */
public enum EventRsvpStatus implements CharSequence {
	// the supported event RSVP status values
	ATTENDING("attending"), UNSURE("unsure"), DECLINED("declined"), NOT_REPLIED(
			"not_replied");

	private String name;

	EventRsvpStatus(String name) {
		this.name = name;
	}

	public char charAt(int index) {
		return name.charAt(index);
	}

	public int length() {
		return name.length();
	}

	public CharSequence subSequence(int start, int end) {
		return name.subSequence(start, end);
	}

	public String toString() {
		return name;
	}
}
