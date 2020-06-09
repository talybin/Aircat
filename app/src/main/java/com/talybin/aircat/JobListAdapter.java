package com.talybin.aircat;

import android.content.Context;
import android.text.format.DateUtils;
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

    static class JobViewHolder extends RecyclerView.ViewHolder
    {
        private TextView ssid;
        private TextView status;
        private TextView complete;
        private TextView speed;
        private TextView estTime;
        private ProgressBar progressBar;

        private Job job;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);

            ssid = (TextView)itemView.findViewById(R.id.job_item_ssid);
            status = (TextView)itemView.findViewById(R.id.job_item_status);
            complete = (TextView)itemView.findViewById(R.id.job_item_complete);
            progressBar = (ProgressBar)itemView.findViewById(R.id.job_item_progress_bar);
            speed = (TextView)itemView.findViewById(R.id.job_item_speed);
            estTime = (TextView)itemView.findViewById(R.id.job_item_est_time);

            job = null;
        }

        void bindData(Job job) {
            Context context = itemView.getContext();
            this.job = job;

            ssid.setText(job.ssid);
            status.setText(job.getStateAsStr(context));

            float percentComplete = 0;
            long estimated = 0;

            if (job.status != null && job.status.total > 0) {
                percentComplete = job.status.nr_complete * 100.f / job.status.total;

                speed.setText(context.getString(R.string.cracking_speed, job.status.speed));
                if (job.status.speed > 0)
                    estimated = (job.status.total - job.status.nr_complete) / job.status.speed;
            }
            else {
                speed.setText(context.getString(R.string.cracking_speed, 0));
            }
            complete.setText(context.getString(R.string.complete_percent, percentComplete));
            progressBar.setProgress(Math.round(percentComplete), true);

            estTime.setText(context.getString(R.string.estimated_time, estimated > 0 ?
                    DateUtils.formatElapsedTime(estimated) : context.getString(android.R.string.unknownName)));
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
