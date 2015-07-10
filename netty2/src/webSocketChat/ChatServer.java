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
 * 基于netty的websocket接口的在线聊天室。
 * 
 **/
public class ChatServer {

	private final ChannelGroup group = new DefaultChannelGroup(
			ImmediateEventExecutor.INSTANCE); // 存储各个客户端的channel。
	
	private final UserMap map = new UserMap(new ConcurrentHashMap<String, Channel>());
	
	private final EventLoopGroup workerGroup = new NioEventLoopGroup();
	
	private Channel channel;

	public ChannelFuture start(InetSocketAddress address) {
		ServerBootstrap b = new ServerBootstrap();
		b.group(workerGroup).channel(NioServerSocketChannel.class)
				.childHandler(createInitializer(group, map));
		
		ChannelFuture f = b.bind(address).syncUninterruptibly();
		channel = f.channel();// 获得serversocket的Channel
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

		System.out.println("聊天服务器已经启动");
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			// 添加一个关闭钩子，钩子本质是一个线程，也就是在jvm关闭前，跑一下这个线程，再关闭（即在所有非守护进程结束之后执行的一个线程。）
			@Override
			public void run() {
				server.destroy();
			}
		});
		f.channel().closeFuture().syncUninterruptibly();// 主线程会一直阻塞在这里直到serverSocket对应的channel关闭。
	}

}
