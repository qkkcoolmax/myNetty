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
 * ���������ʼ��http�����handler��
 * 
 * ֱ��ʹ����netty�Ա�дweb�������ṩ��֧�֡�
 * 
 * */

public class HttpRequestHandler extends
		SimpleChannelInboundHandler<FullHttpRequest> {

	// websocket��ʶ
	private final String wsUri;

	public HttpRequestHandler(String wsUri) {
		this.wsUri = wsUri;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg)
			throws Exception {
		// �����websocket���������ַuri����wsuri
		System.out.println(msg.getUri());
		System.out.println(msg.toString());

		if (wsUri.equalsIgnoreCase(msg.getUri())) {

			// ����Ϣת������һ��ChannelHandler
			System.out.println("��websocketС���Ѿ�ת������һ��handler");
			ctx.fireChannelRead(msg.retain());// ʹ��retain�����ɡ�read����������SimHandlerĬ�ϻ��ͷ�msg����������Щ�����ֶ����첽�ģ����Կ���
												// ֮��ִ�е�ʱ��msg�Ѿ����ͷŵ��ˡ�
		} else {// �������websocket����
			if (HttpHeaders.is100ContinueExpected(msg)) {
				// ���HTTP����ͷ������Expect: 100-continue��
				// ����Ӧ����
				FullHttpResponse response = new DefaultFullHttpResponse(
						HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
				ctx.writeAndFlush(response);// ����writeandflush���Ѿ���response�ش�����
				// netty��װ��httpЭ�飬����ֱ��ʹ����Щ����Ϳ����ˡ�

			}
			// ��ȡindex.html��������Ӧ���ͻ���
			RandomAccessFile file = getStaticRes(msg.getUri());
			if (file == null) {
				return;
			}

			HttpResponse response = new DefaultHttpResponse(
					msg.getProtocolVersion(), HttpResponseStatus.OK);
			System.out.println("��ִ���˺ö��" + file.length());

			if (msg.getUri().equals("/")) {

				response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
						"text/html; charset=UTF-8");
			} else {
				response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
						"image/jpeg");

			}
			System.out.println(Thread.currentThread().getName());
			boolean keepAlive = HttpHeaders.isKeepAlive(msg);

			// ����Ӧ����
			if (keepAlive) {

				response.headers().set(HttpHeaders.Names.CONNECTION,
						HttpHeaders.Values.KEEP_ALIVE);
				response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
						file.length());
				// HttpHeaders.setTransferEncodingChunked(response);
				// response.headers().set(HttpHeaders.Names.TRANSFER_ENCODING,HttpHeaders.Values.CHUNKED);
			}

			ctx.write(response);// ���ﲻ��ʵ�ʵĴ��䣬���ǽ����ڹܵ������´���

			// �������https���� ��index.html����д��ͨ��
			ChannelFuture sendFileFuture;

			if (ctx.pipeline().get(SslHandler.class) == null) {
				sendFileFuture = ctx.write(
						new DefaultFileRegion(file.getChannel(), 0, file
								.length()), ctx.newProgressivePromise());// д���ļ���
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
			// ��ʶ��Ӧ���ݽ�����ˢ��ͨ��
			ChannelFuture future = null;
			future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

			System.out.println("end2");

			if (!keepAlive) {
				// ���http���󲻻�Ծ���ر�http����
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
