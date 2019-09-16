package com.map.android.proxy.mission.item.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.map.android.R;

public class SelectFormatAdapter extends BaseAdapter {

    Context context;
    String[] data;
    int mSelectedItem;
    private static LayoutInflater inflater = null;

    public SelectFormatAdapter(Context context, String[] data, int mSelectedItem) {
        // TODO Auto-generated constructor stub
        this.context = context;
        this.data = data;
        this.mSelectedItem = mSelectedItem;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.length;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data[position];
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.list_select_format_item, null);
        TextView text = (TextView) vi.findViewById(R.id.text);
        if (position == mSelectedItem) {
            // set your color
            text.setBackgroundColor(Color.rgb(171, 215, 255));
        }
        else
        {
            text.setBackgroundColor(Color.WHITE);
        }

        text.setText(data[position]);
        return vi;
    }
}