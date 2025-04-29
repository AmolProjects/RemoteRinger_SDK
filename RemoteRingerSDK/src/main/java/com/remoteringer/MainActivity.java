package com.remoteringer;

import static com.remoteringer.Constant.Constant_Variable.WifiSsidPassword;
import static com.remoteringer.Constant.Constant_Variable.doorLockId;
import static com.remoteringer.Constant.Constant_Variable.encryptionKey;
import static com.remoteringer.Constant.Constant_Variable.onBoardingActivationMode;
import static com.remoteringer.Constant.Constant_Variable.onBoardingSuccess;
import static com.remoteringer.Constant.Constant_Variable.patch;
import static com.remoteringer.Constant.Constant_Variable.serialNumber;
import static com.remoteringer.Constant.Constant_Variable.setApplicationBootMode;
import static com.remoteringer.Constant.Constant_Variable.setSystemMode;
import static com.remoteringer.Constant.Constant_Variable.setSystemProvisionMode;
import static com.remoteringer.Constant.Constant_Variable.setWifiActivation;
import static com.remoteringer.Constant.Constant_Variable.setWifiSsidName;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.remoteringer.Constant.DeviceInfo;
import com.remoteringer.adapter.DeviceListAdapter;
import com.remoteringer.callbacks.RingerCallbacks;
import com.remoteringer.manager.RingerSdkManager_ApiKey;

