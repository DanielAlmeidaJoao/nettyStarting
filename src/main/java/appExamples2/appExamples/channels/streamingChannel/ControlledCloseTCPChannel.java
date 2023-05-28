package appExamples2.appExamples.channels.streamingChannel;

import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.io.IOException;
import java.util.*;

public class ControlledCloseTCPChannel<T> extends BabelStreamingChannel{
    private Map<Host,Set<Short>> protocolsUsingTheChannel;
    public ControlledCloseTCPChannel(BabelMessageSerializerInterface serializer, ChannelListener list, Properties properties,short proto) throws IOException {
        super(serializer, list, properties,proto);
        protocolsUsingTheChannel = new HashMap<>();
    }

    @Override
    public void sendMessage(Object msg, Host peer, short connection) {
        BabelMessage message = (BabelMessage) msg;
        Set<Short> shorts = protocolsUsingTheChannel.get(peer);
        if(shorts==null){
            shorts=new HashSet<>();
            protocolsUsingTheChannel.put(peer,shorts);
        }
        shorts.add(message.getSourceProto());
        super.sendMessage(msg, peer, connection);
    }

    @Override
    public void closeConnection(Host peer, short proto) {

        super.closeConnection(peer, proto);
    }
}
