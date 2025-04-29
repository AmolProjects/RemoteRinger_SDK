package com.remoteringer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.remoteringer.callbacks.RingerCallbacks;
import com.remoteringer.manager.BluetoothManager;
import com.remoteringer.manager.DeviceSettingsManager;
import com.remoteringer.manager.RingerSdkManager_ApiKey;

import java.util.List;

/**
 * The main SDK class for RemoteRinger functionality.
 * <p>
 * This class provides the primary interface for interacting with RemoteRinger devices,
 * including device discovery, connection management, and various device operations.
 * It acts as a facade for underlying managers that handle Bluetooth communication
 * and device settings management.
 * </p>
 */
public class RemoteRingerSDK {
    Activity activity;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothManager bluetoothManager;
    private DeviceSettingsManager deviceSettingsManager;
    private RingerSdkManager_ApiKey sdkManager_apiKey;
    private static final String TAG = "RemoteRingerSdk";

    /**
     * Constructs a new RemoteRingerSDK instance.
     *
     * @param activity The Android activity context required for Bluetooth operations
     * @param apiKey   The API key for SDK authentication and initialization
     * @throws IllegalStateException if SDK initialization fails
     */
    public RemoteRingerSDK(Activity activity, String apiKey) {
        this.activity = activity;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try {
            this.sdkManager_apiKey = RingerSdkManager_ApiKey.initialize(activity, apiKey);
            this.bluetoothManager = BluetoothManager.getInstance(activity);


            // ✅ Use singleton instead of creating a new instance
            this.deviceSettingsManager = DeviceSettingsManager.getInstance(activity, bluetoothManager, activity);
        } catch (IllegalStateException e) {
            Log.e(TAG, "SDK Initialization Failed: " + e.getMessage());
        }

        this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }


    /**
     * Scans for nearby RemoteRinger BLE devices.
     *
     * @param callback The callback to receive scanned devices or errors
     */
    public void RemoteRinger_GetNearbyDevices(RingerCallbacks.NearbyDevicesCallback callback) {
        bluetoothManager.RemoteRinger_GetNearbyDevices(callback);
    }

    /**
     * Connects to a specific RemoteRinger device.
     *
     * @param deviceAddress The deviceAddress of the device to connect to
     * @param callback      The callback for connection status updates
     */
    public void RemoteRinger_ConnectDevice(String deviceAddress, RingerCallbacks.RingerDeviceCallback callback) {
        bluetoothManager.connectToDevice(deviceAddress, callback);
    }

    /**
     * Authenticates with the connected RemoteRinger device.
     *
     * @param ringerSerialNo The serial number for authentication
     * @param callback       The callback for authentication result
     */
    public void RemoteRinger_Authentication(String ringerSerialNo, RingerCallbacks.AuthenticationCallback callback) {
        deviceSettingsManager.RemoteRinger_Authentication(ringerSerialNo, callback);
    }

    /**
     * Sets the system mode on the connected device.
     *
     * @param systemMode The system mode value to set (0-255)
     * @param callback   The callback for operation result
     */
    public void RemoteRinger_setSystemMode(int systemMode, RingerCallbacks.SystemModeCallback callback) {
        deviceSettingsManager.RemoteRinger_setSystemMode(systemMode, callback);
    }



    /**
     * Alternative method to set system mode (variant 1).
     *
     * @param systemMode The system mode value to set (0-255)
     * @param callback   The callback for operation result
     */
    public void RemoteRinger_setSystemMode1(int systemMode, RingerCallbacks.SystemModeCallback callback) {
        deviceSettingsManager.RemoteRinger_setSystemMode1(systemMode, callback);
    }
    public void RemoteRinger_deviceCommissioning( RingerCallbacks.SystemModeCallback callback) {
        deviceSettingsManager.RemoteRinger_deviceCommissioning(callback);
    }

    /**
     * Retrieves the current system mode from the connected device.
     *
     * @param callback The callback to receive the system mode
     */
    public void RemoteRinger_getSystemMode(RingerCallbacks.SystemModeCallback callback) {
        deviceSettingsManager.RemoteRinger_getSystemMode(callback);
    }