import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_CODE_SELECT_FILE = 1;
    RingerSdkManager_ApiKey sdkManager_apiKey;
    RemoteRingerSDK remoteRingerSDK;
    Activity activity;
    int setSystemMode1 = 1;
    EditText pickFile, setSerialNumber, edtsetWifiSsid, edtsetWifiPassword, edtsetDoorLockId, edtsetDoorLockBleAddress,
            edtsetDoorLockSecretKey, edtsetAudioTone, edtsetVolumeLevel, edtpreviewaudiotone, edtMajor, edtMinor, edtPatch, edtDeviceModelID;
    Button btnScan, btnConnect, btnGetSerialNumber, btnSetSerialNumber, btnSetSystemMode, btnGetSystemMode, GETHardwareVersion, btnSETHardwareVersion, GETFirmwareVersion, btnSETFirmwareVersion,
            GET_APPLICATION_BOOT_MODE, SET_APPLICATION_BOOT_MODE, FACTORY_RESET, DEVICE_REBOOT,
            GET_DEVICE_MODEL_ID, SET_DEVICE_MODEL_ID, GET_DOOR_LOCK_ID, SET_DOOR_LOCK_ID, SET_FACTORY_DEFAULT_TONE_ID,
            SET_FACTORY_DEFAULT_TONE_VOLUME_LEVEL, TEST_AUDIO_TONE, PLAY_AUDIO_TONE, NEXT_AUDIO_TONE, PREV_AUDIO_TONE,
            STOP_AUDIO_TONE, GET_AUDIO_TONE, SET_AUDIO_TONE, GET_VOLUME_LEVEL, SET_VOLUME_LEVEL, INC_VOLUME_LEVEL, btNextAudioTone, bt_PreviousAudioTone,
            DEC_VOLUME_LEVEL, btCurrentToneID, bt_PreviewAudioTone, btnDisConnect, btnMobileAuth, btnMobileUnAuth, btnSetWifiSsid, btnGetWifiSsid, btnSetWifiPass, btnWifiActivation, btnWifiProvisionMode, btnsetDoorLockId, btnsetDoorLockBleAddress, btnsetDoorLockSecretKey, btnsetDoorLockBleActivation, btnOnBoardingSuccess, btnOperationalMode,
            btngrpWifiProvision, Play_Melody_list,Play_Melody, btngrpOnBoarding, GET_DEVICE_INFO, btnSetSystemMode1, btStartOtaUpdate,btnAbortOtaUpdate;
    ProgressBar otaProgressBar;
    TextView otaProgressText;
    private DeviceListAdapter deviceListAdapter;
    private ListView listViewDevices;
    private String selectedDeviceMac, formattedMac, bleAddress;
    private Uri selectedFileUri; // store selected file uri
    private Spinner spinnerDoorLockType;
    private int onBoardingType;

    // Data for Spinner
    String[] doorLockTypes = {
            "Select Door Lock Type",   // Default (invalid) selection
            "Door_Lock_Type_BLE",
            "Door_Lock_Type_WiFi",
            "Door_Lock_Type_BLE_AND_WiFi",
            "Door_Lock_Type_BLE_Or_WiFi"
    };
    // Map to hold label -> value
    HashMap<String, Integer> doorLockTypeMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // init the object
        initObject();


        // Initialize the map
        doorLockTypeMap.put("Door_Lock_Type_BLE", 1);
        doorLockTypeMap.put("Door_Lock_Type_WiFi", 2);
        doorLockTypeMap.put("Door_Lock_Type_BLE_AND_WiFi", 3);
        doorLockTypeMap.put("Door_Lock_Type_BLE_Or_WiFi", 4);

        // Set up the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, doorLockTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoorLockType.setAdapter(adapter);

        /**
         * **🔹  initialize And ManageSession for ble device **
         */
        remoteRingerSDK.initializeAndManageSession(new RingerCallbacks.SessionCallback() {
            @Override
            public void onLastConnectedDevices(List<String> devices) {
                Log.d(TAG, "Last connected devices: " + devices);
            }

            @Override
            public void onDeviceConnected(String connectedDevice) {
                Log.d(TAG, "Currently connected to: " + connectedDevice);
                Toast.makeText(MainActivity.this, "Connected to: " + connectedDevice, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error: " + errorMessage);
                Toast.makeText(MainActivity.this, " " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });


        // :::::  Handle list item click to select a device for connection ::::
        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            RingerCallbacks.NearbyDevice selectedDevice = deviceListAdapter.getItem(position);
            if (selectedDevice != null) {
                selectedDeviceMac = selectedDevice.getAddress();
                bleAddress = selectedDeviceMac;
                formattedMac = bleAddress.replace(":", ""); // Removes all colons
                Toast.makeText(MainActivity.this, "Selected: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // :::::::::::  Scan Device ::::::::::::
        btnScan.setOnClickListener(v -> {scanForDevices();
        });
        // :::::::::::  set Application Boot Mode ::::::::::::
        SET_APPLICATION_BOOT_MODE.setOnClickListener(v -> setApplicationBootMode());
        // :::::::::::  Select file ::::::::::::
        pickFile.setOnClickListener(view -> openFileManager());
        // :::::::::::  connect Device ::::::::::::
        btnConnect.setOnClickListener(v -> connectToDevice());
        // :::::::::::  Mobile Auth ::::::::::::
        btnMobileAuth.setOnClickListener(v -> mobileAuth());
        // :::::::::::  set system mode ::::::::::::
        btnSetSystemMode.setOnClickListener(v -> setSystemMode());
        //set factory out
        btnSetSystemMode1.setOnClickListener(v -> setSystemMode1());

        // :::::::::::  set Door Lock Ble Address ::::::::::::
        btnsetDoorLockBleAddress.setOnClickListener(view -> setDoorLockBleAddress());
        // :::::::::::  set door lock id ::::::::::::
        btnsetDoorLockId.setOnClickListener(view -> setDoorLockId());
        // :::::::::::  get System Mode ::::::::::::
        btnGetSystemMode.setOnClickListener(v -> getSystemMode());
        // :::::::::::  set Wifi Ssid ::::::::::::
        btnSetWifiSsid.setOnClickListener(v -> setWifiSsid());
        // :::::::::::  get Wifi Ssid ::::::::::::
        btnGetWifiSsid.setOnClickListener(v -> getWifiSsid());
        // :::::::::::  set Wifi Password ::::::::::::
        btnSetWifiPass.setOnClickListener(v -> setWifiPass());
        // :::::::::::   Wifi Activation ::::::::::::
        btnWifiActivation.setOnClickListener(v -> setWifiActivation());
        // :::::::::::   Wifi Activation ::::::::::::
        btnMobileUnAuth.setOnClickListener(v -> mobileUnAuth());
        // :::::::::::   set setSystemProvisionModeBoarding ::::::::::::
        btnWifiProvisionMode.setOnClickListener(v -> setSystemProvisionModeBoarding());
        // :::::::::::   set Serial Number:::::::::::
        btnSetSerialNumber.setOnClickListener(v -> setSerialNumber());
        // :::::::::::   Get Serial Number:::::::::::
        btnGetSerialNumber.setOnClickListener(v -> getSerialNumber());
        // :::::::::::   set Hardware version:::::::::::
        btnSETHardwareVersion.setOnClickListener(v -> setHardwareVersion());
        // :::::::::::   get Hardware version:::::::::::
        GETHardwareVersion.setOnClickListener(v -> getHardwareVersion());
        // :::::::::::   set door lock encryption key:::::::::::
        btnsetDoorLockSecretKey.setOnClickListener(v -> setDoorLockSecretKey());
        // :::::::::::   Activate onBoarding:::::::::::
        btnsetDoorLockBleActivation.setOnClickListener(v -> setActivateOnBoarding());
        // :::::::::::   Activate onBoarding Success:::::::::::
        btnOnBoardingSuccess.setOnClickListener(v -> setOnBoardingSuccess());
        // :::::::::::  get Firmware Version ::::::::::::
        GETFirmwareVersion.setOnClickListener(v -> getFirmwareVersion());
        // :::::::::::  get Application Boot Mode ::::::::::::
        GET_APPLICATION_BOOT_MODE.setOnClickListener(v -> getApplicationBootMode());
        // :::::::::::  Factory Reset ::::::::::::
        FACTORY_RESET.setOnClickListener(v -> FactoryReset());
        // :::::::::::  Device Reboot ::::::::::::
        DEVICE_REBOOT.setOnClickListener(v -> DeviceReboot());
        // :::::::::::  get Device Model ::::::::::::
        GET_DEVICE_MODEL_ID.setOnClickListener(v -> getDeviceModelId());
        // :::::::::::  set Device Model ::::::::::::
        SET_DEVICE_MODEL_ID.setOnClickListener(v -> setDeviceModelId());
        // :::::::::::  get Door lock id ::::::::::::
        GET_DOOR_LOCK_ID.setOnClickListener(v -> getDoorLockId());
        // :::::::::::  stop Audio Tone ::::::::::::
        STOP_AUDIO_TONE.setOnClickListener(v -> stopAudioTone());
        // :::::::::::  set Audio Tone ::::::::::::
        SET_AUDIO_TONE.setOnClickListener(v -> setAudioTone());
        // :::::::::::  get volume level ::::::::::::
        GET_VOLUME_LEVEL.setOnClickListener(v -> getVolumeLevel());
        // :::::::::::  set Volume Level ::::::::::::
        SET_VOLUME_LEVEL.setOnClickListener(v -> setVolumeLevel());
        // :::::::::::  set Volume Level ::::::::::::
        INC_VOLUME_LEVEL.setOnClickListener(v -> incrementVolumeLevel());
        // :::::::::::  next Audio Tone ::::::::::::
        NEXT_AUDIO_TONE.setOnClickListener(v -> nextAudioTones());
        // :::::::::::  Previous Audio Tone ::::::::::::
        bt_PreviousAudioTone.setOnClickListener(v -> previousAudioTone());
        // :::::::::::  Preview Audio Tone ::::::::::::
        bt_PreviewAudioTone.setOnClickListener(v -> previewAudioTone());
        // :::::::::::  Current Audio Tone ::::::::::::
        btCurrentToneID.setOnClickListener(v -> currentAudioToneId());
        // :::::::::::  Play Melody List ::::::::::::
       // Play_Melody_list.setOnClickListener(v -> showToneList(MainActivity.this));
        Play_Melody_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                remoteRingerSDK.RemoteRinger_playMelodyList(MainActivity.this, new RingerCallbacks.ToneCallback() {
                    @Override
                    public void onSuccess(String message) {

                    }

                    @Override
                    public void onError(String errorMessage) {

                    }
                });
            }
        });
        // :::::::::::  Decrement volume level ::::::::::::
        DEC_VOLUME_LEVEL.setOnClickListener(v -> decrementVolumeLevel());
        // :::::::::::  Play_Melody ::::::::::::
        Play_Melody.setOnClickListener(v -> playMelody());

        btngrpWifiProvision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String wifiName1 = edtsetWifiSsid.getText().toString();
                String password1 = edtsetWifiPassword.getText().toString();
                    remoteRingerSDK.RemoteRinger_Provision(wifiName1, password1, new RingerCallbacks.ProvisionCallback() {
                        @Override
                        public void onSuccess(String message) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Provision State Main: " +message, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, message);
                            });
                        }

                        @Override
                        public void onError(String errorMessage) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,  errorMessage, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, errorMessage);
                            });

                        }
                    });
                }

        });
