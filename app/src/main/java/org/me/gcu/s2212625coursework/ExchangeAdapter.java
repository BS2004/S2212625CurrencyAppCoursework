package org.me.gcu.s2212625coursework;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ExchangeAdapter extends ArrayAdapter<ExchangeItem> implements Filterable {

    public interface OnSearchErrorListener {
        void onSearchError();
    }

    private OnSearchErrorListener onSearchErrorListener;

    public void setOnSearchErrorListener(OnSearchErrorListener listener) {
        this.onSearchErrorListener = listener;
    }

    private ArrayList<ExchangeItem> fullList = new ArrayList<>();

    public ExchangeAdapter(Context context, ArrayList<ExchangeItem> data) {
        super(context, 0, data);
        fullList.addAll(data);
    }

    public void setFullList(List<ExchangeItem> list) {
        fullList.clear();
        fullList.addAll(list);
    }

    private String extractCurrencyCode(String title) {

        int start = title.lastIndexOf("(");
        int end = title.lastIndexOf(")");

        if (start != -1 && end != -1 && end > start) {
            return title.substring(start + 1, end);
        }
        return "";
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_item, parent, false);
        }

        ExchangeItem item = getItem(position);


        TextView titleText = convertView.findViewById(R.id.titleText);
        TextView rateText = convertView.findViewById(R.id.rateText);
        TextView dateText = convertView.findViewById(R.id.dateText);
        ImageView flagImage = convertView.findViewById(R.id.flagImage);
        LinearLayout container = convertView.findViewById(R.id.listItemText);


        if (item != null) {


            titleText.setText(item.title);


            rateText.setText("1 GBP = " + item.ratio + " " + extractCurrencyCode(item.title));


            dateText.setText(item.date);


            String code = extractCurrencyCode(item.title);
            int flagResId = FlagCache.getFlag(code);
            flagImage.setImageResource(flagResId);



            if (item.ratio < 1.0) {
                container.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.LightGreen));
                titleText.setTextColor(ContextCompat.getColor(getContext(), R.color.Black));
                rateText.setTextColor(ContextCompat.getColor(getContext(), R.color.Black));
                dateText.setTextColor(ContextCompat.getColor(getContext(), R.color.Black));
            }
            else if (item.ratio >= 1 && item.ratio < 5) {
                container.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.Green));
                titleText.setTextColor(ContextCompat.getColor(getContext(), R.color.White));
                rateText.setTextColor(ContextCompat.getColor(getContext(), R.color.White));
                dateText.setTextColor(ContextCompat.getColor(getContext(), R.color.White));
            }
            else if (item.ratio >= 5 && item.ratio < 10) {
                container.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.Orange));
                titleText.setTextColor(ContextCompat.getColor(getContext(), R.color.Black));
                rateText.setTextColor(ContextCompat.getColor(getContext(), R.color.Black));
                dateText.setTextColor(ContextCompat.getColor(getContext(), R.color.Black));
            }
            else {
                container.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.DarkRed));
                titleText.setTextColor(ContextCompat.getColor(getContext(), R.color.White));
                rateText.setTextColor(ContextCompat.getColor(getContext(), R.color.White));
                dateText.setTextColor(ContextCompat.getColor(getContext(), R.color.White));
            }
        }

        return convertView;
    }


    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<ExchangeItem> filtered = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(fullList);
                } else {
                    String q = constraint.toString().toLowerCase();
                    for (ExchangeItem item : fullList) {
                        if (item.title.toLowerCase().contains(q)) {
                            filtered.add(item);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                ArrayList<ExchangeItem> filteredList = (ArrayList<ExchangeItem>) results.values;

                if (filteredList.isEmpty() && constraint != null && constraint.length() > 0) {
                    if (onSearchErrorListener != null) {
                        onSearchErrorListener.onSearchError();
                    }
                }

                addAll(filteredList);
                notifyDataSetChanged();
            }
        };
    }
}

