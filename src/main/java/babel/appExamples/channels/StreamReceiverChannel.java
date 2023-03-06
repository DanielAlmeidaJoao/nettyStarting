package babel.appExamples.channels;

import org.streamingAPI.server.StreamReceiver;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.network.data.Host;

public class StreamReceiverChannel<T> implements IChannel<T> {
    private final StreamReceiver streamReceiver;
    private final ChannelListener<T> listener;

    public StreamReceiverChannel(StreamReceiver streamReceiver, ChannelListener<T> listener) {
        this.streamReceiver = streamReceiver;
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