    /**
     * Sets the WiFi SSID on the connected device.
     *
     * @param wifiSSID The SSID to set
     * @param callback The callback for operation result
     */
    public void RemoteRinger_setWifiSsid(String wifiSSID, RingerCallbacks.WiFiSSIDCallback callback) {
        deviceSettingsManager.RemoteRinger_setWifiSsid(wifiSSID, callback);
    }

    /**
     * Sets the WiFi password on the connected device.
     *
     * @param wifiPass The password to set
     * @param callback The callback for operation result
     */
    public void RemoteRinger_setWifiPass(String wifiPass, RingerCallbacks.WifiPasswordcallback callback) {
        deviceSettingsManager.RemoteRinger_setWifiPass(wifiPass, callback);
    }

    /**
     * Activates WiFi on the connected device.
     *
     * @param wifiActivationMode The activation mode to set
     * @param callback           The callback for operation result
     */
    public void RemoteRinger_wifiActivation(int wifiActivationMode, RingerCallbacks.WifiActivationCallback callback) {
        deviceSettingsManager.RemoteRinger_wifiActivation(wifiActivationMode, callback);
    }

    /**
     * Sets the door lock ID on the connected device.
     *
     * @param doorLockId The door lock ID to set
     * @param callback   The callback for operation result
     */
    public void RemoteRinger_SetDoorLockId(String doorLockId, RingerCallbacks.DoorLockIDCallback callback) {
        deviceSettingsManager.RemoteRinger_SetDoorLockId(doorLockId, callback);
    }

    /**
     * Sets the device serial number.
     *
     * @param serialNumber The serial number to set
     * @param callback     The callback for operation result
     */
    public void RemoteRinger_setSerialNumber(String serialNumber, RingerCallbacks.SerialNumberCallback callback) {
        deviceSettingsManager.RemoteRinger_setSerialNumber(serialNumber, callback);
    }

    /**
     * Reboots the connected device.
     *
     * @param callback The callback for operation result
     */
    public void RemoteRinger_RebootDevice(RingerCallbacks.RebootCallback callback) {
        deviceSettingsManager.RemoteRinger_RebootDevice(callback);
    }

    /**
     * Performs a factory reset on the connected device.
     *
     * @param callback The callback for operation result
     */
    public void RemoteRinger_FactoryReset(RingerCallbacks.FactoryResetCallback callback) {
        deviceSettingsManager.RemoteRinger_FactoryReset(callback);
    }

    /**
     * Sets the application boot mode on the connected device.
     *
     * @param mode     The boot mode to set
     * @param callback The callback for operation result
     */
    public void RemoteRinger_setApplicationBootMode(int mode, RingerCallbacks.SetApplicationBootMode callback) {
        deviceSettingsManager.RemoteRinger_setApplicationBootMode(mode, callback);
    }

    /**
     * Sets the device model ID on the connected device.
     *
     * @param modelId  The model ID to set
     * @param callback The callback for operation result
     */
    public void RemoteRinger_setDeviceModelID(Integer modelId, RingerCallbacks.setDeviceModelID callback) {
        deviceSettingsManager.RemoteRinger_setDeviceModelID(modelId, callback);
    }

    /**
     * Sets the hardware version on the connected device.
     *
     * @param major    The major version number
     * @param minor    The minor version number
     * @param patch    The patch version number
     * @param callback The callback for operation result
     */
    public void RemoteRinger_setHardwareVersion(int major, int minor, int patch, RingerCallbacks.HardwareVersionCallback callback) {
        deviceSettingsManager.RemoteRinger_setHardwareVersion(major, minor, patch, callback);
    }

    /**
     * Retrieves the serial number from the connected device.
     *
     * @param callback The callback to receive the serial number
     */
    public void RemoteRinger_GetSerialNumber(RingerCallbacks.SerialNumberCallback callback) {
        deviceSettingsManager.RemoteRinger_getSerialNumber(callback);
    }

