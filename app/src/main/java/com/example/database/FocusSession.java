package com.example.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_sessions")
public class FocusSession {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String userEmail;
    private String date;
    private String startTime;
    private String endTime;
    private long focusTime; // focused time in seconds
    private long completedTime; // total goal time in seconds
    private int distractionCount;
    private String distractionApps; // comma separated list
    private double focusPercentage;
    private String focusStatus; // e.g. "Completed", "Interrupted"

    public FocusSession(String userEmail, String date, String startTime, String endTime, 
                        long focusTime, long completedTime, int distractionCount, 
                        String distractionApps, double focusPercentage, String focusStatus) {
        this.userEmail = userEmail;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.focusTime = focusTime;
        this.completedTime = completedTime;
        this.distractionCount = distractionCount;
        this.distractionApps = distractionApps;
        this.focusPercentage = focusPercentage;
        this.focusStatus = focusStatus;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getFocusTime() {
        return focusTime;
    }

    public void setFocusTime(long focusTime) {
        this.focusTime = focusTime;
    }

    public long getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(long completedTime) {
        this.completedTime = completedTime;
    }

    public int getDistractionCount() {
        return distractionCount;
    }

    public void setDistractionCount(int distractionCount) {
        this.distractionCount = distractionCount;
    }

    public String getDistractionApps() {
        return distractionApps;
    }

    public void setDistractionApps(String distractionApps) {
        this.distractionApps = distractionApps;
    }

    public double getFocusPercentage() {
        return focusPercentage;
    }

    public void setFocusPercentage(double focusPercentage) {
        this.focusPercentage = focusPercentage;
    }

    public String getFocusStatus() {
        return focusStatus;
    }

    public void setFocusStatus(String focusStatus) {
        this.focusStatus = focusStatus;
    }
}
