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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class JobsFragment extends Fragment implements JobManager.Listener {

    private RecyclerView jobList;
    private RecyclerView.Adapter adapter;
    private TextView emptyView;

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

        jobManager = JobManager.getInstance();

        // Job list
        jobList = view.findViewById(R.id.job_list);
        jobList.setLayoutManager(new LinearLayoutManager(ctx));
        jobList.setHasFixedSize(true);

        // Specify an adapter
        adapter = new JobListAdapter();
        jobList.setAdapter(adapter);

        // Empty view (visible when ap list is empty)
        emptyView = view.findViewById(R.id.jobs_empty_view);

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
        jobManager.addListener(this);
    }

    @Override
    public void onNewJob(Job job) {
        Log.d("JobsFragment::onNewJob", "---> nr jobs: " + jobManager.getJobs().size());
        if (jobManager.getJobs().size() == 1) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("JobsFragment::onNewJob", "---> after: job list visible " + jobList.getVisibility());
                    Log.d("JobsFragment::onNewJob", "---> after: empty view visible " + emptyView.getVisibility());
                    jobList.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
            }, 3000);
            jobList.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            Log.d("JobsFragment::onNewJob", "---> job list visible " + jobList.getVisibility());
            Log.d("JobsFragment::onNewJob", "---> empty view visible " + emptyView.getVisibility());
            //jobList.postInvalidate();
            //emptyView.postInvalidate();
        }
        adapter.notifyDataSetChanged();
    }
}
