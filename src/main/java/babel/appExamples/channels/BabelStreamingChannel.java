package babel.appExamples.channels;

import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.channel.SingleThreadedStreamingChannel;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
public class BabelStreamingChannel<T> extends SingleThreadedStreamingChannel implements IChannel<T> {
    private static final Logger logger = LogManager.getLogger(BabelStreamingChannel.class);
    public final static String TRIGGER_SENT_KEY = "trigger_sent";

    private final ISerializer<T> serializer;
    private final ChannelListener<T> listener;
    private final boolean triggerSent;

    public BabelStreamingChannel(ISerializer<T> serializer, ChannelListener<T> list, Properties properties) throws IOException {
        super(properties);
        this.serializer = serializer;
        this.listener = list;
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        try {
            FactoryMethods.toInetSOcketAddress(peer);
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            send(toSend,toSend.length,toInetSocketAddress(peer));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeConnection(Host peer, int connection) {
        super.closeConnection(toInetSocketAddress(peer));
    }

    @Override
    public void openConnection(Host peer) {
        super.openConnection(toInetSocketAddress(peer));
    }

    @Override
    public void onChannelInactive(InetSocketAddress peer) {
        Throwable cause = new Throwable(String.format("CHANNEL %S CLOSED.",peer));
        listener.deliverEvent(new InConnectionDown(toBabelHost(peer), cause));
    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes,InetSocketAddress from) {
        try {
            listener.deliverMessage(FactoryMethods.unSerialize(serializer,bytes),FactoryMethods.toBabelHost(from));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer) {
        listener.deliverEvent(new InConnectionUp(toBabelHost(peer)));
    }

    @Override
    public void sendFailed(InetSocketAddress peer, Throwable reason) {

    }

    @Override
    public void sendSuccess(byte[] data, InetSocketAddress peer) {
        try {
            if(triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,data),FactoryMethods.toBabelHost(peer));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {
        logger.info("CONNECTION TO {} FAILED. CAUSE = {}.",peer,cause);
    }
    private InetSocketAddress toInetSocketAddress(Host host){
        return new InetSocketAddress(host.getAddress().getHostAddress(),host.getPort());
    }
    private Host toBabelHost(InetSocketAddress address){
        return new Host(address.getAddress(),address.getPort());
    }
}
