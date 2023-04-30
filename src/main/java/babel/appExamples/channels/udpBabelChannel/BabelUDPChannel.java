package babel.appExamples.channels.udpBabelChannel;

import babel.appExamples.channels.babelQuicChannel.utils.BabelQuicChannelLogics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;
import udpSupport.channels.SingleThreadedUDPChannel;
import udpSupport.metrics.ChannelStats;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class BabelUDPChannel<T> extends SingleThreadedUDPChannel implements IChannel<T> {
    private static final Logger logger = LogManager.getLogger(BabelUDPChannel.class);
    public final boolean metrics;
    public final static String NAME = "BABEL_UDP_CHANNEL";
    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String DEFAULT_METRICS_INTERVAL = "-1";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";

    private final boolean triggerSent;


    private final ISerializer<T> serializer;
    private final ChannelListener<T> listener;

    public BabelUDPChannel(ISerializer<T> serializer, ChannelListener<T> list, Properties properties) throws Exception {
        super(properties);
        this.serializer = serializer;
        this.listener = list;
        metrics = super.metricsEnabled();
        if(metrics){
            int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY, DEFAULT_METRICS_INTERVAL));
            new DefaultEventExecutor().scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval, TimeUnit.MILLISECONDS);
        }
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
    }
    void readMetricsMethod(ChannelStats stats){
        UDPMetricsEvent quicMetricsEvent = new UDPMetricsEvent(stats);
        listener.deliverEvent(quicMetricsEvent);
    }
    void triggerMetricsEvent() {
        readMetrics(this::readMetricsMethod);
    }

    @Override
    public void onDeliverMessage(byte[] message, InetSocketAddress from) {
        //logger.info("MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",channelId,from,bytes.length);
        //logger.info("{}. MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",getSelf(),channelId,from,bytes.length);
        ByteBuf in = Unpooled.copiedBuffer(message);
        try {
            T payload = serializer.deserialize(in);
            listener.deliverMessage(payload, BabelQuicChannelLogics.toBabelHost(from));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(T msg, Host peer, int connection) {
        InetSocketAddress dest = BabelQuicChannelLogics.toInetSOcketAddress(peer);
        ByteBuf out = Unpooled.buffer();
        try {
            serializer.serialize(msg, out);
            byte [] toSend = new byte[out.readableBytes()];
            out.readBytes(toSend);
            super.sendMessage(toSend,dest,toSend.length);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest){
        ByteBuf buf = Unpooled.copiedBuffer(message,0,message.length);
        T msg;
        try {
            msg = (T) serializer.deserialize(buf);
            Host host = BabelQuicChannelLogics.toBabelHost(dest);
            if(error==null&&triggerSent){
                listener.messageSent(msg, host);
            }else if(error!=null){
                listener.messageFailed(msg, host,error);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        buf.release();
    }

    @Override
    public void closeConnection(Host peer, int connection) {
        logger.debug("CLOSE CONNECTION. UNSUPPORTED OPERATION ON UDP");
        //Throwable t = new Throwable("PEER DISCONNECTED!");
        listener.deliverEvent(new OutConnectionDown(peer,null));
    }

    @Override
    public void openConnection(Host peer) {
        logger.debug("OPEN CONNECTION. UNSUPPORTED OPERATION ON UDP");
        listener.deliverEvent(new OutConnectionUp(peer));
    }
}
