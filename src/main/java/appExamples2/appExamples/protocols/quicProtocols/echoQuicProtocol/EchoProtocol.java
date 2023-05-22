package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.babelQuicChannel.BabelQuicChannel;
import appExamples2.appExamples.channels.babelQuicChannel.events.QUICMetricsEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamClosedEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamCreatedEvent;
import appExamples2.appExamples.channels.streamingChannel.BabelStreamingChannel;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.EchoMessage;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.SampleTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.channel.StreamingChannel;
import org.tcpStreamingAPI.utils.TCPStreamUtils;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionUp;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionUp;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import quicSupport.utils.QUICLogics;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

public class EchoProtocol extends GenericProtocol {


    private static final Logger logger = LogManager.getLogger(EchoProtocol.class);
    public static final short PROTOCOL_ID = 200;
    public int channelId;
    private final Host myself; //My own address/port
    private Host dest;
    private Properties properties;
    public EchoProtocol(Properties properties) throws IOException {
        super(EchoProtocol.class.getName(),PROTOCOL_ID);
        String address = properties.getProperty("address");
        String port = properties.getProperty("port");
        logger.info("Receiver on {}:{}", address, port);
        this.myself = new Host(InetAddress.getByName(address), Integer.parseInt(port));

        //channelProps.setProperty("metrics_interval","2000");


        channelId = makeChan("quic",address,port);
        System.out.println(myself);
        System.out.println("CHANNEL CREATED "+channelId);
        this.properties = properties;
    }
    private int makeChan(String channelName,String address, String port) throws IOException {
        Properties channelProps = new Properties();
        if(channelName.equalsIgnoreCase("quic")){

            //channelProps.setProperty("metrics_interval","2000");

            channelProps.setProperty(QUICLogics.ADDRESS_KEY,address);
            channelProps.setProperty(QUICLogics.PORT_KEY,port);
            //channelProps.setProperty(QUICLogics.QUIC_METRICS,"true");

            channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_FILE_KEY,"keystore.jks");
            channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_PASSWORD_KEY,"simple");
            channelProps.setProperty(QUICLogics.SERVER_KEYSTORE_ALIAS_KEY,"quicTestCert");

            channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_FILE_KEY,"keystore2.jks");
            channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_PASSWORD_KEY,"simple");
            channelProps.setProperty(QUICLogics.CLIENT_KEYSTORE_ALIAS_KEY,"clientcert");
            channelProps.setProperty(QUICLogics.CONNECT_ON_SEND,"true");
            channelProps.setProperty(QUICLogics.MAX_IDLE_TIMEOUT_IN_SECONDS,"300");
            return createChannel(BabelQuicChannel.NAME, channelProps);
        }else{
            channelProps.setProperty(StreamingChannel.ADDRESS_KEY,address);
            channelProps.setProperty(StreamingChannel.PORT_KEY,port);
            channelProps.setProperty(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
            return createChannel(BabelStreamingChannel.NAME, channelProps);
        }
    }
    @Override
    public void init(Properties props) {
        //Nothing to do here, we just wait for event from the membership or the application
        registerMessageSerializer(channelId, EchoMessage.MSG_ID, EchoMessage.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            registerChannelEventHandler(channelId, QUICMetricsEvent.EVENT_ID, this::uponChannelMetrics);
            //registerMessageHandler(channelId, EchoMessage.MSG_ID, this::uponFloodMessage, this::uponMsgFail);
            registerQUICMessageHandler(channelId, EchoMessage.MSG_ID, this::uponFloodMessageQUIC,null,this::uponMsgFail);

            registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
            registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);

            registerChannelEventHandler(channelId, StreamCreatedEvent.EVENT_ID, this::uponStreamCreated);
            registerChannelEventHandler(channelId, StreamClosedEvent.EVENT_ID, this::uponStreamClosed);

            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                openConnection(dest);
                //registerTimerHandler(SampleTimer.TIMER_ID,this::handTimer);
                //setupPeriodicTimer(new SampleTimer(),8000L,5000L);
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
    public void sendMessage(String message, String stream){
        EchoMessage echoMessage = new EchoMessage(myself,message);
        super.sendMessage(echoMessage,stream);
    }
    public void sendMessage(String message){
        EchoMessage echoMessage = new EchoMessage(myself,message);
        sendMessage(echoMessage,dest);
    }
    public void createStream(){
        super.createStream(dest);
    }

    public void closeStreamM(String stream){
        super.closeStream(stream);
    }
    int hh = 0 ;
    private void handTimer (SampleTimer time, long id ){
        hh++;
        System.out.println("MESSAGE SENT!! ++ "+hh);
        if(hh<8){
            EchoMessage message = new EchoMessage(myself,"TIME: "+ System.currentTimeMillis());
            sendMessage(message,dest);
        }else {
            closeConnection(dest);
            cancelTimer(id);
        }

    }
    private void uponChannelMetrics(QUICMetricsEvent event, int channelId) {
        System.out.println("METRICS TRIGGERED!!!");
        System.out.println("CURRENT: "+QUICLogics.gson.toJson(event.getCurrent()));
        System.out.println("OLD: "+QUICLogics.gson.toJson(event.getOld()));
    }
    private void uponStreamCreated(StreamCreatedEvent event, int channelId) {
        logger.info("STREAM {}::{} IS UP.",event.streamId,event.host);
    }
    private void uponStreamClosed(StreamClosedEvent event, int channelId) {
        logger.info("STREAM {}[::]{} IS DOWN.",event.streamId,event.host);
    }
    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP.",event.getNode());
        if(dest==null){
            dest = event.getNode();
        }
        /**
        if(dest!=null){
            EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
        **/
    }
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP.",event.getNode());
        if(dest==null){
            dest = event.getNode();
        }
        /**
        if(dest!=null){
            EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
         **/
    }
    private void uponFloodMessage(EchoMessage msg, Host from, short sourceProto, int channelId) {
        logger.info("Received {} from {}", msg.getMessage(), from);
    }
    private void uponFloodMessageQUIC(EchoMessage msg, Host from, short sourceProto, int channelId, String streamId) {
        logger.info("Received QUIC {} from {} {}", msg.getMessage(), from, streamId);
    }
    private void uponMsgFail(EchoMessage msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }
}