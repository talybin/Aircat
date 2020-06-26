package com.talybin.aircat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = { WordList.class, Job.class }, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WordListDao wordListDao();
    public abstract JobDao jobDao();

    private static final int NUMBER_OF_THREADS = 4;

    private static volatile AppDatabase instance;
    static final ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            // Add default entries
            databaseExecutor.execute(() -> {
                WordListDao dao = instance.wordListDao();

                dao.insert(new WordList(WordList.getDefault()));

                // Add test job
                instance.jobDao().insert(new Job(
                        "5265b2887ac349c4096eb7c2e4aaba61",
                        "IterationRentalsWifi",
                        "c4:72:95:64:51:26",
                        "6c:c7:ec:95:3d:63",
                        WordList.getDefault(),
                        null
                ));
            });
        }
    };

    static AppDatabase getDatabase(final Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "aircat_database")
                            .addCallback(roomDatabaseCallback)
                            .build();
                }
            }
        }
        return instance;
    }
}