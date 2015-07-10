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
		//注意发送端发的什么位模式这里就收什么位模式。
		//其实这里也要注意，如果传的刚好是0x8000，解析出来也要注意符号的问题。主要要弄清java是怎么补位和截断，然后保证传的位模式不要被误解。
		
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
