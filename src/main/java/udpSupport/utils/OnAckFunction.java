package udpSupport.utils;

@FunctionalInterface
public interface OnAckFunction {
    void execute(long msgId);
}
