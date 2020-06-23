package com.talybin.aircat;

import android.os.Handler;

import java.util.List;
import java.util.function.Consumer;

public class JobManager {

    private static JobManager instance = null;

    private JobDao jobDao;

    private Handler uiHandler = new Handler();
    // Cached copy of database table
    private List<Job> jobList = null;

    public interface Listener {
        void onJobListChange(List<Job> jobs);
    }

    static JobManager getInstance() {
        if (instance == null) {
            synchronized (JobManager.class) {
                if (instance == null)
                    instance = new JobManager();
            }
        }
        return instance;
    }

    private JobManager() {
        jobDao = AppDatabase.getDatabase(App.getContext()).jobDao();
    }

    // Return -1 if not found
    int find(Job job) {
        int idx = jobList.size() - 1;
        for (; idx >= 0; ++idx) {
            if (jobList.get(idx).getPmkId().equals(job.getPmkId()))
                break;
        }
        return idx;
    }

    boolean contains(Job job) {
        return find(job) >= 0;
    }

    void getAll(Consumer<List<Job>> callback) {
        if (jobList != null)
            callback.accept(jobList);
        else {
            AppDatabase.databaseExecutor.execute(() -> {
                jobList = jobDao.getJobs();
                uiHandler.post(() -> callback.accept(jobList));
            });
        }
    }

    void deleteAll() {
        AppDatabase.databaseExecutor.execute(jobDao::deleteAll);
        jobList.clear();
    }

    // Return position of new job in list. Useful for list notify.
    int add(Job job) {
        if (!contains(job)) {
            int position = jobList.size();
            AppDatabase.databaseExecutor.execute(() -> jobDao.insert(job));
            jobList.add(job);
            return position;
        }
        return -1;
    }

    void delete(Job job) {
        int position = find(job);
        if (position >= 0) {
            AppDatabase.databaseExecutor.execute(() -> jobDao.delete(job));
            jobList.remove(position);
        }
    }
}
