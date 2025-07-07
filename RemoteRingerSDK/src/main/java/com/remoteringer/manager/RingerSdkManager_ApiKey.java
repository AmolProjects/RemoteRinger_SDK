package com.remoteringer.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RingerSdkManager_ApiKey {
    private static final String TAG = "RingerSdkManager_ApiKey";
    private static final String PREFS_NAME = "DoorbellRingerSDK_Prefs";
    private static final String LAST_CONNECTED_DEVICES = "last_connected_ringer_ids";
    private static final String ACTIVE_DEVICE_KEY = "active_device";
    private static final String API_KEY_PREF = "api_key"; // Store the API Key
    private static final String VALID_API_KEY = "1234ABCD877"; // Simulated API Key validation
    private static RingerSdkManager_ApiKey instance;
    private final Context context;
    private Set<String> connectedDevices = new HashSet<>();
    private String activeDevice;

    private RingerSdkManager_ApiKey(Context context, String apiKey) {
        this.context = context;

        // ✅ Validate API Key before proceeding
        if (!validateApiKey(apiKey)) {
            throw new IllegalStateException("Invalid API Key. Please provide a valid key.");
        }

        saveApiKey(apiKey);
        loadSessionData(); // ✅ Load previous session data
    }

    /**
     * ✅ Initialize SDK with API Key
     */
    public static synchronized RingerSdkManager_ApiKey initialize(Context context, String apiKey) {
        if (instance == null) {
            instance = new RingerSdkManager_ApiKey(context, apiKey);
        }
        return instance;
    }

    /**
     * ✅ Get SDK Instance
     */
    public static synchronized RingerSdkManager_ApiKey getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SDK not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * ✅ Validate API Key
     */
    private boolean validateApiKey(String apiKey) {
        return VALID_API_KEY.equals(apiKey); // Can be replaced with a server call
    }

    /**
     * ✅ Save API Key in SharedPreferences
     */
    private void saveApiKey(String apiKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(API_KEY_PREF, apiKey).apply();
    }

    /**
     * ✅ Retrieve API Key
     */
    public String getSavedApiKey() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(API_KEY_PREF, null);
    }

    // ========================== DEVICE MANAGEMENT ========================== //

    public void setActiveDevices(String deviceId1, String deviceId2) {
        if (deviceId1 != null && !deviceId1.isEmpty()) connectedDevices.add(deviceId1);
        if (deviceId2 != null && !deviceId2.isEmpty()) connectedDevices.add(deviceId2);
        saveSessionData();
    }

    public List<String> getActiveDevices() {
        return new ArrayList<>(connectedDevices);
    }

    /**
     * ✅ Get Active Device
     */
    public String getActiveDevice() {
        return activeDevice;
    }

    /**
     * ✅ Set Active Device
     */
    public void setActiveDevice(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return;
        this.activeDevice = deviceId;
        connectedDevices.add(deviceId);
        saveSessionData();
    }

    /**
     * ✅ Get List of All Connected Devices
     */
    public List<String> getAllConnectedDevices() {
        return new ArrayList<>(connectedDevices); // ✅ Convert Set to List for safe usage
    }

    /**
     * ✅ Add a new connected device
     */
    public void addConnectedDevice(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return;
        connectedDevices.add(deviceId);
        saveSessionData();
    }

    /**
     * ✅ Remove a Device (Disconnect)
     */
    public void removeConnectedDevice(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return;
        connectedDevices.remove(deviceId);

        if (deviceId.equals(activeDevice)) {
            activeDevice = null;
        }
//        saveSessionData();
    }

    // ========================== PERSISTENT STORAGE ========================== //

    /**
     * ✅ Save Device Data Persistently
     */
    private void saveSessionData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // ✅ Workaround to properly store Set<String> as List<String>
        editor.putString(ACTIVE_DEVICE_KEY, activeDevice);
        editor.putString(LAST_CONNECTED_DEVICES, String.join(",", connectedDevices));
        editor.apply();
    }

    /**
     * ✅ clear the session data
     */
    public void clearSessionDataOnDisconnect() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Remove specific keys
        editor.remove(ACTIVE_DEVICE_KEY);
        editor.remove(LAST_CONNECTED_DEVICES);

        // Apply changes
        editor.apply();
    }


    /**
     * ✅ Load Data Persistently
     */
    private void loadSessionData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        activeDevice = prefs.getString(ACTIVE_DEVICE_KEY, null);
        String devicesString = prefs.getString(LAST_CONNECTED_DEVICES, "");

        // ✅ Convert stored String back to Set<String>
        if (!devicesString.isEmpty()) {
            String[] devicesArray = devicesString.split(",");
            connectedDevices = new HashSet<>(List.of(devicesArray));
        } else {
            connectedDevices = new HashSet<>();
        }

        Log.d(TAG, "Loaded session data. Active device: " + activeDevice + ", Connected devices: " + connectedDevices);
    }
}
