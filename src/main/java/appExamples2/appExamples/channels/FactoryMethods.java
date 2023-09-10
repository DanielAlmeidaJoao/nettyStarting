package appExamples2.appExamples.channels;

import java.util.Properties;

public class FactoryMethods {

    public final static String SINGLE_THREADED_PROP="singleThreaded";
    public final static String SERVER_THREADS = "serverThreads";
    public final static String CLIENT_THREADS = "clientThreads";


    public static int serverThreads(Properties properties){
        return Integer.parseInt((String) properties.getOrDefault(SERVER_THREADS,"0"));
    }
    public static int clientThreads(Properties properties){
        return Integer.parseInt((String) properties.getOrDefault(CLIENT_THREADS,"1"));
    }
}
