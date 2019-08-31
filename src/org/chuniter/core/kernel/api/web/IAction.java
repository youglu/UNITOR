package org.chuniter.core.kernel.api.web;

import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.kernelunit.StateCode;

public interface IAction extends Cloneable{
	 
	String SUCCESS = StateCode.SUCCESS;
	String FAILD = StateCode.ERROR;
	Integer NOLOGIN = 2;
	Integer SERVEREXCEPTION = 3;
	Integer USERNAMEERROR = 4;
	Integer PASSWORDERROR = 5;
	Integer VALIDCODEERROR = 6;
	Integer NOTHING = 5; 
	
	String DATA = "data";
	String COUNT = "count";
	String PERMISSION = "permission";
	String STATECODE = "statecode";
	String MNAME = "mname";
	String DEFAULTSOLUTION = "默认方案";
	String ESES = "eses";
	
	/*MultipartConfig所有的属性都是可选的，具体属性如下： 
	fileSizeThreshold int 当数据量大于该值时，内容将被写入文件。 
	location String 存放生成的文件地址。 
	maxFileSize long 允许上传的文件最大值。默认值为 -1，表示没有限制。 
	maxRequestSize long 针对该 multipart/form-data 请求的最大数量，默认值为 -1，表示没有限制。 
	1、location属性，既是保存路径(在写入的时候，可以忽略路径设定)，又是上传过程中临时文件的保存路径，一旦执行write方法之后，临时文件将被自动清除。 
	2、上传过程中无论是单个文件超过maxFileSize值，或者上传总的数据量大于maxRequestSize值都会抛出IllegalStateException异常*/
	String location = "/";
	long maxFileSize = 104857600;
	long maxRequestSize = 104857600;
	int fileSizeThreshold = 2621440; 
	 
	public final static String ANDROID="Android";
	public final static  String IPHONE = "iPhone";
	public final static  String IPOD =  "iPod";
	public final static  String IPAD = "iPad";
	public final static  String WINP = "Windows Phone";
	public final static  String MQQ = "MQQBrowser"; 

	
	public String find() throws Exception;

	public String create() throws Exception;

	public String update() throws Exception;

	public String delete() throws Exception;
	
	public String downloadFile() throws Exception;
	
	IGeneralService<?> getGService();
}
