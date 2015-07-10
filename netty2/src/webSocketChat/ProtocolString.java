package webSocketChat;

public final class ProtocolString {

	//定义协议字符串的长度
		final static int PROTOCOL_LEN = 2;
		//下面是一些协议字符串，服务器和客户端交换的信息
		//都应该在前、后添加这种特殊字符串。
		
		
		final static String MSG_ROUND = "§γ";
		final static String USER_LOGIN = "∏∑";
		final static String LOGIN_SUCCESS = "1";
		final static String NAME_REP = "-1";
		final static String PRIVATE_ROUND = "★【";
		final static String SPLIT_SIGN = "※";
}
