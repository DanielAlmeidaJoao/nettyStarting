package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
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
    @Override
    public void sendMessage(T msg, Host peer, short proto) {
        Set<Short> protosUsingThisConnection = hostChannelsMap.get(peer);
        if(protosUsingThisConnection!=null){
            protosUsingThisConnection.add(proto);
        }
        super.sendMessage(msg,peer,proto);
    }
    @Override
    public void sendMessage(T msg,String streamId,short proto) {
        Set<Short> protosUsingThisStream = streamChannelsMap.get(streamId);
        if(protosUsingThisStream!=null){
            protosUsingThisStream.add(proto);
        }else{
            addMessageFailedSent(msg,null,new Throwable("UNKNOWN STREAM ID : "+streamId));
            return;
        }
        super.sendMessage(msg,streamId,proto);
    }
    @Override
    public void onConnectionUp(boolean incoming, InetSocketAddress peer, ConnectionOrStreamType type){
        hostChannelsMap.put(FactoryMethods.toBabelHost(peer),new HashSet<>(registeredProtos));
        super.onConnectionUp(incoming,peer, type);
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
                if(protosUsingThisConnection.remove(proto) && protosUsingThisConnection.isEmpty()){
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
                if(protosUsingThisStream.remove(proto) && protosUsingThisStream.isEmpty()){
                    super.closeStream(streamId,proto);
                    hostChannelsMap.remove(proto);
                }
            }else{
                super.closeStream(streamId,proto);
            }
        }
    }
    @Override
    public void onStreamCreatedHandler(InetSocketAddress peer, String streamId, ConnectionOrStreamType type) {
        streamChannelsMap.put(streamId,new HashSet<>());
        super.onStreamCreatedHandler(peer,streamId, type);
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
}
