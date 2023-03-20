import babel.appExamples.channels.BabelStreamingChannel;
import babel.appExamples.channels.initializers.BabelStreamInitializer;
import babel.appExamples.channels.messages.StreamMessage;
import babel.appExamples.protocols.ReceiveFileProtocol;
import babel.appExamples.protocols.SendFileProtocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.Babel;
import quicSupport.testing.TestQuicChannel;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

public class Main2 {

    static {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
    }
    private static final Logger logger = LogManager.getLogger(TestQuicChannel.class);

    private static final String DEFAULT_CONF = "config.properties";
    public static void main(String[] args) throws Exception {
        logger.info("STARREDD");
        Properties properties = new Properties();
        properties.setProperty("address","localhost");
        String port = args[0];
        properties.setProperty("port",port);
        TestQuicChannel testQuicChannel = new TestQuicChannel(properties);
        /**
        if("8081".equals(port)){
            InetSocketAddress remote = new InetSocketAddress("localhost",8082);
            testQuicChannel.openConnection(remote);
        }
        System.out.println("CONNECTED"); **/
        //testQuicChannel.end();
        Scanner scanner = new Scanner(System.in);


        String input = "";

        while (!"quit".equalsIgnoreCase(input)){
            input = scanner.nextLine();
            if(input.equalsIgnoreCase("S")){//send message
                System.out.println("Enter data:");
                input = scanner.nextLine();
                System.out.println("Enter streamId:");
                String streamId = scanner.nextLine().trim();
                System.out.println("ENTERED: "+input);
                System.out.println("STREAM_ID: "+streamId);
                testQuicChannel.send(streamId,input.getBytes(),input.length());
            }else if(input.equalsIgnoreCase("C")){//create stream
                System.out.println("HOST NAME:");
                String host = scanner.nextLine();
                System.out.println("PORT");
                int p = scanner.nextInt();
                InetSocketAddress address = new InetSocketAddress(host,p);
                testQuicChannel.createStream(address);
            }else if(input.equalsIgnoreCase("CS")){//close stream
                System.out.println("STREAM ID:");
                String streamId = scanner.nextLine();
                testQuicChannel.closeStream(streamId);
            }else if(input.equalsIgnoreCase("O")){//OPEN CONNECTION
                System.out.println("HOST NAME:");
                String host = scanner.nextLine();
                System.out.println("PORT");
                int p = scanner.nextInt();
                InetSocketAddress address = new InetSocketAddress(host,p);
                testQuicChannel.openConnection(address);
            }else if(input.equalsIgnoreCase("CC")){//CLOSE CONNECTIONS
                System.out.println("HOST NAME:");
                String host = scanner.nextLine();
                System.out.println("PORT");
                int p = scanner.nextInt();
                InetSocketAddress address = new InetSocketAddress(host,p);
                testQuicChannel.closeConnection(address);
            } else if ("SS".equalsIgnoreCase(input)) {
                System.out.println("STREAM ID:");
                String streamId = scanner.nextLine();
                System.out.println("HOW MUCH:");
                int toSend = scanner.nextInt();
                Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
                //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
                FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
                int bufferSize = toSend; // 8KB buffer size
                byte [] bytes = new byte[bufferSize];

                //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                int read=0, totalSent = 0;
                if ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                    totalSent += read;
                    testQuicChannel.send(streamId,bytes,read);
                    System.out.println("SENT "+totalSent);
                }
            }
        }
        System.out.println("STILL HERE!!!");
        scanner.close();
    }
}