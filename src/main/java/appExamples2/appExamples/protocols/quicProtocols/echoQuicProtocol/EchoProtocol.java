package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelQuicChannel.BabelQuicChannel;
import appExamples2.appExamples.channels.babelQuicChannel.BytesMessageSentOrFail;
import appExamples2.appExamples.channels.babelQuicChannel.events.QUICMetricsEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamClosedEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamCreatedEvent;
import appExamples2.appExamples.channels.streamingChannel.BabelStreamingChannel;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.EchoMessage;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.SampleTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tcpSupport.tcpStreamingAPI.channel.StreamingChannel;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionUp;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionUp;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;

public class EchoProtocol extends GenericProtocolExtension {
    private static final Logger logger = LogManager.getLogger(EchoProtocol.class);
    public static final short PROTOCOL_ID = 200;
    public int channelId;
    private final Host myself; //My own address/port
    private Host dest;
    private Properties properties;
    public EchoProtocol(Properties properties) throws Exception {
        super(EchoProtocol.class.getName(),PROTOCOL_ID);
        String address = properties.getProperty("address");
        String port = properties.getProperty("port");
        logger.info("Receiver on {}:{}", address, port);
        this.myself = new Host(InetAddress.getByName(address), Integer.parseInt(port));

        //channelProps.setProperty("metrics_interval","2000");


        channelId = makeChan("TCP",address,port);
        System.out.println(myself);
        System.out.println("CHANNEL CREATED "+channelId);
        this.properties = properties;
    }
    private int makeChan(String channelName,String address, String port) throws Exception {
        Properties channelProps = new Properties();
        if(channelName.equalsIgnoreCase("quic")){
            System.out.println("QUIC ON");
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
            channelId = createChannel(BabelQuicChannel.NAME, channelProps);
            registerQUICMessageHandler(channelId, EchoMessage.MSG_ID, this::uponFloodMessageQUIC,null,this::uponMsgFail);

        }else{
            System.out.println("TCP ON");
            channelProps.setProperty(StreamingChannel.ADDRESS_KEY,address);
            channelProps.setProperty(StreamingChannel.PORT_KEY,port);
            channelProps.setProperty(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
            channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"TRUE");

            channelId = createChannel(BabelStreamingChannel.NAME, channelProps);

            registerMessageHandler(channelId, EchoMessage.MSG_ID, this::uponFloodMessage, this::uponMsgFail);

        }

        return channelId;
    }
    @Override
    public void init(Properties props) {
        //Nothing to do here, we just wait for event from the membership or the application
        registerMessageSerializer(channelId, EchoMessage.MSG_ID, EchoMessage.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            registerChannelEventHandler(channelId, QUICMetricsEvent.EVENT_ID, this::uponChannelMetrics);
            registerBytesMessageHandler(channelId,HANDLER_ID,this::uponBytesMessage,null, this::uponMsgFail3);
            registerMandatoryStreamDataHandler(channelId,this::uponStreamBytes,null, this::uponMsgFail2);
            registerStreamDataHandler(channelId,HANDLER_ID2,this::uponStreamBytes2,null, this::uponMsgFail2);

            registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
            registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);

            registerChannelEventHandler(channelId, StreamCreatedEvent.EVENT_ID, this::uponStreamCreated);
            registerChannelEventHandler(channelId, StreamClosedEvent.EVENT_ID, this::uponStreamClosed);

            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                System.out.println(openConnection(dest));
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
    boolean sendByte = true;
    public static final short HANDLER_ID = 2;
    public static final short HANDLER_ID2 = 3;
    public void sendMessage(String message, String stream){
        TransmissionType transmissionType = getConnectionType(channelId,stream);
        if(TransmissionType.UNSTRUCTURED_STREAM == transmissionType){
            super.sendStream(channelId,message.getBytes(),message.length(),stream);
        }else{
            if(sendByte){
                super.sendMessage(channelId,message.getBytes(),message.length(),stream,getProtoId(),getProtoId(),HANDLER_ID);
            }else {
                EchoMessage echoMessage = new EchoMessage(myself,message);
                super.sendMessage(echoMessage,stream);
            }
            sendByte =!sendByte;
        }

    }
    public void openSS(String port, String type){
        try{
            Host host = new Host(myself.getAddress(),Integer.parseInt(port));
            if("M".equalsIgnoreCase(type)){
                System.out.println("OPENNED MESSAGE CONNECTION "+openConnection(host));
            }else {
                System.out.println("OPENNED STREAM CONNECTION"+openStreamConnection(host,channelId));
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
    public void sendMessage(String message){
        System.out.println(sendByte+" SENDBYTE");
        if(sendByte){
            super.sendMessage(channelId,message.getBytes(),message.length(),dest,getProtoId(),getProtoId(),HANDLER_ID);
        }else{
            EchoMessage echoMessage = new EchoMessage(myself,message);
            sendMessage(echoMessage,dest);
        }
        sendByte =!sendByte;
    }
    public void sendStream(String message){
        System.out.println("SENDING "+message.length());
        super.sendStream(channelId,message.getBytes(),message.length(),dest);
    }
    public void sendStream(String message, String streamId){
        System.out.println("SENDING "+message.length());
        super.sendStream(channelId,message.getBytes(),message.length(),streamId);
    }
    public void createStream(){
        if(sendByte){
            super.createStream(channelId,getProtoId(),getProtoId(),HANDLER_ID2, dest, TransmissionType.UNSTRUCTURED_STREAM);
        }else{
            super.createStream(channelId,getProtoId(),getProtoId(),HANDLER_ID2, dest, TransmissionType.STRUCTURED_MESSAGE);
        }
        sendByte =!sendByte;
    }

    public void closeStreamM(String stream){
        super.closeStream(stream);
    }

    public void isConnected(){
        System.out.println("IS CONNECTED: "+isConnected(channelId,dest));
    }
    public void connections(){
        InetSocketAddress [] cons = getConnections(channelId);
        System.out.println("CONS: "+cons.length);
        for (InetSocketAddress con : cons) {
            System.out.println(con);
        }
    }
    public void numberConnected(){
        System.out.println("NUMBER CONNECTED: "+numConnectedPeers(channelId));
    }
    public void streamsAvailable(){
        String [] strings = getStreams(channelId);
        System.out.println("STREAMS: "+strings.length);
        for (String string : strings) {
            System.out.println("STREAM: "+string);
        }
    }
    public void shutDown(){
        shutDownChannel(channelId,getProtoId());
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
        logger.info("STREAM {}::{} IS UP. DATA TRANSMISSION TYPE: {}",event.streamId,event.host,event.transmissionType);
        //System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.streamId));
    }
    private void uponStreamClosed(StreamClosedEvent event, int channelId) {
        logger.info("STREAM {}[::]{} IS DOWN.",event.streamId,event.host);
        //System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.streamId));

    }
    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE: {}. id: {}",event.getNode(),event.type,event.conId);
        if(dest==null){
            dest = event.getNode();
        }
        System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.conId));
        /**
        if(dest!=null){
            EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
        **/
    }
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE {}. conId: {}",event.getNode(),event.type,event.conId);
        if(dest==null){
            dest = event.getNode();
        }
        System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.conId));
        /**
        if(dest!=null){
            EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
         **/
    }
    private void uponBytesMessage(byte [] msg, Host from, short sourceProto, int channelId, String streamId) {
        logger.info("Received bytes: {} from {}", new String(msg), from);
    }
    private void uponStreamBytes(byte [] msg, Host from, short sourceProto, int channelId, String streamId) {
        logger.info("Received bytes: {} from {}",msg.length, from);
    }
    private void uponStreamBytes2(byte [] msg, Host from, short sourceProto, int channelId, String streamId) {
        logger.info("Received 2bytes2: {} from {}",msg.length, from);
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
    private void uponMsgFail3(BytesMessageSentOrFail msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }
    private void uponMsgFail2(BytesMessageSentOrFail msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }
}
