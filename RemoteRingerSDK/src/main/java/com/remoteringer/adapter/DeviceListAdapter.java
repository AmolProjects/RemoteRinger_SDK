package com.remoteringer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.remoteringer.R;
import com.remoteringer.callbacks.RingerCallbacks;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends ArrayAdapter<RingerCallbacks.NearbyDevice> {

    private List<RingerCallbacks.NearbyDevice> devices;

    public DeviceListAdapter(Context context) {
        super(context, R.layout.device_list_item);
        this.devices = new ArrayList<>();
    }

    public void setDevices(List<RingerCallbacks.NearbyDevice> devices) {
        this.devices.clear();
        this.devices.addAll(devices);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public RingerCallbacks.NearbyDevice getItem(int position) {
        return devices.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_list_item, parent, false);
        }

        RingerCallbacks.NearbyDevice device = getItem(position);

        TextView deviceNameText = convertView.findViewById(R.id.deviceName);
        TextView deviceMacText = convertView.findViewById(R.id.deviceMac);

        if (device != null) {
            deviceNameText.setText(device.getName());
            deviceMacText.setText(device.getAddress());
        }

        return convertView;
    }


}
