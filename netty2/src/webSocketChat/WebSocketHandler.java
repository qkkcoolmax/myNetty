package webSocketChat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebSocketHandler extends
		SimpleChannelInboundHandler<TextWebSocketFrame> {

	private final ChannelGroup group;// 所有连接到服务端的channel。
	private final UserMap map;

	public WebSocketHandler(ChannelGroup group, UserMap map) {
		this.group = group;
		this.map = map;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		// 如果WebSocket握手完成
		if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
			// 删除ChannelPipeline中的HttpRequestHandler
			ctx.pipeline().remove(HttpRequestHandler.class);
			// 写一个消息到ChannelGroup
			group.writeAndFlush(new TextWebSocketFrame("用户: "
					+ ctx.channel().remoteAddress() + " 加入了进入了聊天室"));
			// 将Channel添加到ChannelGroup
			group.add(ctx.channel());// 将该用户添加到channelGroup中。
			map.put(ctx.channel().remoteAddress().toString(), ctx.channel());
			notifyCurrentUsers();

		} else {
			super.userEventTriggered(ctx, evt); // 握手未完成则依旧传递给了下一个handler中的本方法。
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx,
			TextWebSocketFrame msg) throws Exception {
		// 将接收的消息通过ChannelGroup转发到所以已连接的客户端
		Channel incoming = ctx.channel();

		for (Channel channel : group) {
			if (channel != incoming) {
				channel.writeAndFlush(new TextWebSocketFrame("用户["
						+ incoming.remoteAddress() + "]说: " + msg.text()));
			} else {
				channel.writeAndFlush(new TextWebSocketFrame("我说:" + " "
						+ msg.text()));
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		Channel incoming = ctx.channel();
		System.out.println("Client:" + incoming.remoteAddress() + "异常");
		// 当出现异常就关闭连接
		cause.printStackTrace();
		ctx.close();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception { // (5)
		Channel incoming = ctx.channel();
		System.out.println("Client:" + incoming.remoteAddress() + "在线");
	}

	private void notifyCurrentUsers() {
		StringBuilder users = new StringBuilder();
		users.append(ProtocolString.USER_LOGIN);
		users.append(map.size());
		users.append(ProtocolString.USER_LOGIN);
		for (String user : map.getMap().keySet()) {
			users.append(user + ProtocolString.USER_LOGIN);
		}
		// 告诉所有人新的在线情况
		group.writeAndFlush(new TextWebSocketFrame(users.toString()));
		/*
		 * for (Channel channel : group) { channel.writeAndFlush(new
		 * TextWebSocketFrame(users.toString())); }
		 */
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception { // (6)
		Channel incoming = ctx.channel();
		System.out.println("Client:" + incoming.remoteAddress() + "掉线");
		// 将掉线的从map和group中去掉
		group.remove(incoming);
		map.remove(incoming.remoteAddress().toString());
		notifyCurrentUsers();
	}
}