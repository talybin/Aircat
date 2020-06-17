package com.talybin.aircat;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Set;

public class JobListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Listener {
        void onItemClick(JobViewHolder holder);
        boolean onItemLongClick(JobViewHolder holder);
    }

    private JobManager jobManager;
    private Listener listener;

    private Set<Integer> selectedItems = new HashSet<>();

    static class EmptyViewHolder extends RecyclerView.ViewHolder {

        EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class JobViewHolder extends RecyclerView.ViewHolder
            implements Job.Listener, View.OnAttachStateChangeListener
    {
        private TextView ssid;
        private TextView password;
        private TextView state;
        private TextView complete;
        private TextView speed;
        private TextView estTime;
        private ProgressBar progressBar;

        private Job job;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);

            ssid = itemView.findViewById(R.id.job_item_ssid);
            password = itemView.findViewById(R.id.job_item_password);
            state = itemView.findViewById(R.id.job_item_status);
            complete = itemView.findViewById(R.id.job_item_complete);
            progressBar = itemView.findViewById(R.id.job_item_progress_bar);
            speed = itemView.findViewById(R.id.job_item_speed);
            estTime = itemView.findViewById(R.id.job_item_est_time);

            job = null;
            itemView.addOnAttachStateChangeListener(this);
        }

        void bindData(Job job) {
            if (this.job != null)
                this.job.removeListener(this);
            this.job = job;
            this.job.addListener(this);

            ssid.setText(job.getSSID());

            onJobStateChange(job);
            onHashCatProgressChange(job, job.getProgress());
        }

        @Override
        public void onJobStateChange(Job job) {
            state.setText(job.getStateAsStr(itemView.getContext()));
        }

        @Override
        public void onHashCatProgressChange(Job job, HashCat.Progress progress) {
            Context context = itemView.getContext();

            float percentComplete = 0;
            long estimated = -1;

            if (progress != null) {
                speed.setText(context.getString(R.string.cracking_speed, progress.speed));
                if (progress.total > 0) {
                    percentComplete = progress.nr_complete * 100.f / progress.total;
                    //if (progress.speed > 0)
                        //estimated = (progress.total - progress.nr_complete) / progress.speed;
                    estimated = progress.remainSecs;
                }
            }
            else {
                speed.setText(context.getString(R.string.cracking_speed, 0));
            }
            complete.setText(context.getString(R.string.complete_percent, percentComplete));
            progressBar.setProgress(Math.round(percentComplete), true);

            estTime.setText(context.getString(R.string.estimated_time, estimated >= 0 ?
                    DateUtils.formatElapsedTime(estimated) : context.getString(android.R.string.unknownName)));

            String pw = job.getPassword();
            if (pw != null) {
                password.setText(pw);
                password.setVisibility(View.VISIBLE);
            }
            else
                password.setVisibility(View.GONE);
        }

        @Override
        public void onViewAttachedToWindow(View v) {}

        @Override
        public void onViewDetachedFromWindow(View v) {
            if (job != null)
                job.removeListener(this);
            v.removeOnAttachStateChangeListener(this);
        }
    }

    public JobListAdapter(Listener listener) {
        super();

        this.jobManager = JobManager.getInstance();
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        if (viewType == R.layout.job_list_empty)
            return new EmptyViewHolder(view);
        else {
            JobViewHolder holder = new JobViewHolder(view);
            view.setOnClickListener(v -> listener.onItemClick(holder));
            view.setOnLongClickListener(v -> listener.onItemLongClick(holder));
            return holder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == R.layout.job_list_item) {
            JobViewHolder jobHolder = (JobViewHolder)holder;
            jobHolder.bindData(jobManager.getJobs().get(position));
            // What magic is this?
            jobHolder.itemView.setSelected(selectedItems.contains(position));
        }
    }

    @Override
    public int getItemCount() {
        // If no jobs present use empty view
        return Math.max(1, jobManager.getJobs().size());
    }

    @Override
    public int getItemViewType(int pos) {
        return jobManager.getJobs().size() > 0 ?
                R.layout.job_list_item : R.layout.job_list_empty;
    }

    public void toggleSelection(int pos) {
        if (selectedItems.contains(pos))
            selectedItems.remove(pos);
        else
            selectedItems.add(pos);
        notifyItemChanged(pos);
    }

    public void selectRange(int startPos, int count) {
        for (; count-- > 0; ++startPos)
            selectedItems.add(startPos);
        notifyDataSetChanged();
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public Set<Integer> getSelectedItems() {
        return selectedItems;
    }
}
