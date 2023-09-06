package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.babelNewChannels.events.ConnectionProtocolChannelMetricsEvent;
import appExamples2.appExamples.channels.babelNewChannels.quicChannels.BabelQUIC_P2P_Channel;
import appExamples2.appExamples.channels.babelNewChannels.tcpChannels.BabelTCP_P2P_Channel;
import appExamples2.appExamples.channels.babelNewChannels.udpBabelChannel.BabelUDPChannel;
import appExamples2.appExamples.channels.babelNewChannels.udpBabelChannel.UDPMetricsEvent;
import appExamples2.appExamples.channels.messages.BytesToBabelMessage;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.EchoMessage;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.SampleTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.events.*;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import pt.unl.fct.di.novasys.babel.internal.BabelStreamDeliveryEvent;
import pt.unl.fct.di.novasys.network.data.Host;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;
import udpSupport.metrics.UDPNetworkStatsWrapper;
import udpSupport.utils.UDPLogics;

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
        logger.error("STARTED THE APPP");
        this.myself = new Host(InetAddress.getByName(address), Integer.parseInt(port));

        //channelProps.setProperty("metrics_interval","2000");


        channelId = makeChan(properties.getProperty("NETWORK_PROTO"),address,port);
        System.out.println(myself);
        System.out.println("CHANNEL CREATED "+channelId);
        this.properties = properties;
    }
    private int makeChan(String channelName,String address, String port) throws Exception {
        Properties channelProps;
        if(channelName.equalsIgnoreCase("quic")){
            System.out.println("QUIC ON");
            channelProps = TCPChannelUtils.quicChannelProperty(address,port);
            //channelProps.setProperty("metrics_interval","2000");

            channelId = createChannel(BabelQUIC_P2P_Channel.CHANNEL_NAME, channelProps);

        }else if(channelName.equalsIgnoreCase("tcp")){
            channelProps = TCPChannelUtils.tcpChannelProperties(address,port);
            System.out.println("TCP ON");
            //channelProps.setProperty(NettyTCPChannel.ADDRESS_KEY,address);
            //channelProps.setProperty(NettyTCPChannel.PORT_KEY,port);
            //channelProps.setProperty(TCPChannelUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
            //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"FALSE");

            channelId = createChannel(BabelTCP_P2P_Channel.CHANNEL_NAME, channelProps);


        }else{
            channelProps = TCPChannelUtils.udpChannelProperties(address,port);
            System.out.println("UDP ON");
            //channelProps.setProperty(NettyTCPChannel.ADDRESS_KEY,address);
            //channelProps.setProperty(NettyTCPChannel.PORT_KEY,port);
            //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"FALSE");

            channelId = createChannel(BabelUDPChannel.NAME, channelProps);
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

            registerChannelEventHandler(channelId, ConnectionProtocolChannelMetricsEvent.EVENT_ID, this::uponChannelMetrics);
            registerChannelEventHandler(channelId, UDPMetricsEvent.EVENT_ID, this::uponUDPChannelMetrics);

            registerMessageHandler(channelId,BytesToBabelMessage.ID,this::uponBytesMessage,null, this::uponMsgFail3);
            registerStreamDataHandler(channelId,this::uponStreamBytes,null, this::uponMsgFail2);

            registerChannelEventHandler(channelId, OnStreamConnectionUpEvent.EVENT_ID, this::uponStreamConnectionUp);
            //uponOpenConnectionFailed
            registerChannelEventHandler(channelId, OnMessageConnectionUpEvent.EVENT_ID, this::uponMessageConnectionUp);
            registerChannelEventHandler(channelId, OnOpenConnectionFailed.EVENT_ID, this::uponOpenConnectionFailed);

            registerChannelEventHandler(channelId, OnConnectionDownEvent.EVENT_ID, this::uponConnectionDown);


            //registerChannelEventHandler(channelId, StreamCreatedEvent.EVENT_ID, this::uponStreamCreated);
            //registerChannelEventHandler(channelId, StreamClosedEvent.EVENT_ID, this::uponStreamClosed);

            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                //System.out.println(openMessageConnection(dest,channelId));
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
        //System.out.println("CACCCLED "+transmissionType);

        if(TransmissionType.UNSTRUCTURED_STREAM == transmissionType){
            //super.sendStream(channelId,message.getBytes(),message.length(),stream);
            for (BabelInputStream babelInputStream : streams) {
                babelInputStream.writeBytes(message.getBytes());
            }
        }else{
            if(sendByte){
                super.sendMessage(channelId,message.getBytes(),message.length(),stream,getProtoId(),getProtoId());
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
        String aux = message;
        for (int i = 0; i < 1; i++) {
            message = aux;
            if(message.length()%2==0){
                message = message.repeat(message.length()*UDPLogics.MAX_UDP_PAYLOAD_SIZE+10);
            }
            System.out.println(sendByte+" SENDBYTE"+" HASH: "+message.hashCode()+" "+message.length());
            if(sendByte){
                super.sendMessage(channelId,message.getBytes(),message.length(),dest,getProtoId(),getProtoId());
            }else{
                EchoMessage echoMessage = new EchoMessage(myself,message);
                sendMessage(echoMessage,dest);
            }
            sendByte =!sendByte;
            //super.closeConnection(dest);
        }
    }
    public void sendStream(String message){
        System.out.println("SENDING "+message.length());
        for (BabelInputStream stream : streams) {
            stream.writeBytes(message.getBytes());
        }
        //super.sendStream(channelId,message.getBytes(),message.length(),dest);
    }
    public void sendStream(String message, String streamId){
        System.out.println("SENDING "+message.length());
        for (BabelInputStream stream : streams) {
            stream.writeBytes(message.getBytes());
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
    int countMetricsTime = 0;
    private void uponChannelMetrics(ConnectionProtocolChannelMetricsEvent event, int channelId) {
        countMetricsTime ++;
        System.out.println("METRICS TRIGGERED!!!");
        System.out.println("CURRENT: "+TCPChannelUtils.g.toJson(event.getCurrent()));
        System.out.println("OLD: "+TCPChannelUtils.g.toJson(event.getOld()));
        if(countMetricsTime==2){
            var p =event.getCurrent();
            if(p != null && p.size()>0){
                //if(myself.getPort()==8081){
                    closeConnection(Host.toBabelHost(p.get(0).getHostAddress()));
                    System.out.println("CLOSED CONNECTIONNN");
                //}
            }
        }
        if(countMetricsTime>4){
            System.exit(1);
        }
    }


    private void uponUDPChannelMetrics(UDPMetricsEvent event, int channelId) {
        System.out.println("UDP METRICS TRIGGERED!!!");
        for (UDPNetworkStatsWrapper stat : event.getStats()) {
            System.out.printf("HOST: %s\n",stat.getDest());
            System.out.println(TCPChannelUtils.g.toJson(stat.ackStats));
            System.out.println(TCPChannelUtils.g.toJson(stat.totalMessageStats));
            System.out.println(TCPChannelUtils.g.toJson(stat.sentAckedMessageStats));
        }
    }
    public List<BabelInputStream> streams = new LinkedList<>();
    private void uponStreamConnectionUp(OnStreamConnectionUpEvent event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE: {}. id: {}",event.getNode(),event.type,event.conId);
        streams.add(event.babelInputStream);
        event.babelInputStream.setFlushMode(true);
        if(event != null){
            return;
        }
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
                        stream.writeInt(i);
                        stream.flushStream();
                    }
                }
            }
        }
    }
    List<String> cons = new LinkedList<>();
    private void uponMessageConnectionUp(OnMessageConnectionUpEvent event, int channelId) {
        logger.info("SELF: {} | CONNECTION UP: {} {} {}",myself,event.conId,event.inConnection,event.type);
        dest = event.getNode();
        if(event != null){
            return;
        }
        cons.add(event.conId);
        if(dest==null){
            dest = event.getNode();
        }

        for (int v = 0; v < 1; v++) {
            new Thread(() -> {
                for (String con : cons) {
                    for (int i = 1; i <= 1; i++) {
                        //+ UDPLogics.MAX_UDP_PAYLOAD_SIZE
                        String m1 = ("0 ++"+myself).repeat(i+ UDPLogics.MAX_UDP_PAYLOAD_SIZE) + con;
                        //System.out.println();
                        //EchoMessage echoMessage = new EchoMessage(myself, m1);
                        System.out.println("SENT: "+m1.hashCode()+" "+m1.length());
                        sendMessage(m1,con);
                        //super.sendMessage(echoMessage, con);
                    }
                }
            }).run();
        }
        /**
        for (String con : cons) {
            String m1 = "OLA23 ".repeat(1000)+con;
            System.out.println("SENT2: "+m1.hashCode()+" "+m1.length());
            sendMessage(m1,con);
        } **/
    }
    private void uponOpenConnectionFailed(OnOpenConnectionFailed event, int channelId) {
        logger.info("CONNECTION FAILED: {} {} {}",event.connectionId,event.node,event.type);
        if(dest==null){
            dest = event.getNode();
        }
    }

    private void uponConnectionDown(OnConnectionDownEvent event, int channelId) {
        logger.info("CONNECTION DOWN: {} {} {}",event.connectionId,event.getNode(),event.type);
    }
    private void uponBytesMessage(BytesToBabelMessage message,Host from, short sourceProto, int channelId, String streamId) {
        logger.info("Received bytes3: {} from {}", (new String(message.message).hashCode()),from);
        //System.exit(0);
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
                event.babelInputStream.writeInt(read*2);
            }
        }
        logger.info("CONTAINS ? {}",streams.contains(event.babelInputStream));
    }

    private void uponFloodMessageQUIC(EchoMessage msg, Host from, short sourceProto, int channelId, String streamId) {
        logger.info("Received QUIC {} from {} {}", msg.getMessage().hashCode(), from, streamId);
    }
    private void uponMsgFail(EchoMessage msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("NOT BYTES Message {} to {} failed, reason: {}", msg, host, throwable);
        logger.info("DATA SENT <{}>",msg.getMessage());

    }
    private void uponMsgFail3(BytesToBabelMessage msg, Host host, short destProto,
                              Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("BYTES Message {} to {} failed, reason: {}", msg, host, throwable);
        //logger.info("SENT MESSAGE <{}>",new String(msg.message));
    }

    private void uponMsgFail2(OnStreamDataSentEvent msg, Host host, short destProto,
                              Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
        /**
         try {
         if(msg.inputStream!=null){
         logger.info("AVAILABLE {}",msg.inputStream.available());
         }
         }catch (Exception e){
         e.printStackTrace();
         } **/
    }
}
