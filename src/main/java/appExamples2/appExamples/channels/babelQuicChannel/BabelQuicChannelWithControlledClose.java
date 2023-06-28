package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.TransmissionType;

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
    public boolean sendMessage(T msg, Host peer, short proto) {
        addProtoOnSend(peer,proto);
        return super.sendMessage(msg,peer,proto);
    }
    @Override
    public boolean sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto, short handlerId){
        addProtoOnSend(dest,sourceProto);
        return super.sendMessage(data,dataLen,dest,sourceProto,destProto,handlerId);
    }
    @Override
    public boolean sendStream(byte [] stream,int len,Host host,short proto){
        addProtoOnSend(host,proto);
        return super.sendStream(stream,len,host,proto);
    }

    @Override
    public boolean sendMessage(T msg,String streamId,short proto) {
        addProtoOnSend(streamId,proto);
        return super.sendMessage(msg,streamId,proto);
    }
    @Override
    public boolean sendMessage(byte[] data,int dataLen, String streamId, short sourceProto, short destProto, short handlerId){
        addProtoOnSend(streamId,sourceProto);
        return super.sendMessage(data,dataLen,streamId,sourceProto,destProto,handlerId);
    }
    public boolean sendStream(byte [] stream,int len,String streamId,short proto){
        addProtoOnSend(streamId,proto);
        return super.sendStream(stream,len,streamId,proto);
    }
    //////
    @Override
    public void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String defaultStream){
        hostChannelsMap.put(FactoryMethods.toBabelHost(peer),new HashSet<>(registeredProtos));
        super.onConnectionUp(incoming,peer, type, defaultStream);
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
    public void closeLink(String streamId, short proto){
        if(proto<0){
            super.closeLink(streamId,proto);
        }else{
            Set<Short> protosUsingThisStream = streamChannelsMap.get(streamId);
            if (protosUsingThisStream!=null){
                protosUsingThisStream.remove(proto);
                if(protosUsingThisStream.isEmpty()){
                    super.closeLink(streamId,proto);
                    hostChannelsMap.remove(proto);
                }
            }else{
                super.closeLink(streamId,proto);
            }
        }
    }
    @Override
    public void onStreamCreatedHandler(InetSocketAddress peer, String streamId, TransmissionType type, Triple<Short,Short,Short> args) {
        streamChannelsMap.put(streamId,new HashSet<>());
        super.onStreamCreatedHandler(peer,streamId, type,args);
    }
    @Override
    public void onStreamClosedHandler(InetSocketAddress peer, String streamId, boolean inConnection) {
        streamChannelsMap.remove(streamId);
        super.onStreamClosedHandler(peer,streamId, inConnection);
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
