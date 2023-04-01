package babel.appExamples.channels.babelQuicChannel;

import babel.appExamples.channels.babelQuicChannel.utils.BabelQuicChannelLogics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.channels.SingleThreadedQuicChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class BabelQuicChannel<T> extends SingleThreadedQuicChannel  implements IChannel<T> {

    private static final Logger logger = LogManager.getLogger(BabelQuicChannel.class);
    public final boolean metrics;


    private final ISerializer<T> serializer;
    private final ChannelListener<T> listener;

    public BabelQuicChannel(ISerializer<T> serializer, ChannelListener<T> list, Properties properties) throws IOException {
        super(properties);
        this.serializer = serializer;
        this.listener = list;
        metrics = super.enabledMetrics();
    }

    @Override
    public void onStreamErrorHandler(InetSocketAddress peer, QuicStreamChannel channel) {

    }

    @Override
    public void onStreamCreatedHandler(InetSocketAddress peer, QuicStreamChannel channel) {

    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from) {

    }

    @Override
    public void onConnectionUp(boolean incoming, InetSocketAddress peer) {
        Host host = BabelQuicChannelLogics.toBabelHost(peer);
        if(incoming){
            logger.debug("InboundConnectionUp " + peer);
            listener.deliverEvent(new InConnectionUp(host));
        }else{
            logger.debug("OutboundConnectionUp " + host);
            listener.deliverEvent(new OutConnectionUp(host));
        }
    }

    @Override
    public void onConnectionDown(InetSocketAddress peer, boolean incoming) {
        if(incoming){
            logger.debug("OutboundConnectionDown to " +peer+ "");
        }else{
            logger.error("Inbound connection from {} is down" + peer);
        }

        if (metrics){
            //TODO in the implementation method
        }
    }

    @Override
    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {
        logger.info("FAILED TO OPEN CONNECTION TO {}. REASON: {}",peer,cause.getLocalizedMessage());
    }

    @Override
    public void failedToCloseStream(String streamId, Throwable reason) {
        logger.info("FAILED TO CLOSE STREAM {}. REASON: {}",streamId,reason.getLocalizedMessage());
    }



    @Override
    public void failedToCreateStream(InetSocketAddress peer, Throwable error) {
        logger.info("FAILED TO CREATE A STREAM TO {}. REASON: {}",peer,error.getLocalizedMessage());
    }

    @Override
    public void failedToGetMetrics(Throwable cause) {
        logger.info("FAILED TO GET METRICS. REASON: {}",cause.getLocalizedMessage());
    }

    @Override
    public void onStreamClosedHandler(InetSocketAddress peer, QuicStreamChannel channel) {

    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        InetSocketAddress dest = BabelQuicChannelLogics.toInetSOcketAddress(peer);
        ByteBuf out = Unpooled.buffer();
        try {
            serializer.serialize(msg, out);
            byte [] toSend =  out.array();
            super.send(dest, out.array(),toSend.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessageSent(byte[] message, int len, Throwable error,InetSocketAddress peer) {
        ByteBuf buf = Unpooled.buffer(len);
        buf.writeBytes(message);
        T msg = null;
        try {
            msg = (T) serializer.deserialize(buf);
            Host host = BabelQuicChannelLogics.toBabelHost(peer);
            if(error==null){
                listener.messageSent(msg, host);
            }else{
                listener.messageFailed(msg, host,error);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeConnection(Host peer, int connection) {
        super.closeConnection(BabelQuicChannelLogics.toInetSOcketAddress(peer));
    }

    @Override
    public void openConnection(Host peer) {
        openConnection(BabelQuicChannelLogics.toInetSOcketAddress(peer));
    }
}
