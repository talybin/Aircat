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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

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
        jobManager.add(new Job());
    }

    @Override
    public void onDestroyView() {
        jobManager.removeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.job_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start:
                startSelectedJobs();
                return true;
            case R.id.action_remove:
                removeSelectedJobs();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewJob(Job job) {
        // TODO add listeners here
        adapter.notifyDataSetChanged();
    }

    /*
    @Override
    public void onStart(final HashCatHandler handler, final Job job) {
        handler.addListener(new HashCatHandler.Listener() {
            @Override
            public void onJobState(Job.State state) {
                if (state == Job.State.NOT_RUNNING) {
                    handler.removeListener(this);
                    job.status = null;
                }
                job.setState(state);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onStatus(HashCat2.Status status) {
                Log.d("JobsFragment", "---> new status: " + status);
                job.status = status;
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception ex) {
                Toast.makeText(getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }*/

    private void startSelectedJobs() {
        // Just a test right now
        List<Job> jobs = jobManager.getJobs();
        if (jobs.isEmpty())
            return;

        Context context = getContext();

        if (!jobs.get(0).start(context))
            Toast.makeText(context, R.string.failed_to_start_job, Toast.LENGTH_LONG).show();
    }

    private void removeSelectedJobs() {
        // Just a test right now
        List<Job> jobs = jobManager.getJobs();
        if (!jobs.isEmpty()) {
            jobManager.remove(jobs.size() - 1);
            adapter.notifyDataSetChanged();
        }
    }
}
