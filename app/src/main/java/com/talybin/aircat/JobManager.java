package com.talybin.aircat;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class JobManager {

    private static JobManager instance = null;

    static JobManager getInstance() {
        if (instance == null) {
            synchronized (JobManager.class) {
                if (instance == null)
                    instance = new JobManager();
            }
        }
        return instance;
    }

    public interface Listener {
        void onChange(List<Job> jobs);
    }

    private JobDao jobDao;
    private List<Job> jobList;

    private Handler uiHandler;

    private Listener listener;

    private JobManager() {
        jobDao = AppDatabase.getDatabase(App.getContext()).jobDao();
        // Avoid crash while loading
        jobList = new ArrayList<>();

        uiHandler = new Handler();
        listener = null;

        AppDatabase.databaseExecutor.execute(() -> {
            jobList = jobDao.getJobs();
            uiHandler.post(this::listUpdated);
        });
    }

    private void listUpdated() {
        if (listener != null)
            listener.onChange(jobList);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void removeListeners() {
        this.listener = null;
        jobList.forEach(Job::removeListeners);
    }

    @NonNull
    List<Job> getAll() {
        return jobList;
    }

    @Nullable
    Job get(String id) {
        for (Job job : jobList) {
            if (job.getPmkId().equals(id))
                return job;
        }
        return null;
    }

    boolean add(Job job) {
        // The job list is unique
        if (get(job.getPmkId()) != null)
            return false;

        AppDatabase.databaseExecutor.execute(() -> jobDao.insert(job));
        jobList.add(job);
        listUpdated();
        return true;
    }

    void update(Job job) {
        AppDatabase.databaseExecutor.execute(() -> jobDao.update(job));
    }

    void remove(Job job) {
        if (jobList.remove(job)) {
            AppDatabase.databaseExecutor.execute(() -> jobDao.delete(job));
            HashCat.getInstance().stop(job);
            listUpdated();
        }
    }

    void remove(List<Job> jobs) {
        if (jobs.size() == jobList.size()) {
            AppDatabase.databaseExecutor.execute(jobDao::deleteAll);
            jobList.clear();
        }
        else {
            for (Job job : jobs) {
                AppDatabase.databaseExecutor.execute(() -> jobDao.delete(job));
                jobList.remove(job);
            }
        }
        HashCat.getInstance().stop(jobs);
        listUpdated();
    }
}