    /**
     * Sets the door lock BLE address on the connected device.
     *
     * @param doorLockBleAddress The BLE address to set
     * @param callback           The callback for operation result
     */
    public void RemoteRinger_setDoorLockBleAddress(String doorLockBleAddress, RingerCallbacks.DoorLockBleAddressCallback callback) {
        deviceSettingsManager.RemoteRinger_setDoorLockBleAddress(doorLockBleAddress, callback);
    }

    /**
     * Sets the onboarding activation mode on the connected device.
     *
     * @param onBoardingMode The activation mode to set
     * @param callback       The callback for operation result
     */
    public void RemoteRinger_setOnBoardingActivation(int onBoardingMode, int onBoardingType, RingerCallbacks.OnBoardingActivationCallback callback) {
        deviceSettingsManager.RemoteRinger_setOnBoardingActivation(onBoardingMode, onBoardingType, callback);
    }

    /**
     * Sets the door lock encryption key on the connected device.
     *
     * @param doorLockEncryptionKey The encryption key to set
     * @param callback              The callback for operation result
     */
    public void RemoteRinger_setDoorLockEncryptionKey(String doorLockEncryptionKey, RingerCallbacks.DoorLockEncryptionKey callback) {
        deviceSettingsManager.RemoteRinger_setDoorLockEncryptionKey(doorLockEncryptionKey, callback);
    }

    /**
     * Notifies the device of successful onboarding.
     *
     * @param onBoardingSucessMode The success mode to set
     * @param callback             The callback for operation result
     */
    public void RemoteRinger_OnBoardingSuccess(int onBoardingSucessMode, RingerCallbacks.OnBoardingSuccessCallback callback) {
        deviceSettingsManager.RemoteRinger_OnBoardingSuccess(onBoardingSucessMode, callback);
    }

    /**
     * Retrieves the hardware version from the connected device.
     *
     * @param callback The callback to receive the hardware version
     */
    public void RemoteRinger_GetHardwareVersion(RingerCallbacks.HardwareVersionCallback callback) {
        deviceSettingsManager.RemoteRinger_GetHardwareVersion(callback);
    }

    /**
     * Performs mobile device unauthentication.
     *
     * @param callback The callback for operation result
     */
    public void RemoteRinger_MobileUnAuth(RingerCallbacks.MobileAunAuthCallback callback) {
        deviceSettingsManager.RemoteRinger_MobileUnAuth(callback);
    }

    /**
     * Retrieves the firmware version from the connected device.
     *
     * @param callback The callback to receive the firmware version
     */
    public void RemoteRinger_GetFirmwareVersion(RingerCallbacks.FirmwareVersionCallback callback) {
        deviceSettingsManager.RemoteRinger_GetFirmwareVersion(callback);
    }

    /**
     * Retrieves the application boot mode from the connected device.
     *
     * @param callback The callback to receive the boot mode
     */
    public void RemoteRinger_GetApplicationBootMode(RingerCallbacks.BootModeCallback callback) {
        deviceSettingsManager.RemoteRinger_GetApplicationBootMode(callback);
    }

    /**
     * Retrieves the device model ID from the connected device.
     *
     * @param callback The callback to receive the model ID
     */
    public void RemoteRinger_GetDeviceModelID(RingerCallbacks.DeviceModelCallback callback) {
        deviceSettingsManager.RemoteRinger_GetDeviceModelID(callback);
    }

    /**
     * Cycles to the next melody audio tone on the connected device.
     *
     * @param callback The callback for operation result
     */
    public void RemoteRinger_PlayNextMelody(RingerCallbacks.VolumeCallback callback) {
        deviceSettingsManager.RemoteRinger_NextMelodyAudioTone(callback);
    }

    /**
     * Cycles to the previous melody audio tone on the connected device.
     *
     * @param callback The callback for operation result
     */
    public void RemoteRinger_PlayPreviousMelody(RingerCallbacks.VolumeCallback callback) {
        deviceSettingsManager.RemoteRinger_PreviousMelodyAudioTone(callback);
    }

