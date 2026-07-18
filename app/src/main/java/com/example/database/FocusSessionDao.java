package com.example.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FocusSessionDao {
    @Insert
    long insertSession(FocusSession session);

    @Query("UPDATE focus_sessions SET distractionCount = :count, distractionApps = :apps WHERE id = :id")
    void updateDistractions(long id, int count, String apps);

    @Query("UPDATE focus_sessions SET endTime = :endTime, focusTime = :focusTime, focusPercentage = :pct, focusStatus = :status WHERE id = :id")
    void completeSession(long id, String endTime, long focusTime, double pct, String status);

    @Query("SELECT * FROM focus_sessions WHERE userEmail = :email ORDER BY id DESC")
    LiveData<List<FocusSession>> getSessionsForUser(String email);

    @Query("SELECT * FROM focus_sessions WHERE userEmail = :email ORDER BY id DESC")
    List<FocusSession> getSessionsForUserSync(String email);

    @Query("SELECT * FROM focus_sessions WHERE userEmail = :email ORDER BY id DESC LIMIT :limit")
    LiveData<List<FocusSession>> getRecentSessions(String email, int limit);
}