package com.talybin.aircat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class JobsFragment extends Fragment implements JobManager.Listener {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_jobs, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Job list
        RecyclerView jobList = view.findViewById(R.id.job_list);
        jobList.setHasFixedSize(true);

        // Create new job button
        FloatingActionButton fab = view.findViewById(R.id.createNewJobBut);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(JobsFragment.this)
                        .navigate(R.id.action_JobsFragment_to_CreateJobFragment);
            }
        });

        // Listen to job list changes
        JobManager.getInstance().addListener(this);
    }

    @Override
    public void onNewJob() {
        Log.d("JobsFragment", "----> new job added");
    }
}
