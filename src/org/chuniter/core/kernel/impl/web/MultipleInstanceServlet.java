package org.chuniter.core.kernel.impl.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.impl.service.UBaseService;

@MultipartConfig
public class MultipleInstanceServlet extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7365096900714577980L;	
	public BaseServlet bac = null;	
	protected Log log = LogFactory.getLog(MultipleInstanceServlet.class);
	
	public MultipleInstanceServlet(){ }
	public MultipleInstanceServlet(BaseServlet exeAc){
		this.bac = exeAc;
	}
	@Override
	protected void service(HttpServletRequest arg0, HttpServletResponse arg1)
			throws ServletException, IOException {
		//Calendar c = Calendar.getInstance();
		//c.setTime(new Date());
		//long startTime = c.getTimeInMillis();
		BaseServlet exeAc = bac.createAC(); 
		exeAc.ucache = bac.ucache;
		exeAc.init(this.getServletConfig()); 
		//扫描服务
		//KernelActivitor.fetchService2(exeAc); 
		if(null == exeAc.gservice)
			exeAc.gservice = UBaseService.getInstance();
		exeAc.service(arg0, arg1);
		//c.setTime(new Date());
		//long endTime = c.getTimeInMillis();
		//log.debug("--处理总时长为："+(endTime-startTime)+"毫秒");
	}
}
