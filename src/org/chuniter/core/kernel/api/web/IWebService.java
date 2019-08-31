package org.chuniter.core.kernel.api.web;

import javax.servlet.Servlet;

public interface IWebService {

	void addServlet(Servlet servlet);
	void updateServlet(Servlet servlet);
	void deleteServlet(String path);
}
