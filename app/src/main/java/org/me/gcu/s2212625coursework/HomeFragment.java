package org.me.gcu.s2212625coursework;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private AdapterView<?> listView;
    private AbsListView absListView;
    private SearchView searchView;
    private TextView lastUpdatedView;

    private final ArrayList<ExchangeItem> items = new ArrayList<>();
    private ExchangeAdapter adapter;

    private static final String RSS_URL = "https://www.fx-exchange.com/gbp/rss.xml";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Handler refreshHandler = new Handler();
    private static final long refreshInterval = 300000;

    private final Handler timeAgoHandler = new Handler();
    private long lastUpdatedMillis = 0;


    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            new Thread(new RssOperational()).start();
            refreshHandler.postDelayed(this, refreshInterval);
        }
    };

    private final Runnable timeAgoRunnable = new Runnable() {
        @Override
        public void run() {
            updateDynamicLastUpdatedText();
            timeAgoHandler.postDelayed(this, 60000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        listView = view.findViewById(R.id.listView);
        absListView = (AbsListView) listView;

        searchView = view.findViewById(R.id.searchBar);
        lastUpdatedView = view.findViewById(R.id.lastUpdated);

        adapter = new ExchangeAdapter(requireContext(), items);
        absListView.setAdapter(adapter);

        adapter.setOnSearchErrorListener(() ->
                Toast.makeText(requireContext(),
                        "Currency name/code incorrect. Please try again.",
                        Toast.LENGTH_SHORT).show()
        );

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            ExchangeItem selected = adapter.getItem(position);
            if (selected != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity())
                        .loadFragment(ConverterFragment.newInstance(selected.title, selected.ratio), true);
            }
        });

        new Thread(new RssOperational()).start();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.postDelayed(refreshTask, refreshInterval);
        timeAgoHandler.post(timeAgoRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshTask);
        timeAgoHandler.removeCallbacks(timeAgoRunnable);
    }

    private class RssOperational implements Runnable {
        @Override
        public void run() {

            Log.d("RSS_DEBUG", "Downloading " + RSS_URL);

            try {
                URL url = new URL(RSS_URL);

                BufferedReader reader;
                try {
                    InputStream inputStream = url.openConnection().getInputStream();
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                } catch (Exception e) {
                    showError("RSS feed unavailable. Please try again later.");
                    return;
                }

                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) builder.append(line);
                reader.close();

                String xmlData = builder.toString();

                int start = xmlData.indexOf("<?xml");
                if (start > 0) xmlData = xmlData.substring(start);

                xmlData = cleanUpXml(xmlData);

                ArrayList<ExchangeItem> parsedItems = parseRSS(xmlData);

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    items.clear();
                    items.addAll(parsedItems);

                    adapter.setFullList(new ArrayList<>(items));
                    adapter.notifyDataSetChanged();

                    lastUpdatedMillis = System.currentTimeMillis();
                    updateDynamicLastUpdatedText();
                });

            } catch (Exception ignored) {}
        }
    }

    private String cleanUpXml(String xml) {
        xml = xml.replaceAll("(?s)<script.*?>.*?</script>", "");
        xml = xml.replaceAll("<(?!/?(rss|channel|item|title|link|description|pubDate|category|guid)\\b)[^>]+>", "");
        xml = xml.replaceAll("&(?!(amp;|lt;|gt;|quot;|apos;))", "&amp;");
        xml = xml.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        return xml;
    }

    private ArrayList<ExchangeItem> parseRSS(String xml) throws Exception {
        ArrayList<ExchangeItem> parsed = new ArrayList<>();

        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(new StringReader(xml));

        int event = parser.getEventType();
        String tag = "";
        boolean insideItem = false;

        String title = "";
        String pubDate = "";
        String description = "";

        while (event != XmlPullParser.END_DOCUMENT) {

            switch (event) {
                case XmlPullParser.START_TAG:
                    tag = parser.getName();
                    if ("item".equalsIgnoreCase(tag)) {
                        insideItem = true;
                        title = "";
                        pubDate = "";
                        description = "";
                    }
                    break;

                case XmlPullParser.TEXT:
                    String text = parser.getText();
                    if (insideItem) {
                        if ("title".equalsIgnoreCase(tag)) title = text;
                        else if ("pubDate".equalsIgnoreCase(tag)) pubDate = text;
                        else if ("description".equalsIgnoreCase(tag)) description = text;
                    }
                    break;

                case XmlPullParser.END_TAG:
                    if ("item".equalsIgnoreCase(parser.getName())) {
                        insideItem = false;
                        parsed.add(new ExchangeItem(title, pubDate, extractRatio(description)));
                    }
                    break;
            }

            event = parser.next();
        }

        return parsed;
    }

    private double extractRatio(String desc) {
        try {
            String right = desc.split("=")[1].trim();
            return Double.parseDouble(right.split(" ")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void showError(String msg) {
        mainHandler.post(() -> {
            if (isAdded())
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
    }


    private void updateDynamicLastUpdatedText() {
        if (lastUpdatedView == null) return;

        if (lastUpdatedMillis == 0) {
            lastUpdatedView.setText("Updated: --");
            return;
        }

        long now = System.currentTimeMillis();
        long diff = now - lastUpdatedMillis;
        long minutes = diff / 60000;

        if (minutes == 0) {
            lastUpdatedView.setText("Updated just now");
        } else if (minutes == 1) {
            lastUpdatedView.setText("Updated 1 minute ago");
        } else {
            lastUpdatedView.setText("Updated " + minutes + " minutes ago");
        }
    }
}

