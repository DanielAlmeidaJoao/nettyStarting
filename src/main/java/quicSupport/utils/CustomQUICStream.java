package quicSupport.utils;

import io.netty.incubator.codec.quic.QuicStreamChannel;

public class CustomQUICStream {

    public final QuicStreamChannel streamChannel;
    public final String customStreamId;

    public CustomQUICStream(QuicStreamChannel streamChannel, String customStreamId) {
        this.streamChannel = streamChannel;
        this.customStreamId = customStreamId;
    }
}