    /**
     * Previews a specific melody audio tone on the connected device.
     *
     * @param melodyId The tone ID to preview
     * @param callback               The callback for operation result
     */
    public void RemoteRinger_PreviewMelodyAudioTone(Integer melodyId, RingerCallbacks.VolumeCallback callback) {
        deviceSettingsManager.RemoteRinger_PreviewMelodyAudioTone(melodyId, callback);
    }

    /**
     * Retrieves the WiFi SSID from the connected device.
     *
     * @param callback The callback to receive the SSID
     */
    public void RemoteRinger_GetWiFiSSID(RingerCallbacks.WiFiSSIDCallback callback) {
        deviceSettingsManager.RemoteRinger_GetWiFiSSID(callback);
    }

    /**
     * Retrieves the door lock encryption key from the connected device.
     *
     * @param callback The callback to receive the encryption key
     */
    public void RemoteRinger_getDoorLockEncryptionKey(RingerCallbacks.DoorLockEncryptionKey callback) {
        deviceSettingsManager.RemoteRinger_getDoorLockEncryptionKey(callback);
    }

    /**
     * Retrieves the door lock ID from the connected device.
     *
     * @param callback The callback to receive the door lock ID
     */
    public void RemoteRinger_GetDoorLockID(RingerCallbacks.DoorLockIDCallback callback) {
        deviceSettingsManager.RemoteRinger_GetDoorLockID(callback);
    }

    /**
     * Plays a specific melody on the connected device.
     *
     * @param melodyId The ID of the melody to play
     * @param callback The callback for operation result
     */
    public void RemoteRinger_PlayMelody(RingerCallbacks.PlayMelodyCallback callback) {
        deviceSettingsManager.RemoteRinger_PlayMelody(callback);
    }

    /**
     * Sets the audio tone on the connected device.
     *
     * @param melodyId The tone ID to set
     * @param callback  The callback for operation result
     */
    public void RemoteRinger_SetMelody(int melodyId, RingerCallbacks.PlayMelodyCallback callback) {
        deviceSettingsManager.RemoteRinger_SetMelody(melodyId, callback);
    }

    /**
     * Stops the currently playing melody on the connected device.
     *
     * @param callback The callback for operation result
     */
    public void RemoteRinger_StopMelody(RingerCallbacks.StopMelodyCallback callback) {
        deviceSettingsManager.RemoteRinger_StopMelody(callback);
    }

    /**
     * Sets the melody volume level on the connected device.
     *
     * @param volumeLevel The volume level to set
     * @param callback    The callback for operation result
     */
    public void RemoteRinger_SetMelodyVolume(int volumeLevel, RingerCallbacks.VolumeCallback callback) {
        deviceSettingsManager.RemoteRinger_SetMelodyVolume(volumeLevel, callback);
    }

    /**
     * Increments the melody volume on the connected device.
     *
     * @param callback The callback for operation result
     */
    public void RemoteRinger_IncrementMelodyVolume(RingerCallbacks.VolumeCallback callback) {
        deviceSettingsManager.RemoteRinger_IncrementMelodyVolume(callback);
    }

    /**
     * Retrieves the device activation status.
     *
     * @param callback The callback to receive the activation status
     */
    public void RemoteRinger_GetDeviceActivationStatus(RingerCallbacks.OnBoardingActivationCallback callback) {
        deviceSettingsManager.RemoteRinger_GetDeviceActivationStatus(callback);
    }

    /**
     * Decrements the melody volume on the connected device.
     *
     * @param callback The callback for operation result
     */
    public void RemoteRinger_DecrementMelodyVolume(RingerCallbacks.VolumeCallback callback) {
        deviceSettingsManager.RemoteRinger_DecrementMelodyVolume(callback);
    }

    /**
     * Retrieves the current melody volume level from the connected device.
     *
     * @param callback The callback to receive the volume level
     */

    public void RemoteRinger_GetVolumeMelodyLevel(RingerCallbacks.VolumeLevelCallback callback) {
        deviceSettingsManager.RemoteRinger_GetVolumeMelodyLevel(callback);
    }

