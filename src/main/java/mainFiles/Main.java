package mainFiles;

import appExamples2.appExamples.channels.initializers.BabelStreamInitializer;
import appExamples2.appExamples.channels.newTCPChannel.BabelNewTCPChannel;
import appExamples2.appExamples.protocols.ReceiveFileProtocol;
import appExamples2.appExamples.protocols.SendFileProtocol;
import pt.unl.fct.di.novasys.babel.core.Babel;

import java.util.Properties;

public class Main {
    static {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
    }
    private static final String DEFAULT_CONF = "config.properties";
    public static void main(String[] args) throws Exception {
        System.out.println("Hello world 2!");


        //Get the (singleton) pt.unl.fct.di.novasys.babel instance
        Babel babel = Babel.getInstance();
        babel.registerChannelInitializer(BabelNewTCPChannel.NAME,new BabelStreamInitializer());


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
        //Start pt.unl.fct.di.novasys.babel and protocol threads
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