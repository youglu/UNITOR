package org.chuniter.core.kernel.api;



public interface IMessageListenerContianer {
	/**
	 * 注册到监听容器中，如果该SERVICE需要接收消息的话
	 */
	void regeditToMessageListenerContainer();
	/**
	 * 当整合了JMS消息服务时，每个服务类需要一个处理接收消息的方法
	 * @param msg
	 */
	//public void onMessage(Message msg);
}
