package org.me.gcu.s2212625coursework;


public class ExchangeItem {
    public String title;
    public String date;
    public double ratio;

    public ExchangeItem(String title, String date, double ratio) {
        this.title = title;
        this.date = date;
        this.ratio = ratio;
    }

    public String getTitle() { return title; }
    public double getRate() { return ratio; }
    public String getPubDate() { return date; }


}

