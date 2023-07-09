package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelQuicChannel.BabelQUIC_TCP_Channel;
import appExamples2.appExamples.channels.babelQuicChannel.BytesMessageSentOrFail;
import appExamples2.appExamples.channels.babelQuicChannel.events.QUICMetricsEvent;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.EchoMessage;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.SampleTimer;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import pt.unl.fct.di.novasys.babel.internal.BabelInBytesWrapperEvent;
import pt.unl.fct.di.novasys.babel.internal.BytesMessageInEvent;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.channel.StreamingChannel;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;

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


        channelId = makeChan(properties.getProperty("NETWORK_PROTO"),address,port);
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
            channelId = createChannel(BabelQUIC_TCP_Channel.NAME_QUIC, channelProps);

        }else{
            System.out.println("TCP ON");
            channelProps.setProperty(StreamingChannel.ADDRESS_KEY,address);
            channelProps.setProperty(StreamingChannel.PORT_KEY,port);
            channelProps.setProperty(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
            channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"TRUE");

            channelId = createChannel(BabelQUIC_TCP_Channel.NAME_TCP, channelProps);


        }
        return channelId;
    }
    @Override
    public void init(Properties props) {
        //Nothing to do here, we just wait for event from the membership or the application
        registerMessageSerializer(channelId, EchoMessage.MSG_ID, EchoMessage.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            registerMessageHandler(channelId, EchoMessage.MSG_ID, this::uponFloodMessageQUIC, this::uponMsgFail);
            registerChannelEventHandler(channelId, QUICMetricsEvent.EVENT_ID, this::uponChannelMetrics);
            registerBytesMessageHandler(channelId,HANDLER_ID,this::uponBytesMessage,null, this::uponMsgFail3);
            registerMandatoryStreamDataHandler(channelId,this::uponStreamBytes,null, this::uponMsgFail2);
            //registerStreamDataHandler(channelId,HANDLER_ID2,this::uponStreamBytes2,null, this::uponMsgFail2);

            registerChannelEventHandler(channelId, OnConnectionUpEvent.EVENT_ID, this::uponInConnectionUp);

            //registerChannelEventHandler(channelId, StreamCreatedEvent.EVENT_ID, this::uponStreamCreated);
            //registerChannelEventHandler(channelId, StreamClosedEvent.EVENT_ID, this::uponStreamClosed);

            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                System.out.println(openStreamConnection(dest,channelId));
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
    private void toDo(){
        (new Exception("TO DO THIS BAGULHO")).printStackTrace();
    }
    public void sendMessage(String message, String stream){
        TransmissionType transmissionType = getConnectionType(channelId,stream);
        if(TransmissionType.UNSTRUCTURED_STREAM == transmissionType){
            //super.sendStream(channelId,message.getBytes(),message.length(),stream);
            toDo();
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
                System.out.println("OPENNED MESSAGE CONNECTION "+ openMessageConnection(host));
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
        toDo();
        //super.sendStream(channelId,message.getBytes(),message.length(),dest);
    }
    public void sendStream(String message, String streamId){
        System.out.println("SENDING "+message.length());
        toDo();
        //super.sendStream(channelId,message.getBytes(),message.length(),streamId);
    }
    public void createStream(){

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
    private void uponInConnectionUp(OnConnectionUpEvent event, int channelId) {
        System.out.println("PORRRAS 444");

        if(event.inConnection){
            System.out.println("PORRRAS 66");
            uponOutConnectionUp(event,channelId);
            return;
        }
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE: {}. id: {}",event.getNode(),event.type,event.conId);
        System.out.println("PORRRAS 21");
        if(dest==null){
            dest = event.getNode();
        }
        TransmissionType tp = getConnectionType(channelId,event.conId);
        System.out.println("CONNECTION TYPR +++ "+tp);
        if(tp == TransmissionType.UNSTRUCTURED_STREAM){
            for (int i = 0; i < 10; i++) {
                byte [] hh = new byte[4];
                Unpooled.buffer(4).writeInt(i).readBytes(hh,0,4);
                toDo();
                //super.sendStream(channelId,hh,hh.length,event.conId);
            }
        }

        /**
        if(dest!=null){
            EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
        **/
    }
    private void uponOutConnectionUp(OnConnectionUpEvent event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE {}. conId: {}",event.getNode(),event.type,event.conId);
        if(dest==null){
            dest = event.getNode();
        }
        //System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.conId));
        /**
        if(dest!=null){
            EchoMessage message = new EchoMessage(myself,"OLA BABEL SUPPORTING QUIC PORRAS!!!");
            sendMessage(message,dest);
            logger.info("{} MESSAGE SENT!!! TO {} ",myself,dest);
        }
         **/
    }
    private void uponBytesMessage(BytesMessageInEvent event) {
        logger.info("Received bytes3: {} from {}", new String(event.getMsg()),event.getFrom());
    }
    private void uponStreamBytes(BabelInBytesWrapperEvent event) {
        logger.info("Received bytes4: {} from {}",event.babelOutputStream.readableBytes(),event.getFrom());
    }
    private void uponStreamBytes2(BytesMessageInEvent event) {
        logger.info("Received 2bytes2: {} from {}",event.getMsg().length,event.getFrom());
    }

    private void uponFloodMessageQUIC(EchoMessage msg, Host from, short sourceProto, int channelId, String streamId) {
        logger.info("Received QUIC {} from {} {}", msg.getMessage(), from, streamId);
    }
    private void uponMsgFail(EchoMessage msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("NOT BYTES Message {} to {} failed, reason: {}", msg, host, throwable);
        logger.info("DATA SENT <{}>",msg.getMessage());

    }
    private void uponMsgFail3(BytesMessageSentOrFail msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("BYTES Message {} to {} failed, reason: {}", msg, host, throwable);
        logger.info("SENT MESSAGE <{}>",new String(msg.data));
    }
    private void uponMsgFail2(BytesMessageSentOrFail msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
        logger.info("GGG SENT MESSAGE <{}>",new String(msg.data));
    }
}
