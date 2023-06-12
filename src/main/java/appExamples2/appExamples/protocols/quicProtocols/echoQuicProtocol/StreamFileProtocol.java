package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelQuicChannel.BabelQuicChannel;
import appExamples2.appExamples.channels.babelQuicChannel.BytesMessageSentOrFail;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamClosedEvent;
import appExamples2.appExamples.channels.babelQuicChannel.events.StreamCreatedEvent;
import appExamples2.appExamples.channels.streamingChannel.BabelStreamingChannel;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.EchoMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.channel.StreamingChannel;
import org.tcpStreamingAPI.utils.TCPStreamUtils;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.events.InConnectionUp;
import pt.unl.fct.di.novasys.babel.channels.events.OutConnectionUp;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class StreamFileProtocol extends GenericProtocolExtension {
    private static final Logger logger = LogManager.getLogger(StreamFileProtocol.class);
    public static final short PROTOCOL_ID = 202;
    public int channelId;
    private final Host myself; //My own address/port
    private Host dest;
    private Properties properties;
    public final String NETWORK_PROTO;
    List<String> sentHahes = new LinkedList<>();
    List<String> receivedHahes = new LinkedList<>();
    List<String> receivedHashes = new LinkedList<>();
    public StreamFileProtocol(Properties properties) throws Exception {

        super(EchoProtocol.class.getName(),PROTOCOL_ID);
        String address = properties.getProperty("address");
        String port = properties.getProperty("port");
        logger.info("Receiver on {}:{}", address, port);
        this.myself = new Host(InetAddress.getByName(address), Integer.parseInt(port));

        //channelProps.setProperty("metrics_interval","2000");


        System.out.println(myself);
        System.out.println("CHANNEL CREATED "+channelId);
        this.properties = properties;
        NETWORK_PROTO = properties.getProperty("NETWORK_PROTO");
        channelId = makeChan(NETWORK_PROTO,address,port);
        System.out.println("PROTO "+NETWORK_PROTO);
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
        }else{
            System.out.println("TCP ON");
            channelProps.setProperty(StreamingChannel.ADDRESS_KEY,address);
            channelProps.setProperty(StreamingChannel.PORT_KEY,port);
            channelProps.setProperty(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP,"TRUE");
            channelProps.setProperty(FactoryMethods.SINGLE_THREADED_PROP,"TRUE");

            channelId = createChannel(BabelStreamingChannel.NAME, channelProps);

        }

        return channelId;
    }
    @Override
    public void init(Properties props) {
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            //registerChannelEventHandler(channelId, QUICMetricsEvent.EVENT_ID, this::uponChannelMetrics);
            registerBytesMessageHandler(channelId,HANDLER_ID,this::uponBytesMessage,null, this::uponMsgFail3);
            registerMandatoryStreamDataHandler(channelId,this::uponStreamBytes,null, this::uponMsgFail2);
            registerStreamDataHandler(channelId,HANDLER_ID2,this::uponStreamBytes2,null, this::uponMsgFail2);

            registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
            registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);

            registerChannelEventHandler(channelId, StreamCreatedEvent.EVENT_ID, this::uponStreamCreated);
            registerChannelEventHandler(channelId, StreamClosedEvent.EVENT_ID, this::uponStreamClosed);

            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                openStreamConnection(dest,channelId);
                //startStreaming();
            }
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
    public static final short HANDLER_ID2 = 43;
    public void sendMessage(String message, String stream){
        if(sendByte){
            super.sendMessage(channelId,message.getBytes(),message.length(),stream,getProtoId(),getProtoId(),HANDLER_ID);
        }else {
            EchoMessage echoMessage = new EchoMessage(myself,message);
            super.sendMessage(echoMessage,stream);
        }
        sendByte =!sendByte;
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

    private void uponStreamCreated(StreamCreatedEvent event, int channelId) {
        streamId = event.streamId;
        if(myself.getPort()==8081){
            startStreaming();
        }
        if(myself.getPort()==8082){
            System.out.println("PORRAS "+streamId);
        }
        logger.info("STREAM {}::{} IS UP. DATA TRANSMISSION TYPE: {}",event.streamId,event.host,event.connectionOrStreamType);
        System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.streamId));
    }
    private void uponStreamClosed(StreamClosedEvent event, int channelId) {
        logger.info("STREAM {}[::]{} IS DOWN.",event.streamId,event.host);
        System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.streamId));

    }
    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE: {}",event.getNode(),event.type);
        /**
        if(dest==null){
            dest = event.getNode();
        }
        **/
        try{
            fos = new FileOutputStream(myself.getPort()+NETWORK_PROTO+"_STREAM.MP4");
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.getNode()));

    }
    String streamId;
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE {}",event.getNode(),event.type);
        if(dest==null){
            dest = event.getNode();
        }
        try{
            fos2 = new FileOutputStream(myself.getPort()+NETWORK_PROTO+"_copy.mp4");
        }catch (Exception e){
            e.printStackTrace();
        }
        super.createStream(channelId,getProtoId(),getProtoId(),HANDLER_ID2, dest, ConnectionOrStreamType.UNSTRUCTURED_STREAM);

        System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.getNode()));
    }
    private void uponBytesMessage(byte [] msg, Host from, short sourceProto, int channelId, String streamId) {
        logger.info("Received bytes: {} from {}", new String(msg), from);
    }
    long received = 0;
    FileOutputStream fos, fos2;
    ByteBuf buf = Unpooled.buffer();
    private void uponStreamBytes(byte [] msg, Host from, short sourceProto, int channelId, String streamId) {
        if(myself.getPort()==8081){
            System.out.println("CANNOT RECEIVED HERE");
            System.exit(1);
        }
        received += msg.length;
        sendStream(channelId,msg,msg.length,this.streamId);
        try {
            if(received>=813782079){
                fos.write(msg);
                fos.close();
                logger.info("ENDED "+received);
                buf.writeBytes(msg);
                byte [] m = new byte[buf.readableBytes()];
                buf.readBytes(m);
                buf.release();
                //System.out.println(buf.readableBytes()+" "+Hex.encodeHexString(QUICLogics.hash(m)));
                receivedHashes.add(Hex.encodeHexString(QUICLogics.hash(m)));
                sumHashes(receivedHashes);
            }else{
                fos.write(msg);
                buf.writeBytes(msg);
                if(buf.readableBytes()>=bufferSize){
                    byte [] m = new byte[bufferSize];
                    buf.readBytes(m);
                    buf.discardReadBytes();
                    receivedHashes.add(Hex.encodeHexString(QUICLogics.hash(m)));
                    //System.out.println(Hex.encodeHexString(QUICLogics.hash(m)));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        //logger.info("Received bytes2: {} from {}",msg.length, from);
    }
    int gg = 0;
    private void uponStreamBytes2(byte [] msg, Host from, short sourceProto, int channelId, String streamId) {
        gg += msg.length;
        try{
            fos2.write(msg);
            if(gg >= 813782079){
                fos2.close();
                System.out.println("CLOSEDD "+gg);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        //logger.info("Received 2bytes2: {} from {}",msg.length, from);
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
    int bufferSize = 128*1024; // 8KB buffer size
    public void startStreaming(){
        System.out.println("STREAMING STARTED!!!");
        try{
            //String p = "/home/tsunami/Downloads/Avatar The Way Of Water (2022) [1080p] [WEBRip] [5.1] [YTS.MX]/Avatar.The.Way.Of.Water.2022.1080p.WEBRip.x264.AAC5.1-[YTS.MX].mp4";
            //Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            //Path filePath = Paths.get(p);
            //
            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            byte [] bytes = new byte[bufferSize];
            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read, totalSent = 0;
            int cc = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                if(read==bufferSize){
                    sentHahes.add(Hex.encodeHexString(QUICLogics.hash(bytes)));
                }else {
                    byte [] n = new byte[read];
                    System.arraycopy(bytes,0,n,0,read);
                    bytes = n;
                    sentHahes.add(Hex.encodeHexString(QUICLogics.hash(n)));
                }
                sendStream(channelId,bytes,read,dest);
                //System.out.println("LAST READ "+read+" "+Hex.encodeHexString(QUICLogics.hash(bytes)));
                cc++;
                //Thread.sleep(1000);
                bytes = new byte[bufferSize];
            }
            sumHashes(sentHahes);
            Thread.sleep(10000);
            System.out.println("METRICS OUT ? "+totalSent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void sumHashes(List<String> set){
        long sum = 0;
        for (String receivedHash : set) {
            sum += receivedHash.hashCode();
        }
        System.out.println(" SUMM "+sum);
    }
}
