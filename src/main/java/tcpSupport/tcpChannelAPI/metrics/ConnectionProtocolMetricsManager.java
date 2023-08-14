package tcpSupport.tcpChannelAPI.metrics;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//EVERY STREAM HAS THIS OBJECT????
public class ConnectionProtocolMetricsManager {
    private static final Logger logger = LogManager.getLogger(ConnectionProtocolMetricsManager.class);

    private final InetSocketAddress self;

    @Getter
    private final Map<String, ConnectionProtocolMetrics> currentConnections;
    private final ModelMapper modelMapper;


    @Getter
    private final Queue<ConnectionProtocolMetrics> oldConnections;
    //private Map<InetSocketAddress, QuicConnectionMetrics> metricsMap;

    public ConnectionProtocolMetricsManager(InetSocketAddress host, boolean singleThreaded){
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

    public void initConnectionMetrics(String connectionId, InetSocketAddress dest, boolean incoming, int len){
        logger.info("SELF: {}. METRICS TO {} ADDED.",self,connectionId);
        ConnectionProtocolMetrics m = new ConnectionProtocolMetrics(
                dest,0,0,0,0,
                0,0,
                0,0,0,0,false,connectionId
        );
        m.setIncoming(incoming);
        currentConnections.put(connectionId,m);
        if(incoming){
            m.setReceivedControlBytes(len);
            m.setReceivedControlMessages(1);
        }else{
            m.setSentControlBytes(len);
            m.setSentControlMessages(1);
        }
    }
    /**
    public void updateConnectionMetrics(SocketAddress connectionId, InetSocketAddress dest,boolean incoming){
        ConnectionProtocolMetrics m = currentConnections.get(connectionId);
        m.setIncoming(incoming);
        m.setDest(dest);
        logger.info("{} TO {} METRICS ENABLED.",self,dest);
    }
     **/
    public void onConnectionClosed(String connectionId){
        oldConnections.add(currentConnections.remove(connectionId));
    }
    public ConnectionProtocolMetrics getConnectionMetrics(String conId){
        return currentConnections.get(conId);
    }

    private ConnectionProtocolMetrics cloneChannelMetric(ConnectionProtocolMetrics chanMetrics){
        return modelMapper.map(chanMetrics, ConnectionProtocolMetrics.class);
    }
    public List<ConnectionProtocolMetrics> oldMetrics(){
        var copy = new LinkedList<ConnectionProtocolMetrics>();
        for (ConnectionProtocolMetrics oldConnection : oldConnections) {
            copy.add(cloneChannelMetric(oldConnection));
        }
        return copy;
    }
    public List<ConnectionProtocolMetrics> currentMetrics(){
        var copy = new LinkedList<ConnectionProtocolMetrics>();
        for (ConnectionProtocolMetrics value : currentConnections.values()) {
            copy.add(cloneChannelMetric(value));
        }
        return copy;
    }

    public void calcMetricsOnReceived(String conId,long bytes){
        ConnectionProtocolMetrics metrics1 = getConnectionMetrics(conId);
        if(metrics1==null){
            return;
        }
        metrics1.setReceivedAppMessages(metrics1.getReceivedAppMessages()+1);
        metrics1.setReceivedAppBytes(metrics1.getReceivedAppBytes()+bytes);
    }
    public void calcMetricsOnSend(boolean success, String connectionId, long length){
        if(success){
            ConnectionProtocolMetrics metrics1 = getConnectionMetrics(connectionId);
            if(metrics1==null){
                return;
            }
            metrics1.setSentAppBytes(metrics1.getSentAppBytes()+length);
            metrics1.setSentAppMessages(metrics1.getSentAppMessages()+1);
        }
    }
    public void calcControlMetricsOnReceived(String conId,long bytes){
        ConnectionProtocolMetrics metrics1 = getConnectionMetrics(conId);
        if(metrics1==null){
            return;
        }
        metrics1.setReceivedControlMessages(metrics1.getReceivedControlMessages()+1);
        metrics1.setReceivedControlBytes(metrics1.getReceivedControlBytes()+bytes);
    }
    public void calcControlMetricsOnSend(boolean success, String connectionId, long length){
        if(success){
            ConnectionProtocolMetrics metrics1 = getConnectionMetrics(connectionId);
            if(metrics1==null){
                return;
            }
            metrics1.setSentControlMessages(metrics1.getSentControlMessages()+length);
            metrics1.setSentControlBytes(metrics1.getSentControlBytes()+1);
        }
    }
}
