package tcpSupport.tcpChannelAPI.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

@Getter
@Setter
@AllArgsConstructor
public class ConnectionProtocolMetrics {
    private InetSocketAddress dest;
    private long receivedAppBytes, sentAppBytes, receivedControlBytes, sentControlBytes;
    private long receivedAppMessages, sentAppMessages, receivedControlMessages, sentControlMessages;

    private long sentKeepAliveMessages, receivedKeepAliveMessages;

    private int streamCount;
    private int createdStreamCount;

    private boolean isIncoming;



}
