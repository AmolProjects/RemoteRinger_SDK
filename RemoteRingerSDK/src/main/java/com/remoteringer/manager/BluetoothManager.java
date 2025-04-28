package com.remoteringer.manager;

import static com.remoteringer.Constant.Constant_Variable.SVR_CHR_OTA_CONTROL_REQUEST;
import static com.remoteringer.Constant.Constant_Variable.serialNumber;
import static com.remoteringer.manager.DeviceSettingsManager.isOtaUpdatePending;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.remoteringer.callbacks.RingerCallbacks;
import com.remoteringer.handlers.BleResponseHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private final Activity activity;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private BleResponseHandler responseHandler;
    // SERVICE_UUID
    public static final UUID SERVICE_UUID = UUID.fromString("000012ab-0000-1000-8000-00805f9b34fb");
    // CHARACTERISTIC_UUID
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("00004001-0000-1000-8000-00805f9b34fb");
    // for Ota operation SERVICE_UUID
    public static final UUID SERVICE_UUID_Ota = UUID.fromString("d6f1d96d-594c-4c53-b1c6-244a1dfde6d8");
    // for ota operation 1 byte send CHARACTERISTIC_UUID
    public static final UUID CHARACTERISTIC_UUID_Ota_1_byte_Data = UUID.fromString("7AD671AA-21C0-46A4-B722-270E3AE3D830");
    // for ota operation 512 byte send CHARACTERISTIC_UUID
    public static final UUID CHARACTERISTIC_UUID_Ota_512_data = UUID.fromString("23408888-1F40-4CD8-9B89-CA8D45F8A5B0");
    private static final int SCAN_DURATION = 5000;
    private RingerSdkManager_ApiKey sdkManager_apiKey;
    String activeDevice;
    private DeviceSettingsManager deviceSettingsManager;
    private static BluetoothManager instance;

    /**
     * create single object of BluetoothManager
     */
    public static synchronized BluetoothManager getInstance(Activity activity) {
        if (instance == null) {
            instance = new BluetoothManager(activity);
        }
        return instance;
    }

    /**
     * create BluetoothManager of constructor
     */
    private BluetoothManager(Activity activity) {
        this.activity = activity;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        this.deviceSettingsManager = DeviceSettingsManager.getInstance(activity, this, activity);
        this.responseHandler = deviceSettingsManager.getBleResponseHandler(); // ✅ Use the one with callback

        try {
            this.sdkManager_apiKey = RingerSdkManager_ApiKey.getInstance(); // ✅ Fix added
        } catch (IllegalStateException e) {
            Log.e(TAG, "sdkManager_apiKey initialization failed: " + e.getMessage());
            this.sdkManager_apiKey = null;
        }
    }


    public void RemoteRinger_GetNearbyDevices(RingerCallbacks.NearbyDevicesCallback callback) {
        // Ensure location services are enabled
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            callback.onError("Location services not enabled.");
            return;
        }

        // Ensure location permission is granted
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            callback.onError("Location permission not granted.");
            return;
        }

        // Ensure Bluetooth is enabled
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            callback.onError("Bluetooth is disabled or not available.");
            return;
        }

        BluetoothLeScanner bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothScanner == null) {
            callback.onError("Bluetooth Scanner not available.");
            return;
        }

        final List<RingerCallbacks.NearbyDevice> foundDevices = new ArrayList<>();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                String deviceName = (device.getName() != null) ? device.getName() : "Unknown Device";
                Log.d(TAG, "Discovered Device: " + deviceName + " [" + device.getAddress() + "]");

                if (device.getName() != null && device.getName().contains("GRR")) {
                    RingerCallbacks.NearbyDevice nearbyDevice = new RingerCallbacks.NearbyDevice(device.getName(), device.getAddress());

                    // Add only if it's a new device
                    if (!foundDevices.contains(nearbyDevice)) {
                        foundDevices.add(nearbyDevice);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                callback.onError("BLE Scan failed: " + errorCode);
            }
        };

        // Start scanning
        bluetoothScanner.startScan(scanCallback);

        // Stop scanning after SCAN_DURATION milliseconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bluetoothScanner.stopScan(scanCallback);
            callback.onDevicesRetrieved(foundDevices); // Final callback after scan stops
        }, SCAN_DURATION);
    }

    /**
     * scan for RemoteRinger connectToDevice
     */
    public void connectToDevice(String deviceAddress, RingerCallbacks.RingerDeviceCallback callback) {

        if (deviceAddress == null || deviceAddress.isEmpty()) {
            callback.onError("Invalid Bluetooth Address:");
            Log.e(TAG, "Invalid Bluetooth Address: " + deviceAddress);
            return; // Prevent further execution
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Close previous GATT instance before reconnecting
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        bluetoothGatt = device.connectGatt(activity, true, new BluetoothGattCallback() {

            /**
             * to check bluetooth connected device or not
             */
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    //added for permission
                    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    Log.d(TAG, "Connected to " + device.getName());

                    // Store updated GATT instance
                    bluetoothGatt = gatt;

                    String ringerDeviceName = device.getName();
                    serialNumber = ringerDeviceName.replaceFirst("GRR_", "");
                    Log.d("Get Ringer SerialNumber :", serialNumber);
                    sdkManager_apiKey.setActiveDevice(device.getAddress());

                    // Request a higher MTU size of bluetooth controller 512
                    gatt.requestMtu(512);

                    //changes as per new document
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess("Connected to " + device.getName()));
                    activeDevice=device.getName();
                    deviceSettingsManager.RemoteRinger_Authentication(serialNumber, new RingerCallbacks.AuthenticationCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG,"Authentication Done..Move to next stage");

                            //added for ota
                            if (isOtaUpdatePending) {
                                Log.d(TAG, "🚀 OTA flag detected. Sending OTA control command...");
                                deviceSettingsManager.sendOtaControlCommand(SVR_CHR_OTA_CONTROL_REQUEST);
                                isOtaUpdatePending = false; // Reset after sending
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {

                        }
                    });


                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "Disconnected from " + device.getName());

                    // Remove device from active session
                    sdkManager_apiKey.removeConnectedDevice(device.getAddress());

                    // Close GATT connection if it's not already null
                    if (gatt != null) {
                        gatt.disconnect();
                        gatt.close();
                    }
                    bluetoothGatt = null;

                    // Retry Reconnection
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "Attempting Reconnection...");
                        connectToDevice(deviceAddress, callback);
                    }, 3000); // 3-second delay before retry

                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Disconnected from " + device.getName()));
                }
            }

            /**
             * to check bluetooth controller mtu size change or not
             */

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "MTU successfully changed to " + mtu);
                } else {
                    Log.e(TAG, "Failed to change MTU, status: " + status);
                }

                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                // After setting MTU, discover services
                gatt.discoverServices();
            }

            /**
             * using onServicesDiscovered bluetooth device discovered and enable the notification
             */

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "✅ Services Discovered. Enabling notifications...");

                    // Enable notifications for Router WiFi Connection
                    enableNotification(gatt, UUID.fromString("00004321-0000-1000-8000-00805f9b34fb"),
                            UUID.fromString("00003a2a-0000-1000-8000-00805f9b34fb"));

                    // Enable notifications for PROVISIONING ACTIVATION STATUS
                    enableNotification(gatt, UUID.fromString("00004321-0000-1000-8000-00805f9b34fb"),
                            UUID.fromString("00003a27-0000-1000-8000-00805f9b34fb"));

                    // Enable notifications for Onboarding Over BLE
                    enableNotification(gatt, UUID.fromString("00004321-0000-1000-8000-00805f9b34fb"),
                            UUID.fromString("00003a28-0000-1000-8000-00805f9b34fb"));

                    // HOST CONNECTION OVER BLE STATUS
                    enableNotification(gatt, UUID.fromString("00004321-0000-1000-8000-00805f9b34fb"),
                            UUID.fromString("00003a2b-0000-1000-8000-00805f9b34fb"));

                    // ✅ Enable notifications for APPLICATION COMMAND UUID (00004001)
                    enableNotification(gatt, UUID.fromString("000012ab-0000-1000-8000-00805f9b34fb"),
                            UUID.fromString("00004001-0000-1000-8000-00805f9b34fb"));

                    // ✅ Enable notifications for For Ota control 1 byte data UUID (00004001)
                    enableNotification(gatt, UUID.fromString(String.valueOf(SERVICE_UUID_Ota)),
                            UUID.fromString(String.valueOf(CHARACTERISTIC_UUID_Ota_1_byte_Data)));

                    // ✅ Enable notifications for For Ota control 512  data UUID (00004001)
                    enableNotification(gatt, UUID.fromString(String.valueOf(SERVICE_UUID_Ota)),
                            UUID.fromString(String.valueOf(CHARACTERISTIC_UUID_Ota_512_data)));




                } else {
                    Log.e(TAG, "⚠️ Service Discovery Failed, status: " + status);
                }
            }

            /**
             *  It is triggered when a BLE peripheral sends an update for a subscribed characteristic.
             */

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] data = characteristic.getValue();

                if (data == null || data.length == 0) {
                    Log.e(TAG, "⚠️ Received empty frame from " + characteristic.getUuid().toString());
                    return;
                }

                Log.d(TAG, "📥 Raw Data from " + characteristic.getUuid().toString() + ": " + Arrays.toString(data));

                // ✅ Handle Router WiFi Connection Status (1 byte)
                if (UUID.fromString("00003a2a-0000-1000-8000-00805f9b34fb").equals(characteristic.getUuid())) {
                    int state = data[0] & 0xFF;  // Convert to unsigned int
                    String stateDesc = getWiFiStateDescription(state);
                    Log.d(TAG, "📡 Router WiFi Connection Status Changed: " + stateDesc);
                }

                // ✅ Handle Onboarding Over BLE Status (1 byte)
                else if (UUID.fromString("00003a28-0000-1000-8000-00805f9b34fb").equals(characteristic.getUuid())) {
                    Log.d(TAG, "🚀 Onboarding Over BLE Status Changed: " + (data[0] & 0xFF));
                    int state = data[0] & 0xFF;  // Convert to unsigned int
                    String stateDesc = getOnBoardingBleStatus(state);
                    Log.d(TAG, "HOST CONNECTION OVER BLE STATUS Changed: " + stateDesc);
                }

                //HOST CONNECTION OVER BLE STATUS
                else if (UUID.fromString("00003a2b-0000-1000-8000-00805f9b34fb").equals(characteristic.getUuid())) {
                    int state = data[0] & 0xFF;  // Convert to unsigned int
                    String stateDesc = getHostConnectionDescription(state);
                    Log.d(TAG, "HOST CONNECTION OVER BLE STATUS Changed: " + stateDesc);
                }

                // ✅ Handle Application Command Responses
                else if (UUID.fromString("00004001-0000-1000-8000-00805f9b34fb").equals(characteristic.getUuid())) {
                    Log.d(TAG, "🛠 Application Command Response: " + Arrays.toString(data));
                }

                try {
                    responseHandler.handleCharacteristicChanged(gatt, characteristic);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }


            /**
             * Maps the state byte to a human-readable description.
             */
            private String getWiFiStateDescription(int state) {
                switch (state) {
                    case 0:
                        return "STATE IDLE";
                    case 1:
                        return "STATE INIT";
                    case 2:
                        return "STATE CONNECTING";
                    case 3:
                        return "STATE CONNECTED";
                    case 4:
                        return "STATE DISCONNECTED";
                    case 5:
                        return "STATE SSID NOT FOUND";
                    case 6:
                        return "STATE AUTH FAILED";
                    case 7:
                        return "STATE RETRY";
                    case 8:
                        return "STATE RECONNECT";
                    case 9:
                        return "STATE STOP";
                    case 10:
                        return "STATE ACTIVATION";
                    default:
                        return "UNKNOWN STATE (" + state + ")";
                }
            }

            private String getHostConnectionDescription(int state) {
                switch (state) {
                    case 0:
                        return "STATE IDLE";
                    case 1:
                        return "STATE CONNECTED";
                    case 2:
                        return "STATE DISCONNECTED";
                    default:
                        return "UNKNOWN STATE (" + state + ")";
                }
            }

            private String getOnBoardingBleStatus(int state) {
                switch (state) {
                    case 0:
                        return "ACTIVATION_IDLE";
                    case 1:
                        return "ACTIVATION_START";
                    case 2:
                        return "ACTIVATION_FAILED";
                    case 3:
                        return "ACTIVATION_TIMEOUT";
                    case 4:
                        return "ACTIVATION_STOP";
                    case 5:
                        return "ASSOCIATE_FAILED";
                    case 6:
                        return "ASSOCIATE_SUCCESS";
                    case 7:
                        return "ACTIVATION_SUCCESS";
                    default:
                        return "UNKNOWN STATE (" + state + ")";


                }
            }

        });
    }

    // Enable Notifications for a given UUID
    private void enableNotification(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID) {
        BluetoothGattService service = gatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (characteristic != null) {
                gatt.setCharacteristicNotification(characteristic, true);

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    // ✅ Delay descriptor write to avoid BLE write conflicts
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        gatt.writeDescriptor(descriptor);
                        Log.d(TAG, "✅ Notification Enabled for " + characteristicUUID);
                    }, 150); // 150ms delay
                } else {
                    Log.e(TAG, "⚠️ Descriptor not found for " + characteristicUUID);
                }
            } else {
                Log.e(TAG, "⚠️ Characteristic not found: " + characteristicUUID);
            }
        } else {
            Log.e(TAG, "⚠️ Service not found: " + serviceUUID);
        }
    }


    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    // :::::::::::::  RemoteRinger_Disconnect:::::::::::::::::


    public void RemoteRinger_DisconnectDevice(RingerCallbacks.DisconnectionCallback callback) {
        BluetoothGatt bluetoothGatt = getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("Already disconnected.");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID)));
        if (service == null) {
            callback.onError("Service not found. Already disconnected or not initialized.");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(String.valueOf(CHARACTERISTIC_UUID)));
        if (characteristic == null) {
            callback.onError("Characteristic not found.");
            return;
        }

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            callback.onError("Missing Bluetooth permission.");
            return;
        }

        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        this.bluetoothGatt = null;

        if (sdkManager_apiKey != null) {
            sdkManager_apiKey.removeConnectedDevice(activeDevice);
        } else {
            Log.e(TAG, "SdkManager_apiKey is null, cannot remove connected device.");
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            callback.onSuccess("Disconnected from: " + activeDevice);
        }, 300);
    }

    public void attemptAutoReconnect() {
        if (sdkManager_apiKey == null) return;

        List<String> lastConnectedDevices = sdkManager_apiKey.getActiveDevices();
        if (lastConnectedDevices.isEmpty()) {
            Log.d(TAG, "No previously connected devices found.");
            return;
        }

        for (String deviceAddress : lastConnectedDevices) {
            Log.d(TAG, "Attempting to reconnect to " + deviceAddress);
            connectToDevice(deviceAddress, new RingerCallbacks.RingerDeviceCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Reconnected successfully: " + message);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to reconnect: " + error);
                }
            });
        }
    }

}