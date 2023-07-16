package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.babelQuicChannel.BabelQUIC_TCP_Channel;
import appExamples2.appExamples.channels.babelQuicChannel.BytesMessageSentOrFail;
import appExamples2.appExamples.channels.babelQuicChannel.events.QUICMetricsEvent;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.EchoMessage;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.SampleTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.events.OnMessageConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.channels.events.OnOpenConnectionFailed;
import pt.unl.fct.di.novasys.babel.channels.events.OnStreamConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import pt.unl.fct.di.novasys.babel.internal.BabelStreamDeliveryEvent;
import pt.unl.fct.di.novasys.babel.internal.BytesMessageInEvent;
import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.channel.NettyTCPChannel;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.TCPStreamUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
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
            channelProps.setProperty(NettyTCPChannel.ADDRESS_KEY,address);
            channelProps.setProperty(NettyTCPChannel.PORT_KEY,port);
            channelProps.setProperty(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
            //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"FALSE");

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

            registerChannelEventHandler(channelId, OnStreamConnectionUpEvent.EVENT_ID, this::uponStreamConnectionUp);
            //uponOpenConnectionFailed
            registerChannelEventHandler(channelId, OnMessageConnectionUpEvent.EVENT_ID, this::uponMessageConnectionUp);
            registerChannelEventHandler(channelId, OnOpenConnectionFailed.EVENT_ID, this::uponOpenConnectionFailed);


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
        System.out.println("CACCCLED "+transmissionType);

        if(TransmissionType.UNSTRUCTURED_STREAM == transmissionType){
            //super.sendStream(channelId,message.getBytes(),message.length(),stream);
            for (BabelInputStream babelInputStream : streams) {
                babelInputStream.sendBytes(message.getBytes());
            }
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
        for (BabelInputStream stream : streams) {
            stream.sendBytes(message.getBytes());
        }
        //super.sendStream(channelId,message.getBytes(),message.length(),dest);
    }
    public void sendStream(String message, String streamId){
        System.out.println("SENDING "+message.length());
        for (BabelInputStream stream : streams) {
            stream.sendBytes(message.getBytes());
        }
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
    public List<BabelInputStream> streams = new LinkedList<>();
    private void uponStreamConnectionUp(OnStreamConnectionUpEvent event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE: {}. id: {}",event.getNode(),event.type,event.conId);
        streams.add(event.babelInputStream);
        event.babelInputStream.setFlushMode(true);
        if(event.inConnection){
            if(dest==null){
                dest = event.getNode();
            }
        }else {
            System.out.println("PORRRAS 21");
            if(dest==null){
                dest = event.getNode();
            }
            TransmissionType tp = getConnectionType(channelId,event.conId);
            System.out.println("CONNECTION TYPR +++ "+tp);
            if(tp == TransmissionType.UNSTRUCTURED_STREAM){
                for (int i = 0; i < 10; i++) {
                    for (BabelInputStream stream : streams) {
                        stream.sendInt(i);
                        stream.flushStream();
                    }
                }
            }
        }
    }
    private void uponMessageConnectionUp(OnMessageConnectionUpEvent event, int channelId) {
        logger.info("CONNECTION UP: {} {} {}",event.conId,event.inConnection,event.type);
        if(dest==null){
            dest = event.getNode();
        }
    }
    private void uponOpenConnectionFailed(OnOpenConnectionFailed event, int channelId) {
        logger.info("CONNECTION FAILED: {} {} {}",event.connectionId,event.node,event.type);
        if(dest==null){
            dest = event.getNode();
        }
    }

    private void uponBytesMessage(BytesMessageInEvent event) {
        logger.info("Received bytes3: {} from {}", new String(event.getMsg()),event.getFrom());
    }
    private void uponStreamBytes(BabelStreamDeliveryEvent event) {
        System.out.println("AVAILABLE "+event.babelOutputStream.readableBytes());
        while(event.babelOutputStream.readableBytes()>=4){
            if(85 == event.babelOutputStream.readableBytes()){
                System.exit(-1);
            }
            int read = event.babelOutputStream.readInt();
            logger.info("Received bytes4: {} from {}. ID: {}",read,event.getFrom(),event.conId);
            if(8082==myself.getPort()){
                event.babelInputStream.sendInt(read*2);
            }
        }
        logger.info("CONTAINS ? {}",streams.contains(event.babelInputStream));
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
