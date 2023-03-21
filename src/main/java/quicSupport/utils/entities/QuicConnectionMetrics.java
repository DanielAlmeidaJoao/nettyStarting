package quicSupport.utils.entities;

import io.netty.incubator.codec.quic.QuicConnectionStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

@Getter
@Setter
@AllArgsConstructor
public class QuicConnectionMetrics {
    private InetSocketAddress dest;
    private long receivedAppBytes;
    private long receivedAppMessages;
    private long receivedControlBytes;
    private long receivedControlMessages;
    private int streamCount;
    private int createdStreamCount;

    private final boolean isIncoming;
    private final QuicConnectionStats stats;


}
