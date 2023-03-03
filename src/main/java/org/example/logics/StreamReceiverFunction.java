package org.example.logics;

@FunctionalInterface
public interface StreamReceiverFunction {
    public void execute(String id, byte [] data);
}
