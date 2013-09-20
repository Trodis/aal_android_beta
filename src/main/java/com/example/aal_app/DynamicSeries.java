package com.example.aal_app;

import com.androidplot.xy.XYSeries;

public class DynamicSeries implements XYSeries {

    private DynamicXYDatasource datasource;
    private int seriesIndex;
    private String title;

    public DynamicSeries(DynamicXYDatasource datasource, int seriesIndex,
                         String title)
    {

        this.datasource = datasource;
        this.seriesIndex = seriesIndex;
        this.title = title;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public int size()
    {
        return datasource.getItemCount(seriesIndex);
    }

    @Override
    public Number getX(int index)
    {
        return datasource.getX(seriesIndex, index);
    }

    @Override
    public Number getY(int index)
    {
        return datasource.getY(seriesIndex, index);
    }
}