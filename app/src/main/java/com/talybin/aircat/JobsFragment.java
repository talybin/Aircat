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

public class JobsFragment extends Fragment
        implements JobManager.Listener, JobListAdapter.Listener, ActionMode.Callback
{

    private JobListAdapter adapter;
    private FloatingActionButton createJobBut;

    private NavController navController;

    private JobManager jobManager;

    private androidx.appcompat.view.ActionMode actionMode = null;

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

        Context ctx = requireContext();
        navController = NavHostFragment.findNavController(JobsFragment.this);

        jobManager = JobManager.getInstance();

        // Job list
        RecyclerView jobList = view.findViewById(R.id.job_list);
        jobList.setLayoutManager(new LinearLayoutManager(ctx));
        jobList.setHasFixedSize(true);

        // Specify an adapter
        // Navigate to job details on view click
        adapter = new JobListAdapter(this);
        jobList.setAdapter(adapter);

        // Create new job button
        createJobBut = view.findViewById(R.id.createNewJobBut);
        createJobBut.setOnClickListener(
                v -> navController.navigate(R.id.action_JobsFragment_to_CreateJobFragment));

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
    public void onNewJob(Job job) {
        // TODO add listeners here
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(JobListAdapter.JobViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (actionMode == null) {
            // Show details
            Bundle args = new Bundle();
            args.putInt("job_position", position);
            navController.navigate(R.id.action_JobsFragment_to_jobDetailsFragment, args);
        }
        else // We are in multi-selection mode, do the same as onItemLongClick
            toggleSelection(position);
    }

    @Override
    public boolean onItemLongClick(JobListAdapter.JobViewHolder holder) {
        toggleSelection(holder.getAdapterPosition());
        return true;
    }

    private void updateActionModeTitle() {
        if (actionMode != null)
            actionMode.setTitle("" + adapter.getSelectedItemCount());
    }

    private void toggleSelection(int idx) {
        adapter.toggleSelection(idx);

        if (adapter.getSelectedItemCount() == 0)
            actionMode.finish();
        else {
            if (actionMode == null)
                actionMode = ((AppCompatActivity)requireActivity()).startSupportActionMode(this);
            updateActionModeTitle();
        }
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
                adapter.getSelectedItems().stream()
                        .sorted((i1, i2) -> i2 - i1)    // Reverse sort
                        .forEach(jobManager::remove);
                // Selections in adapter will be cleared in finish()
                actionMode.finish();
                return true;

            case R.id.action_select_all:
                adapter.selectRange(0, jobManager.getJobs().size());
                updateActionModeTitle();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.clearSelections();
        createJobBut.setVisibility(View.VISIBLE);
        actionMode = null;
    }
}
