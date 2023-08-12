package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol;

import appExamples2.appExamples.channels.babelQuicChannel.BabelQUIC_TCP_Channel;
import appExamples2.appExamples.channels.udpBabelChannel.BabelUDPChannel;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages.FileBytesCarrier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.events.OnMessageConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.channels.events.OnStreamConnectionUpEvent;
import pt.unl.fct.di.novasys.babel.channels.events.OnStreamDataSentEvent;
import pt.unl.fct.di.novasys.babel.core.GenericProtocolExtension;
import pt.unl.fct.di.novasys.babel.internal.BabelStreamDeliveryEvent;
import pt.unl.fct.di.novasys.network.data.Host;
import tcpSupport.tcpChannelAPI.channel.NettyTCPChannel;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.TCPStreamUtils;

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
    public final long fileLen;
    final  Path filePath;
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

        filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
        //filePath = Paths.get("/home/tsunami/Downloads/Guardians Of The Galaxy Vol. 3 (2023) [1080p] [WEBRip] [x265] [10bit] [5.1] [YTS.MX]/Guardians.Of.The.Galaxy.Vol..3.2023.1080p.WEBRip.x265.10bit.AAC5.1-[YTS.MX].mp4");
        fileLen = filePath.toFile().length();
        //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
        //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/text.txt");
        //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
        //Path filePath = Paths.get(p);
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
        if(properties.getProperty(NettyTCPChannel.ZERO_COPY)!=null){
            channelProps.setProperty(NettyTCPChannel.ZERO_COPY,properties.getProperty(NettyTCPChannel.ZERO_COPY));
        }
        return channelId;
    }
    @Override
    public void init(Properties props) {
        /*---------------------- Register Message Handlers -------------------------- */
        boolean messageCon = props.getProperty("CON") !=null;
        try {
            registerMessageSerializer(channelId, FileBytesCarrier.ID, FileBytesCarrier.serializer);
            registerMessageHandler(channelId, FileBytesCarrier.ID, this::uponFileBytesMessage, this::uponMsgFail);

            //registerChannelEventHandler(channelId, QUICMetricsEvent.EVENT_ID, this::uponChannelMetrics);
            registerMandatoryStreamDataHandler(channelId,this::uponStreamBytes,null, this::uponMsgFail2);

            registerChannelEventHandler(channelId, OnStreamConnectionUpEvent.EVENT_ID, this::uponStreamConnectionUp);
            registerChannelEventHandler(channelId, OnMessageConnectionUpEvent.EVENT_ID, this::uponMessageConnectionEvent);

            if(myself.getPort()==8081){
                dest = new Host(InetAddress.getByName("localhost"),8082);
                if(messageCon){
                    openMessageConnection(dest,channelId);
                }else {
                    openStreamConnection(dest,channelId);
                }
                System.out.println("OPEN "+messageCon);
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

    private void uponFileBytesMessage(FileBytesCarrier msg, Host from, short sourceProto, int channelId, String streamId) {
        writeToFile(msg.len,msg.data);
    }
    private void uponMsgFail(FileBytesCarrier msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("NOT BYTES Message {} to {} failed, reason: {}", msg, host, throwable);
        logger.info("DATA SENT <{}>",msg.len);

    }
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
        }else{
            uponOutConnectionUp(event, channelId);
        }

    }
    private boolean isMessageConnection = false;
    private void uponMessageConnectionEvent(OnMessageConnectionUpEvent event, int channelId) {
        isMessageConnection = true;
        streamId = event.conId;
        if(event.inConnection){
            logger.info("CONNECTION TO {} IS UP. CONNECTION TYPE: {}",event.getNode(),event.type+" SS "+streamId);
            try{
                fos = new FileOutputStream(myself.getPort()+NETWORK_PROTO+"_STREAM.mp4");
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            if(myself.getPort()==8081){
                new Thread(() -> {
                    startStreaming();
                }).start();
            }
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
    long received = 0;
    FileOutputStream fos, fos2;
    boolean notW = true;
    long start = 0;

    private void writeToFile(int available, byte [] data){
        if(start==0){
            start = System.currentTimeMillis();
        }
        FileOutputStream out;
        if(myself.getPort()==8082){
            out = fos;
            //babelInputStream.sendBabelOutputStream(event.babelOutputStream);
        }else{
            out = fos2;
        }
        try {
            if(available<=0)return;
            received += available;
            out.write(data);
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
    }
    private void uponStreamBytes(BabelStreamDeliveryEvent event) {


        int available = event.babelOutputStream.readableBytes();
        byte [] p = event.babelOutputStream.readRemainingBytes();
        writeToFile(available,p);
        //logger.info("Received bytes2: {} from {} receivedTOTAL {} ",event.getMsg().length,event.getFrom(),received);

    }

    private void uponMsgFail2(OnStreamDataSentEvent msg, Host host, short destProto,
                              Throwable throwable, int channelId) {
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }
    int bufferSize = 128*1024; // 8KB buffer size
    public void startStreaming(){
        System.out.println("STREAMING STARTED!!!");
        try{
            //String p = "/home/tsunami/Downloads/Avatar The Way Of Water (2022) [1080p] [WEBRip] [5.1] [YTS.MX]/Avatar.The.Way.Of.Water.2022.1080p.WEBRip.x264.AAC5.1-[YTS.MX].mp4";
            //Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/Guardians Of The Galaxy Vol. 3 (2023) [1080p] [WEBRip] [x265] [10bit] [5.1] [YTS.MX]/Guardians.Of.The.Galaxy.Vol..3.2023.1080p.WEBRip.x265.10bit.AAC5.1-[YTS.MX].mp4");

            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/text.txt");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            //Path filePath = Paths.get(p);
            //
            if(isMessageConnection){
                System.out.println("NOT ZERO COPY");
                FileInputStream inputStream = new FileInputStream(filePath.toFile());
                int CHUNK = 64*1024;
                byte [] read = new byte[CHUNK];
                int ef = 0;
                while ( (ef = inputStream.read(read))>0){
                    sendMessage(new FileBytesCarrier(read,ef),streamId);
                    read = new byte[CHUNK];
                }
            }else{
                System.out.println("WITH ZERO COPY");
                babelInputStream.sendFile(filePath.toFile());
            }


            //long len = filePath.toFile().length();
            //sendStream(channelId,fileInputStream,len,streamId);
            System.out.println("SENT INPUTFILE TO SEND BYTES "+fileLen);

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
