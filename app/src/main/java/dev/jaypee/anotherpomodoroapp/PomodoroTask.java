package dev.jaypee.anotherpomodoroapp;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class PomodoroTask implements Serializable {
    private String id;
    private String userId;
    private String title;
    private int workTime;
    private int breakTime;
    private int longBreakTime = 15;
    private int longBreakInterval = 4;
    private long timestamp;

    public PomodoroTask() {
        // Required for Firestore
    }

    public PomodoroTask(String id, String userId, String title, int workTime, int breakTime, long timestamp) {
        this(id, userId, title, workTime, breakTime, 15, 4, timestamp);
    }

    public PomodoroTask(String id, String userId, String title, int workTime, int breakTime, int longBreakTime, int longBreakInterval, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.workTime = workTime;
        this.breakTime = breakTime;
        this.longBreakTime = longBreakTime > 0 ? longBreakTime : 15;
        this.longBreakInterval = longBreakInterval > 0 ? longBreakInterval : 4;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getWorkTime() {
        return workTime;
    }

    public void setWorkTime(int workTime) {
        this.workTime = workTime;
    }

    public int getBreakTime() {
        return breakTime;
    }

    public void setBreakTime(int breakTime) {
        this.breakTime = breakTime;
    }

    public int getLongBreakTime() {
        return longBreakTime > 0 ? longBreakTime : 15;
    }

    public void setLongBreakTime(int longBreakTime) {
        this.longBreakTime = longBreakTime;
    }

    public int getLongBreakInterval() {
        return longBreakInterval > 0 ? longBreakInterval : 4;
    }

    public void setLongBreakInterval(int longBreakInterval) {
        this.longBreakInterval = longBreakInterval;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
