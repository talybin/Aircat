package com.talybin.aircat;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JobListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Listener {
        void onItemClick(JobViewHolder holder);
        boolean onItemLongClick(JobViewHolder holder);
    }

    static class JobViewHolder
            extends RecyclerView.ViewHolder
            implements Job.StateListener
    {
        private TextView ssid;
        private TextView password;
        private TextView state;
        private TextView complete;
        private TextView speed;
        private TextView estTime;
        private ProgressBar progressBar;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);

            ssid = itemView.findViewById(R.id.job_item_ssid);
            password = itemView.findViewById(R.id.job_item_password);
            state = itemView.findViewById(R.id.job_item_status);
            complete = itemView.findViewById(R.id.job_item_complete);
            progressBar = itemView.findViewById(R.id.job_item_progress_bar);
            speed = itemView.findViewById(R.id.job_item_speed);
            estTime = itemView.findViewById(R.id.job_item_est_time);
        }

        void bindData(Job job) {
            ssid.setText(job.getSsid());

            job.setStateListener(this);
            job.setProgressListener((progress, ex) -> {
                if (ex != null)
                    Toast.makeText(itemView.getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                else
                    onProgress(job);
            });

            onStateChange(job);
            onProgress(job);
        }

        @Override
        public void onStateChange(Job job) {
            state.setText(job.getState().toString());
        }

        private void onProgress(Job job) {
            HashCat.Progress progress = job.getProgress();
            Context context = itemView.getContext();

            float percentComplete = 0;
            long estimated = -1;

            // While it is running
            if (progress != null && progress.state == 3) {
                speed.setText(context.getString(R.string.cracking_speed, progress.speed));
                if (progress.total > 0) {
                    percentComplete = progress.nr_complete * 100.f / progress.total;
                    if (progress.speed > 0)
                        estimated = (progress.total - progress.nr_complete) / progress.speed;
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
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {

        EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private Listener listener;

    private List<Job> jobs; // Cached copy of jobs
    private Set<Integer> selectedItems = new HashSet<>();

    JobListAdapter(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
            jobHolder.bindData(jobs.get(position));
            // What magic is this?
            jobHolder.itemView.setSelected(selectedItems.contains(position));
        }

    }

    @Override
    public int getItemCount() {
        if (jobs != null && jobs.size() > 0)
            return jobs.size();
        // If no jobs present use empty view
        return 1;
    }

    @Override
    public int getItemViewType(int pos) {
        return jobs != null && jobs.size() > 0 ?
                R.layout.job_list_item : R.layout.job_list_empty;
    }

    void setJobs(List<Job> jobs) {
        this.jobs = jobs;
        clearSelections();
        notifyDataSetChanged();
    }

    List<Job> getJobs() {
        return this.jobs;
    }

    void toggleSelection(int pos) {
        if (selectedItems.contains(pos))
            selectedItems.remove(pos);
        else
            selectedItems.add(pos);
        notifyItemChanged(pos);
    }

    void selectRange(int startPos, int count) {
        for (; count-- > 0; ++startPos)
            selectedItems.add(startPos);
        notifyDataSetChanged();
    }

    void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    int getSelectedItemCount() {
        return selectedItems.size();
    }

    Set<Integer> getSelectedItems() {
        return selectedItems;
    }
}
