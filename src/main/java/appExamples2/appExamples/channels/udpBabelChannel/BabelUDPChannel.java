package appExamples2.appExamples.channels.udpBabelChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionDownEvent;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.StreamType;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;
import udpSupport.channels.SingleThreadedUDPChannel;
import udpSupport.channels.UDPChannel;
import udpSupport.channels.UDPChannelHandlerMethods;
import udpSupport.channels.UDPChannelInterface;
import udpSupport.metrics.ChannelStats;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BabelUDPChannel<T> implements NewIChannel<T>, UDPChannelHandlerMethods {
    private static final Logger logger = LogManager.getLogger(BabelUDPChannel.class);
    public final boolean metrics;
    public final static String NAME = "BABEL_UDP_CHANNEL";
    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String DEFAULT_METRICS_INTERVAL = "-1";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";

    private final boolean triggerSent;


    private final BabelMessageSerializerInterface<T> serializer;
    private final ChannelListener<T> listener;
    private final UDPChannelInterface udpChannelInterface;
    private final Map<String,Host> customConIDToAddress;
    public short ownerProto;

    public BabelUDPChannel(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties, short ownerProto) throws IOException {
        //super(properties);
        this.serializer = serializer;
        this.listener = list;
        if(properties.getProperty(FactoryMethods.SINGLE_THREADED_PROP)!=null){
            udpChannelInterface = new SingleThreadedUDPChannel(properties,this);
            customConIDToAddress = new HashMap<>();
        }else {
            udpChannelInterface = new UDPChannel(properties,false,this);
            customConIDToAddress = new ConcurrentHashMap<>();
        }
        metrics = udpChannelInterface.metricsEnabled();
        if(metrics){
            int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY, DEFAULT_METRICS_INTERVAL));
            new DefaultEventExecutor().scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval, TimeUnit.MILLISECONDS);
        }
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
        this.ownerProto = ownerProto;
    }
    void readMetricsMethod(ChannelStats stats){
        listener.deliverEvent(new UDPMetricsEvent(stats));
    }
    void triggerMetricsEvent() {
        udpChannelInterface.readMetrics(this::readMetricsMethod);
    }

    @Override
    public void onPeerDown(InetSocketAddress peer) {
       Host host = FactoryMethods.toBabelHost(peer);
        for (Map.Entry<String,Host> entry : customConIDToAddress.entrySet()) {
            if(entry.getValue().equals(host)){
                listener.deliverEvent(new OnConnectionDownEvent(host,new Throwable("PEER DISCONNECTED!"),entry.getKey(),true));
            }
            return;
        }
    }

    @Override
    public void onDeliverMessage(byte[] message, InetSocketAddress from) {
        //logger.info("MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",channelId,from,bytes.length);
        //logger.info("{}. MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",getSelf(),channelId,from,bytes.length);
        try {
            FactoryMethods.deserialize(message,serializer,listener,from,null);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(T msg, Host peer, short proto) {
        try {
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            udpChannelInterface.sendMessage(toSend,FactoryMethods.toInetSOcketAddress(peer),toSend.length);
            msgSent(toSend,peer);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto,short handlerId) {
        byte [] toSend = FactoryMethods.serializeWhenSendingBytes(sourceProto,destProto,handlerId,data,dataLen);
        udpChannelInterface.sendMessage(toSend,FactoryMethods.toInetSOcketAddress(dest),toSend.length);
    }

    @Override
    public void sendMessage(T msg,String streamId,short proto) {
        Host host = customConIDToAddress.get(streamId);
        sendMessage(msg,host,proto);
    }

    @Override
    public void sendMessage(byte[] data, int dataLen, String streamId, short sourceProto, short destProto,short handlerId) {
        Host host = customConIDToAddress.get(streamId);
        sendMessage(data,dataLen,host,sourceProto,destProto,handlerId);
    }

    @Override
    public void sendStream(byte[] stream,int len,String streamId, short proto) {
        unsupportedOperation();
    }

    @Override
    public void sendStream(byte[] msg,int len,Host host, short proto) {
        unsupportedOperation();
    }

    @Override
    public void sendStream(InputStream inputStream, int len,Host dest, short proto) {
        unsupportedOperation();
    }
    @Override
    public void sendStream(InputStream inputStream, int len,String conId, short proto) {
        unsupportedOperation();    }
    @Override
    public void sendStream(InputStream inputStream,Host dest, short proto) {
        unsupportedOperation();    }
    @Override
    public void sendStream(InputStream inputStream, String conId, short proto) {
        unsupportedOperation();
    }
    private void unsupportedOperation(){
        new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY QUIC AND TCP CHANNELS").printStackTrace();
    }
    @Override
    public void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest){

    }
    private void msgSent(byte[] message, Host host){
        try {
            if(triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,message,null,TransmissionType.STRUCTURED_MESSAGE,ownerProto),host, TransmissionType.STRUCTURED_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeConnection(Host peer, short connection) {
        for (Map.Entry<String, Host> stringHostEntry : customConIDToAddress.entrySet()) {
            if(stringHostEntry.getValue().equals(peer)){
                customConIDToAddress.remove(stringHostEntry.getKey());
            }
        }
        listener.deliverEvent(new OnConnectionDownEvent(peer,null, "",true));
    }

    @Override
    public boolean isConnected(Host peer) {
        return true;
    }

    @Override
    public String[] getLinks() {
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
    public NetworkProtocol getNetWorkProtocol() {
        return NetworkProtocol.UDP;
    }

    public String nextId(){
        return "udpchan"+ TCPStreamUtils.channelIdCounter.getAndIncrement();
    }

    @Override
    public String openMessageConnection(Host peer, short proto) {
        logger.debug("OPEN CONNECTION. UNSUPPORTED OPERATION ON UDP");
        String id = nextId();
        customConIDToAddress.put(id,peer);
        listener.deliverEvent(new OnConnectionDownEvent(peer,null,id,true));
        return id;
    }

    @Override
    public String openStreamConnection(Host var1, short protoId, StreamType streamType) {
        unsupportedOperation();
        return null;
    }

    @Override
    public TransmissionType getTransmissionType(Host host) throws NoSuchElementException {
        return TransmissionType.STRUCTURED_MESSAGE;
    }

    @Override
    public TransmissionType getTransmissionType(String streamId) throws NoSuchElementException {
        return TransmissionType.STRUCTURED_MESSAGE;
    }

    @Override
    public void registerChannelInterest(short protoId) {

    }


    @Override
    public void closeLink(String streamId, short protoId) {
        customConIDToAddress.remove(streamId);
    }
}
