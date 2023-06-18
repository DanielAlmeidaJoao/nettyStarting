package tcpSupport.tcpStreamingAPI.metrics;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//EVERY STREAM HAS THIS OBJECT????
public class TCPMetrics {
    private static final Logger logger = LogManager.getLogger(TCPMetrics.class);

    private final InetSocketAddress self;

    @Getter
    private final Map<SocketAddress, TCPSConnectionMetrics> currentConnections;
    private final ModelMapper modelMapper;


    @Getter
    private final Queue<TCPSConnectionMetrics> oldConnections;
    //private Map<InetSocketAddress, QuicConnectionMetrics> metricsMap;

    public TCPMetrics(InetSocketAddress host, boolean singleThreaded){
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
        currentConnections.put(connectionId,new TCPSConnectionMetrics(
                null,0,0,0,0,
                0,0,
                0,0,0,0,0,0,false
        ));
    }
    public void updateConnectionMetrics(SocketAddress connectionId, InetSocketAddress dest,boolean incoming){
        TCPSConnectionMetrics m = currentConnections.get(connectionId);
        m.setIncoming(incoming);
        m.setDest(dest);
        logger.info("{} TO {} METRICS ENABLED.",self,dest);
    }
    public void onConnectionClosed(SocketAddress connectionId){
        oldConnections.add(currentConnections.remove(connectionId));
    }
    public TCPSConnectionMetrics getConnectionMetrics(SocketAddress peer){
        return currentConnections.get(peer);
    }

    private TCPSConnectionMetrics cloneChannelMetric(TCPSConnectionMetrics chanMetrics){
        return modelMapper.map(chanMetrics, TCPSConnectionMetrics.class);
    }
    public List<TCPSConnectionMetrics> oldMetrics(){
        var copy = new LinkedList<TCPSConnectionMetrics>();
        for (TCPSConnectionMetrics oldConnection : oldConnections) {
            copy.add(cloneChannelMetric(oldConnection));
        }
        return copy;
    }
    public List<TCPSConnectionMetrics> currentMetrics(){
        var copy = new LinkedList<TCPSConnectionMetrics>();
        for (TCPSConnectionMetrics value : currentConnections.values()) {
            copy.add(cloneChannelMetric(value));
        }
        return copy;
    }
}
