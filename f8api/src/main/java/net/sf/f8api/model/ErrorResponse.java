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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.f8api.xml.ErrorResponseHandler;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class ErrorResponse {

	private int code;
	private String message;
	private HashMap<String, CharSequence> params = new HashMap<String, CharSequence>();

	public ErrorResponse() {
	}

	public ErrorResponse(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void addParam(String name, CharSequence value) {
		params.put(name, value);
	}

	public HashMap<String, CharSequence> getParams() {
		return params;
	}

	public void setParams(HashMap<String, CharSequence> params) {
		this.params = params;
	}

	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this,
				ToStringStyle.MULTI_LINE_STYLE).append("code", this.code)
				.append("message", this.message);
		for (Map.Entry<String, CharSequence> entry : params.entrySet()) {
			builder.append(entry.getKey(), entry.getValue());
		}

		return builder.toString();

	}

	public byte[] getXmlByteArray() throws TransformerException,
			ParserConfigurationException {
		// TODO: finish this method
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();

		Element codeE = (Element) document
				.createElement(ErrorResponseHandler.error_code);
		codeE.appendChild(document.createTextNode(Integer.toString(code)));

		Element root = (Element) document
				.createElement(ErrorResponseHandler.error_response);
		// root.setAttribute("type", "MESSAGE");
		root.appendChild(codeE);

		// create output
		document.appendChild(root);
		document.setXmlVersion("1.0");
		document.normalizeDocument();

		ByteArrayOutputStream buff = new ByteArrayOutputStream();

		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		// indent
		transformerFactory.setAttribute("indent-number", new Integer(4));
		try {
			// identity transform
			Transformer xformer = transformerFactory.newTransformer();
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			xformer.transform(new DOMSource(document), new StreamResult(buff));
			return buff.toByteArray(); // buff.toString("UTF-8");

		} catch (TransformerException te) {
			throw te;
		}
	}

}
