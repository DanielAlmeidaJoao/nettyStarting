package babel.appExamples.channels;

import org.streamingAPI.client.StreamSender;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.network.data.Host;

public class StreamSenderChannel<T> implements IChannel<T> {
    private final StreamSender streamSender;
    private final ChannelListener<T> listener;

    public StreamSenderChannel(StreamSender streamSender, ChannelListener<T> listener) {
        this.streamSender = streamSender;
        this.listener = listener;
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {

    }

    @Override
    public void closeConnection(Host peer, int connection) {

    }

    @Override
    public void openConnection(Host peer) {

    }
}
