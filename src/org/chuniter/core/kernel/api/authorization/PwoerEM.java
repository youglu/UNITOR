package org.chuniter.core.kernel.api.authorization;

public enum PwoerEM {

	ENTER("进入"),CREATE("新增"),UPDATE("修改"),DELETE("删除"),FIND("查询"),DETAIL("详情"),
	REPORT("报表"),PRINT("打印"),EXPORT("导出"),IMPORT("导入"),UPLOAD("上传"),DOWNLOAD("下载")
	,AUDIT("审核"),ENABLE("启动"),DISABLE("停动");
	private String description;
	private PwoerEM(String description){
		this.description = description;
	}
	public String getDescription(){
		return this.description;
	}
	
	public static void main(String[] args){
		for(PwoerEM pem:values()){
			System.out.println(pem.getDescription()+"  "+pem.toString());
		}
	}
}
