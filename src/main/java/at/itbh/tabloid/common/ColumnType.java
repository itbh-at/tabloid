package at.itbh.tabloid.common;

public final class ColumnType {

    private ColumnType() {
        // Prevent instantiation
    }

    public static final String STRING = "string";
    public static final String NUMBER = "number";
    public static final String CURRENCY = "currency";
    public static final String DATE = "date";
    public static final String TIMESTAMP = "timestamp";
}