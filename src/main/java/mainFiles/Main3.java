package mainFiles;

import appExamples2.appExamples.channels.babelNewChannels.quicChannels.BabelQUIC_P2P_Channel;
import appExamples2.appExamples.channels.babelNewChannels.tcpChannels.BabelTCP_P2P_Channel;
import appExamples2.appExamples.channels.initializers.BabelQUICChannelInitializer;
import appExamples2.appExamples.channels.initializers.BabelTCPChannelInitializer;
import appExamples2.appExamples.channels.babelNewChannels.udpBabelChannel.BabelUDPChannel;
import appExamples2.appExamples.channels.babelNewChannels.udpBabelChannel.BabelUDPInitializer;
import appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.EchoProtocol;
import pt.unl.fct.di.novasys.babel.core.Babel;
import quicSupport.utils.enums.NetworkRole;

import java.util.Properties;
import java.util.Scanner;

public class Main3 {
    static {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
    }
    private static final String DEFAULT_CONF = "config.properties";
    public static void main(String[] args) throws Exception {
        System.out.println("Hello world 2!");

        //Get the (singleton) pt.unl.fct.di.novasys.babel instance
        Babel babel = Babel.getInstance();
        babel.registerChannelInitializer(BabelQUIC_P2P_Channel.CHANNEL_NAME,new BabelQUICChannelInitializer(NetworkRole.P2P_CHANNEL));
        babel.registerChannelInitializer(BabelTCP_P2P_Channel.CHANNEL_NAME,new BabelTCPChannelInitializer(NetworkRole.P2P_CHANNEL));
        babel.registerChannelInitializer(BabelUDPChannel.NAME,new BabelUDPInitializer());



        //Loads properties from the configuration file, and merges them with
        // properties passed in the launch argumentsSD=300
        Properties props = Babel.loadConfig(args, DEFAULT_CONF);

        //If you pass an interface name in the properties (either file or arguments), this wil get the
        // IP of that interface and create a property "address=ip" to be used later by the channels.
        EchoProtocol echoProtocol = new EchoProtocol(props);
        //EchoProtocol2 echoProtocol2 = new EchoProtocol2(props);
        babel.registerProtocol(echoProtocol);
        //babel.registerProtocol(echoProtocol2);
        echoProtocol.init(props);
        //echoProtocol2.addChan(echoProtocol.channelId);

        //Start pt.unl.fct.di.novasys.babel and protocol threads
        babel.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("APP ENEDED!!");
        }));
        System.out.println("APP STARTED ww!!");
        Scanner scanner = new Scanner(System.in);

        String input = "";
        while (!input.equalsIgnoreCase("quit")){
            System.out.print("COMMAND: ");
            input = scanner.nextLine();
            if(input.equalsIgnoreCase("open")){
                System.out.printf("port:");
                String port = scanner.nextLine();
                System.out.printf("type: M or S" );
                String type = scanner.nextLine();
                echoProtocol.openSS(port,type);
            }else if(input.equalsIgnoreCase("stream")){
                System.out.printf("MESSAGE TO SEND:");
                String message = scanner.nextLine();
                System.out.printf("STREAM:");
                String stream = scanner.nextLine();
                echoProtocol.sendMessage(message,stream);
            }else if(input.equalsIgnoreCase("createstream")){
                echoProtocol.createStream();
            }else if(input.equalsIgnoreCase("send")){
                System.out.printf("MESSAGE TO SEND:");
                String message = scanner.nextLine();
                echoProtocol.sendMessage(message);
            }else if(input.equalsIgnoreCase("closestream")){
                System.out.printf("STREAM:");
                String stream = scanner.nextLine();
                echoProtocol.closeStreamM(stream);
            }else if(input.equalsIgnoreCase("connected")){
                echoProtocol.isConnected();
            }else if(input.equalsIgnoreCase("connections")){
                echoProtocol.connections();
            }else if(input.equalsIgnoreCase("numbercons")){
                echoProtocol.numberConnected();
            }else if(input.equalsIgnoreCase("avstreams")){
                echoProtocol.streamsAvailable();
            }else if(input.equalsIgnoreCase("shutdown")){
                echoProtocol.shutDown();
            }else if(input.equalsIgnoreCase("sendstream")){
                System.out.printf("MESSAGE TO SEND:");
                String message = scanner.nextLine();
                echoProtocol.sendStream(message.repeat(1000*1000));
            }else if(input.equalsIgnoreCase("streamstream")){
                    System.out.printf("MESSAGE TO SEND:");
                    String message = scanner.nextLine();
                    System.out.printf("STREAM:");
                    String stream = scanner.nextLine();
                    echoProtocol.sendStream(message.repeat(1000*1000),stream);
            } else {
                System.out.println("UNKNOWN COMMAND: "+input);
            }
        }
        /**
        int sleep = Integer.parseInt(props.getProperty("SD"));
        Thread.sleep(sleep*1000);
        System.exit(1);**/
    }
}