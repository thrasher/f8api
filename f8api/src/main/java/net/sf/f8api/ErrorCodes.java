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

public class ErrorCodes {
	/**
	 * An unknown error occured.
	 */
	public static final int FB_UNKNOWN_ERROR = 1;

	/**
	 * The service is not available at this time.
	 */
	public static final int FB_TEMP_UNAVAILABLE = 2;

	/**
	 * Returned by FB API when method is unknown.
	 */
	public static final int FB_UNKNOWN_METHOD = 3;

	/**
	 * The application has reached the maximum number of requests allowed. More
	 * requests are allowed once the time window has completed.
	 */
	public static final int FB_BUSY = 4;

	/**
	 * The request came from a remote address not allowed by this application.
	 */
	public static final int FB_IP_ADDR_DENIED = 5;

	/**
	 * One of the parameters specified was missing or invalid.
	 */
	public static final int FB_BAD_PARAMETER = 100;

	/**
	 * The api key submitted is not associated with any known application.
	 */
	public static final int FB_UNKNOWN_APIKEY = 101;

	/**
	 * The session key was improperly submitted or has reached its timeout.
	 * Direct the user to log in again to obtain another key.
	 */
	public static final int FB_SESSION_TIMEOUT = 102;

	/**
	 * The submitted call_id was not greater than the previous call_id for this
	 * session.
	 */
	public static final int FB_CALL_ID_EXPIRED = 103;

	/**
	 * Incorrect signature.
	 */
	public static final int FB_BAD_SIGNATURE = 104;

	/**
	 * Error while parsing FQL statement.
	 */
	public static final int FQL_PARSE_ERROR = 601;

	/**
	 * The field you requested does not exist.
	 */
	public static final int FQL_BAD_FIELD = 602;

	/**
	 * The table you requested does not exist.
	 */
	public static final int FQL_BAD_TABLE = 603;

	/**
	 * Your statement is not indexable.
	 */
	public static final int FQL_NO_INDEX = 604;

	/**
	 * The function you called does not exist.
	 */
	public static final int FQL_BAD_FUNCTION = 605;

	/**
	 * Wrong number of arguments passed into the function.
	 */
	public static final int FQL_WRONG_ARGUMENTS = 606;

}
