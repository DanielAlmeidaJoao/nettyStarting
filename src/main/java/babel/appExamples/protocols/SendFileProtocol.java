package babel.appExamples.protocols;

import babel.appExamples.channels.BabelStreamingChannel;
import babel.appExamples.channels.StreamReceiverChannel;
import babel.appExamples.channels.StreamSenderChannel;
import babel.appExamples.channels.messages.EndOfStreaming;
import babel.appExamples.channels.messages.StreamMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SendFileProtocol extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(SendFileProtocol.class);

    private Host receiver;
    public static final short ID = 204;
    private int channelId;
    private String streamId;
    private FileOutputStream fos;

    public SendFileProtocol(Properties props) throws IOException {
        super(ReceiveFileProtocol.class.getSimpleName(),ID);
        String address = props.getProperty("address");
        String port = props.getProperty("p2p_port");
        logger.info("Receiver on {}:{}", address, port);
        this.receiver = new Host(InetAddress.getByName(address), Integer.parseInt(port));

        Properties channelProps = new Properties();
        channelId = createChannel(BabelStreamingChannel.NAME, props);
        try {
            fos = new FileOutputStream("DANIEL_copy.mp4");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        //registerMessageSerializer(channelId,StreamMessage.ID, StreamMessage.serializer);
        //registerMessageHandler(channelId,JoinRequestMessage.MSG_ID,this::uponJoinRequestMessage,this::uponMsgFail);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerMessageHandler(channelId,StreamMessage.ID,this::uponReceiveMessage);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
        registerMessageHandler(channelId, EndOfStreaming.ID,this::uponEndOfStreamingMessage);
        Host peer = new Host(InetAddress.getByName("localhost"),Integer.parseInt(props.getProperty("p2p_port")));
        if(props.getProperty("forwarder")==null){
            sendFile(peer);
        }
    }

    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        try {
            System.out.println("CONNECTION CLOSED! "+totoal);
            fos.close();
        }catch (Exception e){e.printStackTrace();};
    }
    int totoal = 0;
    private void uponReceiveMessage(StreamMessage msg, Host from, short sourceProto, int channelId){
        try{
            totoal +=msg.getDataLength();
            fos.write(msg.getData(), 0,msg.getDataLength());
            fos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.info("CONNECTION TO {} IS UP.",event.getNode());
    }

    public void sendFile(Host peer){
        openConnection(peer);
        try{
            Thread.sleep(1000);
            System.out.println("STARTING SENDING FILE!");
            //Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            int bufferSize = 128*1024; // 8KB buffer size
            byte [] bytes = new byte[bufferSize];

            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read=0, totalSent = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                StreamMessage streamMessage = new StreamMessage(bytes,read,"OLA");
                sendMessage(streamMessage,receiver);
            }
            StreamMessage streamMessage = new StreamMessage(new byte[0],0,"OLA");

            sendMessage(streamMessage,receiver);
            System.out.println("TOTAL SENT "+totalSent);
            Thread.sleep(5*1000);
            closeConnection(receiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void uponEndOfStreamingMessage(EndOfStreaming msg, Host from, short sourceProto, int channelId){
        logger.info("RECEIVED EOS FROM {}.",from);
        try {
            logger.info("Stream Ended! THANKS");
            fos.close();
        }catch (Exception e){e.printStackTrace();};
    }

}
