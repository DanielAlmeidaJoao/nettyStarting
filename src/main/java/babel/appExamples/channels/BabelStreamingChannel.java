package babel.appExamples.channels;

import io.netty.channel.Channel;
import org.streamingAPI.channel.StreamingChannel;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.network.data.Host;

import java.util.Properties;

public class BabelStreamingChannel<T> extends StreamingChannel implements IChannel<T> {
    public BabelStreamingChannel(Properties properties) throws Exception {
        super(properties);
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        sen
    }

    @Override
    public void closeConnection(Host peer, int connection) {

    }

    @Override
    public void openConnection(Host peer) {

    }

    @Override
    public void channelClosed(String channelId) {

    }

    @Override
    public void channelRead(String channelId, byte[] bytes) {

    }

    @Override
    public void channelReadConfigData(String s, byte[] bytes) {

    }

    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage) {

    }
}
