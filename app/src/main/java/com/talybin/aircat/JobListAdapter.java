package com.talybin.aircat;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class JobListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface ClickListener {
        void onClick(Job job);
    }

    private JobManager jobManager;
    private ClickListener clickListener;

    static class EmptyViewHolder extends RecyclerView.ViewHolder {

        EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class JobViewHolder
            extends RecyclerView.ViewHolder implements Job.Listener, View.OnAttachStateChangeListener
    {
        private TextView ssid;
        private TextView pmkId;
        private TextView status;
        private TextView complete;
        private TextView macAp;
        private TextView macClient;
        private ProgressBar progressBar;

        private Job job;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);

            ssid = (TextView)itemView.findViewById(R.id.job_item_ssid);
            pmkId = (TextView)itemView.findViewById(R.id.job_item_pmkid);
            status = (TextView)itemView.findViewById(R.id.job_item_status);
            complete = (TextView)itemView.findViewById(R.id.job_item_complete);
            macAp = (TextView)itemView.findViewById(R.id.job_item_mac_ap);
            macClient = (TextView)itemView.findViewById(R.id.job_item_mac_client);
            progressBar = (ProgressBar)itemView.findViewById(R.id.job_item_progress_bar);

            job = null;
            itemView.addOnAttachStateChangeListener(this);
        }

        void bindData(Job job) {
            if (this.job != null)
                this.job.removeListener(this);
            job.addListener(this);

            Context context = itemView.getContext();
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

        @Override
        public void onStateChange() {
            status.setText(job.getStateAsStr(itemView.getContext()));
        }

        @Override
        public void onViewAttachedToWindow(View v) {
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            if (job != null)
                job.removeListener(this);
            v.removeOnAttachStateChangeListener(this);
        }
    }

    public JobListAdapter(ClickListener clickListener) {
        super();

        this.jobManager = JobManager.getInstance();
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        if (viewType == R.layout.job_list_empty)
            return new EmptyViewHolder(view);
        else {
            final JobViewHolder holder = new JobViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onClick(holder.getJob());
                }
            });
            return holder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof JobViewHolder)
            ((JobViewHolder)holder).bindData(jobManager.getJobs().get(position));
    }

    @Override
    public int getItemCount() {
        return Math.max(1, jobManager.getJobs().size());
    }

    @Override
    public int getItemViewType(int pos) {
        return jobManager.getJobs().size() > 0 ?
                R.layout.job_list_item : R.layout.job_list_empty;
    }
}
