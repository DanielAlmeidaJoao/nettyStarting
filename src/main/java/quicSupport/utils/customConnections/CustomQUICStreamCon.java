package quicSupport.utils.customConnections;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.utils.BabelInputStream;

public class CustomQUICStreamCon {

    public final QuicStreamChannel streamChannel;
    public final String customStreamId;
    public final TransmissionType type;
    public CustomQUICConnection customParentConnection;
    public final boolean inConnection;
    public final BabelInputStream inputStream;

    public CustomQUICStreamCon(QuicStreamChannel streamChannel, String customStreamId, TransmissionType type, CustomQUICConnection customParentConnection, boolean inConnection, BabelInputStream babelInputStream) {
        this.streamChannel = streamChannel;
        this.customStreamId = customStreamId;
        this.type = type;
        this.customParentConnection = customParentConnection;
        this.inConnection = inConnection;
        this.inputStream = babelInputStream;
    }
    public void close(){
        streamChannel.disconnect();
        streamChannel.close();
    }
}
