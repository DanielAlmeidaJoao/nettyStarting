package org.streamingAPI.metrics;

import io.netty.incubator.codec.quic.QuicConnectionStats;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.streamingAPI.utils.FactoryMethods;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//EVERY STREAM HAS THIS OBJECT????
public class TCPStreamMetrics {
    private static final Logger logger = LogManager.getLogger(TCPStreamMetrics.class);

    private final InetSocketAddress self;

    @Getter
    private final Map<SocketAddress, TCPStreamConnectionMetrics> currentConnections;
    private final ModelMapper modelMapper;


    @Getter
    private final Queue<TCPStreamConnectionMetrics> oldConnections;
    //private Map<InetSocketAddress, QuicConnectionMetrics> metricsMap;

    public TCPStreamMetrics(InetSocketAddress host, boolean singleThreaded){
        self=host;
        if(singleThreaded){
            logger.info("SINGLE THREADED METRICS ON!");
            currentConnections=new HashMap<>();
            oldConnections=new LinkedList<>();
        }else{
            logger.info("CONCURRENT METRICS ON!");
            currentConnections=new ConcurrentHashMap<>();
            oldConnections= new ConcurrentLinkedQueue<>();
        }
        modelMapper = new ModelMapper();
        logger.info("{} IS GOING TO REGISTER METRICS.",host);
    }

    public void initConnectionMetrics(SocketAddress connectionId){
        logger.info("SELF: {}. METRICS TO {} ADDED.",self,connectionId);
        currentConnections.put(connectionId,new TCPStreamConnectionMetrics(
                null,0,0,0,0,
                0,0,
                0,0,0,0,0,0,false
        ));
    }
    public void updateConnectionMetrics(SocketAddress connectionId, InetSocketAddress dest,boolean incoming){
        TCPStreamConnectionMetrics m = currentConnections.get(connectionId);
        m.setIncoming(incoming);
        m.setDest(dest);
        logger.info("{} TO {} METRICS ENABLED.",self,dest);
    }
    public void onConnectionClosed(SocketAddress connectionId){
        oldConnections.add(currentConnections.remove(connectionId));
    }
    public TCPStreamConnectionMetrics getConnectionMetrics(SocketAddress peer){
        return currentConnections.get(peer);
    }

    private TCPStreamConnectionMetrics cloneChannelMetric(TCPStreamConnectionMetrics chanMetrics){
        return modelMapper.map(chanMetrics,TCPStreamConnectionMetrics.class);
    }
    public List<TCPStreamConnectionMetrics> oldMetrics(){
        var copy = new LinkedList<TCPStreamConnectionMetrics>();
        for (TCPStreamConnectionMetrics oldConnection : oldConnections) {
            copy.add(cloneChannelMetric(oldConnection));
        }
        return copy;
    }
    public List<TCPStreamConnectionMetrics> currentMetrics(){
        var copy = new LinkedList<TCPStreamConnectionMetrics>();
        for (TCPStreamConnectionMetrics value : currentConnections.values()) {
            copy.add(cloneChannelMetric(value));
        }
        return copy;
    }
}
