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
    private long timestamp;

    public PomodoroTask() {
        // Required for Firestore
    }

    public PomodoroTask(String id, String userId, String title, int workTime, int breakTime, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.workTime = workTime;
        this.breakTime = breakTime;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
