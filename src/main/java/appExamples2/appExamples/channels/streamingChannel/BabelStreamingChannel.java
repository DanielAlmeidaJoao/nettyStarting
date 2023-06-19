package appExamples2.appExamples.channels.streamingChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.channel.SingleThreadedStreamingChannel;
import org.tcpStreamingAPI.channel.StreamingChannel;
import org.tcpStreamingAPI.channel.TCPChannelHandlerMethods;
import org.tcpStreamingAPI.channel.TCPChannelInterface;
import pt.unl.fct.di.novasys.babel.channels.*;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionDown;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionUp;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionUp;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Properties;

import static appExamples2.appExamples.channels.FactoryMethods.toBabelHost;

public class BabelStreamingChannel<T> implements NewIChannel<T>, TCPChannelHandlerMethods {
    private static final Logger logger = LogManager.getLogger(BabelStreamingChannel.class);
    public final static String TRIGGER_SENT_KEY = "trigger_sent";
    public final static String NAME = "STREAMING_CHANNEL";

    private final BabelMessageSerializerInterface<T> serializer;
    private ChannelListener<T> listener;
    private final boolean triggerSent;
    private final TCPChannelInterface tcpChannelInterface;
    public final short protoToReceiveStreamData;

    public BabelStreamingChannel(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties,short proto) throws IOException {

        this.serializer = serializer;
        this.listener = list;
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
        if(properties.getProperty(FactoryMethods.SINGLE_THREADED_PROP)!=null){
            tcpChannelInterface = new SingleThreadedStreamingChannel(properties,this, NetworkRole.CHANNEL);
            System.out.println("SINGLE THREADED CHANNEL");
        }else {
            tcpChannelInterface = new StreamingChannel(properties,false,this,NetworkRole.CHANNEL);
            System.out.println("MULTI THREADED CHANNEL");
        }
        protoToReceiveStreamData=proto;
    }

    @Override
    public void sendMessage(T msg, Host peer, short proto) {
        try {
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            tcpChannelInterface.send(toSend,toSend.length,FactoryMethods.toInetSOcketAddress(peer), TransmissionType.STRUCTURED_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto,short handlerId) {
        byte [] toSend = FactoryMethods.serializeWhenSendingBytes(sourceProto,destProto,handlerId,data,dataLen);
        tcpChannelInterface.send(toSend,toSend.length,FactoryMethods.toInetSOcketAddress(dest), TransmissionType.STRUCTURED_MESSAGE);
    }
    @Override
    public void sendStream(byte[] msg,int len, Host host, short proto) {
        tcpChannelInterface.send(msg,len,FactoryMethods.toInetSOcketAddress(host), TransmissionType.UNSTRUCTURED_STREAM);
    }
    @Override
    public void closeConnection(Host peer, short connection) {
        tcpChannelInterface.closeConnection(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public boolean isConnected(Host peer) {
        return tcpChannelInterface.isConnected(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public String[] getStreams() {
        return new String[0];
    }

    @Override
    public InetSocketAddress[] getConnections() {
        return tcpChannelInterface.getConnections();
    }

    @Override
    public int connectedPeers() {
        return tcpChannelInterface.connectedPeers();
    }

    @Override
    public boolean shutDownChannel(short protoId) {
        tcpChannelInterface.shutDown();
        listener = new DummyChannelToProtoForwarder<>();
        return true;
    }

    @Override
    public short getChannelProto() {
        return protoToReceiveStreamData;
    }

    @Override
    public NetworkProtocol getNetWorkProtocol() {
        return NetworkProtocol.TCP;
    }

    @Override
    public String openConnection(Host peer, short proto, TransmissionType type) {
        return tcpChannelInterface.openConnection(FactoryMethods.toInetSOcketAddress(peer),type);
    }

    @Override
    public TransmissionType getConnectionTransmissionType(Host host)  throws NoSuchElementException {
        return tcpChannelInterface.getConnectionType(FactoryMethods.toInetSOcketAddress(host)) ;
    }

    @Override
    public TransmissionType getConnectionStreamTransmissionType(String streamId)  throws NoSuchElementException{
        new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY BabelQuicChannel").printStackTrace();
        return TransmissionType.STRUCTURED_MESSAGE;
    }

    @Override
    public void onChannelInactive(InetSocketAddress peer) {
        Throwable cause = new Throwable(String.format("CHANNEL %S CLOSED.",peer));
        listener.deliverEvent(new InConnectionDown(toBabelHost(peer), cause));
    }

    @Override
    public void onChannelMessageRead(String channelId, byte[] bytes, InetSocketAddress from) {
        try {
            FactoryMethods.deserialize(bytes,serializer,listener,from,null);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onChannelStreamRead(String channelId, byte[] bytes, InetSocketAddress from) {
        listener.deliverMessage(bytes,FactoryMethods.toBabelHost(from),null,
                protoToReceiveStreamData, protoToReceiveStreamData, protoToReceiveStreamData);
    }

    @Override
    public void onChannelActive(Channel channel, boolean incoming, InetSocketAddress peer, TransmissionType type) {
        if(incoming){
            listener.deliverEvent(new InConnectionUp(toBabelHost(peer), type));
        }else{
            listener.deliverEvent(new OutConnectionUp(toBabelHost(peer), type));
        }
    }

    @Override
    public void onMessageSent(byte[] data, InetSocketAddress peer, Throwable cause, TransmissionType type) {
        try {
            if(cause==null && triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,data,type,protoToReceiveStreamData), toBabelHost(peer), type);
            }else if(cause!=null){
                Host dest=null;
                if(peer!=null){
                    dest = FactoryMethods.toBabelHost(peer);
                }
                listener.messageFailed(FactoryMethods.unSerialize(serializer,data,type,protoToReceiveStreamData),dest,cause,type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {
        logger.info("CONNECTION TO {} FAILED. CAUSE = {}.",peer,cause);
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
    public void sendStream(byte[] stream,int len, String streamId, short proto) {
        new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY BabelQuicChannel").printStackTrace();
    }



    @Override
    public String createStream(Host peer, TransmissionType type, short sourceProto, short destProto, short handlerId)
    {
        Throwable throwable = new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY BabelQuicChannel");
        throwable.printStackTrace();
        return null;
    }

    @Override
    public void closeStream(String streamId, short proto) {
        Throwable throwable = new Throwable("UNSUPPORTED OPERATION. SUPPORTED ONLY BY BabelQuicChannel");
        throwable.printStackTrace();
    }
}
