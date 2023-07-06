package quicSupport.utils.streamUtils;

import io.netty.buffer.ByteBuf;

public class BabelInBytesWrapper {

    public final ByteBuf byteBuf;
    public final int availableBytes;
    public BabelInBytesWrapper(ByteBuf buf){
        this.byteBuf = buf;
        this.availableBytes = buf.readableBytes();
    }
}
