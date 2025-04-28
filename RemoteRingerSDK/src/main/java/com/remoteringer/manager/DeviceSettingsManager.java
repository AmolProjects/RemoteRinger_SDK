package com.remoteringer.manager;

import static com.remoteringer.Constant.Constant_Variable.*;
import static com.remoteringer.manager.BluetoothManager.CHARACTERISTIC_UUID;
import static com.remoteringer.manager.BluetoothManager.CHARACTERISTIC_UUID_Ota_1_byte_Data;
import static com.remoteringer.manager.BluetoothManager.CHARACTERISTIC_UUID_Ota_512_data;
import static com.remoteringer.manager.BluetoothManager.SERVICE_UUID;
import static com.remoteringer.manager.BluetoothManager.SERVICE_UUID_Ota;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.remoteringer.Constant.Constant_Variable;
import com.remoteringer.Constant.DeviceInfo;
import com.remoteringer.R;
import com.remoteringer.adapter.PopupListAdapter;
import com.remoteringer.callbacks.RingerCallbacks;
import com.remoteringer.commands.RemoteRingerCommand;
import com.remoteringer.handlers.BleResponseHandler;
import com.remoteringer.utils.JsonReader;
import com.remoteringer.utils.Tone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The DeviceSettingsManager class provides methods to manage and control a remote ringer device
 * through Bluetooth Low Energy (BLE) communication. It handles various device settings, commands,
 * and information retrieval operations.
 *
 * <p>This class serves as the main interface for interacting with the remote ringer device,
 * providing methods for authentication, device configuration, system mode management,
 * WiFi provisioning, door lock settings, audio control, and device information retrieval.</p>
 */
public class DeviceSettingsManager {
    public static final String Host_ID = "FFFFFFFFFFFFFFFF";
    private static final String TAG = "DeviceSettingsManager";
    // Error Code to Error Message Mapping
    private static final Map<String, String> errorMap = new HashMap<>();
    private static BluetoothManager bluetoothManager;
    private static Context context;
    private static int asciiValue;
    private static DeviceSettingsManager instance;
    private final Map<Byte, RingerCallbacks.VolumeCallback> pendingVolumeCallbacks = new HashMap<>();
    private RingerCallbacks.HardwareVersionCallback  hardwareVersionCallback;
    private final Map<Byte, RingerCallbacks.BaseCallback> commandCallbackMap = new HashMap<>();
    private String tempDeviceName = "GRR_123456"; // Example device name
    private long locallyGeneratedRandomNo;
    private RingerCallbacks.DeviceModelCallback deviceModelCallback;
    public static boolean isOtaUpdatePending = false;
    private RingerCallbacks.VolumeLevelCallback volumelevelCallback;
    private  RingerCallbacks.BootModeCallback getBootModeCallback;
    static {
        errorMap.put("ERR01", "Wrong command code");
        errorMap.put("ERR02", "Wrong CRC");
        errorMap.put("ERR03", "Parameter not configured");
        errorMap.put("ERR04", "Wrong Data");
        errorMap.put("ERR05", "Wrong Frame type");
        errorMap.put("ERR06", "Invalid data length");
        errorMap.put("ERR07", "Data transfer complete");
        errorMap.put("ERR08", "Memory full");
        errorMap.put("ERR09", "No Data");
        errorMap.put("ERR10", "Invalid start/End of Frame");
        errorMap.put("ERR11", "Not allowed");
        errorMap.put("ERR12", "Host max authorized");
        errorMap.put("ERR13", "Deletion failed");
        errorMap.put("ERR14", "Host unauthorized");
        errorMap.put("ERR15", "Not configured");
        errorMap.put("ERR16", "Duplicate Request ID");
        errorMap.put("ERR17", "Tone IC is Not Responding");
        errorMap.put("ERR18", "Tone IC Busy");
//        errorMap.put(" private static DeviceSettingsManager instance;ERR17", "Tone IC not responding");
    }

    private final BleResponseHandler bleResponseHandler;
    private final ConcurrentHashMap<Byte, byte[]> responseMap = new ConcurrentHashMap<>();
    DeviceInfo deviceInfo;
    private String serialNumber, hardwareVersion, firmwareNumber, appBootModeID1, deviceModelID;
    private int appBootModeID;
    private RingerCallbacks.SerialNumberCallback serialNumberCallback;
    private volatile boolean isOtaPaused = false;
    private Runnable pendingOtaChunk = null;
    private boolean otaInProgress = false; // Optional for state tracking
    private int currentMtu = 128;
    private Handler handler = new Handler(Looper.getMainLooper());
    private RingerCallbacks.OtaProgressCallback otaProgressCallback;

    //added for hmac mobile auth
    public long getRandomNumber() {
        return Integer.toUnsignedLong(new Random().nextInt());
    }

    public byte[] getSecretKey() {
        return new byte[32]; // 32 zero bytes
    }

