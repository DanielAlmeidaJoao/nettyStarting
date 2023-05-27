package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelQuicChannel.events.QUICMetricsEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamClosedEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamCreatedEvent;
import io.netty.util.concurrent.DefaultEventExecutor;
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
import quicSupport.utils.NetworkRole;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
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

    public BabelQuicChannel(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties) throws IOException {
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
            customQuicChannel.send(FactoryMethods.toInetSOcketAddress(peer),toSend,toSend.length);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto,short handlerId) {
        byte [] toSend = FactoryMethods.serializeWhenSendingBytes(sourceProto,destProto,handlerId,data,dataLen);
        customQuicChannel.send(FactoryMethods.toInetSOcketAddress(dest),toSend,toSend.length);
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
    public void openConnection(Host peer, short proto) {
        customQuicChannel.open(FactoryMethods.toInetSOcketAddress(peer));
    }

    public void createStream(Host peer){
        customQuicChannel.createStream(FactoryMethods.toInetSOcketAddress(peer));
    }

    public void closeStream(String streamId, short proto){
        customQuicChannel.closeStream(streamId);
    }

    public void sendMessage(T msg,String streamId,short proto){
        try {
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            customQuicChannel.send(streamId,toSend,toSend.length);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void sendMessage(byte[] data,int dataLen, String streamId, short sourceProto, short destProto,short handlerId) {
        byte [] toSend = FactoryMethods.serializeWhenSendingBytes(sourceProto,destProto,handlerId,data,dataLen);
        customQuicChannel.send(streamId,
                toSend,toSend.length);
    }
    @Override
    public void registerChannelInterest(short protoId) {
        //TODO
    }
    /******************************** CHANNEL HANDLER METHODS *************************************/
    public void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId) {
        logger.info("ERROR ON STREAM {} BELONGING TO CONNECTION {}. REASON: {}",streamId,peer,error.getLocalizedMessage());
    }

    public void onStreamCreatedHandler(InetSocketAddress peer, String streamId) {
        logger.info("STREAM {} CREATED FOR {} CONNECTION",streamId,peer);
        listener.deliverEvent(new StreamCreatedEvent(streamId,FactoryMethods.toBabelHost(peer)));
    }

    public void onChannelRead(String streamId, byte[] bytes, InetSocketAddress from) {
        //logger.info("MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",channelId,from,bytes.length);
        //logger.info("{}. MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",getSelf(),channelId,from,bytes.length);
        try {
            FactoryMethods.deserialize(bytes,serializer,listener,from,streamId);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void onConnectionUp(boolean incoming, InetSocketAddress peer) {
        Host host = FactoryMethods.toBabelHost(peer);
        if(incoming){
            logger.debug("InboundConnectionUp " + peer);
            listener.deliverEvent(new InConnectionUp(host));
        }else{
            logger.debug("OutboundConnectionUp " + host);
            listener.deliverEvent(new OutConnectionUp(host));
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
        listener.deliverEvent(new StreamClosedEvent(streamId,FactoryMethods.toBabelHost(peer)));

    }
    public void onMessageSent(byte[] message, int len, Throwable error,InetSocketAddress peer) {
        try {
            if(error==null&&triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,message),FactoryMethods.toBabelHost(peer));
            }else if(error!=null){
                Host dest=null;
                if(peer!=null){
                    dest = FactoryMethods.toBabelHost(peer);
                }
                listener.messageFailed(FactoryMethods.unSerialize(serializer,message),dest,error);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void addMessageFailedSent(T msg, Host host, Throwable error){
        listener.messageFailed(msg,host,error);
    }
}
