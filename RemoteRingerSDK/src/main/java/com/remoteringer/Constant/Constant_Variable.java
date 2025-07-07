package com.remoteringer.Constant;

import android.util.Log;

public class Constant_Variable {
    public static final byte START_BYTE = '*';
    public static final byte END_BYTE = '#';
    public static final String TAG = "RemoteRingerSdk";
    public static final byte frameType = 'C';
    /**
     * **🔹  RemoteRinger_Command Code For Api **
     */
    public static final byte setSerialCommand = 0x02;
    public static final byte mobileAuthCommand = 0x13;
    public static final byte secretKeyMobileAuthCommand = 0x11;
    public static final byte setSystemModeCommand = 0x0D;
    public static final byte setHardWareCommand = 0x04;
    public static final byte setWIFISSIDCommand = 0x23;
    public static final byte getWifiSSIDCommand = 0x24;
    public static final byte setWifiPassword = 0x25;
    public static final byte setDoorLockIdCommand = 0x10;
    public static final byte getDoorLockIdCommand = 0x0F;
    public static final byte setDoorLockBleMacCommand = 0x2E;
    public static final byte getDoorLockBleMacCommand = 0x2F;
    public static final byte onBoardingActivationCommand = 0x14;
    public static final byte setDoorLockEncryptionKeyCommand = 0x37;
    public static final byte getDoorLockEncryptionKeyCommand = 0x38;
    public static final byte getDeviceActivationStatusCommand = 0x33;
    public static final byte FactoryResetCommand = 0x09;
    public static final byte playAudioToneCommand = 0x1A;
    public static final byte stopAudioToneCommand = 0x1B;
    public static final byte setAudioToneCommand = 0x1D;
    public static final byte nextAudioToneCommand = 0x2A;
    public static final byte previousAudioToneCommand = 0x2C;
    public static final byte getAudioToneCommand = 0x1C;
    public static final byte previewAudioToneCommand = 0x2D;
    public static final byte getVolumeLevelCommand = 0x1E;
    public static final byte setVolumeLevelCommand = 0x1F;
    public static final byte incrementVolumeLevelCommand = 0x20;
    public static final byte decrementVolumeLevelCommand = 0x21;
    //public static final byte decrementVolumeLevelCommand = 0x3C;
    public static final byte getSystemModeCommand = 0x0E;
    public static final byte getDeviceModelIdCommand = 0x0B;
    public static final byte getApplicationBootModeCommand = 0x07;
    public static final byte getFarmWareVersionCommand = 0x05;
    public static final byte getHardwareVersionCommand = 0x03;
    public static final byte MobileUnAuthCommand = 0x39;
    public static final byte getSerialNumberCommand = 0x01;
    public static final byte deviceRebootCommand = 0x0A;
    public static final byte setApplicationBootModeCommand = 0x08;
    public static final byte setWifiActivationCommand = 0x34;
    public static final byte setDeviceModelId = 0x0C;
    public static final byte StartOtaUpdate = 0x1C;
    // OTA Control Commands (as byte arrays)
    public static final byte[] SVR_CHR_OTA_CONTROL_REQUEST = new byte[]{0x01};
    public static final byte[] SVR_CHR_OTA_CONTROL_DONE = new byte[]{0x04};
    public static final byte[] SVR_CHR_OTA_CONTROL_CHECKSUM = new byte[]{0x07};
    public static final byte[] SVR_CHR_OTA_CONTROL_CHECKSUM_DONE = new byte[]{0x08};
    /**
     * **🔹  RemoteRinger_Input From User **
     */

    public static String serialNumber = "";
    public static long locallyGeneratedRandomNo = 0;
    public static String mobileAuthHmacResp = "";
    public static String getHardwareVersion = "";
    public static String getFirmwareVersion = "";
    public static String setWIFISSID = "";
    public static String setStringWiFiPassword = "";
    public static String setStringDoorLockID = "";
    public static String setStringDeviceModelId = "";
    public static String getSystemBootMode = "";
    public static String getDeviceModelID = "";
    public static int getDeviceModelId;
    public static String getDoorLockID = "";
    public static String getVolumeLevel = "";
    public static String getWIFISSID = "";
    public static String setSytemMode1 = "";
    public static int getApplicationBootMode;
    public static String getResponseOk = "";
    public static String setResponseOk = "";
    public static int major = 1, minor = 0, patch = 1;
    public static int setApplicationBootMode = 1;
    public static String doorLockId = "ABCDEFGH1234567";
    public static String encryptionKey = "BBCDEFGH12345678IJKLMNOP98765431";
    public static int setSystemMode = 2;
    public static int setSystemMode1 = 1;
    public static String getCurrentToneID;
    public static int setWifiActivation = 1;
    public static int DeviceModelId = 2;
    public static int setSystemProvisionMode = 3;
    public static int wifiActivationModeAbort = 4;
    public static String setWifiSsidName = "SDC_Acclivis_Room2";
    public static String WifiSsidPassword = "Acclivis_4321";
    public static int onBoardingActivationMode = 1;
    public static int abortOnBoardingActivation = 4;
    public static int onBoardingSuccess = 5;
    public static int isTriggerd;
    public static int isTriggerdWifiStatus;
    public static int getOnboardingState;
    public static int getWifiOnboardingState;
    public static int getProvisionState;
    public static int setMelodyId = 2;
    public static int setMelodyVolume = 1;
    public static int getOnBoardingType;
    public static int getOnProvisioningType;
    public static int getOnBoardingStatusState;
    public static byte[] actualHmacData = new byte[]{};
    public static String secretKey64 = "";


}

