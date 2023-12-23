package com.example.smartlight;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class TimeIntervalAdapter extends ArrayAdapter<TimeInterval> {

    public TimeIntervalAdapter(Context context, List<TimeInterval> intervals) {
        super(context, 0, intervals);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TimeInterval interval = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.textView);
        String formattedText = String.format("%02d:%02d-%02d:%02d       %d lux", //maybe change the format later
        interval.getStartHour(), interval.getStartMinute(), interval.getEndHour(), interval.getEndMinute(), (int) interval.getValue());
        textView.setText(formattedText);

        return convertView;
    }
}
