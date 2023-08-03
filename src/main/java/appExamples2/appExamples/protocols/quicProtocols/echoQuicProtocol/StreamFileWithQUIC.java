package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.babelQuicChannel.BabelQUIC_TCP_Channel;
import appExamples2.appExamples.channels.babelQuicChannel.BytesMessageSentOrFail;
import appExamples2.appExamples.channels.udpBabelChannel.BabelUDPChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.events.OnStreamConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import pt.unl.fct.di.novasys.babel.internal.BabelStreamDeliveryEvent;
import pt.unl.fct.di.novasys.babel.internal.BytesMessageInEvent;
import pt.unl.fct.di.novasys.network.data.Host;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.TCPStreamUtils;

import java.io.File;
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
    public final long fileLen = 1035368729;

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
        Properties channelProps;
        if(channelName.equalsIgnoreCase("quic")){
            System.out.println("QUIC ON");
            //channelProps.setProperty("metrics_interval","2000");
            channelProps = TCPStreamUtils.quicChannelProperty(address,port);
            channelId = createChannel(BabelQUIC_TCP_Channel.NAME_QUIC, channelProps);
        }else if(channelName.equalsIgnoreCase("tcp")){
            System.out.println("TCP ON");
            channelProps = TCPStreamUtils.tcpChannelProperties(address,port);
            channelId = createChannel(BabelQUIC_TCP_Channel.NAME_TCP, channelProps);
        }else{
            System.out.println("UDP ON");
            channelProps = TCPStreamUtils.udpChannelProperties(address,port);
            channelId = createChannel(BabelUDPChannel.NAME,channelProps);
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

            registerChannelEventHandler(channelId, OnStreamConnectionUpEvent.EVENT_ID, this::uponStreamConnectionUp);

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

    BabelInputStream babelInputStream;

    private void uponStreamConnectionUp(OnStreamConnectionUpEvent event, int channelId) {
        streamId = event.conId;
        if(event.babelInputStream !=null){
            babelInputStream = event.babelInputStream;
            babelInputStream.setFlushMode(true);
        }
        if(event.inConnection){
            logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE: {}",event.getNode(),event.type+" SS "+streamId);
            try{
                fos = new FileOutputStream(myself.getPort()+NETWORK_PROTO+"_STREAM.mp4");
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.getNode()));
        }else{
            uponOutConnectionUp(event, channelId);
        }

    }
    String streamId;
    private void uponOutConnectionUp(OnStreamConnectionUpEvent event, int channelId) {
        logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE {}",event.getNode(),event.type);
        if(dest==null){
            dest = event.getNode();
        }
        try{
            fos2 = new FileOutputStream(myself.getPort()+NETWORK_PROTO+"_copy.mp4");
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("CONNECTION TYPR "+getConnectionType(channelId,event.getNode())+" CON ID "+streamId);

        if(myself.getPort()==8081){
            new Thread(() -> {
                for (int i = 0; i < 1; i++) {
                    startStreaming();
                    //babelInputStream.sendInt(i);
                }
                System.out.println("FLUSH MODE "+ babelInputStream.getFlushMode());
                //babelInputStream.flushStream();

                try{
                    Thread.sleep(10000);
                    System.out.println("UPPPP");
                    //babelInputStream.flushStream();
                }catch (Exception e){}
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
    boolean notW = true;
    long start = 0;

    private void uponStreamBytes(BabelStreamDeliveryEvent event) {
        FileOutputStream out;

        if(myself.getPort()==8082){
            out = fos;
            //babelInputStream.sendBabelOutputStream(event.babelOutputStream);
        }else{
            out = fos2;
        }
        int available = event.babelOutputStream.readableBytes();
        byte [] p = event.babelOutputStream.readRemainingBytes();

        if(start==0){
            start = System.currentTimeMillis();
        }
        try {
            if(available<=0)return;
            received += available;
            out.write(p);
            logger.info("RECEIVED ALL BYTES {} . {}",available,received);
        }catch (Exception e){
            e.printStackTrace();
        }
        //}
        if(received==fileLen){
            try{
                logger.info("ELAPSED TIME : {}",(System.currentTimeMillis()-start));
                out.close();
                notW = false;
            }catch (Exception e){

            }
        }

        //logger.info("Received bytes2: {} from {} receivedTOTAL {} ",event.getMsg().length,event.getFrom(),received);

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
        /**
        try {
            if(msg.inputStream!=null){
                logger.info("AVAILABLE {}",msg.inputStream.available());
            }
        }catch (Exception e){
            e.printStackTrace();
        } **/
    }
    int bufferSize = 128*1024; // 8KB buffer size
    public void startStreaming(){
        System.out.println("STREAMING STARTED!!!");
        try{
            //String p = "/home/tsunami/Downloads/Avatar The Way Of Water (2022) [1080p] [WEBRip] [5.1] [YTS.MX]/Avatar.The.Way.Of.Water.2022.1080p.WEBRip.x264.AAC5.1-[YTS.MX].mp4";
            Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/Guardians Of The Galaxy Vol. 3 (2023) [1080p] [WEBRip] [x265] [10bit] [5.1] [YTS.MX]/Guardians.Of.The.Galaxy.Vol..3.2023.1080p.WEBRip.x265.10bit.AAC5.1-[YTS.MX].mp4");

            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/text.txt");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            //Path filePath = Paths.get(p);
            //
            File f = filePath.toFile();
            babelInputStream.sendFile(f);

            long len = f.length();
            //sendStream(channelId,fileInputStream,len,streamId);
            System.out.println("SENT INPUTFILE TO SEND BYTES "+len);
            if(len>0) return;

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
