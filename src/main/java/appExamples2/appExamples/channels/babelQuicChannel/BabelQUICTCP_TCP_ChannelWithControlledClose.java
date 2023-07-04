package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.TransmissionType;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class BabelQUICTCP_TCP_ChannelWithControlledClose<T> extends BabelQUIC_TCP_Channel<T> {
    private static final Logger logger = LogManager.getLogger(BabelQUICTCP_TCP_ChannelWithControlledClose.class);
    private Map<Host, Set<Short>> hostChannelsMap;
    private Map<String, Set<Short>> streamChannelsMap;
    private Set<Short> registeredProtos;

    public BabelQUICTCP_TCP_ChannelWithControlledClose(BabelMessageSerializerInterface<T> serializer, ChannelListener<T> list, Properties properties, short protoId, NetworkProtocol networkProtocol) throws IOException {
        super(serializer,list,properties,protoId,networkProtocol);
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

    @Override
    public void sendStream(byte [] stream,int len,String streamId,short proto){
        addProtoOnSend(streamId,proto);
        super.sendStream(stream,len,streamId,proto);
    }
    @Override
    public void sendStream(InputStream inputStream, int len, Host peer, short proto){
        addProtoOnSend(peer,proto);
        super.sendStream(inputStream,len,peer,proto);
    }
    @Override
    public void sendStream(InputStream inputStream, int len, String conId, short proto){
        addProtoOnSend(conId,proto);
        super.sendStream(inputStream,len,conId,proto);
    }
    @Override
    public void sendStream(InputStream inputStream, Host peer, short proto){
        addProtoOnSend(peer,proto);
        super.sendStream(inputStream,peer,proto);
    }
    @Override
    public void sendStream(InputStream inputStream, String conId, short proto){
        addProtoOnSend(conId,proto);
        super.sendStream(inputStream,conId,proto);
    }
    //////
    @Override
    public void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId){
        hostChannelsMap.computeIfAbsent(FactoryMethods.toBabelHost(peer),host1 -> new HashSet<>());
        streamChannelsMap.computeIfAbsent(customConId,s -> new HashSet<>());
        super.onConnectionUp(incoming,peer, type, customConId);
    }

    @Override
    public void closeConnection(Host peer, short proto) {
        if(proto<0){
            hostChannelsMap.remove(peer);
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
            streamChannelsMap.remove(streamId);
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