    public byte[] hmacSHA256(byte[] key, String message) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate HMAC SHA256", e);
        }
    }



    //new hmac auth
    public byte[] preparePayloadForAuthRequest() {
        long mobileNonce = getRandomNumber();
        Log.d("AuthUtils", "mobile_nonce: " + mobileNonce);
        Constant_Variable.locallyGeneratedRandomNo = mobileNonce;

        // Convert to 8-character uppercase hex string
        String hexString = String.format(Locale.US, "%08X", mobileNonce);

        // Reverse the string in 2-character chunks
        List<String> bytePairs = new ArrayList<>();
        for (int i = 0; i < hexString.length(); i += 2) {
            bytePairs.add(hexString.substring(i, i + 2));
        }
        Collections.reverse(bytePairs);
        String reversedHex = String.join("", bytePairs);

        // Prepend "0000"
        String dataR = "0000" + reversedHex;

        // Convert hex string to byte array
        byte[] actualData = hexStringToBytes(dataR);
        Log.d("AuthUtils", "actualData: " + bytesToHex(actualData));

        return actualData;
    }

    private byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexStr = new StringBuilder();
        for (byte b : bytes) {
            hexStr.append(String.format("%02X", b));
        }
        return hexStr.toString();
    }

    ///end mobile auth


    /**
     * **🔹 Create constructor of DeviceSettingsManager **
     */



    private DeviceSettingsManager(Context context, BluetoothManager bluetoothManager, Activity activity) {
        DeviceSettingsManager.context = context;
        DeviceSettingsManager.bluetoothManager = bluetoothManager;
        this.bleResponseHandler = new BleResponseHandler(activity, this);
        this.deviceInfo = new DeviceInfo();
    }

    /**
     * **🔹 Create factory method of DeviceSettingsManager **
     */
    public static synchronized DeviceSettingsManager getInstance(Context ctx, BluetoothManager btManager, Activity activity) {
        if (instance == null) {
            instance = new DeviceSettingsManager(ctx, btManager, activity);
        }
        return instance;
    }

    /**
     * **🔹  sendOtaControlCommandRequest **
     */
    public static void sendOtaControlCommandRequest(byte[] command, RingerCallbacks.OtaCallback callback) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("No active Bluetooth connection.");
            return;
        }

        // Get OTA Service
        BluetoothGattService otaService = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID_Ota)));
        if (otaService == null) {
            callback.onError("OTA Service not found.");
            return;
        }

        // Get OTA Control Characteristic
        BluetoothGattCharacteristic characteristic = otaService.getCharacteristic(CHARACTERISTIC_UUID_Ota_1_byte_Data);
        if (characteristic == null) {
            callback.onError("OTA Control Characteristic not found.");
            return;
        }

        // Write Command to OTA Control UUID
        characteristic.setValue(command);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        boolean success = bluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
            callback.onError("Failed to write OTA control command.");
        } else {
            callback.onSuccess("OTA control command sent.");
        }
    }

    /**
     * **🔹  SVR_CHR_OTA_CONTROL_CHECKSUM  **
     */
    public static void sendOtaControlChecksumRequest(byte[] command, RingerCallbacks.OtaCallback callback) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("No active Bluetooth connection.");
            return;
        }

        // Get OTA Service
        BluetoothGattService otaService = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID_Ota)));
        if (otaService == null) {
            callback.onError("OTA Service not found.");
            return;
        }

        // Get OTA Control Characteristic
        BluetoothGattCharacteristic characteristic = otaService.getCharacteristic(CHARACTERISTIC_UUID_Ota_1_byte_Data);
        if (characteristic == null) {
            callback.onError("OTA Control Characteristic not found.");
            return;
        }

        // Write Command to OTA Control UUID
        characteristic.setValue(command);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        boolean success = bluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
            callback.onError("Failed to write OTA control checksum request.");
        } else {
            callback.onSuccess("OTA control checksum command request sent.");
        }
    }

    /**
     * **🔹  SVR_CHR_OTA_CHECKSUM_DATA  **
     */


    public static void sendOtaActionControlChecksumCheck(byte[] checkSum, RingerCallbacks.OtaCallback callback) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("No active Bluetooth connection.");
            return;
        }

        // Get OTA Service
        BluetoothGattService otaService = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID_Ota)));
        if (otaService == null) {
            callback.onError("OTA Service not found.");
            return;
        }

        // Get OTA Control Characteristic
        BluetoothGattCharacteristic characteristic = otaService.getCharacteristic(CHARACTERISTIC_UUID_Ota_512_data);
        if (characteristic == null) {
            callback.onError("OTA Control Characteristic not found.");
            return;
        }

        // Write Command to OTA Control UUID
        characteristic.setValue(checkSum);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        boolean success = bluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
            callback.onError("Failed to write OTA checksum data.");
        } else {
            callback.onSuccess("OTA checksum data sent.");
        }
    }

    /**
     * **🔹  SVR_CHR_OTA_CONTROL_CHECKSUM_DONE  **
     */
    public static void sendOtaControlChecksumDone(byte[] command, RingerCallbacks.OtaCallback callback) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("No active Bluetooth connection.");
            return;
        }

        // Get OTA Service
        BluetoothGattService otaService = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID_Ota)));
        if (otaService == null) {
            callback.onError("OTA Service not found.");
            return;
        }

        // Get OTA Control Characteristic
        BluetoothGattCharacteristic characteristic = otaService.getCharacteristic(CHARACTERISTIC_UUID_Ota_1_byte_Data);
        if (characteristic == null) {
            callback.onError("OTA Control Characteristic not found.");
            return;
        }

        // Write Command to OTA Control UUID
        characteristic.setValue(command);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        boolean success = bluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
            callback.onError("Failed to write OTA control checksum done.");
        } else {
            callback.onSuccess("OTA control checksum done sent.");
        }
    }

    /**
     * **🔹  SVR_CHR_OTA_CONTROL_DONE  **
     */
    public static void sendOtaControlDone(byte[] command, RingerCallbacks.OtaCallback callback) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("No active Bluetooth connection.");
            return;
        }

        // Get OTA Service
        BluetoothGattService otaService = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID_Ota)));
        if (otaService == null) {
            callback.onError("OTA Service not found.");
            return;
        }

        // Get OTA Control Characteristic
        BluetoothGattCharacteristic characteristic = otaService.getCharacteristic(CHARACTERISTIC_UUID_Ota_1_byte_Data);
        if (characteristic == null) {
            callback.onError("OTA Control Characteristic not found.");
            return;
        }

        // Write Command to OTA Control UUID
        characteristic.setValue(command);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        boolean success = bluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
            callback.onError("Failed to write OTA control done request.");
        } else {
            callback.onSuccess("OTA control done sent.");
        }
    }

    /**
     * **🔹  String convert into the integer value   **
     */

    public static String convertToVersionFormat(String decimalString) {
        // Convert decimal string to an integer
        int decimalValue = Integer.parseInt(decimalString);

        // Convert integer to a hex string with at least 6 digits
        String hexString = String.format("%06X", decimalValue);

        // Split into byte pairs and convert each to an integer (removing leading zeros)
        String[] parts = {
                String.valueOf(Integer.parseInt(hexString.substring(0, 2), 16)),
                String.valueOf(Integer.parseInt(hexString.substring(2, 4), 16)),
                String.valueOf(Integer.parseInt(hexString.substring(4, 6), 16))
        };

        // Join with '.' instead of ':'
        return String.join(".", parts);
    }


    public static String convertFirmwareVersionFormat(String decimalString) {
        int value = Integer.parseInt(decimalString);
        String hex = String.format("%06X", value); // Ensure 6 hex digits

        // Reverse by bytes (2 hex digits each)
        String reversedHex = hex.substring(4, 6) + hex.substring(2, 4) + hex.substring(0, 2);

        // Convert reversed hex to decimal
        int reversedValue = Integer.parseInt(reversedHex, 16);

        return convertToVersionFormat(String.valueOf(reversedValue));
    }

    // [Additional methods would follow the same pattern with Javadoc comments]

    /**
     * **🔹  2 byte value convert into the integer   **
     */

    public static int convertTwoBytesToInt(byte[] data) {
        if (data == null || data.length != 2) {
            throw new IllegalArgumentException("Byte array must be exactly 2 bytes.");
        }

        return ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    }

    /**
     * **🔹 if successfully notification return bleResponseHandler **
     */
    public BleResponseHandler getBleResponseHandler() {
        return bleResponseHandler;
    }

    /**
     * **🔹 for the authentication of serialnumber **
     */


    //old auth
