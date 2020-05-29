package com.talybin.aircat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApListAdapter extends RecyclerView.Adapter<ApListAdapter.ApViewHolder> {

    interface ClickListener {
        void onClick(ApInfo apInfo);
    }

    private List<ApInfo> accessPoints;
    private ClickListener clickListener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ApViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener
    {
        private ClickListener clickListener;

        // Each data item is just a string in this case
        private ImageView signal;
        private TextView ssid;
        private TextView info;

        private ApInfo apInfo;

        ApViewHolder(View itemView, ClickListener clickListener) {
            super(itemView);

            this.clickListener = clickListener;

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            signal = (ImageView)itemView.findViewById(R.id.ap_item_signal);
            ssid = (TextView)itemView.findViewById(R.id.ap_item_ssid);
            info = (TextView)itemView.findViewById(R.id.ap_item_info);
        }

        void bindData(ApInfo rec) {
            // Make unique name for transition
            ViewCompat.setTransitionName(this.itemView, rec.hidden ? rec.bssid : rec.ssid);

            apInfo = rec;

            ssid.setText(rec.getSSID());

            // Sort capabilities before set
            List<String> caps = new ArrayList<>(rec.caps);
            Collections.sort(caps);
            info.setText(String.join(" | ", caps));

            if (rec.level > -60)
                signal.setImageResource(R.drawable.ic_signal_wifi_4_bar_lock);
            else if (rec.level > -70)
                signal.setImageResource(R.drawable.ic_signal_wifi_3_bar_lock);
            else if (rec.level > -80)
                signal.setImageResource(R.drawable.ic_signal_wifi_2_bar_lock);
            else
                signal.setImageResource(R.drawable.ic_signal_wifi_1_bar_lock);
        }

        @Override
        public void onClick(View v) {
            clickListener.onClick(apInfo);
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }

    }

    // Provide a suitable constructor (depends on the kind of dataset)
    ApListAdapter(List<ApInfo> dataSet, ClickListener clickListener) {
        this.accessPoints = dataSet;
        this.clickListener = clickListener;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ApListAdapter.ApViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ApViewHolder(view, clickListener);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ApViewHolder holder, int position) {
        // Get element from your dataset at this position.
        // Replace the contents of the view with that element
        holder.bindData(accessPoints.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return accessPoints.size();
    }

    @Override
    public int getItemViewType(int pos) {
        return R.layout.ap_list_item;
    }
}
