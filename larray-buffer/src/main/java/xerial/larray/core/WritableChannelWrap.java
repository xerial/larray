package xerial.larray.core;

import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import static xerial.larray.core.UnsafeUtil.unsafe;

/**
 * @author Taro L. Saito
 */
class WritableChannelWrap implements WritableByteChannel {

    private final Buffer b;
    int cursor = 0;

    WritableChannelWrap(Buffer b) {
        this.b = b;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int len = (int) Math.max(src.limit() - src.position(), 0);
        int writeLen = 0;
        if(src.getClass().isAssignableFrom(DirectBuffer.class)) {
            DirectBuffer d = (DirectBuffer) src;
            unsafe.copyMemory(d.address() + src.position(), cursor, len);
            writeLen = len;
        }
        else if(src.hasArray()) {
            writeLen = b.readFrom(src.array(), src.position(), cursor, len);
        }
        else {
            for(long i=0; i<len; ++i)
                unsafe.putByte(b.data() + i, src.get((int) (src.position() + i)));
            writeLen = len;
        }
        cursor += writeLen;
        src.position(src.position() + writeLen);
        return writeLen;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
