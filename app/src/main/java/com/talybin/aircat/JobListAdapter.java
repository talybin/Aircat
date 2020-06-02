package com.talybin.aircat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class JobListAdapter extends RecyclerView.Adapter<JobListAdapter.JobViewHolder> {

    private JobManager jobManager;

    static class JobViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener
    {
        TextView ssid;
        TextView pmkId;
        TextView status;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);

            ssid = (TextView)itemView.findViewById(R.id.job_item_ssid);
            pmkId = (TextView)itemView.findViewById(R.id.job_item_pmkid);
            status = (TextView)itemView.findViewById(R.id.job_item_status);
        }

        void bindData(Job job) {
            ssid.setText(job.ssid);
            pmkId.setText("PMKID: " + job.pmkId);
            status.setText("Status: Not running");
        }

        @Override
        public void onClick(View v) {
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }

    public JobListAdapter() {
        super();
        jobManager = JobManager.getInstance();
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new JobListAdapter.JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        holder.bindData(jobManager.getJobs().get(position));
    }

    @Override
    public int getItemCount() {
        return jobManager.getJobs().size();
    }

    @Override
    public int getItemViewType(int pos) {
        return R.layout.job_list_item;
    }
}
