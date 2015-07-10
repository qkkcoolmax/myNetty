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
			//�������¼�ʱ�����ܶ������Ĳ�����������һ��Э�鱨�ģ����С��24byte����ô��ͷ�������꣬��������ֱ��return���ȴ���һ�δ���
			//���Կ���ByteBufӦ����������channel����ģ�������һ�ο��Լ�����������ӡ�
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
			state = State.Body;//ͷ�������ˣ�
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
		 * ���Կ���ÿ�������ݶ��ᴥ�����handler������һ�δ������ݿ��ܲ���������ͨ����bytebuf�������ݵĳ�������飬����֪��һ�������Ƿ�����ϣ�
		 * �����ϣ����ǾͿ�ʼ�ϸ���Э�鿪ʼ������������һ�����Ľ��乹�����һ�������Ժ󣬾ͽ�handler��״̬λ�û�header��Ҳ����˵�´��¼��������϶���ͷ��������Ҫ�Ƚ���ͷ��
		 * ÿ��handlerֻ�����^������body�������M��
		 * ÿ��channel�����Լ���handler�������Բ����l���ྀ���µĆ��}��һ��handler�����еĠ�Bֻ����һ��ͬһ��handler�������¼����е�ʹ�á�
		 * 
		 * */
	}
}
