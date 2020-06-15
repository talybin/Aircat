package com.talybin.aircat;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class JobDetailsFragment extends Fragment implements Job.Listener {

    private Context context;

    private JobManager jobManager = null;
    private Job job = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_job_details, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = requireContext();
        jobManager = JobManager.getInstance();

        // Retrieve the job
        Bundle args = getArguments();
        if (args != null)
            job = jobManager.getJobs().get(args.getInt("job_position"));
        if (job == null) {  // Should never happen
            Log.e(this.getClass().getName(), "Missing job argument");
            goBack();
            return;
        }

        // Set fragment title
        //Objects.requireNonNull(((MainActivity) context).getSupportActionBar()).setTitle(job.getSSID());

        JobListAdapter.JobViewHolder viewHolder = new JobListAdapter.JobViewHolder(view);
        viewHolder.bindData(job);

        // Fill fields
        ((TextView)view.findViewById(R.id.job_details_mac_ap)).setText(job.getApMac());
        ((TextView)view.findViewById(R.id.job_details_mac_client)).setText(job.getClientMac());
        ((TextView)view.findViewById(R.id.job_details_pmkid)).setText(job.getHash());
        ((TextView)view.findViewById(R.id.job_details_wordlist)).setText(job.getWordListName());

        // Start button
        Button startButton = (Button)view.findViewById(R.id.job_details_but_start);
        startButton.setOnClickListener(v -> { startJob(); });

        job.addListener(this);
    }

    @Override
    public void onDestroyView() {
        job.removeListener(this);
        job = null;
        context = null;

        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.job_detail_menu, menu);
        menu.findItem(R.id.action_select_all).setVisible(false);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (job != null) {
            boolean notRunning = job.getState() == Job.State.NOT_RUNNING;
            menu.findItem(R.id.action_pause).setVisible(!notRunning);
            menu.findItem(R.id.action_start).setVisible(notRunning);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start:
                startJob();
                return true;
            case R.id.action_pause:
                pauseJob();
                return true;
            case R.id.action_remove:
                removeJob();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goBack() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void startJob() {
        if (!job.start(context))
            Toast.makeText(context, R.string.failed_to_start_job, Toast.LENGTH_LONG).show();
    }

    private void pauseJob() {
    }

    private void removeJob() {
        jobManager.remove(job);
        goBack();
    }

    @Override
    public void onJobStateChange(Job job) {
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onHashCatProgressChange(Job job, HashCat.Progress progress) {

    }
}
