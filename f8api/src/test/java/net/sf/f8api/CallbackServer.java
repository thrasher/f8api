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

import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;

/**
 * Trivial Jetty server to allow us to test login sequence for webapps. Must set
 * callback url on facebook to some localhost url or use your HOSTS file to
 * manage what the local url is.
 * 
 * @author <a href="mailto:jasonthrasher@gmail.com">Jason Thrasher</a>
 */
public class CallbackServer {
	protected static Log mLog = LogFactory.getLog(CallbackServer.class);
	private int port = 8080;
	private Server server;
	private String path;
	private HttpServlet callback;

	public CallbackServer(int port, HttpServlet callback, String path) {
		mLog.info("port: " + port + " path: " + path);

		this.port = port;
		// this.basedir = basedir;
		this.callback = callback;
		this.path = path;
	}

	/**
	 * Simple single servlet server.
	 */
	public void start() throws Exception {
		server = new Server(port);
		server.setStopAtShutdown(true);
		// make sure Jetty does not use URLConnection caches with the plugin
		Resource.setDefaultUseCaches(false);

		// FileServlet fs = new FileServlet();
		// fs.setBaseDir(basedir);

		Context context = new Context(server, "/", Context.SESSIONS);
		// context.addServlet(new ServletHolder(fs), "/*");
		context.addServlet(new ServletHolder(callback), path);

		mLog.info("starting Jetty");
		try {
			server.start();
		} catch (Exception e) {
			mLog.warn("jetty failed to start due to: " + e.getMessage());
			throw e;
		}
	}

	public void stop() throws Exception {
		if (server != null)
			server.stop();
	}
}
