package com.talybin.aircat;

import android.app.Application;
import android.os.Handler;

import androidx.lifecycle.LiveData;

import java.util.List;

// A Repository manages queries and allows you to use multiple backends.
// In the most common example, the Repository implements the logic for
// deciding whether to fetch data from a network or use results cached
// in a local database.
class AircatRepository {

    private WordListDao wordListDao;
    private JobDao jobDao;

    private Handler uiHandler;

    // Note that in order to unit test the AircatRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    AircatRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        wordListDao = db.wordListDao();
        jobDao = db.jobDao();
        uiHandler = new Handler();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    //LiveData<List<Job>> getAllJobs() {
    //    return jobDao.getJobs();
    //}

    LiveData<Job> getJob(String id) {
        return jobDao.get(id);
    }

    /*
    void getAllWordListsSync(Consumer<List<WordList>> callback) {
        AircatRoomDatabase.databaseExecutor.execute(() -> {
            List<WordList> data = wordListDao.getWordListsSync();
            uiHandler.post(() -> callback.accept(data));
        });
    }*/
    /*
    List<WordList> getAllWordListsSync() {
        return wordListDao.getWordListsSync();
    }*/

    //LiveData<List<WordList>> getAllWordLists() {
    //    return wordListDao.getWordLists();
    //}

    //LiveData<WordList> getWordList(Uri id) {
    //    return wordListDao.get(id);
    //}

    void deleteAllJobs() {
        AppDatabase.databaseExecutor.execute(() -> {
            jobDao.deleteAll();
        });
    }

    void insert(WordList wordList) {
        // You must call this on a non-UI thread or your app will throw an exception. Room ensures
        // that you're not doing any long running operations on the main thread, blocking the UI.
        AppDatabase.databaseExecutor.execute(() -> {
            wordListDao.insert(wordList);
        });
    }

    void insert(Job job) {
        AppDatabase.databaseExecutor.execute(() -> {
            jobDao.insert(job);
        });
    }

    void delete(Job job) {
        AppDatabase.databaseExecutor.execute(() -> {
            jobDao.delete(job);
        });
    }

    void update(Job job) {
        AppDatabase.databaseExecutor.execute(() -> {
            jobDao.update(job);
        });
    }
}
