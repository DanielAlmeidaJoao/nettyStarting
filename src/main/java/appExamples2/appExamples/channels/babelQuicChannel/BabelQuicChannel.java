package appExamples2.appExamples.channels.babelQuicChannel;

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;
import pt.unl.fct.di.novasys.babel.channels.NewIChannel;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionDown;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionUp;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionDown;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionUp;
import quicSupport.channels.SingleThreadedQuicChannel;
import quicSupport.utils.NetworkRole;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class BabelQuicChannel<T> extends SingleThreadedQuicChannel implements NewIChannel<T> {
    private static final Logger logger = LogManager.getLogger(BabelQuicChannel.class);
    public final boolean metrics;
    public final static String NAME = "BabelQuicChannel";
    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String DEFAULT_METRICS_INTERVAL = "-1";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";

    private final boolean triggerSent;
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
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));

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
        logger.debug("STREAM {} CREATED FOR {} CONNECTION",streamId,peer);
    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from) {
        //logger.info("MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",channelId,from,bytes.length);
        //logger.info("{}. MESSAGE FROM {} STREAM. FROM PEER {}. SIZE {}",getSelf(),channelId,from,bytes.length);
        try {
            listener.deliverMessage(FactoryMethods.unSerialize(serializer,bytes),FactoryMethods.toBabelHost(from));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onConnectionUp(boolean incoming, InetSocketAddress peer) {
        Host host = FactoryMethods.toBabelHost(peer);
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
        Host host = FactoryMethods.toBabelHost(peer);
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
    public void sendMessage(T msg, Host peer, short proto) {
        try {
            /**
            BabelMessage babelMessage = (BabelMessage) msg;
            System.out.println(babelMessage.getSourceProto());
            System.out.println(babelMessage.getDestProto());
             **/
            byte [] toSend = FactoryMethods.toSend(serializer,msg);
            super.send(FactoryMethods.toInetSOcketAddress(peer),FactoryMethods.toSend(serializer,msg),toSend.length);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessageSent(byte[] message, int len, Throwable error,InetSocketAddress peer) {
        try {
            if(error==null&&triggerSent){
                listener.messageSent(FactoryMethods.unSerialize(serializer,message),FactoryMethods.toBabelHost(peer));
            }else if(error!=null){
                listener.messageFailed(FactoryMethods.unSerialize(serializer,message),FactoryMethods.toBabelHost(peer),error);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeConnection(Host peer, short proto) {
        super.closeConnection(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public void openConnection(Host peer, short proto) {
        super.open(FactoryMethods.toInetSOcketAddress(peer));
    }

    @Override
    public void registerChannelInterest(short protoId) {

    }
}
