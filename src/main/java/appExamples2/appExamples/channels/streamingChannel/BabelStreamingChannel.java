package appExamples2.appExamples.channels.streamingChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionDownEvent;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionUpEvent;
import tcpSupport.tcpStreamingAPI.channel.SingleThreadedStreamingChannel;
import tcpSupport.tcpStreamingAPI.channel.StreamingChannel;
import tcpSupport.tcpStreamingAPI.channel.TCPChannelHandlerMethods;
import tcpSupport.tcpStreamingAPI.channel.TCPChannelInterface;
import pt.unl.fct.di.novasys.babel.channels.*;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.io.InputStream;
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
    public void closeConnection(Host peer, short connection) {
        tcpChannelInterface.closeConnection(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public boolean isConnected(Host peer) {
        return tcpChannelInterface.isConnected(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public String[] getLinks() {
        return tcpChannelInterface.getLinks();
    }

    @Override
    public InetSocketAddress[] getConnections() {
        return tcpChannelInterface.getNettyIdToConnection();
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
    public TransmissionType getTransmissionType(Host host)  throws NoSuchElementException {
        return tcpChannelInterface.getConnectionType(FactoryMethods.toInetSOcketAddress(host)) ;
    }

    @Override
    public TransmissionType getTransmissionType(String streamId)  throws NoSuchElementException{
        return tcpChannelInterface.getConnectionStreamTransmissionType(streamId);
    }

    @Override
    public void onChannelInactive(InetSocketAddress peer, String conId, boolean inConnection) {
        Throwable cause = new Throwable(String.format("CHANNEL %S CLOSED.",peer));
        listener.deliverEvent(new OnConnectionDownEvent(toBabelHost(peer), cause,conId,inConnection));
    }

    @Override
    public void onChannelMessageRead(String channelId, byte[] bytes, InetSocketAddress from, String conId) {
        try {
            FactoryMethods.deserialize(bytes,serializer,listener,from,conId);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onChannelStreamRead(String channelId, byte[] bytes, InetSocketAddress from, String conId) {
        listener.deliverMessage(bytes,FactoryMethods.toBabelHost(from),conId,
                protoToReceiveStreamData, protoToReceiveStreamData, protoToReceiveStreamData);
    }

    @Override
    public void onChannelActive(String channelId, boolean incoming, InetSocketAddress peer, TransmissionType type) {
        listener.deliverEvent(new OnConnectionUpEvent(toBabelHost(peer), type, channelId,incoming));
    }

    @Override
    public void onMessageSent(byte[] data, InputStream inputStream, InetSocketAddress peer, Throwable cause, TransmissionType type) {
        try {
            if(cause==null && triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,data,inputStream,type,protoToReceiveStreamData), toBabelHost(peer), type);
            }else if(cause!=null){
                Host dest=null;
                if(peer!=null){
                    dest = FactoryMethods.toBabelHost(peer);
                }
                listener.messageFailed(FactoryMethods.unSerialize(serializer,data,inputStream,type,protoToReceiveStreamData),dest,cause,type);
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
        new Exception("TO DO. THINK ABOUT IT, IT MAKES NO MUCH SENSE").printStackTrace();
    }

    @Override
    public void sendMessage(T msg,String streamId,short proto) {
        try {
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            tcpChannelInterface.send(toSend, toSend.length, streamId, TransmissionType.STRUCTURED_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void sendMessage(byte[] data, int dataLen, String streamId, short sourceProto, short destProto,short handlerId) {
        byte [] toSend = FactoryMethods.serializeWhenSendingBytes(sourceProto,destProto,handlerId,data,dataLen);
        tcpChannelInterface.send(toSend, toSend.length, streamId, TransmissionType.STRUCTURED_MESSAGE);
    }
    @Override
    public void sendStream(byte[] stream,int len, String streamId, short proto) {
        tcpChannelInterface.send(stream,len,streamId,TransmissionType.UNSTRUCTURED_STREAM);
    }
    @Override
    public void sendStream(byte[] msg,int len, Host host, short proto) {
        tcpChannelInterface.send(msg,len,FactoryMethods.toInetSOcketAddress(host), TransmissionType.UNSTRUCTURED_STREAM);
    }

    @Override
    public void sendStream(InputStream inputStream, int len, Pair<Host, String> peerOrConId, short proto) {
        InetSocketAddress address=null;
        if(peerOrConId.getKey()!=null){
            address = FactoryMethods.toInetSOcketAddress(peerOrConId.getKey());
        }
        tcpChannelInterface.sendInputStream(inputStream,len,address,peerOrConId.getValue());
    }

    @Override
    public void closeLink(String streamId, short proto) {
        tcpChannelInterface.closeConnection(streamId);
    }
}
