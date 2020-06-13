package com.talybin.aircat;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class JobsFragment extends Fragment implements JobManager.Listener {

    private RecyclerView jobList;
    private RecyclerView.Adapter adapter;

    private NavController navController;

    private JobManager jobManager;

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

        Context ctx = requireActivity();
        navController = NavHostFragment.findNavController(JobsFragment.this);

        jobManager = JobManager.getInstance();

        // Job list
        jobList = view.findViewById(R.id.job_list);
        jobList.setLayoutManager(new LinearLayoutManager(ctx));
        jobList.setHasFixedSize(true);

        // Specify an adapter
        // Navigate to job details on view click
        adapter = new JobListAdapter(new JobListAdapter.ClickListener() {
            @Override
            public void onClick(Job job, int position) {
                Bundle args = new Bundle();
                args.putInt("job_position", position);
                navController.navigate(R.id.action_JobsFragment_to_jobDetailsFragment, args);
            }
        });
        jobList.setAdapter(adapter);

        // Create new job button
        FloatingActionButton fab = view.findViewById(R.id.createNewJobBut);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_JobsFragment_to_CreateJobFragment);
            }
        });

        // Listen to job list changes
        jobManager.addListener(this);

        // Test
        JobManager.getInstance().add(new Job());
    }

    @Override
    public void onDestroyView() {
        jobManager.removeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onNewJob(Job job) {
        // TODO add listeners here
        adapter.notifyDataSetChanged();
    }
}
