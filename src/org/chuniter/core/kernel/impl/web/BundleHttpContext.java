/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation, Cognos Incorporated and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.chuniter.core.kernel.impl.web;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class BundleHttpContext implements HttpContext {
	private Bundle bundle;
	private String bundlePath;
	private String toWelComeFile = "index.html";

	public BundleHttpContext(Bundle bundle) {
		this.bundle = bundle;
	}

	public BundleHttpContext(Bundle b, String bundlePath) {
		this(b);
		if (bundlePath != null) {
			if (bundlePath.endsWith("/")) //$NON-NLS-1$
				bundlePath = bundlePath.substring(0, bundlePath.length() - 1);

			if (bundlePath.length() == 0)
				bundlePath = null;
		}
		this.bundlePath = bundlePath;
	}

	public BundleHttpContext(Bundle b, String bundlePath, String welcomeFile) {
		this(b);
		if (bundlePath != null) {
			if (bundlePath.endsWith("/")) //$NON-NLS-1$
				bundlePath = bundlePath.substring(0, bundlePath.length() - 1);

			if (bundlePath.length() == 0)
				bundlePath = null;
		}
		if (null != welcomeFile && !"".equals(welcomeFile))
			this.toWelComeFile = welcomeFile;
		this.bundlePath = bundlePath;
	}
	
	@Override
	public String getMimeType(String file) {
		/*if (file.endsWith(".jpg")) {
			return "image/jpeg";
		} else if (file.endsWith(".png")) {
			return "image/png";
		} else {
			return "text/html";
		}*/
		return null;
	}

	public boolean handleSecurity(HttpServletRequest arg0,
			HttpServletResponse arg1) throws IOException {
		return true;
	}

	public URL getResource(String resourceName) {
		if (bundlePath != null)
			resourceName = bundlePath + resourceName;

		int lastSlash = resourceName.lastIndexOf('/');
		if (lastSlash == -1)
			return null;

		String path = resourceName.substring(0, lastSlash);
		if (path.length() == 0)
			path = "/"; //$NON-NLS-1$
		String file = resourceName.substring(lastSlash + 1);
		if (null == file || "".equals(file))
			file = toWelComeFile;
		Enumeration<?> entryPaths = bundle.findEntries(path, file, false);
		if (entryPaths != null && entryPaths.hasMoreElements())
			return (URL) entryPaths.nextElement();

		return null;
	}

	public Set getResourcePaths(String path) {
		if (bundlePath != null)
			path = bundlePath + path;

		Enumeration entryPaths = bundle.findEntries(path, null, false);
		if (entryPaths == null)
			return null;

		Set result = new HashSet();
		while (entryPaths.hasMoreElements()) {
			URL entryURL = (URL) entryPaths.nextElement();
			String entryPath = entryURL.getFile();

			if (bundlePath == null)
				result.add(entryPath);
			else
				result.add(entryPath.substring(bundlePath.length()));
		}
		return result;
	}

}