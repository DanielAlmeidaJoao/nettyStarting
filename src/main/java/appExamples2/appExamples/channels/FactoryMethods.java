package appExamples2.appExamples.channels;

import java.util.Properties;

public class FactoryMethods {

    public final static String SINGLE_THREADED_PROP="SINGLE_THREADED";
    public final static String SERVER_THREADS = "SERVER_THREADS";
    public final static String CLIENT_THREADS = "CLIENT_THREADS";


    public static int serverThreads(Properties properties){
        return Integer.parseInt((String) properties.getOrDefault(SERVER_THREADS,"0"));
    }
    public static int clientThreads(Properties properties){
        return Integer.parseInt((String) properties.getOrDefault(CLIENT_THREADS,"1"));
    }
}
