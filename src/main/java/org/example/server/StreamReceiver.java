package org.example.server;

public interface StreamReceiver {
    void startListening() throws Exception;

    void close();

    void closeStream(String streamId);
}
