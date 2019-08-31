package org.chuniter.core.kernel.kernelunit;

public class StateCode {
	public static String SUCCESS = "1"; // 操作成功

	public static String NOPERMISSION = "您没有该操作的权限";

	public static String MISSING_PARAM = "缺少参数或某参数为NULL";

	public static String ERROR = "操作失败";

	public static String REGISTER = "帐户已被注册";

	public static String ACCOUNT_OR_PWD_INCORRECT = "用户名或密码错误";

	public static String PHONE_BIND_NOTEXISTS = "绑定手机不存在";

	public static String CODE_TIMEOUT = "验证码超时";

	public static String CODE_INCORRECT = "验证码不正确";

	public static String EMAIL_TIMEOUT = "邮件链接超时";

	public static String EMAIL_BIND_NOTEXISTS = "绑定邮箱不存在";

	public static String ACCOUNT_NOTEXISTS = "帐户不存在";

	public static String PHONE_BIND_REPEAT = "手机号重复绑定";

	public static String USER_NOTLOGIN = "用户未登陆";

	public static String INVITATIONCODE_NOTEXISTS = "邀请码不存在";

	public static String USER_SIGNO_ALREADY = "用户已签到";

	public static String MORE_THAN_MAX_RESUME_AMOUNT = "简历数量已达到上限";

	public static String IS_NOT_COMPANY_ACCOUNT = "非企业帐户不能操作";

	public static String IS_NOT_COMPANY_INFO = "企业不存在 ";

	public static String COLLECTION_ALREADY = "已收藏，无需重复操作";

	public static String NODATA = "无数据";

	public static String ACTIVECODEERROR = "激活码错误";

	public static String MESSAGE_SEND_FAIL = "消息发送失败";

	public static String INVALID_ADD_FRIEND = "不能添加自己为好友";

	public static String FRIEND_ADD_FAIL = "好友添加失败";

	public static String PHONE_FORM_ERROR = "手机号码格式不正确";

	public static String AUTHENTICATIONING = "认证中";

	public static String PRAISE_ALREADY = "已点赞";

	public static String ATTENTION_ALREADY = "已关注";
}