    /**
     * Retrieves the door lock BLE address from the connected device.
     *
     * @param callback The callback to receive the BLE address
     */
    public void RemoteRinger_getDoorLockBleAddress(RingerCallbacks.DoorLockBleAddressCallback callback) {
        deviceSettingsManager.RemoteRinger_getDoorLockBleAddress(callback);
    }

    /**
     * Retrieves the current audio tone from the connected device.
     *
     * @param callback The callback to receive the audio tone
     */
    public void RemoteRinger_GetCurrentMelodyId(RingerCallbacks.PlayMelodyCallback callback) {
        deviceSettingsManager.RemoteRinger_getAudioTone(callback);
    }

    /**
     * Provisions WiFi settings on the connected device.
     *
     * @param ssid     The WiFi SSID to provision
     * @param password The WiFi password to provision
     * @param callback The callback for operation result
     */
    public void RemoteRinger_Provision(String ssid, String password, RingerCallbacks.ProvisionCallback callback) {
        deviceSettingsManager.RemoteRinger_setProvision(ssid, password, callback);
    }

    /**
     * Performs device onboarding with the specified parameters.
     *
     * @param doorLockId    The door lock ID to set
     * @param encryptionKey The encryption key to set
     * @param doorLockbleMacId      The BLE MAC ID to set
     * @param callback      The callback for operation result
     */
    public void RemoteRinger_Onboarding(String doorLockId, String encryptionKey, String doorLockbleMacId, int onBoardingType, RingerCallbacks.OnboardingCallback callback) {
        deviceSettingsManager.RemoteRinger_Onboarding(doorLockId, encryptionKey, doorLockbleMacId, onBoardingType, callback);
    }

    /**
     * Disconnects from the currently connected device.
     *
     * @param callback The callback for disconnection result
     */
    public void RemoteRinger_DisconnectDevice(RingerCallbacks.DisconnectionCallback callback) {
        bluetoothManager.RemoteRinger_DisconnectDevice(callback);
    }

