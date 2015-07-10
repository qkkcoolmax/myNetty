package memcachedClient;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

public class MemcachedResponseDecoder extends ByteToMessageDecoder {
	private enum State {
		Header, Body
	}

	private State state = State.Header;
	private int totalBodySize;
	private byte magic;
	private byte opCode;
	private short keyLength;
	private byte extraLength;
	private byte dataType;
	private short status;
	private int id;
	private long cas;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) {

		switch (state) {
		case Header:
			//发生读事件时，可能读进来的并不是完整的一个协议报文，如果小于24byte，那么连头都读不完，所以这里直接return。等待下一次触发
			//可以看到ByteBuf应该是捆绑在channel上面的，所以下一次可以继续往里面添加。
			if (in.readableBytes() < 24) {
				return;// response header is 24 bytes #4
			}
			// read header #5
			magic = in.readByte();
			opCode = in.readByte();
			keyLength = in.readShort();
			extraLength = in.readByte();
			dataType = in.readByte();
			status = in.readShort();
			totalBodySize = in.readInt();
			id = in.readInt(); // referred to in the protocol spec as opaque
			cas = in.readLong();
			state = State.Body;//头部读完了，
			// fallthrough and start to read the body
		case Body:
			if (in.readableBytes() < totalBodySize) {
				return; // until we have the entire payload return #6
			}
			int flags = 0,
			expires = 0;
			int actualBodySize = totalBodySize;
			if (extraLength > 0) {
				flags = in.readInt();
				actualBodySize -= 4;
			}
			if (extraLength > 4) {
				expires = in.readInt();
				actualBodySize -= 4;
			}
			String key = "";
			if (keyLength > 0) {
				ByteBuf keyBytes = in.readBytes(keyLength);
				key = keyBytes.toString(CharsetUtil.UTF_8);
				actualBodySize -= keyLength;
			}
			ByteBuf body = in.readBytes(actualBodySize);
			String data = body.toString(CharsetUtil.UTF_8);
			out.add(new MemcachedResponse(magic, opCode, dataType, status, id,
					cas, flags, expires, key, data));
			state = State.Header;
		}
		/**
		 * 可以看到每次有数据都会触发这个handler，但是一次触发数据可能不够，我们通过对bytebuf作用数据的长度来检查，可以知道一个报文是否发送完毕，
		 * 如果完毕，我们就开始严格按照协议开始解析。解析完一个报文将其构造成了一个对象以后，就将handler的状态位置回header，也就是说下次事件触发，肯定是头部，我们要先解析头部
		 * 每次handler只解析頭部或者body，交替進行
		 * 每個channel都有自己的handler對象，所以不會發生多線程下的問題，一個handler對象中的狀態只會由一個同一個handler触发的事件串行的使用。
		 * 
		 * */
	}
}
