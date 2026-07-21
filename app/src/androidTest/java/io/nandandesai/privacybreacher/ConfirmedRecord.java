package io.nandandesai.privacybreacher;

public class ConfirmedRecord {
    private long id;
    private String date;
    private String time;
    private long timestamp;

    public ConfirmedRecord(long id, String date, String time, long timestamp) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public long getTimestamp() { return timestamp; }
}