    /**
     * Initializes and manages the SDK session, attempting to reconnect to previously
     * connected devices if available.
     *
     * @param callback The callback for session management events
     */
    public void initializeAndManageSession(final RingerCallbacks.SessionCallback callback) {
        try {
            sdkManager_apiKey = RingerSdkManager_ApiKey.getInstance();
        } catch (IllegalStateException e) {
            Log.e(TAG, "SDK Initialization Failed: " + e.getMessage());
            callback.onError("SDK Initialization Failed: " + e.getMessage());
            return;
        }

        String activeDevice = sdkManager_apiKey.getActiveDevice();
        List<String> lastDevices = sdkManager_apiKey.getActiveDevices();
        callback.onLastConnectedDevices(lastDevices);

        if (activeDevice != null && !activeDevice.isEmpty()) {
            Log.d(TAG, "Attempting to reconnect to active device: " + activeDevice);
            bluetoothManager.connectToDevice(activeDevice, new RingerCallbacks.RingerDeviceCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Reconnected successfully to " + activeDevice);
                    callback.onDeviceConnected(activeDevice);
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Reconnection failed for active device " + activeDevice + ": " + errorMessage);
                    sdkManager_apiKey.removeConnectedDevice(activeDevice);

                    for (String backupDevice : lastDevices) {
                        if (!backupDevice.equals(activeDevice)) {
                            Log.d(TAG, "Trying backup reconnection to: " + backupDevice);
                            bluetoothManager.connectToDevice(backupDevice, new RingerCallbacks.RingerDeviceCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    Log.d(TAG, "Reconnected successfully to backup device " + backupDevice);
                                    sdkManager_apiKey.setActiveDevice(backupDevice);
                                    callback.onDeviceConnected(backupDevice);
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Log.e(TAG, "Backup reconnection failed: " + errorMessage);
                                    callback.onError("Failed to reconnect to any device.");
                                }
                            });
                            return;
                        }
                    }
                    callback.onError("Failed to reconnect to any device.");
                }
            });
        } else if (!lastDevices.isEmpty()) {
            String reconnectDevice = lastDevices.get(0);
            Log.d(TAG, "No active device found. Attempting to reconnect to: " + reconnectDevice);
            bluetoothManager.connectToDevice(reconnectDevice, new RingerCallbacks.RingerDeviceCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Reconnected successfully to " + reconnectDevice);
                    sdkManager_apiKey.setActiveDevice(reconnectDevice);
                    callback.onDeviceConnected(reconnectDevice);
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Reconnection failed for " + reconnectDevice + ": " + errorMessage);
                    callback.onError("Failed to reconnect to any device.");
                }
            });
        } else {
            Log.d(TAG, "No previously connected devices found.");
            callback.onError("No previous devices found.");
        }
    }

    /**
     * Retrieves comprehensive device information from the connected device.
     *
     * @param callback The callback to receive the device information
     */
    public void RemoteRinger_GetDeviceInfo(RingerCallbacks.DevicesInfoCallback callback) {
        deviceSettingsManager.RemoteRinger_getDeviceInfo(callback);
    }

    /**
     * Sends a sendOtaControlCommandRequest command to the connected device.
     *
     * @param command,OtaCallback & The callback for success/error response.
     */
    public static void sendOtaControlCommandRequest(byte[] command, RingerCallbacks.OtaCallback callback) {
        DeviceSettingsManager.sendOtaControlCommandRequest(command, callback);
    }

    /**
     * Sends a sendOtaControlChecksumRequest command to the connected device.
     *
     * @param command,OtaCallback & The callback for success/error response.
     */
    public static void sendOtaControlChecksumRequest(byte[] command, RingerCallbacks.OtaCallback callback) {
        DeviceSettingsManager.sendOtaControlChecksumRequest(command, callback);
    }

    /**
     * Sends a sendOtaControlChecksumDone command to the connected device.
     *
     * @param command,OtaCallback & The callback for success/error response.
     */
    public static void sendOtaControlChecksumDone(byte[] command, RingerCallbacks.OtaCallback callback) {
        DeviceSettingsManager.sendOtaControlChecksumDone(command, callback);
    }

    /**
     * Sends a sendOtaControlDone command to the connected device.
     *
     * @param command,OtaCallback & The callback for success/error response.
     */
    public static void sendOtaControlDone(byte[] command, RingerCallbacks.OtaCallback callback) {
        DeviceSettingsManager.sendOtaControlDone(command, callback);
    }


    /**
     * Sends a RemoteRinger_StartOtaUpdate command to the connected device.
     *
     * @param fileUri,data,chunkSize & The callback for success/error response.
     */

    public void RemoteRinger_StartOtaUpdate( Uri fileUri, RingerCallbacks.OtaProgressCallback callback) {
        deviceSettingsManager.RemoteRinger_StartOtaUpdate( fileUri, callback);
    }
    /*public static void RemoteRinger_StartOtaUpdate(byte[] firmWareData, int chunkSize, RingerCallbacks.OtaProgressCallback callback) {
        DeviceSettingsManager.RemoteRinger_StartOtaUpdate(firmWareData,chunkSize,callback);
    }*/

    /**
     * Sends a sendOtaActionControlChecksumCheck command to the connected device.
     *
     * @param sendOtaActionControlChecksumCheck  & The callback for success/error response.
     */
    public static void sendOtaActionControlChecksumCheck(byte[] checkSum, RingerCallbacks.OtaCallback callback) {
        DeviceSettingsManager.sendOtaActionControlChecksumCheck(checkSum, callback);
    }

    /**
     * Sends a RemoteRinger_AbortOtaUpdate command to the connected device.
     *
     * @param RemoteRinger_AbortOtaUpdate  & The callback for success/error response.
     */

    public void RemoteRinger_AbortOtaUpdate(RingerCallbacks.OtaCallback callback) {
        DeviceSettingsManager.getInstance(activity, bluetoothManager, activity).abortOtaUpdate();
        callback.onSuccess("OTA aborted successfully.");
    }

    public void RemoteRinger_playMelodyList(Context context, RingerCallbacks.ToneCallback callback) {
        deviceSettingsManager.RemoteRinger_playMelodyList(context,callback);
    }

}