package com.talybin.aircat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class JobListAdapter extends RecyclerView.Adapter<JobListAdapter.JobViewHolder> {

    interface ClickListener {
        void onClick(Job job);
    }

    private JobManager jobManager;
    private ClickListener clickListener;

    static class JobViewHolder
            extends RecyclerView.ViewHolder
    {
        private TextView ssid;
        private TextView pmkId;
        private TextView status;
        private TextView complete;
        private TextView macAp;
        private TextView macClient;

        private Job job;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);

            ssid = (TextView)itemView.findViewById(R.id.job_item_ssid);
            pmkId = (TextView)itemView.findViewById(R.id.job_item_pmkid);
            status = (TextView)itemView.findViewById(R.id.job_item_status);
            complete = (TextView)itemView.findViewById(R.id.job_item_complete);
            macAp = (TextView)itemView.findViewById(R.id.job_item_mac_ap);
            macClient = (TextView)itemView.findViewById(R.id.job_item_mac_client);

            job = null;
        }

        void bindData(Job job) {
            Context context = this.itemView.getContext();
            this.job = job;

            ssid.setText(job.ssid);
            pmkId.setText(job.pmkId);
            //status.setText("Status: Not running");
            complete.setText(context.getString(R.string.complete_percent, 0));
            macAp.setText(job.apMac);
            macClient.setText(job.clientMac);
        }

        public Job getJob() {
            return job;
        }
    }

    public JobListAdapter(ClickListener clickListener) {
        super();

        this.jobManager = JobManager.getInstance();
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        final JobViewHolder holder = new JobViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onClick(holder.getJob());
            }
        });

        return holder;
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
