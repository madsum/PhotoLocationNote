package com.home.ma.photolocationnote;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by ma on 27/04/2017.
 */

public class GridViewAdapter extends ArrayAdapter<Bitmap> {

    private Context context;
    private int layoutResourceId;
    private ArrayList<Bitmap> data = new ArrayList<>();

    public GridViewAdapter(Context context, int layoutResourceId, ArrayList<Bitmap> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ViewHolder();
            holder.image = (ImageView) row.findViewById(R.id.image);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        Bitmap item = data.get(position);
        holder.image.setImageBitmap(item);
        return row;
    }

    static class ViewHolder {
        ImageView image;
    }
}