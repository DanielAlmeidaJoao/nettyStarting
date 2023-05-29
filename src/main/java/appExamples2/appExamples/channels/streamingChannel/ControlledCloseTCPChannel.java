package appExamples2.appExamples.channels.streamingChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.channel.Channel;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class ControlledCloseTCPChannel<T> extends BabelStreamingChannel{
    private Map<Host,Set<Short>> protocolsUsingTheChannel;
    public ControlledCloseTCPChannel(BabelMessageSerializerInterface serializer, ChannelListener list, Properties properties,short proto) throws IOException {
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
    public void sendMessage(Object msg, Host peer, short proto) {
        //BabelMessage message = (BabelMessage) msg;
        registerProtoOnSend(peer, proto);
        super.sendMessage(msg, peer,proto);
    }
    @Override
    public void sendMessage(byte[] data,int dataLen, Host dest, short sourceProto, short destProto, short handlerId){
       registerProtoOnSend(dest,sourceProto);
       super.sendMessage(data,dataLen,dest,sourceProto,destProto,handlerId);
    }
    @Override
    public void sendStream(byte [] stream,int len,Host host,short proto){
        registerProtoOnSend(host,proto);
        super.sendStream(stream,len,host,proto);
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
    public void onChannelInactive(InetSocketAddress peer){
        protocolsUsingTheChannel.remove(peer);
    }
    @Override
    public void onChannelActive(Channel channel, boolean incoming, InetSocketAddress peer, ConnectionOrStreamType type){
        protocolsUsingTheChannel.put(FactoryMethods.toBabelHost(peer),new HashSet<>());
        super.onChannelActive(channel,incoming,peer,type);
    }

}
