package babel.appExamples.channels.babelQuicChannel;

import babel.appExamples.channels.babelQuicChannel.utils.BabelQuicChannelLogics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.channels.SingleThreadedQuicChannel;
import quicSupport.utils.NetworkRole;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class BabelQuicChannel<T> extends SingleThreadedQuicChannel  implements IChannel<T> {

    private static final Logger logger = LogManager.getLogger(BabelQuicChannel.class);
    public final boolean metrics;
    public final static String NAME = "BabelQuicChannel";
    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String DEFAULT_METRICS_INTERVAL = "-1";



    private final ISerializer<T> serializer;
    private final ChannelListener<T> listener;

    public BabelQuicChannel(ISerializer<T> serializer, ChannelListener<T> list, Properties properties) throws IOException {
        super(properties, NetworkRole.CHANNEL);
        this.serializer = serializer;
        this.listener = list;
        metrics = super.enabledMetrics();
        if(metrics){
            int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY, DEFAULT_METRICS_INTERVAL));
            new DefaultEventExecutor().scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval, TimeUnit.MILLISECONDS);
        }

    }
    void readMetricsMethod(List<QuicConnectionMetrics> current, List<QuicConnectionMetrics> old){
        QUICMetricsEvent quicMetricsEvent = new QUICMetricsEvent(current,old);
        listener.deliverEvent(quicMetricsEvent);
    }
    void triggerMetricsEvent() {
        readMetrics(this::readMetricsMethod);
    }

    @Override
    public void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId) {
        logger.info("ERROR ON STREAM {} BELONG TO CONNECTION {}. REASON: {}",streamId,peer,error.getLocalizedMessage());
    }

    @Override
    public void onStreamCreatedHandler(InetSocketAddress peer, String streamId) {
        logger.info("STREAM {} CREATED FOR {} CONNECTION",streamId,peer);
    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from) {
        logger.info("MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",channelId,from,bytes.length);
        ByteBuf in = Unpooled.copiedBuffer(bytes);
        try {
            T payload = serializer.deserialize(in);
            listener.deliverMessage(payload, BabelQuicChannelLogics.toBabelHost(from));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        Throwable t = new Throwable("PEER DISCONNECTED!");
        Host host = BabelQuicChannelLogics.toBabelHost(peer);
        if(incoming){
            logger.error("Inbound connection from {} is down" + peer);
            listener.deliverEvent(new InConnectionDown(host,t));
        }else{
            logger.debug("OutboundConnectionDown to " +peer+ "");
            listener.deliverEvent(new OutConnectionDown(host,t));
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
    public void onStreamClosedHandler(InetSocketAddress peer, String streamId) {
        logger.info("STREAM {} OF {} CONNECTION {} CLOSED.",streamId,peer);
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        InetSocketAddress dest = BabelQuicChannelLogics.toInetSOcketAddress(peer);
        ByteBuf out = Unpooled.buffer();
        try {
            serializer.serialize(msg, out);
            byte [] toSend = new byte[out.readableBytes()];
            out.readBytes(toSend);
            super.send(dest,toSend,toSend.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessageSent(byte[] message, int len, Throwable error,InetSocketAddress peer) {
        ByteBuf buf = Unpooled.buffer(len);
        buf.writeBytes(message);
        T msg;
        try {
            msg = (T) serializer.deserialize(buf);
            Host host = BabelQuicChannelLogics.toBabelHost(peer);
            if(error==null){
                listener.messageSent(msg, host);
            }else{
                listener.messageFailed(msg, host,error);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
