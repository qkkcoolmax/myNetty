package memcachedClient;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;

public class MemcachedRequestEncoderTest {
	@Test
	public void testMemcachedRequestEncoder() {

		MemcachedRequest request = new MemcachedRequest(Opcode.SET, "key1",
				"value1");

		EmbeddedChannel channel = new EmbeddedChannel(
				new MemcachedRequestEncoder());

		Assert.assertTrue(channel.writeOutbound(request));

		ByteBuf encoded = (ByteBuf) channel.readOutbound();

		Assert.assertNotNull(encoded);

		Assert.assertEquals(request.magic(), encoded.readByte() & 0xFF);

		Assert.assertEquals(request.opCode(), encoded.readByte() & 0xFF);

		Assert.assertEquals(4, encoded.readShort());

		Assert.assertEquals((byte) 0x08, encoded.readByte() & 0xFF);

		Assert.assertEquals((byte) 0, encoded.readByte() & 0xFF);

		Assert.assertEquals(0, encoded.readShort());
		//ע�ⷢ�Ͷ˷���ʲôλģʽ�������ʲôλģʽ��
		//��ʵ����ҲҪע�⣬������ĸպ���0x8000����������ҲҪע����ŵ����⡣��ҪҪŪ��java����ô��λ�ͽضϣ�Ȼ��֤����λģʽ��Ҫ����⡣
		
		Assert.assertEquals(4 + 6 + 8, encoded.readInt());

		Assert.assertEquals(request.id(), encoded.readInt());

		Assert.assertEquals(request.cas(), encoded.readLong());

		Assert.assertEquals(request.flags(), encoded.readInt());

		Assert.assertEquals(request.expires(), encoded.readInt());

		byte[] data = new byte[encoded.readableBytes()];

		encoded.readBytes(data);
		
		Assert.assertEquals(
				(request.key() + request.body()).getBytes(CharsetUtil.UTF_8),
				data);

		Assert.assertFalse(encoded.isReadable());

		Assert.assertFalse(channel.finish());

		Assert.assertNull(channel.readInbound());
	}
}
