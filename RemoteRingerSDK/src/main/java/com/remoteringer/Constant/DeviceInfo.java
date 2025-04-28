package com.remoteringer.Constant;

public class DeviceInfo {
    public DeviceInfo(String mSerialNumber, String mHardwareVersion, String mFirmwareVersion, int mApplicationBootMode, int mDeviceModelId) {
        this.SerialNumber = mSerialNumber;
        this.HardwareVersion = mHardwareVersion;
        this.FirmwareVersion = mFirmwareVersion;
        this.ApplicationBootMode = mApplicationBootMode;
        this.DeviceModelId = mDeviceModelId;
    }

    public String getSerialNumber() {
        return SerialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.SerialNumber = serialNumber;
    }

    public String getHardwareVersion() {
        return HardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.HardwareVersion = hardwareVersion;
    }

    public String getFirmwareVersion() {
        return FirmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.FirmwareVersion = firmwareVersion;
    }

    public int getApplicationBootMode() {
        return ApplicationBootMode;
    }

    public void setApplicationBootMode(int applicationBootMode) {
        this.ApplicationBootMode = applicationBootMode;
    }

    public int getDeviceModelId() {
        return DeviceModelId;
    }

    public void setDeviceModelId(int deviceModelId) {
        this.DeviceModelId = deviceModelId;
    }

    private String SerialNumber;
    private String HardwareVersion;
    private String FirmwareVersion;
    private int ApplicationBootMode;
    private int DeviceModelId;

    public DeviceInfo() {
    }
}