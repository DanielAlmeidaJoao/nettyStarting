package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelQuicChannel.BabelQUIC_TCP_Channel;
import appExamples2.appExamples.channels.babelQuicChannel.BytesMessageSentOrFail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.events.OnConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import pt.unl.fct.di.novasys.babel.internal.BabelInBytesWrapperEvent;
import pt.unl.fct.di.novasys.babel.internal.BytesMessageInEvent;
import quicSupport.utils.QUICLogics;
import tcpSupport.tcpStreamingAPI.channel.StreamingChannel;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class StreamFileWithQUIC extends GenericProtocolExtension {
    private static final Logger logger = LogManager.getLogger(StreamFileWithQUIC.class);
    public static final short PROTOCOL_ID = 202;
    public int channelId;
    private final Host myself; //My own address/port
    private Host dest;
    private Properties properties;
    public final String NETWORK_PROTO;

    public StreamFileWithQUIC(Properties properties) throws Exception {

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
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            //registerChannelEventHandler(channelId, QUICMetricsEvent.EVENT_ID, this::uponChannelMetrics);
            registerBytesMessageHandler(channelId,HANDLER_ID,this::uponBytesMessage,null, this::uponMsgFail3);
            registerMandatoryStreamDataHandler(channelId,this::uponStreamBytes,null, this::uponMsgFail2);

            registerChannelEventHandler(channelId, OnConnectionUpEvent.EVENT_ID, this::uponInConnectionUp);

            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                openStreamConnection(dest,channelId);
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
    public static final short HANDLER_ID = 2;
    public static final short HANDLER_ID2 = 43;

    private void uponInConnectionUp(OnConnectionUpEvent event, int channelId) {
        streamId = event.conId;
        if(event.inConnection){
            logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE: {}",event.getNode(),event.type+" SS "+streamId);
            try{
                fos = new FileOutputStream(myself.getPort()+NETWORK_PROTO+"_STREAM.txt");
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.getNode()));
        }else{
            uponOutConnectionUp(event, channelId);
        }

    }
    String streamId;
    private void uponOutConnectionUp(OnConnectionUpEvent event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE {}",event.getNode(),event.type);
        if(dest==null){
            dest = event.getNode();
        }
        try{
            //fos2 = new FileOutputStream(myself.getPort()+NETWORK_PROTO+"_copy.txt");
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.getNode())+" CON ID "+streamId);

        if(myself.getPort()==8081){
            new Thread(() -> {
                startStreaming();
            }).start();
        }
        if(myself.getPort()==8082){
            System.out.println("PORRAS "+streamId);
        }
    }
    private void uponBytesMessage(BytesMessageInEvent event) {
        logger.info("Received bytes: {} from {}", new String(event.getMsg()),event.getFrom());
    }
    long received = 0;
    FileOutputStream fos, fos2;
    private void uponStreamBytes(BabelInBytesWrapperEvent event) {
        new Exception("TO DO").printStackTrace();
        /**
        received += event.getMsg().length;
        if(myself.getPort()==8082){
            sendStream(channelId,event.getMsg(),event.getMsg().length,event.conId);
            try {
                fos.write(event.getMsg());
                if(received>=813782079){
                    fos.close();
                    logger.info("RECEIVED ALL BYTES");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        logger.info("Received bytes2: {} from {} receivedTOTAL {} ",event.getMsg().length,event.getFrom(),received);
    **/
    }
    int gg = 0;
    private void uponStreamBytes2(BytesMessageInEvent event) {
        gg += event.getMsg().length;
        try{
            fos2.write(event.getMsg());
            if(gg >= 813782079){
                fos2.close();
                logger.info("RECEIVED ALL BYTES");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        //logger.info("Received 2bytes2: {} from {}",msg.length, from);
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
        try {
            if(msg.inputStream!=null){
                logger.info("AVAILABLE {}",msg.inputStream.available());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    int bufferSize = 128*1024; // 8KB buffer size
    public void startStreaming(){
        System.out.println("STREAMING STARTED!!!");
        try{
            //String p = "/home/tsunami/Downloads/Avatar The Way Of Water (2022) [1080p] [WEBRip] [5.1] [YTS.MX]/Avatar.The.Way.Of.Water.2022.1080p.WEBRip.x264.AAC5.1-[YTS.MX].mp4";
            //Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/text.txt");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            //Path filePath = Paths.get(p);
            //
            File f = filePath.toFile();
            FileInputStream fileInputStream = new FileInputStream(f);
            int len = (int) f.length();
            sendStream(channelId,fileInputStream,0,streamId);
            System.out.println("SENT INPUTFILE TO SEND BYTES "+len);
            if(len >-1){
                FileOutputStream fileOutputStream = new FileOutputStream(f);
                int b = 0;
                while (true){
                    sendStream(channelId,fileInputStream,streamId);
                    Thread.sleep(5000);
                    fileOutputStream.write((b+"_OLA ").getBytes());
                    System.out.println("WROTE TO FILE");
                    b++;
                    if(b>5){
                        Thread.sleep(1000);
                        fileInputStream.close();
                        return;
                    }
                }
                //return;
            }

            byte [] bytes = new byte[bufferSize];
            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read, totalSent = 0;

            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                if(read<bufferSize){
                    byte [] n = new byte[read];
                    System.arraycopy(bytes,0,n,0,read);
                    bytes = n;
                }
                sendStream(channelId,bytes,read,dest);
                //Thread.sleep(1000);
                bytes = new byte[bufferSize];
            }
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
