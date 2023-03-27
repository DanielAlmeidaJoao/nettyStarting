package org.streamingAPI.channel;
import java.io.IOException;
import java.util.Properties;

public abstract class MultiThreadedStreamingChannel extends StreamingChannel{
    public MultiThreadedStreamingChannel(Properties properties) throws IOException {
        super(properties,false);
    }
}
