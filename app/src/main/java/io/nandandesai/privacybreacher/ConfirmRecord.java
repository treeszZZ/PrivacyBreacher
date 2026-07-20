package io.nandandesai.privacybreacher;

public class ConfirmRecord {
    private long id;
    private String date;      // sleep_date 用于分组显示
    private String time;      // 格式化的时间 HH:mm
    private long timestamp;   // 原始时间戳

    public ConfirmRecord(long id, String date, String time, long timestamp) {
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