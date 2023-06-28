package appExamples2.appExamples.channels.newTCPChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.TransmissionType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class NewTCPChannelControlledClose<T> extends BabelNewTCPChannel {
    private Map<Host,Set<Short>> protocolsUsingTheChannel;
    public NewTCPChannelControlledClose(BabelMessageSerializerInterface serializer, ChannelListener list, Properties properties, short proto) throws IOException {
        super(serializer, list, properties,proto);
        protocolsUsingTheChannel = new HashMap<>();
    }
    private void registerProtoOnSend(Host peer, short proto){
        Set<Short> shorts = protocolsUsingTheChannel.get(peer);
        if(shorts!=null){
            shorts.add(proto);
        }
    }
    @Override
    public boolean sendMessage(Object msg, Host peer, short proto) {
        //BabelMessage message = (BabelMessage) msg;
        registerProtoOnSend(peer, proto);
        return super.sendMessage(msg, peer,proto);
    }
    @Override
    public boolean sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto, short handlerId){
       registerProtoOnSend(dest,sourceProto);
       return super.sendMessage(data,dataLen,dest,sourceProto,destProto,handlerId);
    }
    @Override
    public boolean sendStream(byte [] stream,int len,Host host,short proto){
        registerProtoOnSend(host,proto);
        return super.sendStream(stream,len,host,proto);
    }
    @Override
    public void closeConnection(Host peer, short proto) {
        if(proto<0){
            super.closeConnection(peer, proto);
        }else{
            Set<Short> shorties = protocolsUsingTheChannel.get(peer);
            if (shorties!=null){
                shorties.remove(proto);
                if(shorties.isEmpty()){
                    super.closeConnection(peer, proto);
                    shorties.remove(proto);
                }
            }else{
                super.closeConnection(peer, proto);
            }
        }
    }
    @Override
    public void onChannelInactive(InetSocketAddress peer, String conId, boolean inConnection){
        protocolsUsingTheChannel.remove(peer);
    }
    @Override
    public void onChannelActive(String channel, boolean incoming, InetSocketAddress peer, TransmissionType type){
        protocolsUsingTheChannel.put(FactoryMethods.toBabelHost(peer),new HashSet<>());
        super.onChannelActive(channel,incoming,peer,type);
    }
    @Override
    public boolean shutDownChannel(short protoId) {
        if(protoId<0){
            return super.shutDownChannel(protoId);
        }else{
            protocolsUsingTheChannel.remove(protoId);
            if(protocolsUsingTheChannel.isEmpty()){
                return super.shutDownChannel(protoId);
            }
        }
        return false;
    }

}
