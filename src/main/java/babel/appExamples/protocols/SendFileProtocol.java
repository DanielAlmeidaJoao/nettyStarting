package babel.appExamples.protocols;

import babel.appExamples.channels.messages.StreamMessage;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SendFileProtocol extends GenericProtocol {

    private Host receiver;
    public static final short ID = 206;
    private int channelId;
    private String streamId;

    public SendFileProtocol(int channelId){
        super(ReceiveFileProtocol.class.getSimpleName(),ID);
        this.channelId = channelId;
    }
    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        //registerMessageSerializer(channelId,StreamMessage.ID, StreamMessage.serializer);
        //registerMessageHandler(channelId,JoinRequestMessage.MSG_ID,this::uponJoinRequestMessage,this::uponMsgFail);
    }

    public void sendFile(){
        try{
            //Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            int bufferSize = 2*128*1024; // 8KB buffer size
            byte [] bytes = new byte[bufferSize];

            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read=0, totalSent = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                StreamMessage streamMessage = new StreamMessage(bytes,read,"OLA");
                sendMessage(streamMessage,receiver);
            }
            Thread.sleep(10*1000);
            System.out.println("TOTAL SENT "+totalSent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
