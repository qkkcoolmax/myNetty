package webSocketChat;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * 
 * 
 * ����netty��websocket�ӿڵ����������ҡ�
 * 
 **/
public class ChatServer {

	private final ChannelGroup group = new DefaultChannelGroup(
			ImmediateEventExecutor.INSTANCE); // �洢�����ͻ��˵�channel��
	
	private final UserMap map = new UserMap(new ConcurrentHashMap<String, Channel>());
	
	private final EventLoopGroup workerGroup = new NioEventLoopGroup();
	
	private Channel channel;

	public ChannelFuture start(InetSocketAddress address) {
		ServerBootstrap b = new ServerBootstrap();
		b.group(workerGroup).channel(NioServerSocketChannel.class)
				.childHandler(createInitializer(group, map));
		
		ChannelFuture f = b.bind(address).syncUninterruptibly();
		channel = f.channel();// ���serversocket��Channel
		return f;
	}

	public void destroy() {
		if (channel != null)
			channel.close();
		group.close();
		workerGroup.shutdownGracefully();
	}

	protected ChannelInitializer<Channel> createInitializer(ChannelGroup group,
			UserMap map) {
		return new ChatServerInitializer(group, map);
	}
	public static void main(String[] args) {
		final ChatServer server = new ChatServer();
		ChannelFuture f = server.start(new InetSocketAddress("0.0.0.0", 60000));

		System.out.println("����������Ѿ�����");
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			// ���һ���رչ��ӣ����ӱ�����һ���̣߳�Ҳ������jvm�ر�ǰ����һ������̣߳��ٹرգ��������з��ػ����̽���֮��ִ�е�һ���̡߳���
			@Override
			public void run() {
				server.destroy();
			}
		});
		f.channel().closeFuture().syncUninterruptibly();// ���̻߳�һֱ����������ֱ��serverSocket��Ӧ��channel�رա�
	}

}
