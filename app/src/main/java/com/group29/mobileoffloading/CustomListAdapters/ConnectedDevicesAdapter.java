package com.group29.mobileoffloading.CustomListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group29.mobileoffloading.R;
import com.group29.mobileoffloading.DataModels.ConnectedDevice;
import com.group29.mobileoffloading.utilities.Constants;

import java.util.List;

public class ConnectedDevicesAdapter extends RecyclerView.Adapter<ConnectedDevicesAdapter.ViewHolder>{

    private Context context;
    private List<ConnectedDevice> connectedDevices;

    public ConnectedDevicesAdapter(@NonNull Context context, List<ConnectedDevice> connectedDevices) {
        this.context = context;
        this.connectedDevices = connectedDevices;
    }

    @NonNull
    @Override
    public ConnectedDevicesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = layoutInflater.inflate(R.layout.connected_device_item, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ConnectedDevicesAdapter.ViewHolder holder, int position) {
        holder.setClientId(connectedDevices.get(position).getEndpointId(), connectedDevices.get(position).getEndpointName());
        holder.setBatteryLevel(connectedDevices.get(position).getDeviceStats().getBatteryLevel());
        holder.setConnectionRequestState(connectedDevices.get(position).getRequestStatus());
    }

    @Override
    public int getItemCount() {
        return connectedDevices.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        private TextView ClientId;
        private TextView BatteryLevel;
        private TextView connectionRequestState;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ClientId = itemView.findViewById(R.id.client_id);
            BatteryLevel = itemView.findViewById(R.id.battery_level);
            connectionRequestState = itemView.findViewById(R.id.connection_request_state);
        }

        public void setClientId(String endpointId, String endpointName) {
            this.ClientId.setText(endpointName.toUpperCase()  + "\n(" + endpointId.toUpperCase() + ")");
        }

        public void setBatteryLevel(int batteryLevel) {
            if (batteryLevel > 0 && batteryLevel <= 100) {
                this.BatteryLevel.setText(batteryLevel + "%");
            } else {
                this.BatteryLevel.setText("");
            }
        }

        public void setConnectionRequestState(String requestStatus) {
            if (requestStatus.equals(Constants.RequestStatus.ACCEPTED)) {
                this.connectionRequestState.setText("Accepted");
            } else if (requestStatus.equals(Constants.RequestStatus.REJECTED)) {
                this.connectionRequestState.setText("Rejected");
            } else {
                this.connectionRequestState.setText("attempting connection");
            }
        }
    }
}
