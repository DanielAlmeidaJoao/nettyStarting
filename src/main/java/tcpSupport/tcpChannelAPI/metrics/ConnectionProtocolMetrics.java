package tcpSupport.tcpChannelAPI.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ConnectionProtocolMetrics {
    @Setter
    private long receivedAppBytes, sentAppBytes, receivedControlBytes, sentControlBytes;
    @Setter
    private long receivedAppMessages, sentAppMessages, receivedControlMessages, sentControlMessages;

    private long sentKeepAliveMessages, receivedKeepAliveMessages;

    //private int streamCount;
    //private int createdStreamCount;

    private InetSocketAddress hostAddress;

    private String connectionId;
    private boolean isIncoming;

    private TransmissionType type;

    //final private QuicChannel futureQUICConStats;
}
