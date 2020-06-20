package com.talybin.aircat;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface JobDao {

    // Allowing the insert of the same word multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Job job);

    @Delete
    void delete(Job job);

    @Query("DELETE FROM job_table")
    void deleteAll();

    @Query("SELECT * FROM job_table")
    LiveData<List<Job>> getJobs();
}
