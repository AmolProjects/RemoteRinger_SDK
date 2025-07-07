package com.remoteringer.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonReader {
    private static final String TAG = "JsonReader";

    public static List<Tone> readJson(Context context) {
        try {
            // Read JSON from file
            InputStream is = context.getAssets().open("tone.json"); // Place JSON in assets folder
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            // Parse JSON
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<Tone>>>() {
            }.getType();
            Map<String, List<Tone>> dataMap = gson.fromJson(json, type);

            // Extract list
            return dataMap.get("data");

        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file", e);
            return null;
        }
    }
}