//    public void RemoteRinger_Authentication(String ringerSerialNo, RingerCallbacks.AuthenticationCallback callback) {
//        ringerSerialNo = Constant_Variable.serialNumber;
//        String serialNumber = ringerSerialNo.replaceFirst("GRR_", "");
//        Log.d(TAG, "Ringer serial number " + serialNumber);
//        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
//        if (bluetoothGatt == null) {
//            callback.onError("No active Bluetooth connection.");
//            return;
//        }
//
//        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID)));
//        if (service == null) {
//            new Handler(Looper.getMainLooper()).postDelayed(() -> RemoteRinger_Authentication(serialNumber, callback), 3000);
//            return;
//        }
//
//        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(String.valueOf(CHARACTERISTIC_UUID)));
//        if (characteristic == null) {
//            callback.onError("Characteristic not found.");
//            return;
//        }
//
//        byte requestId = RemoteRingerCommand.getNextRequestId(context);
//        Log.e(TAG, "RemoteRinger_mobileAuthentication Request Id" + requestId);
//
//        byte[] commandFrame = RemoteRingerCommand.createCommandFrame(frameType, mobileAuthCommand, (byte) requestId, Host_ID,
//                RemoteRingerCommand.RemoteRinger_setMethodVariableSize(serialNumber));
//
//        characteristic.setValue(commandFrame);
//        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        boolean writeSuccess = bluetoothGatt.writeCharacteristic(characteristic);
//        if (!writeSuccess) {
//            callback.onError("Failed to write Mobile Authentication command");
//        } else {
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                callback.onSuccess("Mobile Auth: "+getResponseOk);
//            }, 1000);
//        }
//    }


    //new hmac auth
    public void RemoteRinger_Authentication(String ringerSerialNo, RingerCallbacks.AuthenticationCallback callback) {
        // Ensure we're using the updated serial number from constant

        ringerSerialNo = Constant_Variable.serialNumber;
        String serialNumber = ringerSerialNo.replaceFirst("GRR_", "");
        Log.d(TAG, "Ringer serial number: " + serialNumber);


        byte[] payload = preparePayloadForAuthRequest();


        byte[] frame = buildCommandFrame(mobileAuthCommand, payload);

        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // Delay the success callback by 1 second as in original
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    callback.onSuccess("Mobile Auth: "+getResponseOk);
                }, 1000);
            }

            @Override
            public void onError(String errorMessage) {

                callback.onError("Mobile Auth Error: "+errorMessage);
            }
        }, "RemoteRinger_Authentication");
    }

    public void RemoteRinger_SendHmacResponse(RingerCallbacks.AuthenticationCallback callback) {
        byte[] payload = actualHmacData; // Placeholder for WiFi SSID request
        byte[] frame = buildCommandFrame(mobileAuthCommand,payload);


        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getWifiSSIDCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_GetWiFiSSID");
    }

    /**
     * Sets the system mode of the remote ringer device.
     *
     * @param systemMode The system mode to set (numeric value)
     * @param callback   The callback to handle the operation results
     */


    public void RemoteRinger_setSystemMode(int systemMode, RingerCallbacks.SystemModeCallback callback) {

        byte[] payload = RemoteRingerCommand.setSingleByteCommand(systemMode);
        byte[] frame = buildCommandFrame(setSystemModeCommand, payload);
        commandCallbackMap.put(setSystemModeCommand, callback);


        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setSystemModeCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_SetMelody");

    }


    /**
     * Sets the system mode of the remote ringer device.
     *
     * @param systemMode The system mode to set (numeric value)
     * @param callback   The callback to handle the operation results
     */



    public void RemoteRinger_setSystemMode1(int systemMode, RingerCallbacks.SystemModeCallback callback) {
        byte[] payload = RemoteRingerCommand.setSingleByteCommand(systemMode);
        byte[] frame = buildCommandFrame(setSystemModeCommand, payload);
        commandCallbackMap.put(setSystemModeCommand, callback);


        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setSystemModeCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_setSystemMode1");
    }

    public void RemoteRinger_deviceCommissioning( RingerCallbacks.SystemModeCallback callback) {
        byte[] payload = RemoteRingerCommand.setSingleByteCommand(1);
        byte[] frame = buildCommandFrame(setSystemModeCommand, payload);
        commandCallbackMap.put(setSystemModeCommand, callback);


        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setSystemModeCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_setSystemMode1");
    }


    /**
     * Gets the current system mode of the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_getSystemMode(RingerCallbacks.SystemModeCallback callback) {
        byte[] payload = new byte[]{0x00};
        byte[] frame = buildCommandFrame(getSystemModeCommand, payload);
        commandCallbackMap.put(getSystemModeCommand, callback);


        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getSystemModeCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_getSystemMode");
    }


    /**
     * Sets the WiFi SSID on the remote ringer device.
     *
     * @param wifiSsid The WiFi SSID to set
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_setWifiSsid(String wifiSsid, RingerCallbacks.WiFiSSIDCallback callback) {
        if (wifiSsid.isEmpty()) {

            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Error: SSID cannot be empty ", Toast.LENGTH_LONG).show()

            );
            return;
        }

        byte[] payload = RemoteRingerCommand.RemoteRinger_setMethodVariableSize(wifiSsid);
        byte[] frame = buildCommandFrame(setWIFISSIDCommand, payload);
        // Save the callback in the map
        commandCallbackMap.put(setWIFISSIDCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setWIFISSIDCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_setWifiSsid");
    }


    /**
     * Sets the WiFi password on the remote ringer device.
     *
     * @param wifiPass The WiFi password to set
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_setWifiPass(String wifiPass, RingerCallbacks.WifiPasswordcallback callback) {
        if (wifiPass.isEmpty()) {
            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] payload = RemoteRingerCommand.RemoteRinger_setMethodVariableSize(wifiPass);
        byte[] frame = buildCommandFrame(setWifiPassword, payload);

        commandCallbackMap.put(setWifiPassword, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setWifiPassword);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_setWifiPass");
    }

    /**
     * Activates or deactivates WiFi on the remote ringer device.
     *
     * @param wifiActivationMode The activation mode (1 for activate, 0 for deactivate)
     * @param callback           The callback to handle the operation results
     */


    public void RemoteRinger_wifiActivation(int wifiActivationMode, RingerCallbacks.WifiActivationCallback callback) {
        byte[] payload = RemoteRingerCommand.setSingleByteCommand(wifiActivationMode);
        byte[] frame = buildCommandFrame(setWifiActivationCommand, payload);

        commandCallbackMap.put(setWifiActivationCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setWifiActivationCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_wifiActivation");
    }

    /**
     * Sets the door lock ID on the remote ringer device.
     *
     * @param doorLockId The door lock ID to set
     * @param callback   The callback to handle the operation results
     */


    public void RemoteRinger_SetDoorLockId(String doorLockId, RingerCallbacks.DoorLockIDCallback callback) {
        if (doorLockId.isEmpty()) {
            Toast.makeText(context, "DoorLockId cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] payload = RemoteRingerCommand.RemoteRinger_setMethodVariableSize(doorLockId);
        byte[] frame = buildCommandFrame(setDoorLockIdCommand, payload);

        commandCallbackMap.put(setDoorLockIdCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setDoorLockIdCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_SetDoorLockId");
    }


    /**
     * Sets the serial number on the remote ringer device.
     *
     * @param serialNumber The serial number to set
     * @param callback     The callback to handle the operation results
     */


    public void RemoteRinger_setSerialNumber(String serialNumber, RingerCallbacks.SerialNumberCallback callback) {
        if (serialNumber.isEmpty()) {
            Toast.makeText(context, "SerialNumber cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] payload = RemoteRingerCommand.RemoteRinger_setMethodVariableSize(serialNumber);
        byte[] frame = buildCommandFrame(setSerialCommand, payload);

        commandCallbackMap.put(setSerialCommand, callback);


        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setSerialCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_setSerialNumber");
    }

    /**
     * Reboots the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */

    public void RemoteRinger_RebootDevice(RingerCallbacks.RebootCallback callback) {
        byte[] payload = new byte[]{0x00}; // Empty payload
        byte[] frame = buildCommandFrame(deviceRebootCommand, payload);

        commandCallbackMap.put(deviceRebootCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(deviceRebootCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_RebootDevice");
    }


    /**
     * Performs a factory reset on the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_FactoryReset(RingerCallbacks.FactoryResetCallback callback) {
        byte[] payload = new byte[]{0x00}; // No additional data
        byte[] frame = buildCommandFrame(FactoryResetCommand, payload);

        commandCallbackMap.put(FactoryResetCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(FactoryResetCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_FactoryReset");
    }


    /**
     * Sets the application boot mode on the remote ringer device.
     *
     * @param mode     The boot mode to set
     * @param callback The callback to handle the operation results
     */

    public void RemoteRinger_setApplicationBootMode(int mode, RingerCallbacks.SetApplicationBootMode callback) {
        byte[] payload = RemoteRingerCommand.setSingleByteCommand(mode);
        byte[] frame = buildCommandFrame(setApplicationBootModeCommand, payload);

        commandCallbackMap.put(setApplicationBootModeCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setApplicationBootModeCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_setApplicationBootMode");
    }



    /**
     * Sets the device model ID on the remote ringer device.
     *
     * @param modelID  The model ID to set
     * @param callback The callback to handle the operation results
     */

    public void RemoteRinger_setDeviceModelID(int modelID, RingerCallbacks.setDeviceModelID callback) {

        if (modelID < 0 || modelID > 65535) {
            callback.onError("Enter valid value.");
            Toast.makeText(context, "Enter valid value.", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] payload = RemoteRingerCommand.setTwoByteCommand(modelID);
        byte[] frame = buildCommandFrame(setDeviceModelId, payload);

        commandCallbackMap.put(setDeviceModelId, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setDeviceModelId);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_setDeviceModelID");
    }


    /**
     * Sets the hardware version on the remote ringer device.
     *
     * @param major    The major version number
     * @param minor    The minor version number
     * @param patch    The patch version number
     * @param callback The callback to handle the operation results
     */

    public void RemoteRinger_setHardwareVersion(int major, int minor, int patch, RingerCallbacks.HardwareVersionCallback callback) {



        byte[] payload = RemoteRingerCommand.RemoteRinger_setHardwareVersion(major, minor, patch);
        byte[] frame = buildCommandFrame(setHardWareCommand, payload);

        commandCallbackMap.put(setHardWareCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setHardWareCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_setHardwareVersion");
    }


    /**
     * Sets the door lock BLE address on the remote ringer device.
     *
     * @param bleAddress The BLE address to set
     * @param callback   The callback to handle the operation results
     */

   /* public void RemoteRinger_setDoorLockBleAddress(String bleAddress, RingerCallbacks.DoorLockBleAddressCallback callback) {
        if (bleAddress == null || bleAddress.length() < 12) {
            bleAddress = "000000000000";
        }

        byte[] payload = RemoteRingerCommand.RemoteRinger_setMethodVariableSizes(bleAddress);
        byte[] frame = buildCommandFrame(setDoorLockBleMacCommand, payload);

        commandCallbackMap.put(setDoorLockBleMacCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setDoorLockBleMacCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_setDoorLockBleAddress");
    }
*/

    public void RemoteRinger_setDoorLockBleAddress(String bleAddress, RingerCallbacks.DoorLockBleAddressCallback callback) {
        if (bleAddress == null) {
            if (callback != null) {
                callback.onError("BLE address is null.");
            }
            return;
        }

        byte[] payload;
        try {
            payload = RemoteRingerCommand.RemoteRinger_setMethodVariableSizes(bleAddress);
        } catch (IllegalArgumentException e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
            return;
        }

        byte[] frame = buildCommandFrame(setDoorLockBleMacCommand, payload);

        commandCallbackMap.put(setDoorLockBleMacCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // Optional: handle success
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setDoorLockBleMacCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_setDoorLockBleAddress");
    }



    /**
     * Sets the door lock encryption key on the remote ringer device.
     *
     * @param doorLockEncryptionKey The encryption key to set
     * @param callback              The callback to handle the operation results
     */


    public void RemoteRinger_setDoorLockEncryptionKey(String doorLockEncryptionKey, RingerCallbacks.DoorLockEncryptionKey callback) {
        if (doorLockEncryptionKey == null || doorLockEncryptionKey.length() < 32) {
            doorLockEncryptionKey = "00000000000000000000000000000000";
        }

        byte[] payload = doorLockEncryptionKey.getBytes(StandardCharsets.US_ASCII);
        byte[] frame = buildCommandFrame(setDoorLockEncryptionKeyCommand, payload);

        commandCallbackMap.put(setDoorLockEncryptionKeyCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setDoorLockEncryptionKeyCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_setDoorLockEncryptionKey");
    }

    /**
     * Sets the onboarding activation status on the remote ringer device.
     *
     * @param onBoardingMode The activation mode (1 for activate, 0 for deactivate)
     * @param callback       The callback to handle the operation results
     */

    public void RemoteRinger_setOnBoardingActivation(int onBoardingMode, RingerCallbacks.OnBoardingActivationCallback callback) {
        byte[] payload = RemoteRingerCommand.setSingleByteCommand(onBoardingMode);
        byte[] frame = buildCommandFrame(onBoardingActivationCommand, payload);

        commandCallbackMap.put(onBoardingActivationCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(onBoardingActivationCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_setOnBoardingActivation");
    }


    /**
     * Confirms successful onboarding of the remote ringer device.
     *
     * @param onBoardingSucessMode The success mode (1 for success)
     * @param callback             The callback to handle the operation results
     */

    public void RemoteRinger_OnBoardingSuccess(int onBoardingSucessMode, RingerCallbacks.OnBoardingSuccessCallback callback) {
        byte[] payload = RemoteRingerCommand.setSingleByteCommand(onBoardingSucessMode);
        byte[] frame = buildCommandFrame(onBoardingActivationCommand, payload);

        commandCallbackMap.put(onBoardingActivationCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(onBoardingActivationCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_OnBoardingSuccess");
    }


    /**
     * Gets the serial number from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */

    public void RemoteRinger_getSerialNumber(RingerCallbacks.SerialNumberCallback callback) {
        byte[] payload = new byte[12]; // Empty payload for serial number request
        byte[] frame = buildCommandFrame(getSerialNumberCommand, payload);


        commandCallbackMap.put(getSerialNumberCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getSerialNumberCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_getSerialNumber");
    }

    /**
     * Gets the hardware version from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */




    public void RemoteRinger_GetHardwareVersion(RingerCallbacks.HardwareVersionCallback callback) {

        byte[] payload = new byte[]{0x00};
        byte[] frame = buildCommandFrame(getHardwareVersionCommand, payload);

        commandCallbackMap.put(getHardwareVersionCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getHardwareVersionCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_GetHardwareVersion");
    }



    /**
     * Gets the firmware version from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */

    public void RemoteRinger_GetFirmwareVersion(RingerCallbacks.FirmwareVersionCallback callback) {
        byte[] payload = new byte[]{0x00}; // Used to request firmware version
        byte[] frame = buildCommandFrame(getFarmWareVersionCommand, payload);

        commandCallbackMap.put(getFarmWareVersionCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getFarmWareVersionCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_GetFirmwareVersion");
    }

    /**
     * Gets the application boot mode from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */

    public void RemoteRinger_GetApplicationBootMode(RingerCallbacks.BootModeCallback callback) {
        this.getBootModeCallback = callback;
        byte[] payload = new byte[]{0x00}; // Payload to request application boot mode
        byte[] frame = buildCommandFrame(getApplicationBootModeCommand, payload);

//        commandCallbackMap.put(getApplicationBootModeCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getApplicationBootModeCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_GetApplicationBootMode");
    }

    /**
     * Gets the device model ID from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */

    public void RemoteRinger_GetDeviceModelID(RingerCallbacks.DeviceModelCallback callback) {
        this.deviceModelCallback=callback;
        byte[] payload = new byte[]{0x00}; // Request payload
        byte[] frame = buildCommandFrame(getDeviceModelIdCommand, payload);


        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getDeviceModelIdCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_GetDeviceModelID");
    }

    /**
     * Gets the WiFi SSID from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_GetWiFiSSID(RingerCallbacks.WiFiSSIDCallback callback) {
        byte[] payload = new byte[32]; // Placeholder for WiFi SSID request
        byte[] frame = buildCommandFrame(getWifiSSIDCommand, payload);

        commandCallbackMap.put(getWifiSSIDCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getWifiSSIDCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_GetWiFiSSID");
    }


    /**
     * Gets the door lock ID from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_GetDoorLockID(RingerCallbacks.DoorLockIDCallback callback) {
        byte[] payload = new byte[0]; // No payload for DoorLockID request
        byte[] frame = buildCommandFrame(getDoorLockIdCommand, payload);

        commandCallbackMap.put(getDoorLockIdCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getDoorLockIdCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_GetDoorLockID");
    }


    /**
     * Plays a melody on the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_PlayMelody(RingerCallbacks.PlayMelodyCallback callback) {
        byte[] payload = new byte[]{0x00}; // payload for PlayMelody
        byte[] frame = buildCommandFrame(playAudioToneCommand, payload);

        commandCallbackMap.put(playAudioToneCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(playAudioToneCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_PlayMelody");
    }



    /**
     * Stops the currently playing melody on the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_StopMelody(RingerCallbacks.StopMelodyCallback callback) {
        byte[] payload = new byte[]{0x00}; // Payload for StopMelody
        byte[] frame = buildCommandFrame(stopAudioToneCommand, payload);

        commandCallbackMap.put(stopAudioToneCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(stopAudioToneCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_StopMelody");
    }


    /**
     * Sets the melody volume on the remote ringer device.
     *
     * @param volumeLevel The volume level to set (0-100)
     * @param callback    The callback to handle the operation results
     */


    public void RemoteRinger_SetMelodyVolume(int volumeLevel, RingerCallbacks.VolumeCallback callback) {
        if (volumeLevel < 0 || volumeLevel > 255) {
            if (callback != null) {
                callback.onError("Volume value must fit within one byte.");
            }
            return;
        }
        byte[] payload = RemoteRingerCommand.setSingleByteCommand(volumeLevel);
        byte[] frame = buildCommandFrame(setVolumeLevelCommand, payload);

        commandCallbackMap.put(setVolumeLevelCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setVolumeLevelCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_SetMelodyVolume");
    }

    /**
     * Increments the melody volume on the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_IncrementMelodyVolume(RingerCallbacks.VolumeCallback callback) {
        byte[] frame = buildCommandFrame(incrementVolumeLevelCommand, new byte[]{0x00});

        commandCallbackMap.put(incrementVolumeLevelCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(incrementVolumeLevelCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_IncrementMelodyVolume");
    }

    public void RemoteRinger_DecrementMelodyVolume(RingerCallbacks.VolumeCallback callback) {
        byte[] frame = buildCommandFrame(decrementVolumeLevelCommand, new byte[]{0x00});

        commandCallbackMap.put(decrementVolumeLevelCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(decrementVolumeLevelCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_DecrementMelodyVolume");
    }

    /**
     * Gets the current melody volume level from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    /*public void GetMelodyVolumeLevel(RingerCallbacks.VolumeCallback callback) {
        byte[] frame = buildCommandFrame(getVolumeLevelCommand, new byte[]{0x00});

        commandCallbackMap.put(getVolumeLevelCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getVolumeLevelCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_GetVolumeMelodyLevel");
    }*/

    public void RemoteRinger_GetVolumeMelodyLevel(RingerCallbacks.VolumeLevelCallback callback) {
        this.volumelevelCallback = callback;
        byte[] frame = buildCommandFrame(getVolumeLevelCommand, new byte[]{0x00});
//        commandCallbackMap.put(getVolumeLevelCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getVolumeLevelCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_GetVolumeMelodyLevel");
    }


    /**
     * Plays the next audio tone on the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_NextMelodyAudioTone(RingerCallbacks.VolumeCallback callback) {
        byte[] frame = buildCommandFrame(nextAudioToneCommand, new byte[]{0x00});

        commandCallbackMap.put(nextAudioToneCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(nextAudioToneCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_NextMelodyAudioTone");
    }


    /**
     * Plays the previous audio tone on the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_PreviousMelodyAudioTone(RingerCallbacks.VolumeCallback callback) {
        byte[] frame = buildCommandFrame(previousAudioToneCommand, new byte[]{0x00});

        commandCallbackMap.put(previousAudioToneCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(previousAudioToneCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_PreviousMelodyAudioTone");
    }


    /**
     * Previews an audio tone on the remote ringer device.
     *
     * @param insertPreviewAudioTone The ID of the audio tone to preview
     * @param callback               The callback to handle the operation results
     */

    public void RemoteRinger_PreviewMelodyAudioTone(int tone, RingerCallbacks.VolumeCallback callback) {
        if (tone < 0 || tone > 255) {
            if (callback != null) {
                callback.onError("Tone value must fit within one byte.");
            }
            return;
        }

        byte[] payload = RemoteRingerCommand.setSingleByteCommand(tone);
        byte[] frame = buildCommandFrame(previewAudioToneCommand, payload);
        commandCallbackMap.put(previewAudioToneCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // Optional: trigger success callback here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(previewAudioToneCommand);
                if (cb != null)
                    cb.onError(errorMessage);
            }
        }, "RemoteRinger_PreviewMelodyAudioTone");
    }

    public void RemoteRinger_getDoorLockEncryptionKey(RingerCallbacks.DoorLockEncryptionKey callback) {
        byte[] frame = buildCommandFrame(getDoorLockEncryptionKeyCommand, new byte[32]);

        commandCallbackMap.put(getDoorLockEncryptionKeyCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getDoorLockEncryptionKeyCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_getDoorLockEncryptionKey");
    }


    /**
     * Sets the audio tone on the remote ringer device.
     *
     * @param audioTone The ID of the audio tone to set
     * @param callback  The callback to handle the operation results
     */



    public void RemoteRinger_SetMelody(int tone, RingerCallbacks.PlayMelodyCallback callback) {
        if (tone < 0 || tone > 255) {
            if (callback != null) {
                callback.onError("Tone value must fit within one byte.");
            }
            return;
        }

        byte[] payload = RemoteRingerCommand.setSingleByteCommand(tone);
        byte[] frame = buildCommandFrame(setAudioToneCommand, payload);
        commandCallbackMap.put(setAudioToneCommand, callback);


        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(setAudioToneCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_SetMelody");
    }

    /**
     * Gets the device activation status from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */
   /* public void RemoteRinger_GetDeviceActivationStatus(RingerCallbacks.OnBoardingActivationCallback callback) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("No active Bluetooth connection.");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID)));
        if (service == null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> RemoteRinger_GetDeviceActivationStatus(callback), 3000);
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(String.valueOf(CHARACTERISTIC_UUID)));
        if (characteristic == null) {
            callback.onError("Characteristic not found.");
            return;
        }

        byte requestId = RemoteRingerCommand.getNextRequestId(context);
        Log.e(TAG, "RemoteRinger_GetDeviceActivationStatus RequestId :" + requestId);

        byte[] commandFrame = RemoteRingerCommand.createCommandFrame(frameType, getDeviceActivationStatusCommand, (byte) requestId, Host_ID, new byte[]{0x00});

        characteristic.setValue(commandFrame);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        boolean writeSuccess = bluetoothGatt.writeCharacteristic(characteristic);
        if (!writeSuccess) {
            callback.onError("Failed to write RemoteRinger_GetDeviceActivationStatus command");
        } else {
            callback.onSuccess("RemoteRinger_GetDeviceActivationStatus Command Sent");
        }
    }
*/
    public void RemoteRinger_GetDeviceActivationStatus(RingerCallbacks.OnBoardingActivationCallback callback) {
        byte[] frame = buildCommandFrame(getDeviceActivationStatusCommand, new byte[]{0x00});

        commandCallbackMap.put(getDeviceActivationStatusCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getDeviceActivationStatusCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_GetDeviceActivationStatus");
    }

    /**
     * Gets the door lock BLE address from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */
   /* public void RemoteRinger_getDoorLockBleAddress(RingerCallbacks.DoorLockBleAddressCallback callback) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("No active Bluetooth connection.");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID)));
        if (service == null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> RemoteRinger_getDoorLockBleAddress(callback), 3000);
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(String.valueOf(CHARACTERISTIC_UUID)));
        if (characteristic == null) {
            callback.onError("Characteristic not found.");
            return;
        }

        byte requestId = RemoteRingerCommand.getNextRequestId(context);
        Log.e(TAG, "RemoteRinger_GetDeviceActivationStatus RequestId :" + requestId);

        byte[] commandFrame = RemoteRingerCommand.createCommandFrame(frameType, getDoorLockBleMacCommand, (byte) requestId, Host_ID, new byte[32]);

        characteristic.setValue(commandFrame);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        boolean writeSuccess = bluetoothGatt.writeCharacteristic(characteristic);
        if (!writeSuccess) {
            callback.onError("Failed to write RemoteRinger_getDoorLockBleAddress command");
        } else {
            callback.onSuccess("RemoteRinger_getDoorLockBleAddress Command Sent");
        }
    }
*/
    public void RemoteRinger_getDoorLockBleAddress(RingerCallbacks.DoorLockBleAddressCallback callback) {
        byte[] frame = buildCommandFrame(getDoorLockBleMacCommand, new byte[32]);

        commandCallbackMap.put(getDoorLockBleMacCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed
            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getDoorLockBleMacCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        }, "RemoteRinger_getDoorLockBleAddress");
    }


    /**
     * Gets the current audio tone from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    public void RemoteRinger_getAudioTone(RingerCallbacks.PlayMelodyCallback callback) {
        byte[] frame = buildCommandFrame(getAudioToneCommand, new byte[]{0x00});

        commandCallbackMap.put(getAudioToneCommand, callback);
        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                // You could trigger timeout/failure logic here if needed


            }

            @Override
            public void onError(String errorMessage) {
                RingerCallbacks.BaseCallback cb = commandCallbackMap.remove(getAudioToneCommand);
                if (cb != null) cb.onError(errorMessage);
            }
        },"RemoteRinger_getAudioTone");
    }


    /**
     * Unauthorizes the mobile device from the remote ringer.
     *
     * @param callback The callback to handle the operation results
     */
    public void RemoteRinger_MobileUnAuth(RingerCallbacks.MobileAunAuthCallback callback) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            callback.onError("No active Bluetooth connection.");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID)));
        if (service == null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> RemoteRinger_MobileUnAuth(callback), 3000);
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(String.valueOf(CHARACTERISTIC_UUID)));
        if (characteristic == null) {
            callback.onError("Characteristic not found.");
            return;
        }

        byte requestId = RemoteRingerCommand.getNextRequestId(context);
        Log.e(TAG, "RemoteRinger_MobileUnAuth RequestId :" + requestId);

        byte[] commandFrame = RemoteRingerCommand.createCommandFrame(frameType, MobileUnAuthCommand, (byte) requestId, Host_ID,
                RemoteRingerCommand.RemoteRinger_setMethodVariableSize(serialNumber));

        characteristic.setValue(commandFrame);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        boolean writeSuccess = bluetoothGatt.writeCharacteristic(characteristic);
        if (!writeSuccess) {
            callback.onError("Failed to write RemoteRinger_MobileUnAuth command");
        } else {
            callback.onSuccess("RemoteRinger_MobileUnAuth Command Sent");
        }
    }

    /**
     * Sets up WiFi provisioning on the remote ringer device.
     *
     * @param ssid     The WiFi SSID to set
     * @param password The WiFi password to set
     * @param callback The callback to handle the operation results
     */
   /* public void RemoteRinger_setProvision(String ssid, String password, RingerCallbacks.ProvisionCallback callback) {
        RemoteRinger_setWifiSsid(ssid, new RingerCallbacks.WiFiSSIDCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "SSID set successfully: " + message);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    RemoteRinger_setWifiPass(password, new RingerCallbacks.WifiPasswordcallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "WiFi Password set successfully: " + message);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                RemoteRinger_wifiActivation(1, new RingerCallbacks.WifiActivationCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        Log.d(TAG, "WiFi Activated: " + message);
                                        callback.onSuccess("Provisioning Completed Successfully!");
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e(TAG, "Failed to activate WiFi: " + errorMessage);
                                        callback.onError("Failed at WiFi Activation: " + errorMessage);
                                    }
                                });
                            }, 1000);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to set WiFi password: " + errorMessage);
                            callback.onError("Failed at WiFi Password: " + errorMessage);
                        }
                    });
                }, 1000);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to set SSID: " + errorMessage);
                callback.onError("Failed at SSID: " + errorMessage);
            }
        });
    }
*/

    ///wifi provision start here//
//    public void RemoteRinger_setProvision(String ssid, String password, RingerCallbacks.ProvisionCallback callback) {
//        bleResponseHandler.setProvisionCallback(callback);
//
//        RemoteRinger_setWifiSsid(ssid, new RingerCallbacks.WiFiSSIDCallback() {
//            @Override
//            public void onSuccess(String message) {
//                Log.d(TAG, "SSID set successfully: " + message);
//                delayThen(() -> setWifiPassword(password, callback), 1000);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                Log.e(TAG, "Failed to set SSID: " + errorMessage);
//               callback.onError("Failed at SSID: " + errorMessage);
//            }
//        });
//    }
//
//    private void setWifiPassword(String password, RingerCallbacks.ProvisionCallback callback) {
//        RemoteRinger_setWifiPass(password, new RingerCallbacks.WifiPasswordcallback() {
//            @Override
//            public void onSuccess(String message) {
//                Log.d(TAG, "WiFi Password set successfully: " + message);
//                delayThen(() -> activateWifi(callback), 1000);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                Log.e(TAG, "Failed to set WiFi password: " + errorMessage);
//               callback.onError("Failed at WiFi Password: " + errorMessage);
//            }
//        });
//    }
//
//    private void activateWifi(RingerCallbacks.ProvisionCallback callback) {
//        RemoteRinger_wifiActivation(1, new RingerCallbacks.WifiActivationCallback() {
//            @Override
//            public void onSuccess(String message) {
//                Log.d(TAG, "WiFi Activated: " + message);
//                callback.onSuccess("Provisioning Completed Successfully!");
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                Log.e(TAG, "Failed to activate WiFi: " + errorMessage);
//                callback.onError("Failed at WiFi Activation: " + errorMessage);
//            }
//        });
//    }
//
    private void delayThen(Runnable action, int delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(action, delayMillis);
    }



    //new chnages as per document
    public void RemoteRinger_setProvision(String ssid, String password, RingerCallbacks.ProvisionCallback callback) {

        bleResponseHandler.setProvisionCallback(callback);

        RemoteRinger_getSystemMode(new RingerCallbacks.SystemModeCallback() {
            @Override
            public void onSuccess(String systemMode) {
                Log.d(TAG, "Fetched System Boot Mode: " + systemMode);

                // Step 2: Now decide based on value of systemMode
                if (!"2".equals(systemMode)) {
                    // Not in mode 2, so set it first
                    RemoteRinger_setSystemMode(2, new RingerCallbacks.SystemModeCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "System mode set to 2: " + message);
                            setWiFiCredentials(callback, ssid, password);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to set system mode: " + errorMessage);
                            callback.onError("Failed at System Mode: " + errorMessage);
                        }
                    });
                } else {
                    Log.d(TAG, "System already in mode 2. Proceeding to set WiFi credentials...");
                    setWiFiCredentials(callback, ssid, password);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to get system mode: " + errorMessage);
                callback.onError("Failed to get System Mode: " + errorMessage);
            }
        });

    }

    private void setWiFiCredentials(RingerCallbacks.ProvisionCallback callback, String ssid, String password) {
        RemoteRinger_setWifiSsid(ssid, new RingerCallbacks.WiFiSSIDCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "SSID set successfully: " + message);
                bleResponseHandler.startProvisionTimeout();


                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    RemoteRinger_setWifiPass(password, new RingerCallbacks.WifiPasswordcallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "WiFi Password set successfully: " + message);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                RemoteRinger_wifiActivation(1, new RingerCallbacks.WifiActivationCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        Log.d(TAG, "WiFi Activated: " + message);
                                        callback.onSuccess("Wifi Credentials Set Successfully!");
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e(TAG, "Failed to activate WiFi: " + errorMessage);
                                        callback.onError("WiFi Activation Failed: " + errorMessage);
                                    }
                                });
                            }, 1000);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to set WiFi password: " + errorMessage);
                            callback.onError("Failed to set WiFi Password: " + errorMessage);
                        }
                    });
                }, 1000);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to set SSID: " + errorMessage);
                callback.onError("Failed to set SSID: " + errorMessage);
            }
        });
    }



    ///wifi provision end here//


    /**
     * Performs onboarding of the remote ringer device with door lock settings.
     *
     * @param doorLockId    The door lock ID to set
     * @param encryptionKey The encryption key to set
     * @param bleMacId      The BLE MAC address to set
     * @param callback      The callback to handle the operation results
     */





    public void RemoteRinger_Onboarding(String doorLockId, String encryptionKey, String bleMacId, RingerCallbacks.OnboardingCallback callback) {

        // Step 1: First fetch current system mode
        RemoteRinger_getSystemMode(new RingerCallbacks.SystemModeCallback() {
            @Override
            public void onSuccess(String systemMode) {
                Log.d(TAG, "Fetched System Boot Mode: " + systemMode);

                if (!"3".equals(systemMode)) {
                    // Not in onboarding mode, set it to 3 first
                    RemoteRinger_setSystemMode(3, new RingerCallbacks.SystemModeCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "System mode set to 3: " + message);
                            performOnboarding(doorLockId, encryptionKey, bleMacId, callback);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to set system mode to 3: " + errorMessage);
                            callback.onError("Failed at setting System Mode: " + errorMessage);
                        }
                    });
                } else {
                    Log.d(TAG, "System already in mode 3. Proceeding to onboarding...");
                    performOnboarding(doorLockId, encryptionKey, bleMacId, callback);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to get system mode: " + errorMessage);
                callback.onError("Failed to get System Mode: " + errorMessage);
            }
        });
    }

    private void performOnboarding(String doorLockId, String encryptionKey, String bleMacId, RingerCallbacks.OnboardingCallback callback) {
        bleResponseHandler.startOnboardingTimeout();
        bleResponseHandler.setOnboardingCallback(callback);

        RemoteRinger_SetDoorLockId(doorLockId, new RingerCallbacks.DoorLockIDCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "DoorLockId Set: " + message);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {

                    RemoteRinger_setDoorLockEncryptionKey(encryptionKey, new RingerCallbacks.DoorLockEncryptionKey() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "Encryption Key Set: " + message);
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                                RemoteRinger_setDoorLockBleAddress(bleMacId, new RingerCallbacks.DoorLockBleAddressCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        Log.d(TAG, "BLE MAC ID Set: " + message);
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {

                                            RemoteRinger_setOnBoardingActivation(1, new RingerCallbacks.OnBoardingActivationCallback() {
                                                @Override
                                                public void onSuccess(String message) {
                                                    Log.d(TAG, "Onboarding Activation Started: " + message);

                                                    callback.onSuccess("Onboarding Activation Started");
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    callback.onError("OnBoarding Activation Failed: " + error);
                                                }
                                            });

                                        }, 1000);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        callback.onError("Failed to set BLE MAC ID: " + error);
                                    }
                                });

                            }, 1000);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError("Failed to set Encryption Key: " + error);
                        }
                    });

                }, 1000);
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to set DoorLockId: " + error);
            }
        });
    }



    //onboarding ends here//


    /**
     * Gets comprehensive device information from the remote ringer device.
     *
     * @param callback The callback to handle the operation results
     */


    //get device info starts here//
    public void RemoteRinger_getDeviceInfo(RingerCallbacks.DevicesInfoCallback callback) {
        getSerialNumber(callback);
    }

    private void getSerialNumber(RingerCallbacks.DevicesInfoCallback callback) {
        RemoteRinger_getSerialNumber(new RingerCallbacks.SerialNumberCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "SerialNumber: " + message);
                serialNumber = message;
                delayThen(() -> getFirmwareVersion(callback), 1000);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Serial Number: " + errorMessage);
            }
        });
    }

    private void getFirmwareVersion(RingerCallbacks.DevicesInfoCallback callback) {
        RemoteRinger_GetFirmwareVersion(new RingerCallbacks.FirmwareVersionCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "FirmwareVersion: " + message);
                Constant_Variable.getFirmwareVersion = message;
                delayThen(() -> getHardwareVersion(callback), 1000);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Firmware Version: " + errorMessage);
            }
        });
    }

    private void getHardwareVersion(RingerCallbacks.DevicesInfoCallback callback) {
        RemoteRinger_GetHardwareVersion(new RingerCallbacks.HardwareVersionCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "HardwareVersion: " +  message);
                getHardwareVersion = message;
                delayThen(() -> getBootMode(callback), 1000);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Hardware Version: " + errorMessage);
            }
        });
    }

    private void getBootMode(RingerCallbacks.DevicesInfoCallback callback) {
        RemoteRinger_GetApplicationBootMode(new RingerCallbacks.BootModeCallback() {


            @Override
            public void onSuccess(int mode) {
                Log.d(TAG, "AppBootMode: " + mode);
                appBootModeID = mode;
                delayThen(() -> getDeviceModel(callback), 1000);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("App Boot Mode: " + errorMessage);
            }
        });
    }

    private void getDeviceModel(RingerCallbacks.DevicesInfoCallback callback) {
        RemoteRinger_GetDeviceModelID(new RingerCallbacks.DeviceModelCallback() {

            @Override
            public void onSuccess(int modelId) {
                Log.d(TAG, "DeviceModelID: " + modelId);
                getDeviceModelId = modelId;


                DeviceInfo deviceInfo = new DeviceInfo(
                        Constant_Variable.serialNumber,
                        Constant_Variable.getHardwareVersion,
                        Constant_Variable.getFirmwareVersion,
                        Constant_Variable.getApplicationBootMode,
                        Constant_Variable.getDeviceModelId
                );

                List<DeviceInfo> deviceInfoList = new ArrayList<>();
                deviceInfoList.add(deviceInfo);

                Log.d(TAG, "Passing device info to callback: " + deviceInfoList);
                callback.onDevicesRetrieved(deviceInfoList);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Device Model ID: " + errorMessage);
            }
        });
    }



    //get device info ends here

    /**
     * **🔹  read the file as InputStream **
     */

    private byte[] readBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    /**
     * Sets the response data for a specific command code.
     *
     * @param commandCode  The command code for which to set the response
     * @param responseData The response data as a byte array
     */
    public void setResponse(byte commandCode, byte[] responseData) {
        String responseString;
        responseMap.put(commandCode, responseData);
        RingerCallbacks.BaseCallback callback = commandCallbackMap.remove(commandCode);
        if (commandCode == 0x1C  ) { // Get current Audio Tone (should be integer)
            responseString = String.valueOf(byteArrayToInt(responseData));
        }
        else if (commandCode == 0x0B) {
            int modelId = convertTwoBytesToInt(responseData); // or byteArrayToInt(responseData) based on your format

            Log.d(TAG, "Device ModelId : " + modelId);

            if (deviceModelCallback != null) {
                runOnMainThread(() -> deviceModelCallback.onSuccess(modelId));
            }
            return;
        }
        else if (commandCode == 0x1E) {
            int volumeLevel = convertOneByteToInt(responseData); // or byteArrayToInt(responseData) based on your format
            int bootMode = convertOneByteToInt(responseData);
            Log.d(TAG, "Volume Level : " + volumeLevel);

            if (volumelevelCallback != null) {
                runOnMainThread(() -> volumelevelCallback.onSuccess(volumeLevel));
            }

//            if (getBootModeCallback != null) {
//                runOnMainThread(() -> getBootModeCallback.onSuccess(bootMode));
//            }
            return;
        }
        else if (commandCode == 0x07) {
           // or byteArrayToInt(responseData) based on your format
            int bootMode = convertOneByteToInt(responseData);
            Log.d(TAG, "Boot Mode : " + bootMode);

            if (getBootModeCallback != null) {
                runOnMainThread(() -> getBootModeCallback.onSuccess(bootMode));
            }

            return;
        }
        else if (commandCode == 0x03 ){
            if (isPrintableAscii(responseData)) {
                responseString = new String(responseData, StandardCharsets.UTF_8);
            } else {
                responseString = String.valueOf(byteArrayToInt(responseData));
            }
            String versionFormat = convertToVersionFormat(responseString);
            if (callback != null) {
                runOnMainThread(( )->callback.onSuccess(versionFormat));
                return;
            }
        }

        //firmware reverse string
        //convertFirmwareVersionFormat
        else if (commandCode == 0x05){
            if (isPrintableAscii(responseData)) {
                responseString = new String(responseData, StandardCharsets.UTF_8);
            } else {
                responseString = String.valueOf(byteArrayToInt(responseData));
            }
            String versionFormat = convertFirmwareVersionFormat(responseString);
            if (callback != null) {
                runOnMainThread(( )->callback.onSuccess(versionFormat));
                return;
            }
        }



        else if (isPrintableAscii(responseData)) {
            responseString = new String(responseData, StandardCharsets.UTF_8);
        } else {
            responseString = String.valueOf(byteArrayToInt(responseData));
        }

        if (errorMap.containsKey(responseString)) {
            String errorMessage = errorMap.get(responseString);
            Log.e(TAG, "Error received: " + responseString + " - " + errorMessage);

            if (callback != null) {
                runOnMainThread(() -> callback.onError(errorMessage));
            }

//            showErrorToUser(errorMessage);
            return;
        }
        // If no error, call onSuccess with response
        if (callback != null) {
            String finalResponseString = responseString;
            runOnMainThread(( )->callback.onSuccess(finalResponseString));
        }
        switch (commandCode) {
            case 0x01:  // Command for get Serial Number
                Constant_Variable.serialNumber = responseString;
                break;
            case 0x03:  // Command for get Hardware version
                processResponse(commandCode, responseString);
                break;
            case 0x05:  // Command for get Firmware version
                convertFirmwareVersionFormat(responseString);
                break;

            case 0x02:  // Command for set Serial Number
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x04:  // Command for set hardware version
                Constant_Variable.getResponseOk = responseString;


                break;
            case 0x23:  // Command for set WiFiSSID
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x24:  //Get WiFiSSID
                Constant_Variable.getWIFISSID = responseString;
                break;
            case 0x25:  // Command for WiFi Password
                Constant_Variable.setStringWiFiPassword = responseString;
                break;
            case 0x10:  // Command for Door Lock ID
                Constant_Variable.setStringDoorLockID = responseString;
                break;
            case 0x0E:  // Get System Mode
                Constant_Variable.getSystemBootMode = responseString;
                break;
            case 0x07:  // GET_APPLICATION_BOOT_MODE
                Constant_Variable.getApplicationBootMode = Integer.parseInt(responseString);

                break;
            case 0x0B:  // Get Device Model ID
                Constant_Variable.getDeviceModelID = String.valueOf(convertTwoBytesToInt(responseData));
                Log.d(TAG, "Device ModelId : " + Constant_Variable.getDeviceModelID);
//                Constant_Variable.getDeviceModelID = responseString;
                break;
            case 0x0F:  // Get Door Lock ID
                Constant_Variable.getDoorLockID = responseString;
                break;
            case 0x1E:  // Get Volume Level
                Constant_Variable.getVolumeLevel = responseString;
                break;
            case 0x13:  // Mobile Auth
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x2E:  //  DoorLock Ble Address
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x37:  // Door Lock Encryption Key
                Constant_Variable.getResponseOk = responseString;
                break;

            case 0x1D:  // Set Audio Tone
                Constant_Variable.getResponseOk = responseString;

                break;
            case 0x2A:  //  Set Next Audio Tone
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x2D:  // Preview Audio Tone
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x1C:  // Get current Audio Tone
//                handleResponseMelodyList(responseData);
//                String melodyTone = handleResponseMelodyList(responseData);

//                if (callback instanceof RingerCallbacks.PlayMelodyCallback) {
//                    runOnMainThread(() -> ((RingerCallbacks.PlayMelodyCallback) callback).onSuccess(getCurrentToneID));
//                }
                break;
            case 0x0D: // Set System Mode
                setResponseOk = responseString;
                break;
            case 0x14:  // Device Onboarding activation
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x09:  //factory reset
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x34:  // WiFi Activation
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x20: //Increment volume
                Constant_Variable.getResponseOk = responseString;
                break;

            case 0x0A: // Device Reboot
                Constant_Variable.getResponseOk = responseString;
                break;

            case 0x2C: // Set Previous Tone
                Constant_Variable.getResponseOk = responseString;
                break;

            case 0x39: // unAuth Command
                break;
            case 0x33: // Device Activation Status
                Constant_Variable.getResponseOk = responseString;
                break;

            case 0x29: // Wifi Connectivity Status
                Constant_Variable.getResponseOk = responseString;
                break;

            case 0x17: // Get Wifi Status
                Constant_Variable.getResponseOk = responseString;
                break;

            case 0x1A:  // Example: Play Audio tone
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x08:  // Example: Set Application boot mode
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x0C:  // Example: Set Device Model ID
                Constant_Variable.getResponseOk = responseString;
                break;
            case 0x1F:  // Set Volume level
                Constant_Variable.setResponseOk = responseString;
                break;
            case 0x21:   // Decrement audio tone
                Constant_Variable.setResponseOk = responseString;
                break;

            case 0x1B:  //Stop Audio Tone
                Constant_Variable.setResponseOk = responseString;
                break;
            default:
                Log.w(TAG, "Unknown commandCode received: " + commandCode);
                break;
        }
    }

    private void processResponse(byte commandCode, String responseString) {
        String versionFormat = convertToVersionFormat(responseString);
        switch (commandCode) {
            case 0x03:  // Command for Device Model ID
                Constant_Variable.getHardwareVersion = versionFormat;
                break;
            case 0x05:  // Command for Tone ID
                Constant_Variable.getFirmwareVersion = versionFormat;
        }
    }

    /**
     * Shows an error message to the user via Toast.
     *
     * @param message The error message to display
     */
    private void showErrorToUser(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, "Error: " + message, Toast.LENGTH_LONG).show()

        );
    }

    /**
     * **🔹  handleResponseMelodyList   **
     */

    private void handleResponseMelodyList(byte[] responseData) {

        String response = String.valueOf(byteArrayToInt(responseData));
        Log.d(TAG, "Get Melody List" + " Response: " + response);
        Constant_Variable.getCurrentToneID = response;
//        // Check if response contains a single character
//        char charValue = response.charAt(0); // Get the first character
//        asciiValue = (int) charValue;// Convert char to ASCII
//        Log.e(TAG," current Tone ID:"+ asciiValue);
    }

    /**
     * Checks if a byte array contains printable ASCII characters.
     *
     * @param bytes The byte array to check
     * @return true if all bytes are printable ASCII characters, false otherwise
     */
    private boolean isPrintableAscii(byte[] bytes) {
        for (byte b : bytes) {
            if ((b < 30 || b > 126) && b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a byte array to an integer value.
     *
     * @param bytes The byte array to convert
     * @return The integer value represented by the byte array
     */
    private int byteArrayToInt(byte[] bytes) {
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    /**
     * **🔹  start the RemoteRinger_StartOtaUpdate   **
     */

    public void RemoteRinger_StartOtaUpdate( Uri fileUri, RingerCallbacks.OtaProgressCallback callback) {

        RemoteRinger_setApplicationBootMode(1, new RingerCallbacks.SetApplicationBootMode() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Application Boot Mode set successfully. Reboot will happen, waiting for reconnect...");

                // ✅ Step 2: Set OTA flag so we can resume OTA from BluetoothManager
                isOtaUpdatePending = true;

            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Failed to set application boot mode: " + errorMessage);
            }
        });

        this.otaProgressCallback = callback;
        isOtaPaused = false;
        bleResponseHandler.setOtaProgressCallback(callback);
        Log.d("DBG", "RemoteRinger_StartOtaUpdate | this.hashCode(): " + this.hashCode());

        byte[] firmwareData;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            firmwareData = readBytesFromInputStream(inputStream);
            bleResponseHandler.setFirmwareData(firmwareData);
        } catch (Exception e) {
            callback.onError("Failed to read firmware file: " + e.getMessage());
            return;
        }

    }



    /**
     * **🔹  firmware data is divide  into the chunks 128 byte    **
     */

    public void writeDataInChunks(byte[] data, RingerCallbacks.OtaProgressCallback callback) {
        Log.d(TAG, "writeDataInChunks() called");

        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
            Log.e(TAG, "No active Bluetooth connection.");
            if (callback != null) callback.onError("No active Bluetooth connection.");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID_Ota)));
        if (service == null) {
            Log.e(TAG, "OTA Service not found.");
            if (callback != null) callback.onError("Service not found.");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(String.valueOf(CHARACTERISTIC_UUID_Ota_512_data)));
        if (characteristic == null) {
            Log.e(TAG, "OTA Characteristic not found.");
            if (callback != null) callback.onError("Characteristic not found.");
            return;
        }

        int maxChunkSize = Math.min(currentMtu, 512); // ✅ Use dynamic MTU-based chunk size
        Log.d(TAG, "Current MTU: " + currentMtu + ", Using Chunk Size: " + maxChunkSize);

        int totalChunks = (int) Math.ceil((double) data.length / maxChunkSize);
        AtomicInteger currentChunk = new AtomicInteger(0);

        Handler handler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "Beginning chunk write: totalChunks=" + totalChunks);

        Runnable writeNextChunk = new Runnable() {
            @Override
            public void run() {
                if (isOtaPaused) {
                    Log.d(TAG, "OTA paused. Stopping chunk write.");
                    pendingOtaChunk = this;
                    return;
                }

                int chunkIndex = currentChunk.get();
                if (chunkIndex >= totalChunks) {
                    Log.i(TAG, "✅ All chunks written successfully!");
                    if (callback != null) callback.onSuccess("OTA update completion");
                    return;
                }

                int start = chunkIndex * maxChunkSize;
                int end = Math.min(start + maxChunkSize, data.length);
                byte[] chunk = Arrays.copyOfRange(data, start, end);

                Log.d(TAG, "Writing chunk " + (chunkIndex + 1) + " of " + totalChunks);

                characteristic.setValue(chunk);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                boolean writeSuccess = bluetoothGatt.writeCharacteristic(characteristic);
                if (!writeSuccess) {
                    Log.e(TAG, "BLE Write Failed for chunk " + (chunkIndex + 1));
                    return;
                }

                int progress = (int) (((double) (chunkIndex + 1) / totalChunks) * 100);
                Log.d(TAG, "Callback onProgress: " + progress + "%");
                if (callback != null) callback.onDownloadProgress(progress);

                currentChunk.incrementAndGet();
                int delayMs = (currentMtu >= 512) ? 10 : 40; // ✅ Adjust delay dynamically
                handler.postDelayed(this, delayMs);
            }
        };

        writeNextChunk.run();
    }


    /**
     * **🔹  send OtaControl Command for 0ne byte request  **
     */

    public void RemoteRinger_sendMobileAuth_HmacResponse(RingerCallbacks.WiFiSSIDCallback callback) {
        byte[] payload = mobileAuthHmacResp.getBytes(); // Placeholder for WiFi SSID request
        byte[] frame = buildCommandFrame(mobileAuthCommand, payload);

        sendBleCommand(frame, new RingerCallbacks.BaseCallback() {
            @Override
            public void onSuccess(String message) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    callback.onSuccess(Constant_Variable.getWIFISSID);
                }, 1000);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        }, "RemoteRinger_GetWiFiSSID");
    }



    public void sendOtaControlCommand(byte[] command) {
        BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
        if (bluetoothGatt == null) {
//            callback.onError("No active Bluetooth connection.");
            return;
        }

        // Get OTA Service
        BluetoothGattService otaService = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID_Ota)));
        if (otaService == null) {
//            callback.onError("OTA Service not found.");
            return;
        }

        // Get OTA Control Characteristic
        BluetoothGattCharacteristic characteristic = otaService.getCharacteristic(CHARACTERISTIC_UUID_Ota_1_byte_Data);
        if (characteristic == null) {
//            callback.onError("OTA Control Characteristic not found.");
            return;
        }

        // Write Command to OTA Control UUID
        characteristic.setValue(command);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        boolean success = bluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
