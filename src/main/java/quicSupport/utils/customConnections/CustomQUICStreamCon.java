package quicSupport.utils.customConnections;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.streamUtils.BabelOutputStream;

public class CustomQUICStreamCon {

    public final QuicStreamChannel streamChannel;
    public final String customStreamId;
    public final TransmissionType type;
    public final BabelOutputStream babelOutputStream;

    public CustomQUICConnection customQUICConnection;
    public final boolean inConnection;

    public CustomQUICStreamCon(QuicStreamChannel streamChannel, String customStreamId, TransmissionType type, CustomQUICConnection customQUICConnection, boolean inConnection, BabelOutputStream babelOutputStream) {
        this.streamChannel = streamChannel;
        this.customStreamId = customStreamId;
        this.type = type;
        this.customQUICConnection = customQUICConnection;
        this.inConnection = inConnection;
        this.babelOutputStream = babelOutputStream;
    }
    public void close(){
        streamChannel.disconnect();
        streamChannel.close();
    }
}
