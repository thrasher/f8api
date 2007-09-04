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

import java.util.Date;

public class Event {
	long id;
	String name;
	String tagline;
	long nId;
	String pic;
	String picBig;
	String picSmall;
	String host;
	String description;
	String type;
	String subType;
	Date startTime;
	Date endTime;
	long creatorId;
	Date updateTime;
	String location;
	String venueCity;
	String venuState;
	String venuCountry;
}
