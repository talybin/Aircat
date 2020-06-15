package com.talybin.aircat;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashSet;
import java.util.Set;

public class JobsFragment extends Fragment
        implements JobManager.Listener, JobListAdapter.Listener, ActionMode.Callback
{

    private RecyclerView jobList;
    private RecyclerView.Adapter adapter;
    private FloatingActionButton createJobBut;

    private NavController navController;

    private JobManager jobManager;

    private Set<JobListAdapter.JobViewHolder> selectedItems = new HashSet<>();
    private ActionMode actionMode = null;

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
        //AppCompatActivity acActivity = (AppCompatActivity)ctx;
        adapter = new JobListAdapter(this);
        jobList.setAdapter(adapter);

        // Create new job button
        createJobBut = view.findViewById(R.id.createNewJobBut);
        createJobBut.setOnClickListener(
                v -> navController.navigate(R.id.action_JobsFragment_to_CreateJobFragment));

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

    @Override
    public void onItemClick(JobListAdapter.JobViewHolder holder) {
        if (actionMode == null) {
            // Show details
            Bundle args = new Bundle();
            args.putInt("job_position", holder.getAdapterPosition());
            navController.navigate(R.id.action_JobsFragment_to_jobDetailsFragment, args);
        }
        else // We are in multi-selection mode, do the same as onItemLongClick
            toggleSelection(holder);
    }

    @Override
    public boolean onItemLongClick(JobListAdapter.JobViewHolder holder) {
        toggleSelection(holder);
        return true;
    }

    private void toggleSelection(JobListAdapter.JobViewHolder holder) {
        if (selectedItems.contains(holder)) {
            // Unselect item
            selectedItems.remove(holder);
            holder.select(false);
            // Exit action mode on last one
            if (selectedItems.size() == 0) {
                actionMode.finish();
                actionMode = null;
            }
        }
        else { // Add selected item
            selectedItems.add(holder);
            holder.select(true);
            // Enter action mode on first one
            if (selectedItems.size() == 1 && actionMode == null) {
                AppCompatActivity activity = (AppCompatActivity)requireActivity();
                actionMode = activity.startSupportActionMode(this);
            }
        }

        // Set title
        if (actionMode != null && selectedItems.size() > 0)
            actionMode.setTitle("" + selectedItems.size());
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.job_detail_menu, menu);
        // Add Settings menu item from main_menu
        inflater.inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_pause).setVisible(false);
        menu.findItem(R.id.action_start).setVisible(false);

        createJobBut.setVisibility(View.GONE);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove:
                // Remove selected items. Since removal is by position, we
                // need to sort and remove in reverse order.
                selectedItems.stream()
                        .map(JobListAdapter.JobViewHolder::getAdapterPosition)
                        .sorted((i1, i2) -> i2 - i1)    // Reverse sort
                        .forEach(jobManager::remove);
                // Redraw
                actionMode.finish();
                actionMode = null;
                adapter.notifyDataSetChanged();
                return true;
            case R.id.action_select_all:
                selectedItems.clear();
                for (int i = 0; i < jobManager.getJobs().size(); ++i) {
                    toggleSelection((JobListAdapter.JobViewHolder)
                        jobList.findViewHolderForAdapterPosition(i));
                }
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        for (JobListAdapter.JobViewHolder selHolder : selectedItems)
            selHolder.select(false);
        selectedItems.clear();
        createJobBut.setVisibility(View.VISIBLE);
    }
}
