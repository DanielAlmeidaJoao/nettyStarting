package babel.appExamples.channels.streamingChannel;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.*;

public class ControlledCloseTCPChannel<T> extends BabelStreamingChannel{
    private Map<Host,Set<Short>> protocolsUsingTheChannel;
    public ControlledCloseTCPChannel(ISerializer serializer, ChannelListener list, Properties properties) throws IOException {
        super(serializer, list, properties);
        protocolsUsingTheChannel = new HashMap<>();
    }

    @Override
    public void sendMessage(Object msg, Host peer, int connection) {
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
    public void closeConnection(Host peer, int connection) {

        super.closeConnection(peer, connection);
    }
}
