package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelQuicChannel.events.QUICMetricsEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamClosedEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamCreatedEvent;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionDown;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionUp;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionDown;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionUp;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.channels.CustomQuicChannel;
import quicSupport.channels.CustomQuicChannelInterface;
import quicSupport.channels.SingleThreadedQuicChannel;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BabelQuicChannel<T> implements NewIChannel<T>, ChannelHandlerMethods {
    private static final Logger logger = LogManager.getLogger(BabelQuicChannel.class);
    public final boolean metrics;
    public final static String NAME = "BabelQuicChannel";
    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String DEFAULT_METRICS_INTERVAL = "-1";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";

    private final boolean triggerSent;
    private final BabelMessageSerializerInterface<T> serializer;
    private final ChannelListener<T> listener;
    private final CustomQuicChannelInterface customQuicChannel;
    public final short protoToReceiveStreamData;
    private final Map<String,Triple<Short,Short,Short>> unstructuredStreamHandlers;


    public BabelQuicChannel(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties, short protoId) throws IOException {
        this.serializer = serializer;
        this.listener = list;
        if(properties.getProperty("SINLGE_TRHEADED")!=null){
            customQuicChannel = new SingleThreadedQuicChannel(properties, NetworkRole.CHANNEL,this);
            System.out.println("SINGLE THREADED CHANNEL");
        }else {
            customQuicChannel = new CustomQuicChannel(properties,false,NetworkRole.CHANNEL,this);
            System.out.println("MULTI THREADED CHANNEL");
        }
        metrics = customQuicChannel.enabledMetrics();

        if(metrics){
            int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY, DEFAULT_METRICS_INTERVAL));
            new DefaultEventExecutor().scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval, TimeUnit.MILLISECONDS);
        }
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
        this.protoToReceiveStreamData = protoId;
        unstructuredStreamHandlers = new HashMap<>();
    }
    void readMetricsMethod(List<QuicConnectionMetrics> current, List<QuicConnectionMetrics> old){
        QUICMetricsEvent quicMetricsEvent = new QUICMetricsEvent(current,old);
        listener.deliverEvent(quicMetricsEvent);
    }
    void triggerMetricsEvent() {
        customQuicChannel.readMetrics(this::readMetricsMethod);
    }


    @Override
    public void sendMessage(T msg, Host peer, short proto) {
        try {
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            customQuicChannel.send(FactoryMethods.toInetSOcketAddress(peer),toSend,toSend.length, TransmissionType.STRUCTURED_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto,short handlerId) {
        byte [] toSend = FactoryMethods.serializeWhenSendingBytes(sourceProto,destProto,handlerId,data,dataLen);
        customQuicChannel.send(FactoryMethods.toInetSOcketAddress(dest),toSend,toSend.length, TransmissionType.STRUCTURED_MESSAGE);
    }

    @Override
    public void closeConnection(Host peer, short proto) {
        customQuicChannel.closeConnection(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public boolean isConnected(Host peer) {
        return customQuicChannel.isConnected(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public String[] getStreams() {
        return customQuicChannel.getStreams();
    }

    @Override
    public InetSocketAddress[] getConnections() {
        return customQuicChannel.getConnections();
    }

    @Override
    public int connectedPeers() {
        return customQuicChannel.connectedPeers();
    }

    @Override
    public boolean shutDownChannel(short protoId) {
        customQuicChannel.shutDown();
        return true;
    }

    @Override
    public short getChannelProto() {
        return protoToReceiveStreamData;
    }

    @Override
    public NetworkProtocol getNetWorkProtocol() {
        return NetworkProtocol.QUIC;
    }

    @Override
    public void openConnection(Host peer, short proto, TransmissionType type) {
        customQuicChannel.open(FactoryMethods.toInetSOcketAddress(peer),type);
    }

    @Override
    public TransmissionType getConnectionTransmissionType(Host host) throws NoSuchElementException {
        return customQuicChannel.getConnectionType(FactoryMethods.toInetSOcketAddress(host));
    }

    @Override
    public TransmissionType getConnectionStreamTransmissionType(String streamId) {
        return customQuicChannel.getConnectionType(streamId);
    }

    public void createStream(Host peer, TransmissionType type, short sourceProto, short destProto, short handlerId)
    {
        customQuicChannel.createStream(FactoryMethods.toInetSOcketAddress(peer),type,Triple.of(sourceProto,destProto,handlerId));
    }

    public void closeStream(String streamId, short proto){
        customQuicChannel.closeStream(streamId);
    }

    public void sendMessage(T msg,String streamId,short proto){
        try {
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            customQuicChannel.send(streamId,toSend,toSend.length, TransmissionType.STRUCTURED_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void sendMessage(byte[] data,int dataLen, String streamId, short sourceProto, short destProto,short handlerId) {
        byte [] toSend = FactoryMethods.serializeWhenSendingBytes(sourceProto,destProto,handlerId,data,dataLen);
        customQuicChannel.send(streamId,
                toSend,toSend.length, TransmissionType.STRUCTURED_MESSAGE);
    }

    @Override
    public void sendStream(byte[] stream,int len, String streamId, short proto) {
        customQuicChannel.send(streamId, stream,len, TransmissionType.UNSTRUCTURED_STREAM);
    }

    @Override
    public void sendStream(byte[] stream,int len, Host host, short proto) {
        customQuicChannel.send(FactoryMethods.toInetSOcketAddress(host),stream,len, TransmissionType.UNSTRUCTURED_STREAM);
    }

    @Override
    public void registerChannelInterest(short protoId) {
        //TODO
    }




    /******************************** CHANNEL HANDLER METHODS *************************************/
    public void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId) {
        logger.info("ERROR ON STREAM {} BELONGING TO CONNECTION {}. REASON: {}",streamId,peer,error.getLocalizedMessage());
    }

    public void onStreamCreatedHandler(InetSocketAddress peer, String streamId, TransmissionType type, Triple<Short,Short,Short>args) {
        logger.info("STREAM {} CREATED FOR {} CONNECTION",streamId,peer);
        if(TransmissionType.UNSTRUCTURED_STREAM==type){
            unstructuredStreamHandlers.put(streamId,args);
        }
        listener.deliverEvent(new StreamCreatedEvent(streamId,FactoryMethods.toBabelHost(peer),type));
    }

    public void onChannelReadDelimitedMessage(String streamId, byte[] bytes, InetSocketAddress from) {
        //logger.info("MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",channelId,from,bytes.length);
        //logger.info("{}. MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",getSelf(),channelId,from,bytes.length);
        try {
            FactoryMethods.deserialize(bytes,serializer,listener,from,streamId);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onChannelReadFlowStream(String streamId, byte[] bytes, InetSocketAddress from) {
        Triple<Short,Short,Short> triple = unstructuredStreamHandlers.get(streamId);
        listener.deliverMessage(bytes,FactoryMethods.toBabelHost(from),streamId,
                triple.getLeft(),triple.getMiddle(),triple.getRight());
    }

    public void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String defaultStream) {
        Host host = FactoryMethods.toBabelHost(peer);
        if(TransmissionType.UNSTRUCTURED_STREAM==type){
            unstructuredStreamHandlers.put(defaultStream,Triple.of(protoToReceiveStreamData,protoToReceiveStreamData,protoToReceiveStreamData));
        }
        if(incoming){
            logger.debug("InboundConnectionUp " + peer);
            listener.deliverEvent(new InConnectionUp(host,type,"blaba"));
        }else{
            logger.debug("OutboundConnectionUp " + host);
            listener.deliverEvent(new OutConnectionUp(host,type,"dsadas"));
        }
    }

    public void onConnectionDown(InetSocketAddress peer, boolean incoming) {
        Throwable t = new Throwable("PEER DISCONNECTED!");
        Host host = FactoryMethods.toBabelHost(peer);
        if(incoming){
            logger.error("Inbound connection from {} is down" + peer);
            listener.deliverEvent(new InConnectionDown(host,t));
        }else{
            logger.debug("OutboundConnectionDown to " +peer+ "");
            listener.deliverEvent(new OutConnectionDown(host,t));
        }
    }

    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {
        logger.info("FAILED TO OPEN CONNECTION TO {}. REASON: {}",peer,cause.getLocalizedMessage());
    }

    public void failedToCloseStream(String streamId, Throwable reason) {
        logger.info("FAILED TO CLOSE STREAM {}. REASON: {}",streamId,reason.getLocalizedMessage());
    }

    public void failedToCreateStream(InetSocketAddress peer, Throwable error) {
        logger.info("FAILED TO CREATE A STREAM TO {}. REASON: {}",peer,error.getLocalizedMessage());
    }

    public void failedToGetMetrics(Throwable cause) {
        logger.info("FAILED TO GET METRICS. REASON: {}",cause.getLocalizedMessage());
    }

    public void onStreamClosedHandler(InetSocketAddress peer, String streamId) {
        logger.info("STREAM {} OF {} CONNECTION CLOSED.",streamId,peer);
        unstructuredStreamHandlers.remove(streamId);
        listener.deliverEvent(new StreamClosedEvent(streamId,FactoryMethods.toBabelHost(peer)));

    }
    public void onMessageSent(byte[] message, int len, Throwable error, InetSocketAddress peer, TransmissionType type) {
        try {
            if(error==null&&triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,message,type,protoToReceiveStreamData),FactoryMethods.toBabelHost(peer),type);
            }else if(error!=null){
                Host dest=null;
                if(peer!=null){
                    dest = FactoryMethods.toBabelHost(peer);
                }
                listener.messageFailed(FactoryMethods.unSerialize(serializer,message,type,protoToReceiveStreamData),dest,error,type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
