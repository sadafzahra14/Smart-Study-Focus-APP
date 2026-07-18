package com.example.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.database.AppDatabase;
import com.example.database.FocusSession;
import com.example.database.FocusSessionDao;
import com.example.database.User;
import com.example.database.UserDao;

import java.util.List;

public class FocusViewModel extends AndroidViewModel {

    private final UserDao userDao;
    private final FocusSessionDao sessionDao;
    private final AppDatabase database;

    public interface DatabaseCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public FocusViewModel(@NonNull Application application) {
        super(application);
        database = AppDatabase.getDatabase(application);
        userDao = database.userDao();
        sessionDao = database.focusSessionDao();
    }

    // --- FocusSession Operations ---

    public void insertSession(FocusSession session) {
        AppDatabase.databaseWriteExecutor.execute(() -> sessionDao.insertSession(session));
    }

    public LiveData<List<FocusSession>> getSessionsForUser(String email) {
        return sessionDao.getSessionsForUser(email);
    }

    public LiveData<List<FocusSession>> getRecentSessions(String email, int limit) {
        return sessionDao.getRecentSessions(email, limit);
    }

    public void getSessionsForUserSync(String email, DatabaseCallback<List<FocusSession>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<FocusSession> list = sessionDao.getSessionsForUserSync(email);
                callback.onSuccess(list);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // --- User Operations ---

    public void registerUser(User user, DatabaseCallback<Void> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                User existing = userDao.getUserByEmail(user.getEmail());
                if (existing != null) {
                    callback.onError("User already exists with this email.");
                } else {
                    userDao.registerUser(user);
                    callback.onSuccess(null);
                }
            } catch (Exception e) {
                callback.onError("Registration failed: " + e.getMessage());
            }
        });
    }

    public void loginUser(String email, String password, DatabaseCallback<User> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                User user = userDao.loginUser(email, password);
                if (user != null) {
                    callback.onSuccess(user);
                } else {
                    callback.onError("Invalid email or password.");
                }
            } catch (Exception e) {
                callback.onError("Login failed: " + e.getMessage());
            }
        });
    }

    public void getUserByEmail(String email, DatabaseCallback<User> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                User user = userDao.getUserByEmail(email);
                if (user != null) {
                    callback.onSuccess(user);
                } else {
                    callback.onError("User not found.");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void updateUser(User user, DatabaseCallback<Void> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                userDao.updateUser(user);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError("Failed to update profile: " + e.getMessage());
            }
        });
    }
}
