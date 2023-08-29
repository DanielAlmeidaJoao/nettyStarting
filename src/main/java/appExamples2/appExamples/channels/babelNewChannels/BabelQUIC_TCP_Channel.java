package appExamples2.appExamples.channels.babelNewChannels;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelNewChannels.events.ConnectionProtocolChannelMetricsEvent;
import appExamples2.appExamples.channels.messages.BytesToBabelMessage;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.channels.events.*;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.channels.NettyChannelInterface;
import quicSupport.channels.NettyQUICChannel;
import quicSupport.channels.SingleThreadedQuicChannel;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.NettyTCPChannel;
import tcpSupport.tcpChannelAPI.channel.SingleThreadedNettyTCPChannel;
import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetrics;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;
import udpSupport.metrics.UDPNetworkStatsWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class BabelQUIC_TCP_Channel<T> implements NewIChannel<T>, ChannelHandlerMethods<T> {
    private final Logger logger;
    public final boolean metrics;
    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String DEFAULT_METRICS_INTERVAL = "10000";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";

    private final boolean triggerSent;
    private final BabelMessageSerializerInterface<T> serializer;
    private final ChannelListener<T> listener;
    private final NettyChannelInterface nettyChannelInterface;
    public final short protoToReceiveStreamData;
    //private final Map<String,Triple<Short,Short,Short>> unstructuredStreamHandlers;

    public BabelQUIC_TCP_Channel(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties, short protoId, NetworkProtocol networkProtocol, NetworkRole networkRole) throws IOException {
        logger = LogManager.getLogger(getClass().getName());
        this.serializer = serializer;
        BabelMessageSerializer aux = (BabelMessageSerializer) serializer;
        aux.registerProtoSerializer(BytesToBabelMessage.ID,BytesToBabelMessage.serializer);
        this.listener = list;
        nettyChannelInterface = getQUIC_TCP(properties,networkProtocol,networkRole);
        metrics = nettyChannelInterface.enabledMetrics();

        if(metrics && properties.getProperty(METRICS_INTERVAL_KEY)!=null){
            int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY));
            new DefaultEventExecutor().scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval, TimeUnit.SECONDS);
        }
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
        this.protoToReceiveStreamData = protoId;
        logger.info("CHANNEL <{}> STARTED.",getClass().getName());
        //unstructuredStreamHandlers = new HashMap<>();
    }
    private NettyChannelInterface getQUIC_TCP(Properties properties, NetworkProtocol protocol,NetworkRole networkRole) throws IOException {
        NettyChannelInterface i;
        if(NetworkProtocol.QUIC==protocol){
            if(properties.getProperty(FactoryMethods.SINGLE_THREADED_PROP)!=null){
                i = new SingleThreadedQuicChannel(properties,networkRole,this,serializer);
                System.out.println("SINGLE THREADED CHANNEL QUIC ");
            }else {
                i = new NettyQUICChannel(properties,false,networkRole,this,serializer);
                System.out.println("MULTI THREADED CHANNEL QUIC ");
            }
        }else if(NetworkProtocol.TCP==protocol){
            if(properties.getProperty(FactoryMethods.SINGLE_THREADED_PROP)!=null){
                i = new SingleThreadedNettyTCPChannel(properties,this,networkRole,serializer);
                System.out.println("SINGLE THREADED CHANNEL TCP ");
            }else {
                i = new NettyTCPChannel(properties,false,this,networkRole,serializer);
                System.out.println("MULTI THREADED CHANNEL TCP ");
            }
        }else{
            throw new RuntimeException("UNSUPPORTED PROTOCOL BY THIS CLASS: "+protocol);
        }
        return i;
    }
    void readMetricsMethod(List<ConnectionProtocolMetrics> current, List<ConnectionProtocolMetrics> old){
        ConnectionProtocolChannelMetricsEvent quicMetricsEvent = new ConnectionProtocolChannelMetricsEvent(current,old);
        listener.deliverEvent(quicMetricsEvent);
    }
    void triggerMetricsEvent() {
        nettyChannelInterface.readMetrics(this::readMetricsMethod);
    }


    @Override
    public void sendMessage(T message, Host host, short proto) {
        nettyChannelInterface.send(FactoryMethods.toInetSOcketAddress(host),message);
    }

    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto) {
        BabelMessage babelMessage = new BabelMessage(new BytesToBabelMessage(data,dataLen),sourceProto,destProto);
        sendMessage((T) babelMessage,dest,sourceProto);
    }

    @Override
    public void closeConnection(Host peer, short proto) {
        nettyChannelInterface.closeConnection(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public boolean isConnected(Host peer) {
        return nettyChannelInterface.isConnected(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public boolean isConnected(String connectionID) {
        return nettyChannelInterface.isConnected(connectionID);
    }

    @Override
    public String[] getConnectionsIds() {
        return nettyChannelInterface.getStreams();
    }

    @Override
    public InetSocketAddress[] getConnections() {
        return nettyChannelInterface.getAddressToQUICCons();
    }

    @Override
    public int connectedPeers() {
        return nettyChannelInterface.connectedPeers();
    }

    @Override
    public boolean shutDownChannel(short protoId) {
        nettyChannelInterface.shutDown();
        return true;
    }

    @Override
    public short getChannelProto() {
        return protoToReceiveStreamData;
    }

    @Override
    public NetworkProtocol getNetWorkProtocol() {
        return nettyChannelInterface.getNetworkProtocol();
    }

    @Override
    public NetworkRole getNetworkRole() {
        return nettyChannelInterface.getNetworkRole();
    }

    @Override
    public String openMessageConnection(Host host, short proto, boolean always) {
        return nettyChannelInterface.open(FactoryMethods.toInetSOcketAddress(host),TransmissionType.STRUCTURED_MESSAGE,proto,proto,always);
    }

    @Override
    public String openStreamConnection(Host host, short sourceProto,short destProto, boolean always) {
        return nettyChannelInterface.open(FactoryMethods.toInetSOcketAddress(host),TransmissionType.UNSTRUCTURED_STREAM,sourceProto,destProto,always);
    }

    @Override
    public TransmissionType getConnectionType(String connectionId) {
        return nettyChannelInterface.getConnectionType(connectionId);
    }

    public void closeConnection(String connectionID, short proto){
        nettyChannelInterface.closeLink(connectionID);
    }

    public void sendMessage(T msg, String connectionID, short proto){
        nettyChannelInterface.send(connectionID,msg);
    }
    @Override
    public void sendMessage(byte[] data, int dataLen, String connectionID, short sourceProto, short destProto) {
        BabelMessage babelMessage = new BabelMessage(new BytesToBabelMessage(data,dataLen),sourceProto,destProto);
        sendMessage((T) babelMessage,connectionID,sourceProto);
    }
    @Override
    public List<ConnectionProtocolMetrics> activeConnectionsMetrics() {
        return nettyChannelInterface.currentMetrics();
    }

    @Override
    public List<ConnectionProtocolMetrics> closedConnectionsMetrics() {
        return nettyChannelInterface.oldMetrics();
    }

    @Override
    public List<UDPNetworkStatsWrapper> getUDPMetrics() {
        logger.warn("getUDPMetrics SUPPORTED ONLY BY UDP");
        return null;
    }

    @Override
    public void registerChannelInterest(short protoId) {
        //TODO
    }




    /******************************** CHANNEL HANDLER METHODS *************************************/
    public void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId) {
        logger.info("ERROR ON STREAM {} BELONGING TO CONNECTION {}. REASON: {}",streamId,peer,error.getLocalizedMessage());
    }


    public void onChannelReadDelimitedMessage(String connectionId, T message, InetSocketAddress from) {
        listener.deliverMessage(message,FactoryMethods.toBabelHost(from),connectionId);
    }
    @Override
    public void onChannelReadFlowStream(String streamId, BabelOutputStream bytes, InetSocketAddress from, BabelInputStream inputStream, short streamProto) {
        short d = streamProto;
        if(streamProto!=327){
            logger.info("PROTOTOOTOTOT DIFFERENT {}",d);
            System.exit(0);
        }
        listener.deliverStream(bytes,FactoryMethods.toBabelHost(from),streamId,d,d,d,inputStream);
    }

    public void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId, BabelInputStream babelInputStream) {
        Host host = FactoryMethods.toBabelHost(peer);
        logger.debug("OnStreamConnectionUpEvent " + host);
        if(TransmissionType.STRUCTURED_MESSAGE==type){
            listener.deliverEvent(new OnMessageConnectionUpEvent(host,customConId,incoming));
        }else{
            listener.deliverEvent(new OnStreamConnectionUpEvent(host,customConId,incoming, babelInputStream));
        }
    }

    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType transmissionType, String conId) {
        Host h = FactoryMethods.toBabelHost(peer);
        listener.deliverEvent(new OnOpenConnectionFailed(h,conId,transmissionType,cause));
        logger.debug("FAILED TO OPEN CONNECTION TO {}. REASON: {}",peer,cause.getLocalizedMessage());
    }

    public void failedToCloseStream(String streamId, Throwable reason) {
        logger.info("FAILED TO CLOSE STREAM {}. REASON: {}",streamId,reason.getLocalizedMessage());
    }

    @Override
    public void onMessageSent(T message, Throwable error, InetSocketAddress peer, TransmissionType type,String conID) {
        Host dest = null;
        if(peer!=null){
            dest = FactoryMethods.toBabelHost(peer);
        }
        if (error == null && triggerSent) listener.messageSent(message,dest,TransmissionType.STRUCTURED_MESSAGE);
        else if (error != null) listener.messageFailed(message,dest,error,TransmissionType.STRUCTURED_MESSAGE);
    }

    @Override
    public void onStreamDataSent(InputStream inputStream, byte[] data, int len, Throwable error, InetSocketAddress peer, TransmissionType type, String conID) {
        Host dest=null;
        if(peer!=null){
            dest = FactoryMethods.toBabelHost(peer);
        }
        if(error==null&&triggerSent){
            OnStreamDataSentEvent dataSentEvent = new OnStreamDataSentEvent(data,inputStream,len,error,conID,dest);
            T babelMessage = (T) new BabelMessage(dataSentEvent,protoToReceiveStreamData,protoToReceiveStreamData);
            listener.messageSent(babelMessage,dest,type);
        }else if(error!=null){
            OnStreamDataSentEvent dataSentEvent = new OnStreamDataSentEvent(data,inputStream,len,error,conID,dest);
            T babelMessage = (T) new BabelMessage(dataSentEvent,protoToReceiveStreamData,protoToReceiveStreamData);
            listener.messageFailed(babelMessage,dest,error,type);
        }
    }

    public void failedToCreateStream(InetSocketAddress peer, Throwable error) {
        logger.info("FAILED TO CREATE A STREAM TO {}. REASON: {}",peer,error.getLocalizedMessage());
    }

    public void failedToGetMetrics(Throwable cause) {
        logger.info("FAILED TO GET METRICS. REASON: {}",cause.getLocalizedMessage());
    }

    public void onStreamClosedHandler(InetSocketAddress peer, String streamId, boolean inConnection, TransmissionType type) {
        //logger.debug("STREAM {} OF {} CONNECTION CLOSED.",streamId,peer);
        listener.deliverEvent(new OnConnectionDownEvent(FactoryMethods.toBabelHost(peer),null,streamId,inConnection,type));
    }


}
