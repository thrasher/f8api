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

/**
 * Different attributes for an application.
 * 
 * Attachments are not included here! See link for more info.
 * 
 * @author <a href="mailto:jasonthrasher@gmail.com">Jason Thrasher</a>
 * @see http://developers.facebook.com/documentation.php?v=1.0&doc=installation
 */
public class Application {
	private String name;

	private String apiKey;

	private String apiSecret; // should not be set on a desktop app

	private String supportEmail;

	/**
	 * Optional URL.
	 */
	private String callbackUrl;

	/**
	 * Specifies if this app is a desktop app, or webapp. Login and sessions are
	 * handled differently for each one.
	 */
	private boolean isDesktop;

	/**
	 * Once you specify a directory name, going to
	 * http://apps.facebook.com/YOUR_DIRECTORY_NAME/pagex.abc will render
	 * pagex.abc from <YOUR CALLBACK URL>pagex.abc in the Facebook Canvas. You
	 * can specify whether you want these pages to be rendered as FBML or be
	 * loaded as an iframe.
	 */
	private String directoryName;

	/**
	 * This FBML will be displayed in the user's profile once the user installs
	 * your application but you have not called setFBML for that user yet.
	 */
	private String defaultFbml;

	/**
	 * Edit URL (optional)
	 * 
	 * This URL should link to a page where the user can configure the settings
	 * for your application. If specified, the edit URL will be used in the
	 * following places: After the user installs your application, they will be
	 * redirected to the URL with an authentication token. An "edit" link will
	 * appear in the title bar of your application's profile box for a user's
	 * own profile An "edit" link will appear in a user's "Edit Apps" page in
	 * the entry for your application.
	 * 
	 */
	private String editUrl;

	/**
	 * Side Nav URL (optional)
	 * 
	 * If specified, an entry will appear in every user's side nav with your
	 * application's icon and title linking to this URL. This URL must link to a
	 * Facebook Canvas page.
	 */
	private String sideNavUrl;

	/**
	 * Uninstall URL (optional)
	 * 
	 * If specified, Facebook will send a POST request to this URL when a user
	 * uninstalls the application. The request will contain the following
	 * parameters:
	 * 
	 * <pre>
	 * fb_sig_uninstall - set to 1, to indicate the uninstall. 
	 * fb_sig_time - a Unix time indicating when the uninstall occurred. 
	 * fb_sig_user - the ID of the user uninstalling the application. 
	 * fb_sig_session_key - the API session key for the user and application. 
	 * fb_sig - a signature, as described in FBML forms. 
	 * </pre>
	 * 
	 * Note: The application will not be notified if the Post Install URL isn't
	 * also set (via the account form).
	 * 
	 * @see http://www.facebook.com/developers/apps.php
	 */
	private String uninstallUrl;

	/**
	 * Canvas Page URL (optional) If you want to create Facebook Canvas pages,
	 * you must specify a directory name for your pages. Directory names are
	 * unique so you cannot have a name another app has claimed. Once you
	 * specify a directory name, going to
	 * http://apps.facebook.com/YOUR_DIRECTORY_NAME/pagex.abc will render
	 * pagex.abc from <YOUR CALLBACK URL>pagex.abc in the Facebook Canvas. You
	 * can specify whether you want these pages to be rendered as FBML or be
	 * loaded as an iframe.
	 * 
	 * For example: "http://apps.facebook.com/prolefeed/"
	 */
	private String canvasPageUrl;

	/**
	 * If true, the canvas page will use FBML. Otherwise it will use an iFrame.
	 */
	private boolean canvasUsesFbml;

	private String termsOfServiceUrl;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}

	public String getSupportEmail() {
		return supportEmail;
	}

	public void setSupportEmail(String supportEmail) {
		this.supportEmail = supportEmail;
	}

	public String getCallbackUrl() {
		return callbackUrl;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	public boolean isDesktop() {
		return isDesktop;
	}

	public void setDesktop(boolean isDesktop) {
		this.isDesktop = isDesktop;
	}

	public String getDirectoryName() {
		return directoryName;
	}

	public void setDirectoryName(String directoryName) {
		this.directoryName = directoryName;
	}

	public String getDefaultFbml() {
		return defaultFbml;
	}

	public void setDefaultFbml(String defaultFbml) {
		this.defaultFbml = defaultFbml;
	}

	public String getEditUrl() {
		return editUrl;
	}

	public void setEditUrl(String editUrl) {
		this.editUrl = editUrl;
	}

	public String getSideNavUrl() {
		return sideNavUrl;
	}

	public void setSideNavUrl(String sideNavUrl) {
		this.sideNavUrl = sideNavUrl;
	}

	public String getUninstallUrl() {
		return uninstallUrl;
	}

	public void setUninstallUrl(String uninstallUrl) {
		this.uninstallUrl = uninstallUrl;
	}

	public String getCanvasPageUrl() {
		return canvasPageUrl;
	}

	public void setCanvasPageUrl(String canvasPageUrl) {
		this.canvasPageUrl = canvasPageUrl;
	}

	public boolean isCanvasUsesFbml() {
		return canvasUsesFbml;
	}

	public void setCanvasUsesFbml(boolean canvasUsesFbml) {
		this.canvasUsesFbml = canvasUsesFbml;
	}

	public String getTermsOfServiceUrl() {
		return termsOfServiceUrl;
	}

	public void setTermsOfServiceUrl(String termsOfServiceUrl) {
		this.termsOfServiceUrl = termsOfServiceUrl;
	}
}
