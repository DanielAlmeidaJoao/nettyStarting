package appExamples2.appExamples.channels.streamingChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.channel.SingleThreadedStreamingChannel;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionDown;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionUp;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionUp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import static appExamples2.appExamples.channels.FactoryMethods.toBabelHost;

public class BabelStreamingChannel<T> extends SingleThreadedStreamingChannel implements NewIChannel<T> {
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
    public void sendMessage(T msg, Host peer, short proto) {
        try {
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            send(toSend,toSend.length,FactoryMethods.toInetSOcketAddress(peer));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void closeConnection(Host peer, short connection) {
        super.closeConnection(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public void openConnection(Host peer,short proto) {
        super.open(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public void onChannelInactive(InetSocketAddress peer) {
        Throwable cause = new Throwable(String.format("CHANNEL %S CLOSED.",peer));
        listener.deliverEvent(new InConnectionDown(toBabelHost(peer), cause));
    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes,InetSocketAddress from) {
        try {
            listener.deliverMessage(FactoryMethods.unSerialize(serializer,bytes), toBabelHost(from));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer) {
        if(handShakeMessage==null){
            listener.deliverEvent(new OutConnectionUp(toBabelHost(peer)));
        }else{
            listener.deliverEvent(new InConnectionUp(toBabelHost(peer)));
        }
    }

    @Override
    public void sendFailed(InetSocketAddress peer, Throwable reason) {
        logger.error("FAILED TO SEND MESSAGE TO {}. REASON: {}",peer,reason);
    }

    @Override
    public void sendSuccess(byte[] data, InetSocketAddress peer) {
        try {
            if(triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,data), toBabelHost(peer));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {
        logger.info("CONNECTION TO {} FAILED. CAUSE = {}.",peer,cause);
    }



    @Override
    public void registerChannelInterest(short protoId) {

    }
}
