package quicSupport.utils.entities;

import io.netty.incubator.codec.quic.QuicConnectionStats;
import lombok.AllArgsConstructor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//EVERY STREAM HAS THIS OBJECT????
public class QuicChannelMetrics {
    private final InetSocketAddress self;
    private List<QuicConnectionMetrics> currentConnections;
    private List<QuicConnectionMetrics> oldConnections;

    public QuicChannelMetrics(InetSocketAddress host){
        self=host;
        currentConnections=new LinkedList<>();
        oldConnections=new LinkedList<>();
    }
    private Map<InetSocketAddress, QuicConnectionMetrics> metricsMap;

    public void addConnectionMetrics(QuicConnectionMetrics connectionMetrics) throws Exception {
        if(metricsMap.put(connectionMetrics.getDest(),connectionMetrics)!=null){
            throw new Exception("TRYING TO REGISTER CONNECTION_METRICS TWICE");
        }
    }
    private void createConnectionMetrics(InetSocketAddress dest, QuicConnectionStats stats,int handshakeBytes, boolean incoming){
        metricsMap.put(dest,new QuicConnectionMetrics(
                dest,0,0,0,0
                ,0,0,incoming,stats
        ));
    }


}
