package quicSupport.utils.metrics;

import io.netty.incubator.codec.quic.QuicConnectionStats;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

//EVERY STREAM HAS THIS OBJECT????
public class QuicChannelMetrics {
    private static final Logger logger = LogManager.getLogger(QuicChannelMetrics.class);

    private final InetSocketAddress self;

    @Getter
    private final Map<SocketAddress,QuicConnectionMetrics> currentConnections;

    @Getter
    private final List<QuicConnectionMetrics> oldConnections;
    //private Map<InetSocketAddress, QuicConnectionMetrics> metricsMap;

    public QuicChannelMetrics(InetSocketAddress host){
        self=host;
        currentConnections=new HashMap<>();
        oldConnections=new LinkedList<>();
        logger.info("{} IS GOING TO REGISTER METRICS.",host);
    }

    public void initConnectionMetrics(SocketAddress connectionId){
        currentConnections.put(connectionId,new QuicConnectionMetrics(
                null,0,0,0,0,
                0,0,
                0,0,0,0,0,0,false,null
        ));
    }
    public void updateConnectionMetrics(SocketAddress connectionId, InetSocketAddress dest, QuicConnectionStats stats, boolean incoming){
        QuicConnectionMetrics m = currentConnections.get(connectionId);
        m.setIncoming(incoming);
        m.setDest(dest);
        m.setStats(stats);
        logger.info("{} TO {} METRICS ENABLED.",self,dest);
    }
    public void onConnectionClosed(SocketAddress connectionId){
        oldConnections.add(currentConnections.remove(connectionId));
    }
    public QuicConnectionMetrics getConnectionMetrics(SocketAddress peer){
        return currentConnections.get(peer);
    }
}