//            callback.onError("Failed to write OTA control command.");
        } else {
//            callback.onSuccess("OTA control command sent.");
        }

    }
        //added new just to precise code
        private byte[] buildCommandFrame(byte commandCode, byte[] payload) {
            byte requestId = RemoteRingerCommand.getNextRequestId(context);
            Log.e(TAG, "Command RequestId: " + requestId);
            return RemoteRingerCommand.createCommandFrame(frameType, commandCode, requestId, Host_ID, payload);
        }

        private void sendBleCommand(byte[] commandFrame, RingerCallbacks.BaseCallback callback, String commandName) {
            BluetoothGatt bluetoothGatt = bluetoothManager.getBluetoothGatt();
            if (bluetoothGatt == null) {
                callback.onError("No active Bluetooth connection.");
                return;
            }

            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(String.valueOf(SERVICE_UUID)));
            if (service == null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> sendBleCommand(commandFrame, callback, commandName), 3000);
                return;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(String.valueOf(CHARACTERISTIC_UUID)));
            if (characteristic == null) {
                callback.onError("Characteristic not found.");
                return;
            }

            characteristic.setValue(commandFrame);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            boolean writeSuccess = bluetoothGatt.writeCharacteristic(characteristic);
            if (!writeSuccess) {
//                callback.onError("Failed to write " + commandName + " command");
                postToMainThread(() -> callback.onError("Failed to write " + commandName + " command"));
            } else {
//                callback.onSuccess(commandName + " Command Sent");
                postToMainThread(() -> callback.onSuccess(commandName + " Command Sent"));

            }
        }


        //helper method fo rrun ui on thread
        private void postToMainThread(Runnable runnable) {
            new Handler(Looper.getMainLooper()).post(runnable);
        }


        //added here for run on ui thread
        private void runOnMainThread(Runnable runnable) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                runnable.run();
            } else {
                new Handler(Looper.getMainLooper()).post(runnable);
            }
        }

    /**
     * **🔹 send  abortOtaUpdate  **
     */

    /*public void abortOtaUpdate() {
        Log.w(TAG, "OTA Update Aborted by User");

        isOtaPaused = true;         // Stop further writes
        pendingOtaChunk = null;     // Clear saved runnable
        otaInProgress = false;      // Optional: track OTA state if needed

        // Optional: Notify callback (UI)
        if (otaProgressCallback != null) {
            otaProgressCallback.onError("OTA update aborted by user.");
        }

        // Optional: Disconnect BLE if desired
        BluetoothGatt gatt = bluetoothManager.getBluetoothGatt();
        if (gatt != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            gatt.disconnect();
            gatt.close();
        }
    }*/

    public void abortOtaUpdate() {
        Log.w(TAG, "OTA Update Aborted by User");

        isOtaPaused = true;
        pendingOtaChunk = null;
        otaInProgress = false;

        if (otaProgressCallback != null) {
            otaProgressCallback.onError("OTA update aborted by user.");
        }

        BluetoothGatt gatt = bluetoothManager.getBluetoothGatt();
        if (gatt != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }

        // ⏳ Add delay before reconnect
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            reconnectToDevice();
        }, 500); // Delay in milliseconds (e.g., 1500 = 1.5 seconds)
    }


    private void reconnectToDevice() {
        bluetoothManager.attemptAutoReconnect();

    }
    public static int convertOneByteToInt(byte[] data) {
        if (data == null || data.length != 1) {
            throw new IllegalArgumentException("Byte array must be exactly 1 byte.");
        }

        return data[0] & 0xFF;
    }

    public void RemoteRinger_playMelodyList(Context context, RingerCallbacks.ToneCallback callback) {
        List<Tone> tones = JsonReader.readJson(context);

        if (tones == null || tones.isEmpty()) {
            Toast.makeText(context, "No data found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Tone");

        // Inflate Custom Layout Correctly
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.popup_listview, null);
        ListView listView = view.findViewById(R.id.popupListView);

        // Set Adapter
        PopupListAdapter adapter = new PopupListAdapter(context, tones);
        listView.setAdapter(adapter);

        // Set Custom View and Create Dialog
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle List Item Click
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Tone selectedTone = tones.get(position);
            int selectedId = selectedTone.getId();
            String selectedName = selectedTone.getName();

            // Show selected ID & Name in a Toast message
            RemoteRinger_SetMelody(selectedId, new RingerCallbacks.PlayMelodyCallback() {
                @Override
                public void onSuccess(String message) {

                }

                @Override
                public void onError(String errorMessage) {

                }
            });


            // Close dialog after selection
            dialog.dismiss();
        });
    }


}



