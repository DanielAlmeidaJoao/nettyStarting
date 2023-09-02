package appExamples2.appExamples.channels.babelNewChannels;

import appExamples2.appExamples.channels.FactoryMethods;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.enums.NetworkProtocol;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class BabelQUIC_TCP_ChannelWithControlledClose extends BabelQUIC_TCP_Channel {
    private static final Logger logger = LogManager.getLogger(BabelQUIC_TCP_ChannelWithControlledClose.class);
    private Map<Host, Set<Short>> hostChannelsMap;
    private Map<String, Set<Short>> streamChannelsMap;
    private Set<Short> registeredProtos;

    public BabelQUIC_TCP_ChannelWithControlledClose(BabelMessageSerializer serializer, ChannelListener list, Properties properties, short protoId, NetworkProtocol networkProtocol, NetworkRole networkRole) throws IOException {
        super(serializer,list,properties,protoId,networkProtocol,networkRole);
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
    public void sendMessage(BabelMessage message, Host host, short proto) {
        addProtoOnSend(host,proto);
        super.sendMessage(message, host,proto);
    }
    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto){
        addProtoOnSend(dest,sourceProto);
        super.sendMessage(data,dataLen,dest,sourceProto,destProto);
    }

    @Override
    public void sendMessage(BabelMessage msg, String connectionID, short proto) {
        addProtoOnSend(connectionID,proto);
        super.sendMessage(msg, connectionID,proto);
    }
    @Override
    public void sendMessage(byte[] data, int dataLen, String connectionID, short sourceProto, short destProto){
        addProtoOnSend(connectionID,sourceProto);
        super.sendMessage(data,dataLen, connectionID,sourceProto,destProto);
    }

        //////
    @Override
    public void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId, BabelInputStream babelInputStream){
        hostChannelsMap.computeIfAbsent(Host.toBabelHost(peer),host1 -> new HashSet<>());
        streamChannelsMap.computeIfAbsent(customConId,s -> new HashSet<>());
        super.onConnectionUp(incoming,peer, type, customConId, babelInputStream);
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
    public void closeConnection(String connectionID, short proto){
        if(proto<0){
            streamChannelsMap.remove(connectionID);
            super.closeConnection(connectionID,proto);
        }else{
            Set<Short> protosUsingThisStream = streamChannelsMap.get(connectionID);
            if (protosUsingThisStream!=null){
                protosUsingThisStream.remove(proto);
                if(protosUsingThisStream.isEmpty()){
                    super.closeConnection(connectionID,proto);
                    hostChannelsMap.remove(proto);
                }
            }else{
                super.closeConnection(connectionID,proto);
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
