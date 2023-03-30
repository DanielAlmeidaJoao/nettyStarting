package quicSupport.utils.metrics;

import io.netty.incubator.codec.quic.QuicConnectionStats;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.streamingAPI.metrics.TCPStreamConnectionMetrics;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//EVERY STREAM HAS THIS OBJECT????
public class QuicChannelMetrics {
    private static final Logger logger = LogManager.getLogger(QuicChannelMetrics.class);

    private final InetSocketAddress self;
    private final ModelMapper modelMapper;
    @Getter
    private final Map<SocketAddress,QuicConnectionMetrics> currentConnections;

    @Getter
    private final Queue<QuicConnectionMetrics> oldConnections;
    //private Map<InetSocketAddress, QuicConnectionMetrics> metricsMap;

    public QuicChannelMetrics(InetSocketAddress host, boolean singleThreaded){
        self=host;
        if(singleThreaded){
            currentConnections=new HashMap<>();
            oldConnections=new LinkedList<>();
        }else{
            currentConnections=new ConcurrentHashMap<>();
            oldConnections=new ConcurrentLinkedQueue<>();
        }

        modelMapper = new ModelMapper();
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

    private QuicConnectionMetrics cloneChannelMetric(QuicConnectionMetrics chanMetrics){
        return modelMapper.map(chanMetrics,QuicConnectionMetrics.class);
    }
    public List<QuicConnectionMetrics> oldMetrics(){
        var copy = new LinkedList<QuicConnectionMetrics>();
        for (QuicConnectionMetrics oldConnection : oldConnections) {
            copy.add(cloneChannelMetric(oldConnection));
        }
        return copy;
    }
    public List<QuicConnectionMetrics> currentMetrics(){
        var copy = new LinkedList<QuicConnectionMetrics>();
        for (QuicConnectionMetrics value : currentConnections.values()) {
            copy.add(cloneChannelMetric(value));
        }
        return copy;
    }
}
