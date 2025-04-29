package com.remoteringer.commands;

import static com.remoteringer.Constant.Constant_Variable.END_BYTE;
import static com.remoteringer.Constant.Constant_Variable.START_BYTE;
import static com.remoteringer.Constant.Constant_Variable.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;


public abstract class RemoteRingerCommand {
    private static final String PREF_NAME = "RemoteRingerPrefs";
    private static final String REQUEST_ID_KEY = "request_id";
    private static int requestIdCounter = -1;
    private static final byte[] actualData = new byte[1]; // Store the 1-byte data

    /**
     * set the input form user one argument which value for 1 byte.
     */
    public static byte[] setSingleByteCommand(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Value must be between 0 and 255.");
        }
        return new byte[]{(byte) value};  // Return a new byte array with a single value
    }

    /**
     * set the input form user one argument which value for 1 byte and type for 1 byte.
     */
    public static byte[] setSingleByteCommandType(int value, int type) {
        if (value < 0 || value > 255 || type < 0 || type > 255) {
            throw new IllegalArgumentException("Value and Type must be between 0 and 255.");
        }
        return new byte[]{(byte) value, (byte) type};  // Two bytes: value and type
    }

    /**
     * set the input form user one argument which value for two byte.
     */
    public static byte[] setTwoByteCommand(int value) {
        try {
            if (value < 0 || value > 65535) {
                System.err.println("Error: Value must be between 0 and 65535. Given value: " + value);
                return null;
            }

            return new byte[]{
                    (byte) ((value >> 8) & 0xFF), // Most significant byte (MSB)
                    (byte) (value & 0xFF)         // Least significant byte (LSB)
            };
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }

        // Return a default value in case of error
        return new byte[]{0x00, 0x00};
    }

    /**
     * set the input form user three argument major,minor,patch api name Hardware version.
     */
    public static byte[] RemoteRinger_setHardwareVersion(int major, int minor, int patch) {
        if (major < 0 || major > 255 || minor < 0 || minor > 255 || patch < 0 || patch > 255) {
            throw new IllegalArgumentException("Each input value must be in the range of 0 to 255.");
        }

        // Ensure actualData has a valid size
        byte[] actualData = new byte[3];

        actualData[0] = (byte) major;
        actualData[1] = (byte) minor;
        actualData[2] = (byte) patch;

        return actualData;
    }

    /**
     * set the input form user 6,12,16,32 byte.
     */

    public static byte[] RemoteRinger_setMethodVariableSize(String serialNumber) {
        byte[] dataBytes = serialNumber.getBytes(StandardCharsets.US_ASCII); // Convert string to bytes
        int length = dataBytes.length;

        byte[] actualData;

        if (length <= 6) {
            actualData = new byte[6]; // Allocate 6 bytes
        } else if (length <= 12) {
            actualData = new byte[12]; // Allocate 12 bytes
        } else if (length <= 16) {
            actualData = new byte[16]; // Allocate 16 bytes
        } else if (length <= 32) {
            actualData = new byte[32]; // Allocate 32 bytes
        }

        else if (length <= 34) {
            actualData = new byte[34]; // Allocate 34bytes
        }
        else {
            throw new IllegalArgumentException("Input string exceeds 32 bytes, truncating...");
        }

        // Fill with 0x00 padding
        Arrays.fill(actualData, (byte) 0x00);

        // Copy original data
        System.arraycopy(dataBytes, 0, actualData, 0, dataBytes.length);

        return actualData;
    }

    /*public static byte[] RemoteRinger_setMethodVariableSizes(String bleAddress) {
        if (bleAddress == null || bleAddress.length() != 12) {
            throw new IllegalArgumentException("Invalid BLE MAC address format.");
        }

        bleAddress = bleAddress.replace(":", ""); // Just in case

        byte[] actualData = new byte[6];
        try {
            for (int i = 0; i < 6; i++) {
                int index = i * 2;
                actualData[5 - i] = (byte) Integer.parseInt(bleAddress.substring(index, index + 2), 16); // reverse order
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid BLE MAC address content.", e);
        }

        return actualData;
    }*/

   /* public static byte[] RemoteRinger_setMethodVariableSizes(String bleAddress) {

        if (bleAddress.length() != 12) {
            throw new IllegalArgumentException("Invalid BLE MAC address length after formatting.");
        }

        byte[] actualData = new byte[6];
        try {
            for (int i = 0; i < 6; i++) {
                int index = i * 2;
                // BLE MAC address needs to be reversed (LSB first)
                actualData[5 - i] = (byte) Integer.parseInt(bleAddress.substring(index, index + 2), 16);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid BLE MAC address content.", e);
        }

        return actualData;
    }*/

    public static byte[] RemoteRinger_setMethodVariableSizes(String bleAddress) {
        if (bleAddress == null) {
            throw new IllegalArgumentException("BLE address is null.");
        }

        // Remove colons if present
        bleAddress = bleAddress.replace(":", "").toUpperCase();

        // Must be 12 hex characters after formatting
        if (bleAddress.length() != 12 || !bleAddress.matches("[0-9A-F]{12}")) {
            throw new IllegalArgumentException("Invalid BLE MAC address. Expected 12 hexadecimal characters (e.g., 001A7DDA7113 or 00:1A:7D:DA:71:13).");
        }

        byte[] actualData = new byte[6];
        try {
            for (int i = 0; i < 6; i++) {
                int index = i * 2;
                // BLE MAC usually sent in reverse (LSB first)
                actualData[5 - i] = (byte) Integer.parseInt(bleAddress.substring(index, index + 2), 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex content in BLE MAC address.", e);
        }

        return actualData;
    }




    /**
     * Calculates the crc  value and data intercity and generate the command frame.
     */

    public static byte[] createCommandFrame(byte frameType, byte commandCode, byte requestId, String Host_ID, byte[] actualData) {
        byte length = (byte) actualData.length; // Payload length
        int frameSize = 21 + actualData.length; // 21 is up to the payload start index
        byte[] commandFrameForCrc = new byte[frameSize];
        commandFrameForCrc[0] = START_BYTE;
        commandFrameForCrc[1] = frameType;
        commandFrameForCrc[2] = commandCode;
        commandFrameForCrc[3] = requestId;
        byte[] hostIdBytes = Host_ID.getBytes(StandardCharsets.UTF_8);
        // Ensure it's exactly 16 bytes (trim or pad)
        int offset = Math.max(0, hostIdBytes.length - 16);
        System.arraycopy(hostIdBytes, offset, hostIdBytes, Math.max(0, 16 - hostIdBytes.length), Math.min(16, hostIdBytes.length));
        // Copy Host ID bytes to the frame
        System.arraycopy(hostIdBytes, 0, commandFrameForCrc, 4, 16);

        // Store Length at index 20
        commandFrameForCrc[20] = length;

        // Store Payload at index 21 (Fix: Ensure we stay within bounds)
        System.arraycopy(actualData, 0, commandFrameForCrc, 21, actualData.length);
        // Compute CRC
        int crc = calculateCrc(commandFrameForCrc);
        byte crcHigh = (byte) ((crc >> 8) & 0xFF);
        byte crcLow = (byte) (crc & 0xFF);

        // Final Frame with CRC and END_BYTE
        byte[] commandFrame = new byte[commandFrameForCrc.length + 3]; // Add CRC (2 bytes) + End byte
        System.arraycopy(commandFrameForCrc, 0, commandFrame, 0, commandFrameForCrc.length);
        commandFrame[commandFrameForCrc.length] = crcHigh;
        commandFrame[commandFrameForCrc.length + 1] = crcLow;
        commandFrame[commandFrameForCrc.length + 2] = END_BYTE;

        requestLogFrame(commandFrame);
        return commandFrame;
    }


    /**
     * Calculates CRC16 (XMODEM) checksum.
     */
    private static int calculateCrc(byte[] data) {
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

    /**
     * Logs the command frame in HEX format for debugging.
     */
    private static void requestLogFrame(byte[] commandFrame) {
        StringBuilder hexString = new StringBuilder("Request Frame: ");
        for (byte b : commandFrame) {
            hexString.append(String.format("%02X ", b));
        }
        Log.d("RemoteRingerSDK", hexString.toString());
    }

    /**
     * initialise the request id.
     */
    public static void initializeRequestId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(REQUEST_ID_KEY)) {
            // Generate a random request ID between 0x07 and 0xFF
            Random random = new Random();
            requestIdCounter = random.nextInt(0xF9) + 0x064; // Range: 0x07 - 0xFF
            saveRequestId(context);
            Log.d("RemoteRingerSDK", "Generated Random Request ID: " + requestIdCounter);
        } else {
            requestIdCounter = prefs.getInt(REQUEST_ID_KEY, 0x064);
        }
    }

    /**
     * save the request id in SharedPreferences .
     */
    public static void saveRequestId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(REQUEST_ID_KEY, requestIdCounter);
        editor.apply();
    }

    /**
     * get the next request id .
     */
    public static byte getNextRequestId(Context context) {
        if (requestIdCounter == -1) {
            initializeRequestId(context);
        }
        if (requestIdCounter >= 0xFF) { // Reset when overflowing
            requestIdCounter = 0x064;
        } else {
            requestIdCounter++; // Increment requestId
        }

        saveRequestId(context); // Save the updated request ID
        return (byte) (requestIdCounter & 0xFF);
    }
}