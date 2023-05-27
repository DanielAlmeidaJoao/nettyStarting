package appExamples2.appExamples.channels.udpBabelChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.*;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionDown;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionUp;
import quicSupport.utils.enums.ConnectionOrStreamType;
import udpSupport.channels.SingleThreadedUDPChannel;
import udpSupport.channels.UDPChannel;
import udpSupport.channels.UDPChannelHandlerMethods;
import udpSupport.channels.UDPChannelInterface;
import udpSupport.metrics.ChannelStats;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
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


    public BabelUDPChannel(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties) throws IOException {
        //super(properties);
        this.serializer = serializer;
        this.listener = list;
        if(properties.getProperty(FactoryMethods.SINGLE_THREADED_PROP)!=null){
            udpChannelInterface = new SingleThreadedUDPChannel(properties,this);
        }else {
            udpChannelInterface = new UDPChannel(properties,false,this);
        }
        metrics = udpChannelInterface.metricsEnabled();
        if(metrics){
            int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY, DEFAULT_METRICS_INTERVAL));
            new DefaultEventExecutor().scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval, TimeUnit.MILLISECONDS);
        }
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
    }
    void readMetricsMethod(ChannelStats stats){
        listener.deliverEvent(new UDPMetricsEvent(stats));
    }
    void triggerMetricsEvent() {
        udpChannelInterface.readMetrics(this::readMetricsMethod);
    }

    @Override
    public void onPeerDown(InetSocketAddress peer) {
        listener.deliverEvent(new OutConnectionDown(FactoryMethods.toBabelHost(peer),new Throwable("PEER DISCONNECTED!")));
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
    public void sendMessage(T msg, Host peer, short connection) {
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
    public void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest){

    }
    private void msgSent(byte[] message, Host host){
        try {
            if(triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,message),host);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeConnection(Host peer, short connection) {
        logger.debug("CLOSE CONNECTION. UNSUPPORTED OPERATION ON UDP");
        //Throwable t = new Throwable("PEER DISCONNECTED!");
        listener.deliverEvent(new OutConnectionDown(peer,null));
    }

    @Override
    public boolean isConnected(Host peer) {
        return true;
    }

    @Override
    public String[] getStreams() {
        return new String[0];
    }

    @Override
    public InetSocketAddress[] getConnections() {
        return new InetSocketAddress[0];
    }

    @Override
    public int connectedPeers() {
        return -1;
    }

    @Override
    public boolean shutDownChannel(short protoId) {
        udpChannelInterface.shutDownServerClient();
        return true;
    }

    @Override
    public void openConnection(Host peer, short proto, ConnectionOrStreamType streamType) {
        logger.debug("OPEN CONNECTION. UNSUPPORTED OPERATION ON UDP");
        listener.deliverEvent(new OutConnectionUp(peer));
    }

    @Override
    public void registerChannelInterest(short protoId) {

    }

    @Override
    public void sendMessage(T msg,String streamId,short proto) {
        Throwable throwable = new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY BabelQuicChannel");
        throwable.printStackTrace();
    }

    @Override
    public void sendMessage(byte[] data, int dataLen, String streamId, short sourceProto, short destProto,short handlerId) {
        Throwable throwable = new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY BabelQuicChannel");
        throwable.printStackTrace();
    }

    @Override
    public void sendStream(byte[] stream,int len,String streamId, short proto) {
        new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY QUIC CHANNELS").printStackTrace();
    }

    @Override
    public void sendStream(byte[] msg,int len,Host host, short proto) {
        new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY QUIC AND TCP CHANNELS").printStackTrace();
    }

    @Override
    public void createStream(Host peer) {
        Throwable throwable = new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY BabelQuicChannel");
        throwable.printStackTrace();
    }

    @Override
    public void closeStream(String streamId, short protoId) {
        Throwable throwable = new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY BabelQuicChannel");
        throwable.printStackTrace();
    }
}
