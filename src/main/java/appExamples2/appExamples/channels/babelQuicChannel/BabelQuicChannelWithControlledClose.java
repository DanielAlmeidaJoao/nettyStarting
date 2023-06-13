package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class BabelQuicChannelWithControlledClose<T> extends BabelQuicChannel<T> {
    private static final Logger logger = LogManager.getLogger(BabelQuicChannelWithControlledClose.class);
    private Map<Host, Set<Short>> hostChannelsMap;
    private Map<String, Set<Short>> streamChannelsMap;
    private Set<Short> registeredProtos;

    public BabelQuicChannelWithControlledClose(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties,short protoId) throws IOException {
        super(serializer,list,properties,protoId);
        initMaps(properties.getProperty(FactoryMethods.SINGLE_THREADED_PROP)!=null);
    }
    private void initMaps(boolean singleThreaded){
        if(singleThreaded){
            hostChannelsMap = new HashMap<>();
            registeredProtos = new HashSet<>();
            streamChannelsMap = new HashMap<>();
        }else{
            hostChannelsMap = new ConcurrentHashMap<>();
            registeredProtos = new ConcurrentSkipListSet<>();
            streamChannelsMap = new ConcurrentHashMap<>();
        }
    }
    private void addProtoOnSend(Host peer,short proto){
        Set<Short> protosUsingThisConnection = hostChannelsMap.get(peer);
        if(protosUsingThisConnection!=null){
            protosUsingThisConnection.add(proto);
        }
    }
    private void addProtoOnSend(String streamId,short proto){
        Set<Short> protosUsingThisStream = streamChannelsMap.get(streamId);
        if(protosUsingThisStream!=null){
            protosUsingThisStream.add(proto);
        }
    }
    @Override
    public void sendMessage(T msg, Host peer, short proto) {
        addProtoOnSend(peer,proto);
        super.sendMessage(msg,peer,proto);
    }
    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto, short handlerId){
        addProtoOnSend(dest,sourceProto);
        super.sendMessage(data,dataLen,dest,sourceProto,destProto,handlerId);
    }
    @Override
    public void sendStream(byte [] stream,int len,Host host,short proto){
        addProtoOnSend(host,proto);
        super.sendStream(stream,len,host,proto);
    }

    @Override
    public void sendMessage(T msg,String streamId,short proto) {
        addProtoOnSend(streamId,proto);
        super.sendMessage(msg,streamId,proto);
    }
    @Override
    public void sendMessage(byte[] data,int dataLen, String streamId, short sourceProto, short destProto, short handlerId){
        addProtoOnSend(streamId,sourceProto);
        super.sendMessage(data,dataLen,streamId,sourceProto,destProto,handlerId);
    }
    public void sendStream(byte [] stream,int len,String streamId,short proto){
        addProtoOnSend(streamId,proto);
        super.sendStream(stream,len,streamId,proto);
    }
    //////
    @Override
    public void onConnectionUp(boolean incoming, InetSocketAddress peer, ConnectionOrStreamType type, String defaultStream){
        hostChannelsMap.put(FactoryMethods.toBabelHost(peer),new HashSet<>(registeredProtos));
        super.onConnectionUp(incoming,peer, type, defaultStream);
    }
    @Override
    public void onConnectionDown(InetSocketAddress peer, boolean incoming) {
        hostChannelsMap.remove(FactoryMethods.toBabelHost(peer));
        super.onConnectionDown(peer,incoming);
    }
    @Override
    public void closeConnection(Host peer, short proto) {
        if(proto<0){
            super.closeConnection(peer,proto);
        }else{
            Set<Short> protosUsingThisConnection = hostChannelsMap.get(peer);
            if (protosUsingThisConnection!=null){
                protosUsingThisConnection.remove(proto);
                if(protosUsingThisConnection.isEmpty()){
                    super.closeConnection(peer,proto);
                    hostChannelsMap.remove(proto);
                }
            }else{
                super.closeConnection(peer,proto);
            }
        }
    }
    @Override
    public void closeStream(String streamId, short proto){
        if(proto<0){
            super.closeStream(streamId,proto);
        }else{
            Set<Short> protosUsingThisStream = streamChannelsMap.get(streamId);
            if (protosUsingThisStream!=null){
                protosUsingThisStream.remove(proto);
                if(protosUsingThisStream.isEmpty()){
                    super.closeStream(streamId,proto);
                    hostChannelsMap.remove(proto);
                }
            }else{
                super.closeStream(streamId,proto);
            }
        }
    }
    @Override
    public void onStreamCreatedHandler(InetSocketAddress peer, String streamId, ConnectionOrStreamType type, Triple<Short,Short,Short> args) {
        streamChannelsMap.put(streamId,new HashSet<>());
        super.onStreamCreatedHandler(peer,streamId, type,args);
    }
    @Override
    public void onStreamClosedHandler(InetSocketAddress peer, String streamId) {
        streamChannelsMap.remove(streamId);
        super.onStreamClosedHandler(peer,streamId);
    }
        @Override
    public void registerChannelInterest(short protoId) {
        registeredProtos.add(protoId);
    }

    @Override
    public boolean shutDownChannel(short protoId) {
        if(protoId<0){
            return super.shutDownChannel(protoId);
        }else{
            registeredProtos.remove(protoId);
            if(registeredProtos.isEmpty()){
                return super.shutDownChannel(protoId);
            }
        }
        return false;
    }
}
