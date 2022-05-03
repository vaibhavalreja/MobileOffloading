package com.group29.mobileoffloading.CustomListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group29.mobileoffloading.R;
import com.group29.mobileoffloading.DataModels.AvailableWorker;
import com.group29.mobileoffloading.utilities.Constants;

import java.util.ArrayList;

public class AvailableWorkersAdapter extends RecyclerView.Adapter<AvailableWorkersAdapter.ViewHolder>{

    private final Context context;
    private final ArrayList<AvailableWorker> availableWorkers;

    public AvailableWorkersAdapter(@NonNull Context context, ArrayList<AvailableWorker> availableWorkers) {
        this.context = context;
        this.availableWorkers = availableWorkers;
    }

    @NonNull
    @Override
    public AvailableWorkersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View list_item = layoutInflater.inflate(R.layout.available_worker_list_item, parent, false);

        return new ViewHolder(list_item);
    }

    @Override
    public void onBindViewHolder(@NonNull AvailableWorkersAdapter.ViewHolder holder, int position) {
        holder.available_worker_id_tv.setText(String.format("%s %s", availableWorkers.get(position).getEndpointId(), availableWorkers.get(position).getEndpointName()));
        holder.battery_percentage_tv.setText(availableWorkers.get(position).getDeviceStats().getBatteryPercentage());
        holder.worker_connection_state_tv.setText(availableWorkers.get(position).getRequestStatus());
    }

    @Override
    public int getItemCount() {
        return availableWorkers.size();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView available_worker_id_tv;
        final TextView battery_percentage_tv;
        final TextView worker_connection_state_tv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            available_worker_id_tv = itemView.findViewById(R.id.client_id);
            battery_percentage_tv = itemView.findViewById(R.id.battery_level);
            worker_connection_state_tv = itemView.findViewById(R.id.connection_request_state);
        }
    }
}
