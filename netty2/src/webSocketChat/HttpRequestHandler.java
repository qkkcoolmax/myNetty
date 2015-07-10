package webSocketChat;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

/**
 * 
 * 用来处理初始的http请求的handler。
 * 
 * 直接使用了netty对编写web服务器提供的支持。
 * 
 * */

public class HttpRequestHandler extends
		SimpleChannelInboundHandler<FullHttpRequest> {

	// websocket标识
	private final String wsUri;

	public HttpRequestHandler(String wsUri) {
		this.wsUri = wsUri;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg)
			throws Exception {
		// 如果是websocket请求，请求地址uri等于wsuri
		System.out.println(msg.getUri());
		System.out.println(msg.toString());

		if (wsUri.equalsIgnoreCase(msg.getUri())) {

			// 将消息转发到下一个ChannelHandler
			System.out.println("是websocket小心已经转发到下一个handler");
			ctx.fireChannelRead(msg.retain());// 使用retain的理由。read方法结束后SimHandler默认会释放msg，又由于这些方法又都是异步的，所以可能
												// 之后执行的时候msg已经被释放掉了。
		} else {// 如果不是websocket请求
			if (HttpHeaders.is100ContinueExpected(msg)) {
				// 如果HTTP请求头部包含Expect: 100-continue，
				// 则响应请求
				FullHttpResponse response = new DefaultFullHttpResponse(
						HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
				ctx.writeAndFlush(response);// 这里writeandflush就已经将response回传给了
				// netty封装了http协议，这里直接使用这些对象就可以了。

			}
			// 获取index.html的内容响应给客户端
			RandomAccessFile file = getStaticRes(msg.getUri());
			if (file == null) {
				return;
			}

			HttpResponse response = new DefaultHttpResponse(
					msg.getProtocolVersion(), HttpResponseStatus.OK);
			System.out.println("我执行了好多次" + file.length());

			if (msg.getUri().equals("/")) {

				response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
						"text/html; charset=UTF-8");
			} else {
				response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
						"image/jpeg");

			}
			System.out.println(Thread.currentThread().getName());
			boolean keepAlive = HttpHeaders.isKeepAlive(msg);

			// 并响应请求
			if (keepAlive) {

				response.headers().set(HttpHeaders.Names.CONNECTION,
						HttpHeaders.Values.KEEP_ALIVE);
				response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
						file.length());
				// HttpHeaders.setTransferEncodingChunked(response);
				// response.headers().set(HttpHeaders.Names.TRANSFER_ENCODING,HttpHeaders.Values.CHUNKED);
			}

			ctx.write(response);// 这里不有实际的传输，而是将其在管道中向下传递

			// 如果不是https请求， 将index.html内容写入通道
			ChannelFuture sendFileFuture;

			if (ctx.pipeline().get(SslHandler.class) == null) {
				sendFileFuture = ctx.write(
						new DefaultFileRegion(file.getChannel(), 0, file
								.length()), ctx.newProgressivePromise());// 写大文件。
			} else {
				sendFileFuture = ctx.write(
						new ChunkedNioFile(file.getChannel()),
						ctx.newProgressivePromise());

			}

			/*
			 * sendFileFuture.addListener(new ChannelProgressiveFutureListener()
			 * {
			 * 
			 * @Override public void operationProgressed(
			 * ChannelProgressiveFuture future, long progress, long total) { if
			 * (total < 0) { // total unknown
			 * System.err.println(future.channel() + " Transfer progress: " +
			 * progress); } else { System.err.println(future.channel() +
			 * " Transfer progress: " + progress + " / " + total);
			 * System.out.println(Thread.currentThread().getName()); } }
			 * 
			 * @Override public void operationComplete(ChannelProgressiveFuture
			 * future) { System.err.println(future.channel() +
			 * " Transfer complete.");
			 * System.out.println(Thread.currentThread().getName());
			 * 
			 * } });
			 */

			System.out.println("end");
			// 标识响应内容结束并刷新通道
			ChannelFuture future = null;
			future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

			System.out.println("end2");

			if (!keepAlive) {
				// 如果http请求不活跃，关闭http连接
				future.addListener(ChannelFutureListener.CLOSE);
			}

			file.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	private RandomAccessFile getStaticRes(String uri) {
		String path = null;

		try {
			if (uri.equals("/favicon.ico")) {
				return null;
			}

			if (uri.equals("/")) {
				path = "/webchat.html";
			} else {
				path = uri;
			}
			return new RandomAccessFile(System.getProperty("user.dir")
					+ "/webroot" + path, "r");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}
}
