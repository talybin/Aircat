package com.talybin.aircat;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavAction;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
            public void onClick(Job job) {
                Bundle args = new Bundle();
                args.putString("job_hash", job.getHash());
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
        Job test = new Job();
        test.apMac = "c4:72:95:64:51:26";
        test.clientMac = "6c:c7:ec:95:3d:63";
        test.pmkId = "5265b2887ac349c4096eb7c2e4aaba61";
        test.ssid = "IterationRentalsWifi";
        jobManager.add(test);
    }

    @Override
    public void onDestroyView() {
        jobManager.removeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onNewJob(Job job) {
        adapter.notifyDataSetChanged();
    }
}
