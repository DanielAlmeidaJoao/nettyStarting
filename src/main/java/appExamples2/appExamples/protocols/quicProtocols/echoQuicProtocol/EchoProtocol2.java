package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.babelNewChannels.events.QUICMetricsEvent;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.EchoMessage2;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.SampleTimer2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.babel.channels.events.OnStreamConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import quicSupport.utils.QUICLogics;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

public class EchoProtocol2 extends GenericProtocol {


    private static final Logger logger = LogManager.getLogger(EchoProtocol2.class);
    public static final short PROTOCOL_ID = 204;
    private int channelId;
    private final Host myself; //My own address/port
    private Host dest;
    private Properties properties;
    public EchoProtocol2(Properties properties) throws IOException {
        super(EchoProtocol2.class.getName(),PROTOCOL_ID);
        this.properties = properties;
        String address = properties.getProperty("address");
        String port = properties.getProperty("port");
        logger.info("Receiver on {}:{}", address, port);
        this.myself = new Host(InetAddress.getByName(address), Integer.parseInt(port));

    }
    public void addChan(int channelId){
        this.channelId=channelId;
        registerSharedChannel(channelId);
        init(properties);
    }

    @Override
    public void init(Properties props) {
        //Nothing to do here, we just wait for event from the membership or the application
        registerMessageSerializer(channelId, EchoMessage2.MSG_ID, EchoMessage2.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            //registerChannelEventHandler(channelId, QUICMetricsEvent.EVENT_ID, this::uponChannelMetrics);
            //registerMessageHandler(channelId, EchoMessage2.MSG_ID, this::uponFloodMessage, this::uponMsgFail);

            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                registerTimerHandler(SampleTimer2.TIMER_ID,this::handTimer);
                setupPeriodicTimer(new SampleTimer2(),10000L,5000L);
            }

            /**
            if(myself.getPort()==8081){
                //Integer.parseInt(props.getProperty("nei_port")
                dest = new Host(InetAddress.getByName("localhost"),8082);
                openConnection(dest);
                logger.info("OPENNING CONNECTION TO {}",dest);
            }**/
        } catch (Exception e) {
            logger.error("Error registering message handler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        //logger.info("OPENING CONNECTION TO {}",myself);
        //EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
        //sendMessage(message,myself);
    }
    int cc = 0;
    private void handTimer (SampleTimer2 time, long id ){
        System.out.println("MESSAGE SENT!! "+cc);
        cc++;
        if(cc < 5){
            EchoMessage2 message = new EchoMessage2(myself,"LLL TIME: "+ System.currentTimeMillis());
            sendMessage(message,dest);
        }else{
            closeConnection(dest);
            cancelTimer(id);
        }
    }
    private void uponChannelMetrics(QUICMetricsEvent event, int channelId) {
        System.out.println("METRICS TRIGGERED!!!");
        System.out.println("CURRENT: "+QUICLogics.gson.toJson(event.getCurrent()));
        System.out.println("OLD: "+QUICLogics.gson.toJson(event.getOld()));
    }
    private void uponInConnectionUp(OnStreamConnectionUpEvent event, int channelId) {
        logger.info("CONNECTION TO {} IS UP.",event.getNode());
        if(dest!=null){

            EchoMessage2 message = new EchoMessage2(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
    }
    private void uponOutConnectionUp(OnStreamConnectionUpEvent event, int channelId) {
        logger.info("CONNECTION TO {} IS UP.",event.getNode());
        if(dest!=null){

            EchoMessage2 message = new EchoMessage2(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
    }
    private void uponFloodMessage(EchoMessage2 msg, Host from, short sourceProto, int channelId) {
        logger.info("{} Received {} from {}",getProtoId(), msg.getMessage(), from);
    }

    private void uponMsgFail(EchoMessage2 msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }
}
