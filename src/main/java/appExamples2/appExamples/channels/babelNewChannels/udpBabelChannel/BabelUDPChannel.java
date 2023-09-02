package appExamples2.appExamples.channels.babelNewChannels.udpBabelChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.messages.BytesToBabelMessage;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionDownEvent;
import pt.unl.fct.di.novasys.babel.channels.events.OnMessageConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetrics;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;
import udpSupport.channels.SingleThreadedUDPChannel;
import udpSupport.channels.UDPChannel;
import udpSupport.channels.UDPChannelHandlerMethods;
import udpSupport.channels.UDPChannelInterface;
import udpSupport.metrics.UDPNetworkStatsWrapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static tcpSupport.tcpChannelAPI.utils.TCPChannelUtils.METRICS_INTERVAL_KEY;

public class BabelUDPChannel implements NewIChannel, UDPChannelHandlerMethods {
    private static final Logger logger = LogManager.getLogger(BabelUDPChannel.class);
    public final boolean metrics;
    public final static String NAME = "BABEL_UDP_CHANNEL";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";

    private final boolean triggerSent;


    private final BabelMessageSerializer serializer;
    private final ChannelListener listener;
    private final UDPChannelInterface udpChannelInterface;
    private final Map<String,Host> customConIDToAddress;
    private final Map<Host,String> hostStringMap;

    public short ownerProto;

