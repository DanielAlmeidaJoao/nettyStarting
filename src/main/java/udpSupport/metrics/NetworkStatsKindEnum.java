package udpSupport.metrics;

public enum NetworkStatsKindEnum {
    MESSAGE_STATS("MS"),
    MESSAGE_DELIVERED("MD"),
    ACK_STATS("AS");

    private final String value;

    NetworkStatsKindEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
