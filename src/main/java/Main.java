import babel.appExamples.channels.BabelStreamingChannel;
import babel.appExamples.channels.StreamReceiverChannel;
import babel.appExamples.channels.StreamSenderChannel;
import babel.appExamples.channels.initializers.BabelStreamInitializer;
import babel.appExamples.channels.initializers.StreamReceiverInitializer;
import babel.appExamples.channels.initializers.StreamSenderInitializers;
import babel.appExamples.protocols.ReceiveFileProtocol;
import babel.appExamples.protocols.SendFileProtocol;
import pt.unl.fct.di.novasys.babel.core.Babel;

import java.io.IOException;
import java.util.Properties;

public class Main {
    private static final String DEFAULT_CONF = "config.properties";
    public static void main(String[] args) throws Exception {
        System.out.println("Hello world 2!");


        //Get the (singleton) babel instance
        Babel babel = Babel.getInstance();
        babel.registerChannelInitializer(BabelStreamingChannel.NAME,new BabelStreamInitializer());


        //Loads properties from the configuration file, and merges them with
        // properties passed in the launch argumentsSD=300
        Properties props = Babel.loadConfig(args, DEFAULT_CONF);

        //If you pass an interface name in the properties (either file or arguments), this wil get the
        // IP of that interface and create a property "address=ip" to be used later by the channels.
        System.out.println(props);
        System.out.println(args.length);
        if(props.get("sender")!=null){
            SendFileProtocol sendFileProtocol = new SendFileProtocol(props);
            babel.registerProtocol(sendFileProtocol);
            sendFileProtocol.init(props);
        }else{
            ReceiveFileProtocol receiveFileProtocol = new ReceiveFileProtocol(props);
            babel.registerProtocol(receiveFileProtocol);
            receiveFileProtocol.init(props);
        }
        //Start babel and protocol threads
        babel.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("APP ENEDED!!");
        }));
        System.out.println("APP STARTED!!");
        /**
        int sleep = Integer.parseInt(props.getProperty("SD"));
        Thread.sleep(sleep*1000);
        System.exit(1);**/
    }
}