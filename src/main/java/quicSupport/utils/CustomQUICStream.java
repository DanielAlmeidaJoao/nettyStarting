package quicSupport.utils;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.utils.enums.TransmissionType;

public class CustomQUICStream {

    public final QuicStreamChannel streamChannel;
    public final String customStreamId;
    public final TransmissionType type;

    public CustomQUICStream(QuicStreamChannel streamChannel, String customStreamId, TransmissionType type) {
        this.streamChannel = streamChannel;
        this.customStreamId = customStreamId;
        this.type = type;
    }
}
