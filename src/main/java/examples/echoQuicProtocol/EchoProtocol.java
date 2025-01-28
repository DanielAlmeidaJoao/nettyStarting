package examples.echoQuicProtocol;

import pt.unl.fct.di.novasys.network.babelChannels.babelNewChannels.events.ConnectionProtocolChannelMetricsEvent;
import pt.unl.fct.di.novasys.network.babelChannels.babelNewChannels.quicChannels.BabelQUIC_P2P_Channel;
import pt.unl.fct.di.novasys.network.babelChannels.babelNewChannels.tcpChannels.BabelTCP_P2P_Channel;
import pt.unl.fct.di.novasys.network.babelChannels.babelNewChannels.udpBabelChannel.BabelUDPChannel;
import pt.unl.fct.di.novasys.network.babelChannels.babelNewChannels.udpBabelChannel.UDPMetricsEvent;
import pt.unl.fct.di.novasys.network.babelChannels.messages.BytesToBabelMessage;
import examples.echoQuicProtocol.messages.EchoMessage;
import examples.echoQuicProtocol.messages.SampleTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.annotations.ChannelEventHandlerAnnotation;
import pt.unl.fct.di.novasys.babel.annotations.MessageFailedHandlerAnnotation;
import pt.unl.fct.di.novasys.babel.annotations.MessageInHandlerAnnotation;
import pt.unl.fct.di.novasys.babel.annotations.StreamInHandlerAnnotation;
import pt.unl.fct.di.novasys.babel.channels.events.*;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import pt.unl.fct.di.novasys.babel.internal.BabelStreamDeliveryEvent;
import pt.unl.fct.di.novasys.babel.internal.MessageFailedEvent;
import pt.unl.fct.di.novasys.babel.internal.MessageInEvent;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.QUICLogics;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.utils.enums.TransmissionType;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.BabelInputStream;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.utils.NewChannelsFactoryUtils;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.metrics.UDPNetworkStatsWrapper;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.utils.UDPLogics;

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
            channelProps = NewChannelsFactoryUtils.quicChannelProperty(address,port);
            //channelProps.setProperty("metrics_interval","2000");
            channelProps.setProperty(QUICLogics.MAX_IDLE_TIMEOUT_IN_SECONDS,"3000");

            channelId = createChannel(BabelQUIC_P2P_Channel.CHANNEL_NAME, channelProps);

        }else if(channelName.equalsIgnoreCase("tcp")){
            channelProps = NewChannelsFactoryUtils.tcpChannelProperties(address,port);
            System.out.println("TCP ON");
            //channelProps.setProperty(NettyTCPChannel.ADDRESS_KEY,address);
            //channelProps.setProperty(NettyTCPChannel.PORT_KEY,port);
            //channelProps.setProperty(NewChannelsFactoryUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
            //channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"FALSE");

            channelId = createChannel(BabelTCP_P2P_Channel.CHANNEL_NAME, channelProps);


        }else{
            channelProps = NewChannelsFactoryUtils.udpChannelProperties(address,port);
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
        //registerMessageSerializer(EchoMessage.MSG_ID, EchoMessage.newSerializer(EchoMessage.class));
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                System.out.println(openMessageConnection(dest,channelId));
                //System.out.println(openStreamConnection(dest,channelId));

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
                System.out.println("OPENNED MESSAGE CONNECTION "+ openMessageConnectionEvenIfItsConnected(host,channelId));
            }else {
                System.out.println("OPENNED STREAM CONNECTION"+openStreamConnectionEvenIfItsConnected(host,channelId));
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
            System.out.println(sendByte+" SENDBYTE"+" HASH: "+message.hashCode()+" BYTES SENT:"+message.length());
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


    public List<BabelInputStream> streams = new LinkedList<>();
    List<String> cons = new LinkedList<>();




    @MessageInHandlerAnnotation(PROTO_MESSAGE_ID=BytesToBabelMessage.ID)
    private void uponBytesMessage(MessageInEvent event, BytesToBabelMessage msg ) {
        logger.info("Received bytes: {} from {}", (new String(msg.message).hashCode()),event.getFrom());
        System.out.println((new String(msg.message).hashCode())+" "+event.getFrom());
        //System.exit(0);
    }

    @StreamInHandlerAnnotation
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

    @MessageInHandlerAnnotation(PROTO_MESSAGE_ID=EchoMessage.MSG_ID)
    private void uponFloodMessageQUIC(MessageInEvent eventClient, EchoMessage message) {
        String mes = message.getMessage();
        logger.info("Received QUIC {} from_ {} {}", mes.hashCode(), eventClient.getFrom(), eventClient.connectionId);
    }

    @MessageFailedHandlerAnnotation(PROTO_MESSAGE_ID=EchoMessage.MSG_ID)
    private void uponMsgFail(MessageFailedEvent event,EchoMessage msg) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("NOT BYTES Message {} to {} failed, reason: {}", msg, event.getTo(), event.getCause());
        logger.info("DATA SENT <{}>",msg.getMessage());

    }

    @MessageFailedHandlerAnnotation(PROTO_MESSAGE_ID=BytesToBabelMessage.ID)
    private void uponMsgFail3(MessageFailedEvent event, BytesToBabelMessage msg) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("BYTES Message {} to {} failed, reason: {}", msg, event.getTo(), event.getCause());
        //logger.info("SENT MESSAGE <{}>",new String(msg.message));
    }

    private void uponMsgFail2(MessageFailedEvent event,OnStreamDataSentEvent msg) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, event.getTo(), event.getCause());
        /**
         try {
         if(msg.inputStream!=null){
         logger.info("AVAILABLE {}",msg.inputStream.available());
         }
         }catch (Exception e){
         e.printStackTrace();
         } **/
    }

    @ChannelEventHandlerAnnotation(EVENT_ID = OnConnectionDownEvent.EVENT_ID)
    private void uponConnectionDown(OnConnectionDownEvent event, int channelId) {
        logger.info("CONNECTION DOWN: {} {} {}",event.connectionId,event.getNode(),event.type);
    }

    @ChannelEventHandlerAnnotation(EVENT_ID = OnOpenConnectionFailed.EVENT_ID)
    private void uponOpenConnectionFailed(OnOpenConnectionFailed event, int channelId) {
        logger.info("CONNECTION FAILED: {} {} {}",event.connectionId,event.node,event.type);
        if(dest==null){
            dest = event.getNode();
        }
    }

    @ChannelEventHandlerAnnotation(EVENT_ID = OnMessageConnectionUpEvent.EVENT_ID)
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

    @ChannelEventHandlerAnnotation(EVENT_ID = OnStreamConnectionUpEvent.EVENT_ID)
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

    @ChannelEventHandlerAnnotation(EVENT_ID = UDPMetricsEvent.EVENT_ID)
    private void uponUDPChannelMetrics(UDPMetricsEvent event, int channelId) {
        System.out.println("UDP METRICS TRIGGERED!!!");
        for (UDPNetworkStatsWrapper stat : event.getStats()) {
            System.out.printf("HOST: %s\n",stat.getDest());
            System.out.println(NewChannelsFactoryUtils.g.toJson(stat.ackStats));
            System.out.println(NewChannelsFactoryUtils.g.toJson(stat.totalMessageStats));
            System.out.println(NewChannelsFactoryUtils.g.toJson(stat.sentAckedMessageStats));
        }
    }

    @ChannelEventHandlerAnnotation(EVENT_ID = ConnectionProtocolChannelMetricsEvent.EVENT_ID)
    private void uponChannelMetrics(ConnectionProtocolChannelMetricsEvent event, int channelId) {
        countMetricsTime ++;
        System.out.println("METRICS TRIGGERED!!!");
        System.out.println("CURRENT: "+ NewChannelsFactoryUtils.g.toJson(event.getCurrent()));
        System.out.println("OLD: "+ NewChannelsFactoryUtils.g.toJson(event.getOld()));
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


}
