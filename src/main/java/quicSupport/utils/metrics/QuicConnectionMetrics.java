package quicSupport.utils.metrics;

import io.netty.incubator.codec.quic.QuicConnectionStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.InetSocketAddress;

@Getter
@Setter
@AllArgsConstructor
@ToString(exclude = "stats")
public class QuicConnectionMetrics{
    private InetSocketAddress dest;
    private long receivedAppBytes, sentAppBytes, receivedControlBytes, sentControlBytes;
    private long receivedAppMessages, sentAppMessages, receivedControlMessages, sentControlMessages;

    private long sentKeepAliveMessages, receivedKeepAliveMessages;

    private int streamCount;
    private int createdStreamCount;

    private boolean isIncoming;

    private QuicConnectionStats stats;
}
