package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelQuicChannel.events.QUICMetricsEvent;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionDownEvent;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionUpEvent;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.channels.NettyQUICChannel;
import quicSupport.channels.NettyChannelInterface;
import quicSupport.channels.SingleThreadedQuicChannel;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicConnectionMetrics;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;
import tcpSupport.tcpStreamingAPI.channel.SingleThreadedStreamingChannel;
import tcpSupport.tcpStreamingAPI.channel.StreamingChannel;
import tcpSupport.tcpStreamingAPI.utils.BabelInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class BabelQUIC_TCP_Channel<T> implements NewIChannel<T>, ChannelHandlerMethods {
    private static final Logger logger = LogManager.getLogger(BabelQUIC_TCP_Channel.class);
    public final boolean metrics;
    public final static String NAME_TCP = "BABEL_TCP_CHANNEL";
    public final static String NAME_QUIC = "BABEL_QUIC_CHANNEL";

    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String DEFAULT_METRICS_INTERVAL = "-1";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";

    private final boolean triggerSent;
    private final BabelMessageSerializerInterface<T> serializer;
    private final ChannelListener<T> listener;
    private final NettyChannelInterface customQuicChannel;
    public final short protoToReceiveStreamData;
    //private final Map<String,Triple<Short,Short,Short>> unstructuredStreamHandlers;

    public BabelQUIC_TCP_Channel(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties, short protoId, NetworkProtocol networkProtocol) throws IOException {
        this.serializer = serializer;
        this.listener = list;
        customQuicChannel = getQUIC_TCP(properties,networkProtocol);
        metrics = customQuicChannel.enabledMetrics();

        if(metrics){
            int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY, DEFAULT_METRICS_INTERVAL));
            new DefaultEventExecutor().scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval, TimeUnit.MILLISECONDS);
        }
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
        this.protoToReceiveStreamData = protoId;
        //unstructuredStreamHandlers = new HashMap<>();
    }
    private NettyChannelInterface getQUIC_TCP(Properties properties, NetworkProtocol protocol) throws IOException {
        NettyChannelInterface i;
        if(NetworkProtocol.QUIC==protocol){
            if(properties.getProperty("SINLGE_TRHEADED")!=null){
                i = new SingleThreadedQuicChannel(properties, NetworkRole.CHANNEL,this);
                System.out.println("SINGLE THREADED CHANNEL");
            }else {
                i = new NettyQUICChannel(properties,false,NetworkRole.CHANNEL,this);
                System.out.println("MULTI THREADED CHANNEL");
            }
        }else if(NetworkProtocol.TCP==protocol){
            if(properties.getProperty(FactoryMethods.SINGLE_THREADED_PROP)!=null){
                i = new SingleThreadedStreamingChannel(properties,this, NetworkRole.CHANNEL);
                System.out.println("SINGLE THREADED CHANNEL");
            }else {
                i = new StreamingChannel(properties,false,this,NetworkRole.CHANNEL);
                System.out.println("MULTI THREADED CHANNEL");
            }
        }else{
            throw new RuntimeException("UNSUPPORTED PROTOCOL BY THIS CLASS: "+protocol);
        }
        return i;
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
    public String[] getLinks() {
        return customQuicChannel.getStreams();
    }

    @Override
    public InetSocketAddress[] getConnections() {
        return customQuicChannel.getAddressToQUICCons();
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
    public String openMessageConnection(Host peer, short proto) {
        return customQuicChannel.open(FactoryMethods.toInetSOcketAddress(peer),TransmissionType.STRUCTURED_MESSAGE);
    }

    @Override
    public String openStreamConnection(Host peer, short protoId) {
        return customQuicChannel.open(FactoryMethods.toInetSOcketAddress(peer),TransmissionType.UNSTRUCTURED_STREAM);
    }

    @Override
    public TransmissionType getTransmissionType(Host host) throws NoSuchElementException {
        return customQuicChannel.getConnectionType(FactoryMethods.toInetSOcketAddress(host));
    }

    @Override
    public TransmissionType getTransmissionType(String streamId) {
        return customQuicChannel.getConnectionType(streamId);
    }

    public void closeLink(String streamId, short proto){
        customQuicChannel.closeLink(streamId);
    }

    public void sendMessage(T msg,String streamId,short proto){
        try {
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            customQuicChannel.send(streamId,toSend,toSend.length, TransmissionType.STRUCTURED_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void sendMessage(byte[] data,int dataLen, String streamId, short sourceProto, short destProto,short handlerId) {
        byte [] toSend = FactoryMethods.serializeWhenSendingBytes(sourceProto,destProto,handlerId,data,dataLen);
        customQuicChannel.send(streamId, toSend,toSend.length, TransmissionType.STRUCTURED_MESSAGE);
    }

    @Override
    public void registerChannelInterest(short protoId) {
        //TODO
    }




    /******************************** CHANNEL HANDLER METHODS *************************************/
    public void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId) {
        logger.info("ERROR ON STREAM {} BELONGING TO CONNECTION {}. REASON: {}",streamId,peer,error.getLocalizedMessage());
    }


    public void onChannelReadDelimitedMessage(String connectionId, byte[] bytes, InetSocketAddress from) {
        //logger.info("MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",channelId,from,bytes.length);
        //logger.info("{}. MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",getSelf(),channelId,from,bytes.length);
        try {
            FactoryMethods.deserialize(bytes,serializer,listener,from,connectionId);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onChannelReadFlowStream(String streamId, BabelOutputStream bytes, InetSocketAddress from) {
        short d = protoToReceiveStreamData;
        listener.deliverMessage(bytes,FactoryMethods.toBabelHost(from),streamId,d,d,d);
    }

    public void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId, BabelInputStream babelInputStream) {
        Host host = FactoryMethods.toBabelHost(peer);
        logger.debug("OnConnectionUpEvent " + host);
        listener.deliverEvent(new OnConnectionUpEvent(host,type,customConId,incoming, babelInputStream));
    }
    /**
    public void onConnectionDown(InetSocketAddress peer, boolean incoming) {
        Throwable t = new Throwable("PEER DISCONNECTED!");
        Host host = FactoryMethods.toBabelHost(peer);
        if(incoming){
            logger.error("Inbound connection from {} is down" + peer);
            listener.deliverEvent(new InConnectionDown(host,t, streamId));
        }else{
            logger.debug("OutboundConnectionDown to " +peer+ "");
            listener.deliverEvent(new OutConnectionDown(host,t, streamId));
        }
    }**/

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

    public void onStreamClosedHandler(InetSocketAddress peer, String streamId, boolean inConnection) {
        logger.info("STREAM {} OF {} CONNECTION CLOSED.",streamId,peer);
        listener.deliverEvent(new OnConnectionDownEvent(FactoryMethods.toBabelHost(peer),null,streamId,inConnection));
    }
    public void onMessageSent(byte[] message, InputStream inputStream, int len, Throwable error, InetSocketAddress peer, TransmissionType type) {
        try {
            if(error==null&&triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,message,inputStream,type,protoToReceiveStreamData),FactoryMethods.toBabelHost(peer),type);
            }else if(error!=null){
                Host dest=null;
                if(peer!=null){
                    dest = FactoryMethods.toBabelHost(peer);
                }
                listener.messageFailed(FactoryMethods.unSerialize(serializer,message,inputStream,type,protoToReceiveStreamData),dest,error,type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