//        btngrpOnBoarding.setOnClickListener(v -> OnBoarding());
        btnDisConnect.setOnClickListener(v -> disconnectDevice());
        GET_DEVICE_INFO.setOnClickListener(v -> getDeviceInfo());
        // :::::::::::  startOtaUpdate::::::::::::
        btStartOtaUpdate.setOnClickListener(v -> {
            sendOtaControlData();
        });
        btnAbortOtaUpdate.setOnClickListener(v -> RemoteRinger_AbortOtaUpdate());

        btngrpOnBoarding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedLabel = spinnerDoorLockType.getSelectedItemPosition();

                if (selectedLabel == 0) {
                    // First item is default hint, invalid selection
                    Toast.makeText(MainActivity.this, "Please select a valid Door Lock Type", Toast.LENGTH_SHORT).show();
                } else {
                    // Valid selection: map to integer value 1,2,3,4
                    onBoardingType = selectedLabel; // because options[1] = 1, etc.
//                    Constant_Variable.onboardingType = selectedValue;  // Store globally
                    // Proceed with logic
                    OnBoarding(onBoardingType);
                }
            }
        });
    }

    private void initObject() {
        requestBluetoothPermissions();
        listViewDevices = findViewById(R.id.listViewDevices);
        btnScan = findViewById(R.id.btnScan);
        Play_Melody_list = findViewById(R.id.Play_Melody_list);
        btnMobileAuth = findViewById(R.id.btnMobileAuth);
        btnMobileUnAuth = findViewById(R.id.btnMobileUnAuth);
        btnSetWifiSsid = findViewById(R.id.btnSetWifiSsid);
        btnGetWifiSsid = findViewById(R.id.btnGetWifiSsid);
        btnSetWifiPass = findViewById(R.id.btnSetWifiPass);
        Play_Melody=findViewById(R.id.Play_Melody);
        btnWifiActivation = findViewById(R.id.btnWifiActivation);
        btnWifiProvisionMode = findViewById(R.id.btnWifiProvisionMode);
        btnsetDoorLockId = findViewById(R.id.btnsetDoorLockId);
        btnsetDoorLockBleAddress = findViewById(R.id.btnsetDoorLockBleAddress);
        btnsetDoorLockSecretKey = findViewById(R.id.btnsetDoorLockSecretKey);
        btnsetDoorLockBleActivation = findViewById(R.id.btnsetDoorLockBleActivation);
        btnOnBoardingSuccess = findViewById(R.id.btnOnBoardingSuccess);
        btnOperationalMode = findViewById(R.id.btnOperationalMode);
        btnSetSystemMode = findViewById(R.id.btnSetSystemMode);
        btnSetSystemMode1 = findViewById(R.id.btnSetSystemMode1);
        btStartOtaUpdate = findViewById(R.id.btStartOtaUpdate);
        btnDisConnect = findViewById(R.id.btnDisConnect);
        btnGetSystemMode = findViewById(R.id.btnGetSystemMode);
        btnConnect = findViewById(R.id.btnConnect);
        btnGetSerialNumber = findViewById(R.id.btnGetSerialNumber);
        btnSetSerialNumber = findViewById(R.id.btnSetSerialNumber);
        GETHardwareVersion = findViewById(R.id.GETHardwareVersion);
        btnSETHardwareVersion = findViewById(R.id.btnSETHardwareVersion);
        GETFirmwareVersion = findViewById(R.id.GETFirmwareVersion);
        GET_APPLICATION_BOOT_MODE = findViewById(R.id.GET_APPLICATION_BOOT_MODE);
        SET_APPLICATION_BOOT_MODE = findViewById(R.id.SET_APPLICATION_BOOT_MODE);
        FACTORY_RESET = findViewById(R.id.FACTORY_RESET);
        DEVICE_REBOOT = findViewById(R.id.DEVICE_REBOOT);
        GET_DEVICE_MODEL_ID = findViewById(R.id.GET_DEVICE_MODEL_ID);
        SET_DEVICE_MODEL_ID = findViewById(R.id.SET_DEVICE_MODEL_ID);
        GET_DOOR_LOCK_ID = findViewById(R.id.GET_DOOR_LOCK_ID);
        SET_FACTORY_DEFAULT_TONE_ID = findViewById(R.id.SET_FACTORY_DEFAULT_TONE_ID);
        SET_FACTORY_DEFAULT_TONE_VOLUME_LEVEL = findViewById(R.id.SET_FACTORY_DEFAULT_TONE_VOLUME_LEVEL);
        TEST_AUDIO_TONE = findViewById(R.id.TEST_AUDIO_TONE);
        PLAY_AUDIO_TONE = findViewById(R.id.PLAY_AUDIO_TONE);
        NEXT_AUDIO_TONE = findViewById(R.id.btNextAudioTone);
        PREV_AUDIO_TONE = findViewById(R.id.PREV_AUDIO_TONE);
        STOP_AUDIO_TONE = findViewById(R.id.STOP_AUDIO_TONE);
        GET_AUDIO_TONE = findViewById(R.id.GET_AUDIO_TONE);
        SET_AUDIO_TONE = findViewById(R.id.SET_AUDIO_TONE);
        GET_VOLUME_LEVEL = findViewById(R.id.GET_VOLUME_LEVEL);
        SET_VOLUME_LEVEL = findViewById(R.id.SET_VOLUME_LEVEL);
        INC_VOLUME_LEVEL = findViewById(R.id.INC_VOLUME_LEVEL);
        bt_PreviousAudioTone = findViewById(R.id.bt_PreviousAudioTone);
        btCurrentToneID = findViewById(R.id.btCurrentToneID);
        bt_PreviewAudioTone = findViewById(R.id.bt_PreviewAudioTone);
        DEC_VOLUME_LEVEL = findViewById(R.id.DEC_VOLUME_LEVEL);
        btngrpWifiProvision = findViewById(R.id.btngrpWifiProvision);
        btngrpOnBoarding = findViewById(R.id.btngrpOnBoarding);
        GET_DEVICE_INFO = findViewById(R.id.GET_DEVICE_INFO);
        pickFile = findViewById(R.id.pickFile);
        setSerialNumber = findViewById(R.id.setSerialNumber);
        edtsetWifiSsid = findViewById(R.id.edtsetWifiSsid);
        edtsetWifiPassword = findViewById(R.id.edtsetWifiPassword);
        edtsetDoorLockId = findViewById(R.id.edtsetDoorLockId);
        edtsetDoorLockBleAddress = findViewById(R.id.edtsetDoorLockBleAddress);
        edtsetDoorLockSecretKey = findViewById(R.id.edtsetDoorLockSecretKey);
        edtsetAudioTone = findViewById(R.id.edtsetAudioTone);
        edtsetVolumeLevel = findViewById(R.id.edtsetVolumeLevel);
        edtpreviewaudiotone = findViewById(R.id.edtpreviewaudiotone);
        edtMajor = findViewById(R.id.edtMajor);
        edtMinor = findViewById(R.id.edtMinor);
        edtPatch = findViewById(R.id.edtPatch);
        edtDeviceModelID = findViewById(R.id.edtDeviceModelID);
        otaProgressBar = findViewById(R.id.otaProgressBar);
        otaProgressText = findViewById(R.id.otaProgressText);
        btnAbortOtaUpdate=findViewById(R.id.btnAbortOtaUpdate);
        spinnerDoorLockType = findViewById(R.id.spinnerDoorLockType);

        // ✅ Initialize SDK with API Key
        try {
            sdkManager_apiKey = RingerSdkManager_ApiKey.initialize(this, "1234ABCD877");
            remoteRingerSDK = new RemoteRingerSDK(this, "1234ABCD877");


        } catch (IllegalStateException e) {
            Log.e(TAG, "SDK Initialization Failed: " + e.getMessage());
            Toast.makeText(this, "Invalid API Key. Please check your credentials.", Toast.LENGTH_LONG).show();
            return;
        }

        // Set up device list adapter
        deviceListAdapter = new DeviceListAdapter(this);
        listViewDevices.setAdapter(deviceListAdapter);
    }

    /**
     * **🔹  Show Tone list **
     */

   /* private void showToneList(Context context) {
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
            remoteRingerSDK.RemoteRinger_SetMelody(selectedId, new RingerCallbacks.PlayMelodyCallback() {
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
    }*/


    //added new
    private void sendOtaControlData() {
        remoteRingerSDK.RemoteRinger_StartOtaUpdate( selectedFileUri, new RingerCallbacks.OtaProgressCallback() {
            @Override
            public void onDownloadProgress(int progress) {
                Log.d(TAG, "OTA Update Progress MainActivity: " + progress + "%");
                runOnUiThread(() -> {
                    otaProgressBar.setVisibility(View.VISIBLE);
                    otaProgressBar.setProgress(progress);
                    otaProgressText.setText("OTA Progress: " + progress + "%");
                });
            }

            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "OTA Update Progress MainActivity onSuccess: " + message);

            }

            @Override
            public void onError(String errorMessage) {
                Log.d(TAG, "OTA Update Progress MainActivity errorMessage: " + errorMessage);

            }
        });
    }

    /**
     * **🔹 File picker**
     */

    private void openFileManager() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String fileName = getFileNameFromUri(uri);

            if (fileName != null && fileName.endsWith(".bin")) {
                selectedFileUri = uri; // ✅ Set only when valid
                pickFile.setText(fileName);
            } else {
                selectedFileUri = null; // ❌ Prevent invalid files from being stored
                pickFile.setText("Invalid file type. Please select a .bin file.");
            }
        }
    }


    // Helper method to get file name from Uri with OpenableColumns
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            try {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return fileName;
    }

    /**
     * **🔹 requestBluetoothPermissions devices**
     */
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ requires extra permissions
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * **🔹 Scans for nearby devices**
     */
    private void scanForDevices() {
        remoteRingerSDK.RemoteRinger_GetNearbyDevices(new RingerCallbacks.NearbyDevicesCallback() {
            @Override
            public void onDevicesRetrieved(List<RingerCallbacks.NearbyDevice> devices) {
                runOnUiThread(() -> {
                    deviceListAdapter.setDevices(devices);
                    Log.d(TAG, "Found " + devices.size() + " devices.");
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Scan Error::: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Scan Error: " + errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 connectToDevice devices**
     */
    private void connectToDevice() {

        remoteRingerSDK.RemoteRinger_ConnectDevice(selectedDeviceMac, new RingerCallbacks.RingerDeviceCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,  message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Connection Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Connection Failed: " + errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 Mobile Auth**
     */

    private void mobileAuth() {
        Log.e(TAG, "Input Serial Number:::::::" + serialNumber);
        remoteRingerSDK.RemoteRinger_Authentication(serialNumber, new RingerCallbacks.AuthenticationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
               Toast.makeText(MainActivity.this, "Mobile Auth error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * **🔹 set Door Lock Ble Address**
     */
    private void setDoorLockBleAddress() {
        Log.e(TAG, "Input BleDoorLockAddress::::::::::::::" + formattedMac);
        String insertBleAddress = edtsetDoorLockBleAddress.getText().toString();
        remoteRingerSDK.RemoteRinger_setDoorLockBleAddress(insertBleAddress, new RingerCallbacks.DoorLockBleAddressCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Door Lock BLE Success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Door Lock BLE Error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 set DoorLockId**
     */
    private void setDoorLockId() {
        Log.e(TAG, "Input DoorLockId:::::::" + doorLockId);
        String insertDoorLockId = edtsetDoorLockId.getText().toString();
        remoteRingerSDK.RemoteRinger_SetDoorLockId(insertDoorLockId, new RingerCallbacks.DoorLockIDCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Door Lock ID Success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Door Lock ID Error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 set System Mode 2**
     */
    private void setSystemMode() {
        Log.e(TAG, "Input setSystemMode:::::::" + setSystemMode);
        remoteRingerSDK.RemoteRinger_setSystemMode(setSystemMode, new RingerCallbacks.SystemModeCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(MainActivity.this, "System Mode:" + message, Toast.LENGTH_SHORT).show();
                Log.d(TAG, message);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "System mode error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });

            }
        });
    }

    private void setSystemMode1() {
        
        Log.e(TAG, "Input setSystemMode 1 :::::::" + setSerialNumber.getText().toString());
        remoteRingerSDK.RemoteRinger_deviceCommissioning( new RingerCallbacks.SystemModeCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Factory mode Success: " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Factory mode Error " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });


    }

    /**
     * **🔹 get System Mode**
     */
    private void getSystemMode() {
        remoteRingerSDK.RemoteRinger_getSystemMode(new RingerCallbacks.SystemModeCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "System mode in : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "System mode error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 set Wifi Ssid**
     */
    private void setWifiSsid() {
        Log.e(TAG, "Input setWifiSsid:::::::" + setWifiSsidName);
        String insertSSID = edtsetWifiSsid.getText().toString();
        remoteRingerSDK.RemoteRinger_setWifiSsid(insertSSID, new RingerCallbacks.WiFiSSIDCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "WiFi SSID : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "WiFi SSID error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 get Wifi Ssid **
     */
    private void getWifiSsid() {
        remoteRingerSDK.RemoteRinger_GetWiFiSSID(new RingerCallbacks.WiFiSSIDCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "WiFi SSID success: " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "WiFi SSID error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 set Wifi Password: **
     */
    private void setWifiPass() {
        Log.e(TAG, "Input setWifiPass:::::::" + WifiSsidPassword);
        String insertWifiPass = edtsetWifiPassword.getText().toString();
        remoteRingerSDK.RemoteRinger_setWifiPass(insertWifiPass, new RingerCallbacks.WifiPasswordcallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "WiFi pass is : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "WiFi pass err : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 set Wifi Activation **
     */
    private void setWifiActivation() {
        Log.e(TAG, "Input setWifiActivation:::::::" + setWifiActivation);
        remoteRingerSDK.RemoteRinger_wifiActivation(setWifiActivation, new RingerCallbacks.WifiActivationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "WiFi Activation Success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "WiFi Activiation erro : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 Mobile unAuth **
     */
    private void mobileUnAuth() {
        remoteRingerSDK.RemoteRinger_MobileUnAuth(new RingerCallbacks.MobileAunAuthCallback() {
            @Override
            public void onSuccess(String message) {

            }

            @Override
            public void onError(String errorMessage) {

            }
        });
    }

    /**
     * **🔹 set System onBoarding**
     */
    private void setSystemProvisionModeBoarding() {
        Log.e(TAG, "Input setSystemProvisionModeBoarding:::::::" + setSystemProvisionMode);
        remoteRingerSDK.RemoteRinger_setSystemMode(setSystemProvisionMode, new RingerCallbacks.SystemModeCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Provision mode success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Provision Mode error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 set Serial Number**
     */
    private void setSerialNumber() {
        Log.e(TAG, "Input set Serial Number :::::::" + serialNumber);
        String insertSerialNo = setSerialNumber.getText().toString();
        Log.d(TAG, "Inserted Serial Number " + insertSerialNo);
        remoteRingerSDK.RemoteRinger_setSerialNumber(insertSerialNo, new RingerCallbacks.SerialNumberCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Serial Number: " + message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Serial number error message: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 get Serial Number**
     */
    private void getSerialNumber() {
        Log.e(TAG, "Input set Serial Number :::::::" + serialNumber);
        remoteRingerSDK.RemoteRinger_GetSerialNumber(new RingerCallbacks.SerialNumberCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Serial Number" + message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Serial Number error " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 set Hardware Version**
     */
    private void setHardwareVersion() {
        try {
            Log.e(TAG, "Input patch :::::::" + patch);
            Log.e(TAG, "Input major :::::::" + patch);
            Log.e(TAG, "Input minor :::::::" + patch);
            int major = Integer.parseInt(edtMajor.getText().toString());
            int minor = Integer.parseInt(edtMinor.getText().toString());
            int patch = Integer.parseInt(edtPatch.getText().toString());
            remoteRingerSDK.RemoteRinger_setHardwareVersion(major, minor, patch, new RingerCallbacks.HardwareVersionCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Hardware Version Success : " +message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Hardware Version Error: " +errorMessage, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, errorMessage);
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Please enter a valid number: ", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * **🔹 get Hardware Version**
     */
    private void getHardwareVersion() {
        remoteRingerSDK.RemoteRinger_GetHardwareVersion(new RingerCallbacks.HardwareVersionCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Hardware version success : " + message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });


            }

            @Override
            public void onError(String errorMessage) {

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Hardware version error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });



            }
        });
    }

    /**
     * **🔹 set Door Lock Ble Address**
     */
    private void setDoorLockSecretKey() {
        Log.e(TAG, "Input setDoorLockSecretKey::::::::::::::" + encryptionKey);
        String insertEncryptionKey = edtsetDoorLockSecretKey.getText().toString();
        remoteRingerSDK.RemoteRinger_setDoorLockEncryptionKey(insertEncryptionKey, new RingerCallbacks.DoorLockEncryptionKey() {
            @Override
            public void onSuccess(String modelId) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Door Lock Secret key : " +modelId, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, modelId);
                });

            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Door Lock Secret key error : " +error, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, error);
                });
            }
        });


    }

    /**
     * **🔹 Activate onBoarding**
     */
    private void setActivateOnBoarding() {
        String selectedLabel = spinnerDoorLockType.getSelectedItem().toString();
        onBoardingType = doorLockTypeMap.get(selectedLabel);
        Log.e(TAG, "Input setActivateOnBoarding::::::::::::::" + onBoardingActivationMode);
        remoteRingerSDK.RemoteRinger_setOnBoardingActivation(onBoardingActivationMode,onBoardingType, new RingerCallbacks.OnBoardingActivationCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Onboarding activation success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Onboarding activation error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });


    }

    /**
     * **🔹 OnBoardingSuccess**
     */
    private void setOnBoardingSuccess() {
        Log.e(TAG, "Input setOnBoardingSuccess::::::::::::::" + onBoardingSuccess);
        remoteRingerSDK.RemoteRinger_OnBoardingSuccess(onBoardingSuccess, new RingerCallbacks.OnBoardingSuccessCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Onboarding success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Onboarding error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });


    }

    /**
     * **🔹 get the Firmware Version**
     */
    private void getFirmwareVersion() {
        remoteRingerSDK.RemoteRinger_GetFirmwareVersion(new RingerCallbacks.FirmwareVersionCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                Log.d(TAG, message);
                Toast.makeText(getApplicationContext(), "Firmware Version :" + message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "firmware version success : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  get Application Boot Mode**
     */
    private void getApplicationBootMode() {
        remoteRingerSDK.RemoteRinger_GetApplicationBootMode(new RingerCallbacks.BootModeCallback() {

            @Override
            public void onSuccess(int mode) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Application boot mode : " +mode, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "mode  "+mode);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Boot mode error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }

        });
    }

    /**
     * **🔹  set Application Boot Mode**
     */
    private void setApplicationBootMode() {
        Log.e(TAG, "Input setApplicationBootMode :::::::" + setApplicationBootMode);
        remoteRingerSDK.RemoteRinger_setApplicationBootMode(setApplicationBootMode, new RingerCallbacks.SetApplicationBootMode() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Boot mode success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Boot mode error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  Factory Reset**
     */
    private void FactoryReset() {
        remoteRingerSDK.RemoteRinger_FactoryReset(new RingerCallbacks.FactoryResetCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Factory reset success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Factory reset error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  Device Reboot **
     */
    private void DeviceReboot() {
        remoteRingerSDK.RemoteRinger_RebootDevice(new RingerCallbacks.RebootCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Device Reboot success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Device reboot error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  get Device Model **
     */
    private void getDeviceModelId() {
        remoteRingerSDK.RemoteRinger_GetDeviceModelID(new RingerCallbacks.DeviceModelCallback() {

            @Override
            public void onSuccess(int modelId) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Device Model ID success : " +modelId, Toast.LENGTH_SHORT).show();
                   // Log.d(TAG, String.valueOf(modelId));
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Device Model ID error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }


    /**
     * **🔹  set get Device Model **
     */
    private void setDeviceModelId() {
        try {
            int newModelID = Integer.parseInt(edtDeviceModelID.getText().toString());
            Log.e(TAG, "Input setDeviceModelId :::::::" + edtDeviceModelID.getText().toString());
            remoteRingerSDK.RemoteRinger_setDeviceModelID(newModelID, new RingerCallbacks.setDeviceModelID() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "set device model id success : " +message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message);
                    });

                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "set device model id error : " +errorMessage, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, errorMessage);
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Please enter a valid number: ", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * **🔹  get Door lock id **
     */
    private void getDoorLockId() {
        remoteRingerSDK.RemoteRinger_GetDoorLockID(new RingerCallbacks.DoorLockIDCallback() {
            @Override
            public void onSuccess(String message) {

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "get Door lock id success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Door lock id error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  stop Audio Tone **
     */
    private void stopAudioTone() {
        remoteRingerSDK.RemoteRinger_StopMelody(new RingerCallbacks.StopMelodyCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Stopping audio tone success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Stopping audio tone error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  set Audio Tone **
     */
    private void setAudioTone() {

        try {
            int insertAudioTone = Integer.parseInt(edtsetAudioTone.getText().toString());
            Log.e(TAG, "Input setAudioTone :::::::" + insertAudioTone);
            remoteRingerSDK.RemoteRinger_SetMelody(insertAudioTone, new RingerCallbacks.PlayMelodyCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Setting audio tone success : " +message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message);
                    });

                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Setting audio tone error : " +errorMessage, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, errorMessage);
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Please enter a valid number: ", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * **🔹  get volume level **
     */

    private void getVolumeLevel() {
        remoteRingerSDK.RemoteRinger_GetVolumeMelodyLevel(new RingerCallbacks.VolumeLevelCallback() {

            @Override
            public void onSuccess(int volumeLevel) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "get volume level success : " +volumeLevel, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, ""+volumeLevel);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "get volume level error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  set Volume Level **
     */
    private void setVolumeLevel() {

        try {
            int insertVolumeLevel = Integer.parseInt(edtsetVolumeLevel.getText().toString().trim());
            Log.e(TAG, "Input setVolumeLevel :::::::" + insertVolumeLevel);
            remoteRingerSDK.RemoteRinger_SetMelodyVolume(insertVolumeLevel, new RingerCallbacks.VolumeCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "set volume level success : " +message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    // Handle error
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "set volume level error : " +errorMessage, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, errorMessage);
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Please enter a valid number: ", Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * **🔹  Increment Volume Level **
     */
    private void incrementVolumeLevel() {
        remoteRingerSDK.RemoteRinger_IncrementMelodyVolume(new RingerCallbacks.VolumeCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Volume increased success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Volume increased error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  Next Audio Tone**
     */
    private void nextAudioTones() {
        remoteRingerSDK.RemoteRinger_PlayNextMelody(new RingerCallbacks.VolumeCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "next audio tone success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Next audio tone error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  Previous Audio Tone **
     */
    private void previousAudioTone() {
        remoteRingerSDK.RemoteRinger_PlayPreviousMelody(new RingerCallbacks.VolumeCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Previous audio tone success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Previous audio tome error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  Preview Audio Tone **
     */



    private void previewAudioTone() {

        try {
            int insertPreviewAudioTone = Integer.parseInt(edtpreviewaudiotone.getText().toString());
            Log.e(TAG, "Input previewAudioToneId :::::::" + insertPreviewAudioTone);
            remoteRingerSDK.RemoteRinger_PreviewMelodyAudioTone(insertPreviewAudioTone, new RingerCallbacks.VolumeCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Preview success : " +message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message);
                    });

                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Preview error : " +errorMessage, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, errorMessage);
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Please enter a valid number: ", Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * **🔹  get the Current Audio Tone **
     */
    private void currentAudioToneId() {
        remoteRingerSDK.RemoteRinger_GetCurrentMelodyId(new RingerCallbacks.PlayMelodyCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Current tone success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Current tone error: " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  Decrement volume level **
     */
    private void decrementVolumeLevel() {
        remoteRingerSDK.RemoteRinger_DecrementMelodyVolume(new RingerCallbacks.VolumeCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "decrease volume success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Decrease volume error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  play melody **
     */
    private void playMelody(){
        remoteRingerSDK.RemoteRinger_PlayMelody(new RingerCallbacks.PlayMelodyCallback() {
            @Override
            public void onSuccess(String message) {

                    Toast.makeText(MainActivity.this, "Play melody success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Play melody error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹  OnBoarding **
     */
    private void OnBoarding(int doorlockUnitType) {
           String doorLockId= edtsetDoorLockId.getText().toString();
           String encryptionKey= edtsetDoorLockSecretKey.getText().toString();
           String macId= edtsetDoorLockBleAddress.getText().toString();

        remoteRingerSDK.RemoteRinger_Onboarding(doorLockId, encryptionKey, macId, doorlockUnitType,new RingerCallbacks.OnboardingCallback() {
            @Override
            public void onSuccess(String message) {

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Onboarding success : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Onboarding error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });


    }
    /**
     * **🔹  disconnectDevice **
     */

    private void disconnectDevice() {
        remoteRingerSDK.RemoteRinger_DisconnectDevice(new RingerCallbacks.DisconnectionCallback() {
            @Override
            public void onSuccess(String message) {

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Disconnected from : " +message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, message);
                });


            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();

            }
        });
    }

    /**
     * **🔹 get the device info detail **
     */
    private void getDeviceInfo() {
        remoteRingerSDK.RemoteRinger_GetDeviceInfo(new RingerCallbacks.DevicesInfoCallback() {
            @Override
            public void onDevicesRetrieved(List<DeviceInfo> devices) {
                StringBuilder messageBuilder = new StringBuilder();
                for (DeviceInfo device : devices) {


                    messageBuilder.append("Serial: ").append(device.getSerialNumber())
                            .append("\nHardware: ").append(device.getHardwareVersion())
                            .append("\nFirmware: ").append(device.getFirmwareVersion())
                            .append("\nBoot Mode: ").append(device.getApplicationBootMode())
                            .append("\nModel ID: ").append(device.getDeviceModelId());

                    String message = messageBuilder.toString();
                    int chunkSize = 3000; // Max safe Toast length
                    for (int i = 0; i < message.length(); i += chunkSize) {
                        String chunk = message.substring(i, Math.min(i + chunkSize, message.length()));

                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Device Info : " +chunk, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, chunk);
                        });
                    }
                }

            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Device Info error : " +errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, errorMessage);
                });
            }
        });
    }

    /**
     * **🔹 for the ota abort **
     */
    private void RemoteRinger_AbortOtaUpdate() {
        remoteRingerSDK.RemoteRinger_AbortOtaUpdate(new RingerCallbacks.OtaCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                otaProgressBar.setVisibility(View.GONE);
                otaProgressText.setText("OTA Aborted");
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Abort failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

}