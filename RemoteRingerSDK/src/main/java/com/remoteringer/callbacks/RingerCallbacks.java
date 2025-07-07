package com.remoteringer.callbacks;

import com.remoteringer.Constant.DeviceInfo;

import java.util.List;
import java.util.Map;

public class RingerCallbacks {
    public static RingerCallbacks.RingerDeviceCallback RingerDeviceCallback;

    // Callback for handling generic results
    public interface Callback<T> {
        void onResult(T result);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger SystemMode.
     */
    public interface SkipProvisionCallBack extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger SystemMode.
     */
    public interface ReProvisionCallBack extends BaseCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    // Callback for Bluetooth device pairing
    public interface ToneCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger scanning and discovering nearby BLE devices.
     */
    public interface NearbyDevicesCallback {
        void onDevicesRetrieved(List<NearbyDevice> devices);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger SerialNumber.
     */
    public interface SerialNumberCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger SystemMode.
     */
    public interface SystemModeCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger WiFiSSID.
     */
    public interface WiFiSSIDCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger DoorLockId.
     */
    public interface DoorLockIDCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger DoorLockId.
     */
    public interface DoorLockBleAddressCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger BoardingActivation.
     */
    public interface OnBoardingActivationCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger HardwareVersion.
     */
    public interface HardwareVersionCallback extends BaseCallback {
//        void onSuccess(String message);
//
//        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger FirmwareVersion.
     */
    public interface FirmwareVersionCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger BootMode.
     */
    public interface BootModeCallback {
        void onSuccess(int mode);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger BoardingSuccess.
     */
    public interface OnBoardingSuccessCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger VolumeLevelCallback.
     */
    public interface VolumeLevelCallback extends BaseCallback{
        void onSuccess(int volumeLevel);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger DeviceModel.
     */
    public interface DeviceModelCallback {
        void onSuccess(int modelId);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger StopMelody.
     */
    public interface StopMelodyCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger RingerDevice.
     */

    public interface RingerDeviceCallback {
        void onSuccess(String message);

        void onError(String errorMessage);

        void onDeviceDataReceived(Map<String, String> dataMap);
    }

    public interface EventDeviceCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /* public interface RingerDeviceCallback {
         void onSuccess(String message);
         void onError(String errorMessage);
         void onEventSuccess(String message);
         void onEventError(String message);
     }


     /**
      * Callback interface for handling responses from the RemoteRinger Authentication.
      */
    public interface AuthenticationCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger DoorLockEncryption.
     */
    public interface DoorLockEncryptionKey extends BaseCallback {
        void onSuccess(String modelId);

        void onError(String error);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger Disconnection.
     */
    public interface DisconnectionCallback {
        void onSuccess(String message);  // Called when disconnection is successful

        void onError(String errorMessage);  // Called when disconnection fails
    }

    /**
     * Callback interface for handling responses from the RemoteRinger FactoryReset.
     */
    public interface FactoryResetCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger WifiPassword.
     */
    public interface WifiPasswordcallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the Remote Ringer Reboot command.
     */
    public interface RebootCallback extends BaseCallback {
        /**
         * Called when the reboot command is successfully sent.
         *
         * @param message Success message.
         */
        void onSuccess(String message);

        /**
         * Called when the reboot command fails.
         *
         * @param errorMessage Error message.
         */
        void onError(String errorMessage);
    }

    /**
     * Called when playing the melody fails.
     *
     * @param errorMessage Error message.
     *//*
        void onError(String errorMessage);
    }*/
    public interface PlayMelodyCallback extends BaseCallback {
        /**
         * Called when the melody plays successfully.
         *
         * @param message Success message.
         */

    }


    /**
     * Callback interface for handling responses from the Remote Ringer Play Melody command.
     */
    /* public interface PlayMelodyCallback {
     *//**
     * Called when the melody plays successfully.
     *
     * @param message Success message.
     *//*
        void onSuccess(String message);

        */

    /**
     * Callback interface for handling responses from the Remote Ringer Set Melody Volume command.
     */
    public interface VolumeCallback extends BaseCallback {
        /**
         * Called when the melody volume is successfully set.
         *
         * @param message Success message.
        //         */
//        void onSuccess(String message);
//
//        /**
//         * Called when setting the melody volume fails.
//         *
//         * @param errorMessage Error message.
//         */
//        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger SetApplicationBootMode.
     */
    public interface SetApplicationBootMode extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger DeviceModelID.
     */
    public interface setDeviceModelID extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger Onboarding.
     */
    public interface OnboardingCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger WifiActivation.
     */
    public interface WifiActivationCallback extends BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger MobileAunAuth.
     */
    public interface MobileAunAuthCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger Provision.
     */
    public interface ProvisionCallback extends BaseCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger Session.
     */
    public interface SessionCallback {
        void onLastConnectedDevices(List<String> devices); // List of last connected devices

        void onDeviceConnected(String connectedDevice); // Currently connected device

        void onError(String errorMessage); // Error handling
    }

    /**
     * Callback interface for handling responses from the RemoteRinger OtaCall.
     */
    public interface OtaCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger OtaProgress.
     */
    public interface OtaProgressCallback {
        void onDownloadProgress(int progress); // Add progress callback
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger Wifi Rss Value.
     */
    public interface WifiRssiCallBack {
        void onWifiRssiUpdated(int rssiValue);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger Wifi Router status.
     */
    public interface WifiStatusCallBack {
        void onWifiStatus(String WifiStatus,int RssiValue);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger Wifi Rss Value.
     */
    public interface DluBleStatusCallBack {
        void onDLuBleStatus(String routerStatus,int bleRssiValue);
    }

    public interface DluUdpStatusCallBack {
        void onDluUdpStatus(String UdpStatus,int UdpRssiValue);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger Wifi Rss Value.
     */
    public interface DluBleRssiCallBack {
        void onDluBleRssiUpdated(int rssiValue);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger DevicesInfo.
     */
    public interface DevicesInfoCallback {
        void onDevicesRetrieved(List<DeviceInfo> devices);

        void onError(String errorMessage);
    }

    public interface BaseCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for handling responses from the RemoteRinger nearby Bluetooth device.
     */
    public static class NearbyDevice {
        private final String name;
        private final String address;

        public NearbyDevice(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            NearbyDevice that = (NearbyDevice) obj;
            return address.equals(that.address); // Compare only the MAC address
        }

        @Override
        public int hashCode() {
            return address.hashCode(); // Use MAC address for hashcode
        }
    }

}