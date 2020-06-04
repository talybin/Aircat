package com.talybin.aircat;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;


public class JobDetailsFragment extends Fragment {

    private Context context;

    private JobManager jobManager;
    private Job job;

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
            job = jobManager.get(args.getString("job_hash"));
        if (job == null) {  // Should never happen
            Log.e(this.getClass().getName(), "Missing job argument");
            goBack();
            return;
        }

        // Set fragment title
        Objects.requireNonNull(((MainActivity) context).getSupportActionBar()).setTitle(job.ssid);

        // Fill fields
        ((TextView)view.findViewById(R.id.job_details_mac_ap)).setText(job.apMac);
        ((TextView)view.findViewById(R.id.job_details_mac_client)).setText(job.clientMac);
        ((TextView)view.findViewById(R.id.job_details_pmkid)).setText(job.pmkId);
        ((TextView)view.findViewById(R.id.job_details_wordlist)).setText(job.getWordlistFile());

        // Start button
        Button startButton = (Button)view.findViewById(R.id.job_details_but_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startJob();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void goBack() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void startJob() {
        if (!jobManager.start(job))
            Toast.makeText(context, R.string.failed_to_start_job, Toast.LENGTH_LONG).show();
    }

}
