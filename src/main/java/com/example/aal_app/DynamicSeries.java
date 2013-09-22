package com.example.aal_app;


import com.androidplot.xy.XYSeries;

public class DynamicSeries implements XYSeries {
    private DynamicXYDatasource datasource;
    private int seriesIndex;
    private String title;

    public DynamicSeries(String title) {
        this.datasource = datasource;
        this.seriesIndex = seriesIndex;
        this.title = title;
    }
    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int size() {
        return 5;
    }

    @Override
    public Number getX(int index) {
        return System.currentTimeMillis()+Math.random()*2000;
    }

    @Override
    public Number getY(int index) {
        return Math.random()*10;
    }
}