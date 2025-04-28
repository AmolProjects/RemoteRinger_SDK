package com.remoteringer.handlers;

import static com.remoteringer.Constant.Constant_Variable.SVR_CHR_OTA_CONTROL_CHECKSUM;
import static com.remoteringer.Constant.Constant_Variable.SVR_CHR_OTA_CONTROL_CHECKSUM_DONE;
import static com.remoteringer.Constant.Constant_Variable.SVR_CHR_OTA_CONTROL_DONE;
import static com.remoteringer.Constant.Constant_Variable.actualHmacData;
import static com.remoteringer.Constant.Constant_Variable.mobileAuthHmacResp;
import static com.remoteringer.manager.BluetoothManager.CHARACTERISTIC_UUID_Ota_1_byte_Data;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.remoteringer.Constant.Constant_Variable;
import com.remoteringer.RemoteRingerSDK;
import com.remoteringer.callbacks.RingerCallbacks;
import com.remoteringer.manager.DeviceSettingsManager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class BleResponseHandler {
    private static final String TAG = "BleResponseHandler";
    private final Activity activity;
    private final WeakReference<Activity> activityRef;
    private String lastSentCommand;
    // Global flags to control actions
    private boolean shouldSetSystemMode = false;
    private boolean shouldHandleProvisioning = false;
    private DeviceSettingsManager deviceSettingsManager; // Store instance reference
    private static final UUID WiFi_Status_UUID = UUID.fromString("00003a2a-0000-1000-8000-00805f9b34fb");
    private static final UUID Provisioning_Status_UUID = UUID.fromString("00003a27-0000-1000-8000-00805f9b34fb");
    private static final UUID Onboarding_Status_UUID = UUID.fromString("00003a28-0000-1000-8000-00805f9b34fb");
    private static byte[] firmwareData;
    private static int asciiValue;
    private RingerCallbacks.OtaProgressCallback otaProgressCallback;
    private RingerCallbacks.OtaProgressCallback otaCallback;
    private RingerCallbacks.OnboardingCallback onboardingCallback;
    private RingerCallbacks.ProvisionCallback provisionCallback;
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    /**
     * create a constructor for which pass context and call DeviceSettingsManager class
     */
    public BleResponseHandler(Activity activity, DeviceSettingsManager deviceSettingsManager) {
        this.activity = activity;
        this.activityRef = new WeakReference<>(activity);
        this.deviceSettingsManager = deviceSettingsManager;
    }

    /**
     * set OtaProgressCallback
     */
    public void setOtaProgressCallback(RingerCallbacks.OtaProgressCallback callback) {
        this.otaProgressCallback = callback;
    }
    public void setOnboardingCallback(RingerCallbacks.OnboardingCallback callback) {
        this.onboardingCallback = callback;
    }

    public void setProvisionCallback(RingerCallbacks.ProvisionCallback callback) {
        this.provisionCallback = callback;
    }

    public RingerCallbacks.OtaProgressCallback getOtaCallback() {
        return otaCallback;
    }

    /**
     * set FirmwareData from DeviceSettingsManager class
     */
    public void setFirmwareData(byte[] firmwareData) {
        this.firmwareData = firmwareData;

        if (firmwareData != null) {
            Log.i(TAG, "✅ Firmware data set successfully. Size: " + firmwareData.length + " bytes");

            // Optional: Preview first 10 bytes
            StringBuilder hexPreview = new StringBuilder();
            for (int i = 0; i < Math.min(10, firmwareData.length); i++) {
                hexPreview.append(String.format("%02X ", firmwareData[i]));
            }
            Log.i(TAG, "Firmware Data Preview (first 10 bytes): " + hexPreview.toString().trim());
        } else {
            Log.w(TAG, "⚠️ Firmware data set to null!");
        }
    }

    /**
     * get the FirmwareData
     */

    public byte[] getFirmwareData() {
        if (firmwareData != null) {
            Log.i(TAG, "📥 getFirmwareData() called. Size: " + firmwareData.length + " bytes");
        } else {
            Log.e(TAG, "❌ getFirmwareData() returned null");
        }
        return firmwareData;
    }

    /**
     * It is triggered when a BLE peripheral sends an update for a subscribed characteristic.
     */

    public void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) throws IOException {
        byte[] receivedData = characteristic.getValue();

        if (receivedData == null || receivedData.length == 0) {
            Log.e(TAG, "⚠️ Received empty frame from " + characteristic.getUuid().toString());
            return;
        }

        // Convert data to hex format for better debugging
        String hexData = bytesToHex(receivedData);
        Log.d(TAG, "📥 Response Frame from " + characteristic.getUuid().toString() + ": " + hexData);

        // ✅ Handle Single-Byte Commands
        if (receivedData.length == 1) {
            int command = receivedData[0] & 0xFF;  // Convert to unsigned int
            String uuidStr = characteristic.getUuid().toString();
            Log.d(TAG, "🔹 Single-Byte Command Received: " + command + " from UUID: " + uuidStr);

            // 🚀 Handle WiFi Connection Status UUID (`00003a27)
            //Provision
            if (Provisioning_Status_UUID.equals(characteristic.getUuid())) {
                Log.d(TAG, "📡 Router WiFi Connection Status Changed: " + getProvisioningStatusDescription(command));

                // ✅ If STATE CONNECTED (3) and provisioning is ongoing, trigger System Mode Boot
                if (command == 5) {
                    Log.d(TAG, "✅ Activation Success...");
                    cancelTimeout();
                    shouldSetSystemMode = false; // Reset flag
                    provisionCallback.onSuccess("Provision Done Successfully");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    }, 1000); // Add small delay before calling
                } else if (command == 2) {
                    Log.e(TAG, "❌ Wrong Credentials: ");
                    cancelTimeout();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(activity, "❌ Wrong Credentials", Toast.LENGTH_SHORT).show();
                        provisionCallback.onError("Provision Failed");
                    });

                }


            }

            //ONBOARDING OVER BLE STATUS
            else if (Onboarding_Status_UUID.equals(characteristic.getUuid())) {
                Log.d(TAG, "⚡ONBOARDING OVER BLE STATUS Changed: " + getHostStatusDescription(command));

                // ✅ If ACTIVATION_SUCCESS (5), we can log success or trigger another action
                // ✅ If STATE CONNECTED (3) and provisioning is ongoing, trigger System Mode Boot
                if (command == 6) {
                    cancelTimeout();
                    Log.d(TAG, "✅ STATE CONNECTED received from ONBOARDING OVER BLE STATUS UUID, setting System Mode Boot to 4...");
                    shouldSetSystemMode = false; // Reset flag

                    // Call RemoteRinger_setSystemMode(3)
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        deviceSettingsManager.RemoteRinger_setOnBoardingActivation(7, new RingerCallbacks.OnBoardingActivationCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Log.d(TAG, "✅ OnBoarding Completed Successfully: " + message);
                                //do changes here for callback
                                onboardingCallback.onSuccess("OnBoarding Completed Successfully");

                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "❌ Failed to set System Mode Boot: " + errorMessage);
                                onboardingCallback.onError("Failed to set OnBoarding");
                            }
                        });
                    }, 1000); // Add small delay before calling
                }
                else if (command == 3) {
                    cancelTimeout();
                    onboardingCallback.onError("Activation Timeout");

                }


            }
            // Start Ota implementation

            else if (UUID.fromString(String.valueOf(CHARACTERISTIC_UUID_Ota_1_byte_Data)).equals(characteristic.getUuid())) {
                Log.e(TAG, "OtaRequest Id" + command);
                // OTA_CONTROL_REQUEST_ACK
                if (command == 2) {

                    byte[] firmwareData = getFirmwareData();

                    if (firmwareData != null) {
                        StringBuilder hexPreview = new StringBuilder();
                        for (int i = 0; i < Math.min(10, firmwareData.length); i++) {
                            hexPreview.append(String.format("%02X ", firmwareData[i]));
                        }
                        Log.i(TAG, "Firmware data preview (first 10 bytes): " + hexPreview.toString().trim());
                        Log.d("DBG", "handleCharacteristicChanged | deviceSettingsManager.hashCode(): " + deviceSettingsManager.hashCode());
                        Log.d("OTA", "otaProgressCallback in BleResponseHandler: " + otaProgressCallback);

//                        deviceSettingsManager.writeDataInChunks(firmwareData, 128, otaProgressCallback);
                        deviceSettingsManager.writeDataInChunks(firmwareData, otaProgressCallback);

                    }

                } else {
                    if (command == 3) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Ota Start abort", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                // OTA_CONTROL_File_Upload
                if (command == 11) {
                    // Control CheckSum Request
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        RemoteRingerSDK.sendOtaControlChecksumRequest(SVR_CHR_OTA_CONTROL_CHECKSUM, new RingerCallbacks.OtaCallback() {
                            @Override
                            public void onSuccess(String message) {
                                // Control SHA_256 Request
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {
                                        byte[] checksum = calculateSHA256(firmwareData);
                                        String hexChecksum = bytesToHex(checksum);

                                        Log.e(TAG, "CheckSumString::::::" + hexChecksum);
                                        RemoteRingerSDK.sendOtaActionControlChecksumCheck(checksum, new RingerCallbacks.OtaCallback() {
                                            @Override
                                            public void onSuccess(String message) {
                                                // Control CheckSum Request Done
                                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                    RemoteRingerSDK.sendOtaControlChecksumDone(SVR_CHR_OTA_CONTROL_CHECKSUM_DONE, new RingerCallbacks.OtaCallback() {
                                                        @Override
                                                        public void onSuccess(String message) {

                                                        }

                                                        @Override
                                                        public void onError(String errorMessage) {

                                                        }
                                                    });

                                                }, 1000);


                                            }

                                            @Override
                                            public void onError(String errorMessage) {

                                            }
                                        });
                                    } catch (NoSuchAlgorithmException e) {
                                        throw new RuntimeException(e);
                                    }

                                    // Handle success
                                }, 1000);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // Handle error
                            }
                        });
                    }, 1000); // 1 seconds delay
                }
                // OTA_CONTROL_CHECKSUM_ACK
                if (command == 9) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        RemoteRingerSDK.sendOtaControlDone(SVR_CHR_OTA_CONTROL_DONE, new RingerCallbacks.OtaCallback() {
                            @Override
                            public void onSuccess(String message) {

                            }

                            @Override
                            public void onError(String errorMessage) {

                            }
                        });

                    }, 1000);

                } else {
                    if (command == 6) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Ota control done fail", Toast.LENGTH_SHORT).show();
                        });

                    }
                }
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (command == 5) {
                                Toast.makeText(activity, "Ota Successfully Done", Toast.LENGTH_SHORT).show();
                            }
                        }, 2000); // Delay of 1 second (1000 ms)
                    });
                }
            }

            return;
        }

        // ✅ Handle Multi-Byte Responses (normal BLE messages)
        if (receivedData.length < 6) {
            Log.e(TAG, "⚠️ Invalid frame: Too short.");
            return;
        }

        // Extract Command Code (Index 2)
        byte commandCode = receivedData[2];
        // Extract CRC (Last two bytes before END_BYTE)
        int crcHigh = receivedData[receivedData.length - 3] & 0xFF;
        int crcLow = receivedData[receivedData.length - 2] & 0xFF;
        int receivedCRC = (crcHigh << 8) | crcLow;

        // Validate CRC
        byte[] dataForCRC = Arrays.copyOf(receivedData, receivedData.length - 3);
        int calculatedCRC = calculateCrc(dataForCRC);

        if (calculatedCRC != receivedCRC) {
            Log.e(TAG, "❌ CRC is INVALID");
            return;
        }

        Log.d(TAG, "✅ CRC is VALID");

        // Extract Data Length
        int dataLength = receivedData[4] & 0xFF;
        Log.d(TAG, "📏 Data Length: " + dataLength);

        if (receivedData.length < 6 + dataLength) {
            Log.e(TAG, "⚠️ Invalid frame: Length mismatch.");
            return;
        }

        // Extract Response Data
        byte[] responseData = Arrays.copyOfRange(receivedData, 5, 5 + dataLength);

        // Route Response Based on Command Code
        handleCommandResponse(commandCode, responseData);
    }

    // Perform the Provisioning completed successfully
    private void performPostActivationSteps() {
        Log.d(TAG, "✅ Provisioning completed successfully! Performing post-activation steps...");

        // Notify user of successful provisioning
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(activity, "Device successfully provisioned!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Converts a byte array to a hex string for better debugging.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Maps single-byte command codes to descriptions.
     */
    private String getCommandDescription(int command) {
        switch (command) {
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
                return "UNKNOWN COMMAND (" + command + ")";
        }
    }

    /**
     * Maps single-byte and get ProvisioningStatusDescription.
     */

    private String getProvisioningStatusDescription(int status) {
        switch (status) {
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
                return "ACTIVATION_SUCCESS";
            default:
                return "UNKNOWN ACTIVATION STATUS (" + status + ")";
        }
    }

    /**
     * Maps single-byte and get HostStatusDescription.
     */

    private String getHostStatusDescription(int status) {
        switch (status) {
            case 0:
                return "STATE IDLE";
            case 1:
                return "STATE CONNECTED";
            case 2:
                return "STATE DISCONNECTED";
            default:
                return "UNKNOWN STATE (" + status + ")";
        }
    }

    /**
     * Match the command code and responseData type using handleCommandResponse method
     */

    private void handleCommandResponse(byte commandCode, byte[] responseData) {
        String response = new String(responseData).trim();  // Convert bytes to string
        Activity activity = activityRef.get();
        if (activity == null) {
            Log.e("BleResponseHandler", "Activity is null, cannot show Toast");
            return;
        }

        switch (commandCode) {
            case 0x10:
                handleResponse("Set Door Lock ID", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x02:
                handleResponse("Set Serial Number", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x05:
                lastSentCommand = "GET_FIRMWARE_VERSION";
                handleResponse("Get Firmware Version", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x20:
                handleResponse("Increment Volume", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x0A:
                handleResponse("Device Reboot", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x09:
                handleResponse("Factory Reset", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x13:// Mobile Authentication Mode
                handleResponse("Mobile Authentication", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                //added for new hmac
                byte auth_cmd = responseData[0];
                if(auth_cmd == 0x00){
                    byte reqId = responseData[1];

                    // Check for enough data
                    if (responseData.length <= 10) {
                        throw new IllegalArgumentException("receivedBytes does not contain enough data");
                    }

                    // Extract rrRandomNo from bytes [7...10]
                    long rrRandomNo = 0;
                    for (int i = 0; i < 4; i++) {
                        rrRandomNo |= ((long) responseData[2 + i] & 0xFF) << (8 * i);
                    }

                    Log.d("MobileAuth", "rrRandomNo: " + rrRandomNo);
                    String extractedValue = String.valueOf(rrRandomNo);

                    // Generate challenge message
                    String challengeMessage = formatChallengeMessage(Constant_Variable.locallyGeneratedRandomNo, rrRandomNo);
                    Log.d("MobileAuth", "challengeMessage: " + challengeMessage);

                    // Generate HMAC SHA256
                    byte[] hmacData = hmacSHA256(getSecretKey(), challengeMessage);

                    // Compose dataR = "01" + reqId (as hex) + hmac hex string
                    StringBuilder dataRBuilder = new StringBuilder();
                    dataRBuilder.append("01");
                    dataRBuilder.append(String.format("%02X", reqId));
                    for (byte b : hmacData) {
                        dataRBuilder.append(String.format("%02X", b));
                    }

//                    String dataR = dataRBuilder.toString();
                    mobileAuthHmacResp = dataRBuilder.toString();

                    byte[] actualData = hexStringToBytes(mobileAuthHmacResp);
                    actualHmacData = hexStringToBytes(mobileAuthHmacResp);

                    Log.d("Actual data to be send now",mobileAuthHmacResp);

                    Log.d("Actual data to be send now", Arrays.toString(actualData));
                    deviceSettingsManager.RemoteRinger_SendHmacResponse(new RingerCallbacks.AuthenticationCallback() {
                        @Override
                        public void onSuccess(String message) {

                        }

                        @Override
                        public void onError(String errorMessage) {

                        }
                    });

//                    Log.d("Actual data to be send back " + dataR);


                }


//                byte commandCodeToSend = 0x13; // 19 decimal
//                byte lengthOfData = (byte) actualData.length;
//
//                List<Byte> commandFrameForCrc = getCommandFrameForCrc(commandCodeToSend);
//
//                // Replace command payload (after index 21)
//                for (int i = 0; i < actualData.length; i++) {
//                    if (21 + i < commandFrameForCrc.size()) {
//                        commandFrameForCrc.set(21 + i, actualData[i]);
//                    } else {
//                        commandFrameForCrc.add(actualData[i]);
//                    }
//                }
//
//                // Replace Host ID values (assuming hostIdValue is a List<Byte>)
//                for (int i = 0; i < hostIdValue.size(); i++) {
//                    commandFrameForCrc.set(4 + i, hostIdValue.get(i));
//                }
//
//                // Send over BLE
//                postCommandFrameToBLE(commandFrameForCrc);
//
//                cameFrom = COMMAND_MOBILE_AUTHENTICATION_HMAC_RESPONSE;

                break;
            case 0x0D: // Set System Mode
                handleResponse("Set System Mode", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x0E: // Get System Mode
                handleResponse("Get System Mode", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x23: // Set WiFi SSID
                handleResponse("Set WiFi SSID", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x2E: // Set DoorLockBle  Address
                handleResponse("Set Door Lock Ble Mac Address", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x01:  // Get Serial Number
                lastSentCommand = "GET_SERIAL_NUMBER"; // Added this line
                handleResponse("Get Serial Number", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x03:
//                lastSentCommand = "GET_SERIAL_NUMBER";
                handleResponse("Get Hardware Version", responseData);
                Log.d(TAG, Arrays.toString(responseData));
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x24:
                handleResponse("Get Wifi SSID", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x25:
                handleResponse("Set Wifi Password", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x34:
                handleResponse("Wifi Activation", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x37:
                handleResponse("Set Door Encryption Key", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x33:
                handleResponse("Get Device Activation Status", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x07:
                lastSentCommand = "GET_APPLICATION_BOOT_MODE";
                handleResponse("Get Application Boot Mode", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x0B:
                lastSentCommand = "GET_DEVICE_MODEL_ID";
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x0F:
                handleResponse("Get Door Lock Id", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x1E:
                handleResponse("Get Volume Level", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x29:
                handleResponse("Get Wifi Connectivity", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x17:
                handleResponse("Get Wifi Status", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x14:
                handleResponse("Device OnBoarding Activation", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x1A:
                handleResponse("Play Audio Tone", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x08:
                handleResponse("Set Application Boot  Mode", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x0C:
                handleResponse("Set Device Model Id", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x04:
                handleResponse("Set Hardware Version", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x1D:
                handleResponse("Set Audio Tone Id", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x1F:
                handleResponse("Set Volume Level", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x28:
                handleResponse("Set Wifi Connectivity", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x1B:
                handleResponse("Stop Audio Tone", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x21:
                handleResponse("Decrement Volume Level", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x39:
                handleResponse("UnAuth Mobile Number", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x2A:
                handleResponse("Set Next Audio Tone", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x2C:
                handleResponse("Set Previous Tone", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x1C:
                handleResponseMelodyList(responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            case 0x2D:
                handleResponse("Preview Audio Tone Id", responseData);
                deviceSettingsManager.setResponse(commandCode, responseData);
                break;
            default:
                Log.e(TAG, "Unknown Command Code: " + String.format("0x%02X", commandCode));
        }
    }


    /**
     * handle the  handleResponseMelodyList
     */
    private void handleResponseMelodyList(byte[] responseData) {

        String response = String.valueOf(byteArrayToInt(responseData));
        Log.d(TAG, "Get Melody List" + " Response: " + response);
        // Check if response contains a single character
        char charValue = response.charAt(0); // Get the first character
        asciiValue = (int) charValue;// Convert char to ASCII
        Log.e(TAG, " current Tone ID:" + asciiValue);
    }

    /**
     * handle the  handleResponse
     */

    private void handleResponse(String type, byte[] responseData) {
        String response;
        if (isPrintableAscii(responseData)) {
            response = new String(responseData, StandardCharsets.UTF_8);
        } else {
            response = String.valueOf(byteArrayToInt(responseData));
        }

        Log.d(TAG, type + " Response: " + response);
    }

    /**
     * return  the  byte to Ascii value
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
     * return  the  byte to Array int
     */
    private int byteArrayToInt(byte[] bytes) {
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    /**
     * calculate the Crc Value
     */
    private int calculateCrc(byte[] data) {
        int crc = 0x0000;
        for (byte b : data) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return crc;
    }

    // Compute SHA-256 checksum of the firmware file
    private byte[] calculateSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(data);
        return digest.digest();
    }


    //added for timeout for OnBoarding
    public void startOnboardingTimeout() {
        timeoutRunnable = () -> {
            Log.e(TAG, "❌ Timeout: Device not responding for onboarding...");
            onboardingCallback.onError("Device not Responding");
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000); // 30 seconds timeout
    }

    public void startProvisionTimeout() {
        timeoutRunnable = () -> {
            Log.e(TAG, "❌ Timeout: Device not responding for Provision...");
            provisionCallback.onError("Device not Responding");
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000); // 30 seconds timeout
    }

    public void cancelTimeout() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }



    //added for new hmac
    public String formatChallengeMessage(long mobileNonce, long nonce) {
        String serialNumber = Constant_Variable.serialNumber;
        return mobileNonce + "|" + nonce + "|" + serialNumber;
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


    public byte[] getSecretKey() {
        return new byte[32]; // 32 zero bytes
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
}