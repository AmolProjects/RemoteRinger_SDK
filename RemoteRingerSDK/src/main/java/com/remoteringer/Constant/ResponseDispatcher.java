package com.remoteringer.Constant;

import com.remoteringer.callbacks.RingerCallbacks;

import java.util.HashMap;
import java.util.Map;

public class ResponseDispatcher {
    private final Map<String, String> liveData = new HashMap<>();
    private RingerCallbacks.RingerDeviceCallback callback;

    public void registerCallback(RingerCallbacks.RingerDeviceCallback callback) {
        this.callback = callback;
    }

    public void unregisterCallback() {
        this.callback = null;
    }

    public void handleResponse(byte responseId, String responseValue) {
        String key = mapResponseIdToKey(responseId);
        if (key == null) return;

        liveData.put(key, responseValue);

        if (callback != null) {
            Map<String, String> update = new HashMap<>();
            update.put(key, responseValue);

            callback.onDeviceDataReceived(update);
        }
    }

    private String mapResponseIdToKey(byte id) {
        switch (id) {
            case 0x1E:
                return "Volume Level";
            case 0x1C:
                return "Current Tone";
            case 0x0E:
                return "Current State";
            // Add more as needed
            default:
                return null;
        }
    }

    public Map<String, String> getAllLiveData() {
        return new HashMap<>(liveData); // Return a copy
    }
}
