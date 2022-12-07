package wethinkcode.service.messages;

public enum Prefix {
    QUEUE("queue://"),
    TOPIC("topic://");

    public final String prefix;

    Prefix(String s) {
        prefix = s;
    }
}
