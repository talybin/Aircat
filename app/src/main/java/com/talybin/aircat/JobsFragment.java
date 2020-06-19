package com.talybin.aircat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static android.app.Activity.RESULT_OK;

public class JobsFragment extends Fragment
        implements JobManager.Listener, JobListAdapter2.Listener, ActionMode.Callback
{
    private static final int NEW_JOB_ACTIVITY_REQUEST_CODE = 1;

    private JobListAdapter adapter;
    private FloatingActionButton createJobBut;

    private NavController navController;

    private JobViewModel jobViewModel;
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
        adapter = new JobListAdapter();
        jobList.setAdapter(adapter);

        jobViewModel = new ViewModelProvider(this).get(JobViewModel.class);
        jobViewModel.getAllJobs().observe(getViewLifecycleOwner(), jobs -> {
            // Update the cached copy of the jobs in the adapter.
            adapter.setJobs(jobs);
        });

        // Create new job button
        createJobBut = view.findViewById(R.id.createNewJobBut);

        createJobBut.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NewJobActivity.class);
            startActivityForResult(intent, NEW_JOB_ACTIVITY_REQUEST_CODE);
        });

        // Listen to job list changes
        jobManager.addListener(this);
    }

    @Override
    public void onDestroyView() {
        jobManager.removeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_JOB_ACTIVITY_REQUEST_CODE &&
                resultCode == RESULT_OK &&
                data != null)
        {
            String pmkId = data.getStringExtra(NewJobActivity.EXTRA_PMKID);
            String ssid = data.getStringExtra(NewJobActivity.EXTRA_SSID);
            String apMac = data.getStringExtra(NewJobActivity.EXTRA_AP_MAC);
            String clMac = data.getStringExtra(NewJobActivity.EXTRA_CLIENT_MAC);

            if (pmkId != null && apMac != null && clMac != null) {
                jobViewModel.insert(new Job(
                        pmkId, ssid, apMac, clMac, WordList.getDefault(), null));
            }
            else
                Toast.makeText(getContext(), R.string.failed_to_start_job, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onNewJob(Job2 j) {
    }

    @Override
    public void onItemClick(JobListAdapter2.JobViewHolder holder) {
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
    public boolean onItemLongClick(JobListAdapter2.JobViewHolder holder) {
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