    public BabelUDPChannel(BabelMessageSerializer serializer, ChannelListener list, Properties properties, short ownerProto) throws IOException {
        //super(properties);
        this.serializer = serializer;
        BabelMessageSerializer aux = (BabelMessageSerializer) serializer;
        aux.registerProtoSerializer(BytesToBabelMessage.ID,BytesToBabelMessage.serializer);
        this.listener = list;
        if(properties.getProperty(FactoryMethods.SINGLE_THREADED_PROP)!=null){
            udpChannelInterface = new SingleThreadedUDPChannel(properties,this,serializer);
            customConIDToAddress = new HashMap<>();
            hostStringMap = new HashMap<>();
            System.out.println("UDP SINGLE THREADED");
        }else {
            udpChannelInterface = new UDPChannel(properties,false,this,serializer);
            customConIDToAddress = new ConcurrentHashMap<>();
            hostStringMap = new ConcurrentHashMap<>();
            System.out.println("UDP MULTITHREADED");
        }
        metrics = udpChannelInterface.metricsEnabled();
        if(metrics && properties.getProperty(METRICS_INTERVAL_KEY)!=null ){
            int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY));
            new DefaultEventExecutor().scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval, TimeUnit.SECONDS);
        }
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
        this.ownerProto = ownerProto;
    }
    void readMetricsMethod(List<UDPNetworkStatsWrapper> stats){
        listener.deliverEvent(new UDPMetricsEvent(stats));
    }
    void triggerMetricsEvent() {
        udpChannelInterface.readMetrics(stats -> readMetricsMethod(stats));
    }

    @Override
    public void onPeerDown(InetSocketAddress peer) {
        Host host = Host.toBabelHost(peer);
        String conId = hostStringMap.remove(host);
        if(conId!=null){
            customConIDToAddress.remove(conId);
            listener.deliverEvent(new OnConnectionDownEvent(host,new Throwable("PEER DISCONNECTED!"),conId,true,TransmissionType.STRUCTURED_MESSAGE));
        }
    }

    @Override
    public void onDeliverMessage(BabelMessage message, InetSocketAddress from) {
        //logger.info("MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",channelId,from,bytes.length);
        //logger.info("{}. MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",getSelf(),channelId,from,bytes.length);
        listener.deliverMessage(message,Host.toBabelHost(from),null);
    }

    @Override
    public void sendMessage(BabelMessage message, Host host, short proto) {
        udpChannelInterface.sendMessage(message,host.address);
    }

    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto) {
        BabelMessage babelMessage = new BabelMessage(new BytesToBabelMessage(data,dataLen),sourceProto,destProto);
        sendMessage(babelMessage,dest,sourceProto);
    }

    @Override
    public void sendMessage(BabelMessage msg, String connectionID, short proto) {
        if(connectionID ==null){
            listener.messageFailed(msg,null,new Throwable("UNKNOWN ID"),TransmissionType.STRUCTURED_MESSAGE);
            return;
        }
        Host host = customConIDToAddress.get(connectionID);
        sendMessage(msg,host,proto);
    }

    @Override
    public void sendMessage(byte[] data, int dataLen, String connectionID, short sourceProto, short destProto) {
        if(connectionID ==null){
            listener.messageFailed(null,null,new Throwable("UNKNOWN CONNECTION ID: "+ connectionID),TransmissionType.STRUCTURED_MESSAGE);
            return;
        }
        Host host = customConIDToAddress.get(connectionID);
        sendMessage(data,dataLen,host,sourceProto,destProto);
    }
    @Override
    public void onMessageSentHandler(boolean success, Throwable error, BabelMessage message, InetSocketAddress dest){
        if (triggerSent) listener.messageSent(message,Host.toBabelHost(dest),TransmissionType.STRUCTURED_MESSAGE);
    }

    @Override
    public void closeConnection(Host peer, short connection) {
        for (Map.Entry<String, Host> stringHostEntry : customConIDToAddress.entrySet()) {
            if(stringHostEntry.getValue().equals(peer)){
                customConIDToAddress.remove(stringHostEntry.getKey());
            }
        }
        hostStringMap.remove(peer);
        listener.deliverEvent(new OnConnectionDownEvent(peer,null, "",true,TransmissionType.STRUCTURED_MESSAGE));
    }

    @Override
    public boolean isConnected(Host peer) {
        return true;
    }

    @Override
    public boolean isConnected(String connectionID) {
        return customConIDToAddress.containsKey(connectionID);
    }

    @Override
    public String[] getConnectionsIds() {
        return customConIDToAddress.keySet().toArray(new String[customConIDToAddress.size()]);
    }

    @Override
    public InetSocketAddress[] getConnections() {
        return customConIDToAddress.values().toArray(new InetSocketAddress[customConIDToAddress.size()]);
    }

    @Override
    public int connectedPeers() {
        return customConIDToAddress.size();
    }

    @Override
    public boolean shutDownChannel(short protoId) {
        udpChannelInterface.shutDownServerClient();
        return true;
    }

    @Override
    public short getChannelProto() {
        return ownerProto;
    }

    @Override
    public List<ConnectionProtocolMetrics> activeConnectionsMetrics() {
        logger.warn("SUPPORTED ONLY BY QUIC AND TCP");
        return null;
    }

    @Override
    public List<ConnectionProtocolMetrics> closedConnectionsMetrics() {
        logger.warn("SUPPORTED ONLY BY QUIC AND TCP");
        return null;
    }

    @Override
    public List<UDPNetworkStatsWrapper> getUDPMetrics(){
        return udpChannelInterface.getMetrics();
    }
    @Override
    public NetworkProtocol getNetWorkProtocol() {
        return NetworkProtocol.UDP;
    }

    @Override
    public NetworkRole getNetworkRole() {
        return udpChannelInterface.getNetworkRole();
    }

    public String nextId(){
        return "udpchan"+ TCPChannelUtils.channelIdCounter.getAndIncrement();
    }

    @Override
    public String openMessageConnection(Host host, short proto,boolean always) {
        String oldID = hostStringMap.get(host);
        if(oldID!=null){
            return oldID;
        }
        logger.debug("OPEN CONNECTION. UNSUPPORTED OPERATION ON UDP");
        String id = nextId();
        hostStringMap.put(host,id);
        customConIDToAddress.put(id, host);
        listener.deliverEvent(new OnMessageConnectionUpEvent(host,id,false));
        return id;
    }

    @Override
    public String openStreamConnection(Host host, short sourceProto,short destProto, boolean always) {
        new Exception("UNSUPPORTED OPERATION").printStackTrace();
        return "";
    }

    @Override
    public TransmissionType getConnectionType(String connectionId) throws NoSuchElementException {
        return TransmissionType.STRUCTURED_MESSAGE;
    }

    @Override
    public void registerChannelInterest(short protoId) {

    }


    @Override
    public void closeConnection(String connectionID, short protoId) {
        customConIDToAddress.remove(connectionID);
    }
}
