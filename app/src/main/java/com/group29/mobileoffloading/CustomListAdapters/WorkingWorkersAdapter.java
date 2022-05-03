package com.group29.mobileoffloading.CustomListAdapters;

import static java.lang.String.format;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group29.mobileoffloading.DataModels.Worker;
import com.group29.mobileoffloading.R;

import java.util.List;

public class WorkingWorkersAdapter extends RecyclerView.Adapter<WorkingWorkersAdapter.ViewHolder> {
    private final Context context;
    private final List<Worker> workingWorkers;

    public WorkingWorkersAdapter(Context context, List<Worker> workingWorkers) {
        this.context = context;
        this.workingWorkers = workingWorkers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View list_item = layoutInflater.inflate(R.layout.worker_list_item, parent, false);

        return new ViewHolder(list_item);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.worked_id_tv.setText(String.format("%s %s", workingWorkers.get(position).getEndpointId(), workingWorkers.get(position).getEndpointName()));
        holder.worker_state_tv.setText(workingWorkers.get(position).getWorkStatus().getStatusInfo());
        
        holder.battery_percentage_tv.setText(workingWorkers.get(position).getDeviceStats().getBatteryPercentage());
        holder.is_charging_tv.setText(String.valueOf(workingWorkers.get(position).getDeviceStats().isCharging()));
        
        holder.work_done_tv.setText(String.valueOf(workingWorkers.get(position).getWorkAmount()));
        holder.distance_tv.setText(String.format("%s meter", format("%.2f",
                workingWorkers.get(position).getDistanceFromMaster())));
    }

    @Override
    public int getItemCount() {
        return workingWorkers.size();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView worked_id_tv;
        final TextView worker_state_tv;
        final TextView battery_percentage_tv;
        final TextView is_charging_tv;
        final TextView work_done_tv;
        final TextView distance_tv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            worked_id_tv = itemView.findViewById(R.id.working_worker_id_tv);
            worker_state_tv = itemView.findViewById(R.id.working_worker_state_tv);
            battery_percentage_tv = itemView.findViewById(R.id.working_worker_battery_level_tv);
            is_charging_tv = itemView.findViewById(R.id.working_worker_charging_state_tv);
            work_done_tv = itemView.findViewById(R.id.working_worker_work_done_tv);
            distance_tv = itemView.findViewById(R.id.working_worker_location_tv);
        }
    }
}
