package webSocketChat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebSocketHandler extends
		SimpleChannelInboundHandler<TextWebSocketFrame> {

	private final ChannelGroup group;// �������ӵ�����˵�channel��
	private final UserMap map;

	public WebSocketHandler(ChannelGroup group, UserMap map) {
		this.group = group;
		this.map = map;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		// ���WebSocket�������
		if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
			// ɾ��ChannelPipeline�е�HttpRequestHandler
			ctx.pipeline().remove(HttpRequestHandler.class);
			// дһ����Ϣ��ChannelGroup
			group.writeAndFlush(new TextWebSocketFrame("�û�: "
					+ ctx.channel().remoteAddress() + " �����˽�����������"));
			// ��Channel��ӵ�ChannelGroup
			group.add(ctx.channel());// �����û���ӵ�channelGroup�С�
			map.put(ctx.channel().remoteAddress().toString(), ctx.channel());
			notifyCurrentUsers();

		} else {
			super.userEventTriggered(ctx, evt); // ����δ��������ɴ��ݸ�����һ��handler�еı�������
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx,
			TextWebSocketFrame msg) throws Exception {
		// �����յ���Ϣͨ��ChannelGroupת�������������ӵĿͻ���
		Channel incoming = ctx.channel();

		for (Channel channel : group) {
			if (channel != incoming) {
				channel.writeAndFlush(new TextWebSocketFrame("�û�["
						+ incoming.remoteAddress() + "]˵: " + msg.text()));
			} else {
				channel.writeAndFlush(new TextWebSocketFrame("��˵:" + " "
						+ msg.text()));
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		Channel incoming = ctx.channel();
		System.out.println("Client:" + incoming.remoteAddress() + "�쳣");
		// �������쳣�͹ر�����
		cause.printStackTrace();
		ctx.close();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception { // (5)
		Channel incoming = ctx.channel();
		System.out.println("Client:" + incoming.remoteAddress() + "����");
	}

	private void notifyCurrentUsers() {
		StringBuilder users = new StringBuilder();
		users.append(ProtocolString.USER_LOGIN);
		users.append(map.size());
		users.append(ProtocolString.USER_LOGIN);
		for (String user : map.getMap().keySet()) {
			users.append(user + ProtocolString.USER_LOGIN);
		}
		// �����������µ��������
		group.writeAndFlush(new TextWebSocketFrame(users.toString()));
		/*
		 * for (Channel channel : group) { channel.writeAndFlush(new
		 * TextWebSocketFrame(users.toString())); }
		 */
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception { // (6)
		Channel incoming = ctx.channel();
		System.out.println("Client:" + incoming.remoteAddress() + "����");
		// �����ߵĴ�map��group��ȥ��
		group.remove(incoming);
		map.remove(incoming.remoteAddress().toString());
		notifyCurrentUsers();
	}
}