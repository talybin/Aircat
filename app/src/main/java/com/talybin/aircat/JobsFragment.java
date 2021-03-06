package com.talybin.aircat;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.stream.Collectors;

public class JobsFragment extends Fragment
        implements JobListAdapter.Listener, ActionMode.Callback
{
    private JobListAdapter adapter;
    private FloatingActionButton createJobBut;

    private NavController navController;

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

        // Job list
        RecyclerView jobList = view.findViewById(R.id.job_list);
        jobList.setLayoutManager(new LinearLayoutManager(ctx));
        jobList.setHasFixedSize(true);
        jobList.addItemDecoration(
                new DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL));

        // Specify an adapter
        // Navigate to job details on view click
        adapter = new JobListAdapter(this);
        jobList.setAdapter(adapter);
        adapter.setJobs(JobManager.getInstance().getAll());

        // Setup listeners
        JobManager.getInstance().setListener(adapter::setJobs);

        // Create new job button
        createJobBut = view.findViewById(R.id.createNewJobBut);

        createJobBut.setOnClickListener(v ->
                navController.navigate(R.id.action_JobsFragment_to_CreateJobFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onItemClick(JobListAdapter.JobViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (actionMode == null) {
            // Show details
            Job job = adapter.getJobs().get(position);
            if (job != null) {
                Bundle args = new Bundle();
                args.putString(JobDetailsFragment.KEY_JOB_ID, job.getPmkId());
                navController.navigate(R.id.action_JobsFragment_to_jobDetailsFragment, args);
            }
        }
        else // We are in multi-selection mode, do the same as onItemLongClick
            toggleSelection(position);
    }

    @Override
    public boolean onItemLongClick(JobListAdapter.JobViewHolder holder) {
        toggleSelection(holder.getAdapterPosition());
        return true;
    }

    private void updateActionMode() {
        if (actionMode != null) {
            actionMode.setTitle("" + adapter.getSelectedItemCount());
            actionMode.invalidate();
        }
    }

    private void toggleSelection(int idx) {
        adapter.toggleSelection(idx);

        if (adapter.getSelectedItemCount() == 0)
            actionMode.finish();
        else {
            if (actionMode == null)
                actionMode = ((AppCompatActivity)requireActivity()).startSupportActionMode(this);
            updateActionMode();
        }
    }

    private List<Job> getSelectedJobs() {
        List<Job> allJobs = adapter.getJobs();
        return adapter.getSelectedItems()
                .stream().map(allJobs::get).collect(Collectors.toList());
    }

    private void startSelectedJobs() {
        HashCat.getInstance().start(
                getSelectedJobs().stream()
                        .filter(job -> !job.isProcessing())
                        .collect(Collectors.toList()));
    }

    private void stopSelectedJobs() {
        HashCat.getInstance().stop(
                getSelectedJobs().stream()
                        .filter(Job::isProcessing)
                        .collect(Collectors.toList()));
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.job_menu, menu);
        // Add Settings menu item from main_menu
        inflater.inflate(R.menu.menu_main, menu);

        createJobBut.setVisibility(View.GONE);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        long runningCnt = getSelectedJobs().stream()
                .filter(Job::isProcessing).count();

        menu.findItem(R.id.action_start).setVisible(runningCnt < adapter.getSelectedItemCount());
        menu.findItem(R.id.action_stop).setVisible(runningCnt > 0);

        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // Double click my cause exception here
        if (actionMode == null)
            return false;

        switch (item.getItemId()) {

            case R.id.action_remove:
                removeJobs(getSelectedJobs());
                return true;

            case R.id.action_select_all:
                adapter.selectRange(0, adapter.getJobs().size());
                updateActionMode();
                return true;

            case R.id.action_start:
                startSelectedJobs();
                actionMode.finish();
                return true;

            case R.id.action_stop:
                stopSelectedJobs();
                actionMode.finish();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        createJobBut.setVisibility(View.VISIBLE);
        adapter.clearSelections();
        actionMode = null;
    }

    private void removeJobs(List<Job> jobs) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.remove_selected_jobs)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    JobManager.getInstance().remove(jobs);
                    actionMode.finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
