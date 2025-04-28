package com.remoteringer.adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.remoteringer.utils.Tone;

import java.util.List;

public class PopupListAdapter extends ArrayAdapter<Tone> {
    public PopupListAdapter(Context context, List<Tone> tones) {
        super(context, 0, tones);
    }
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        Tone tone = getItem(position);
        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(tone.getName());

        return convertView;
    }
}

