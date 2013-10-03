package com.example.aal_app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.*;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.*;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.*;
import org.achartengine.renderer.*;
import org.achartengine.renderer.XYSeriesRenderer;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.types.*;
import java.text.*;
import java.util.*;
import android.graphics.Color;


/**
 * @author Ferhat Özmen
 */



public class Switches extends Activity{

    private String EXTRA_MESSAGE = "UPNP Device";
    private static final String TAG = "AAL LOG: ";
    private final static String seekbar_process_tag = "SeekBar Process";

    private Device upnp_device;
    private ArrayList input_value;

    private AndroidUpnpService upnpService;
    private SubscriptionCallback callback;
    private String unique_device_identifier;

    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataSet = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

    private TimeSeries mCurrentSeries;
    private XYSeriesRenderer mCurrentRenderer;

    private int save_instance_state_counter = 0;

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className,
                                       IBinder service)
        {
            upnpService = (AndroidUpnpService) service;
            onResume();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            upnpService = null;
        }
    };

    private void initChartBoolean(String state_variable)
    {
        if (mCurrentRenderer == null && mCurrentSeries == null)
        {
            mCurrentRenderer = new XYSeriesRenderer();
            mCurrentSeries = new TimeSeries("Monitoring State " +
                                            "Variable: " + state_variable);
            mDataSet.addSeries(mCurrentSeries);
            mRenderer.addSeriesRenderer(mCurrentRenderer);
        }

        mCurrentRenderer.setFillPoints( true );
        mCurrentRenderer.setAnnotationsTextAlign( Paint.Align.LEFT );
        mCurrentRenderer.setAnnotationsTextSize(15);
        mCurrentRenderer.setAnnotationsColor(Color.CYAN);
        mCurrentRenderer.setColor(Color.GREEN);
        mCurrentRenderer.setPointStyle( PointStyle.DIAMOND);

        mRenderer.setPointSize(3);
        mRenderer.setYTitle( "y-Axis Value of State Variable" );
        mRenderer.setXTitle( "x-Axis Times of Received Events" );
        mRenderer.setAxisTitleTextSize(15);
        mRenderer.setYLabelsAlign( Paint.Align.LEFT );
        mRenderer.setLegendTextSize(15);

        mRenderer.setLabelsColor(Color.LTGRAY);
        mRenderer.setAxesColor(Color.LTGRAY);
        mRenderer.setGridColor(Color.rgb(136, 136, 136));
        mRenderer.setBackgroundColor(Color.DKGRAY);
        mRenderer.setApplyBackgroundColor(true);

        mRenderer.setLegendTextSize(20);
        mRenderer.setLabelsTextSize(20);
        mRenderer.setPointSize(4);
        mRenderer.setMargins(new int[] { 60, 60, 60, 60 });

        mRenderer.setFitLegend(true);
        mRenderer.setZoomButtonsVisible( true);
        mRenderer.setShowGrid(true);
        mRenderer.setZoomEnabled(true);
        mRenderer.setExternalZoomEnabled(true);
        mRenderer.setAntialiasing(true);

    }

    private void addBoolData(long x, int y){
        mCurrentSeries.add(x, y);
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat( "HH:mm:ss" );
        mCurrentSeries.addAnnotation(format.format(date)+ " Uhr", x, y);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);

        Bundle extras = getIntent().getExtras();

        this.unique_device_identifier = extras.getString(EXTRA_MESSAGE);
        input_value = new ArrayList();

        if (upnpService != null)
        {
            upnp_device = upnpService.getRegistry().getDevice(UDN.valueOf
                    (unique_device_identifier), true);
        }

        if (this.upnp_device != null)
        {
            for (Service service : upnp_device.getServices())
            {
                for(Action action : service.getActions())
                {
                    generateUI(service, action);
                }
                setSubscription(service);
            }
            createUPnPServiceandActionInformations(upnp_device);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        onCreate(null);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outStat)
    {
        super.onSaveInstanceState( outStat );
        outStat.putSerializable("dataset", mDataSet);
        outStat.putSerializable("renderer", mRenderer);
        outStat.putSerializable("current_series", mCurrentSeries);
        outStat.putSerializable( "current_renderer", mCurrentRenderer );
        outStat.putInt( "xAxisCounter", save_instance_state_counter + 1 );
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        mDataSet = (XYMultipleSeriesDataset)
                savedInstanceState.getSerializable("dataset");
        mRenderer = (XYMultipleSeriesRenderer)
                savedInstanceState.getSerializable("renderer");
        mCurrentSeries = (TimeSeries)
                savedInstanceState.getSerializable("current_series");
        mCurrentRenderer = (XYSeriesRenderer)
                savedInstanceState.getSerializable("current_renderer");
        save_instance_state_counter =
                savedInstanceState.getInt("xAxisCounter");
    }

    @Override protected void onDestroy()
    {
        super.onDestroy();

        if (callback != null)
        {
            callback.end();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //onDestroy();
    }

    protected void executeAction(AndroidUpnpService upnpService,
                                 Service service, final Action action,
                                 final ActionArgument action_argument,
                                 ArrayList input_value, boolean isInput)
    {

        ActionInvocation setTargetInvocation = new SetTargetActionInvocation
                (service, action, action_argument, input_value, isInput);

        // Executes asynchronous in the background
        upnpService.getControlPoint().execute
                (new ActionCallback(setTargetInvocation)
                {
                    @Override
                    public void success(ActionInvocation invocation)
                    {
                        if (action.hasOutputArguments())
                        {
                            ActionArgumentValue value  = invocation.getOutput
                                    (action_argument.getName());
                            showToast("Received Value: " +  value.getValue()
                                    .toString(), false);
                        }
                    }

                    @Override
                    public void failure(ActionInvocation invocation,
                                        UpnpResponse operation,
                                        String defaultMsg)
                    {
                        showToast(defaultMsg, true);
                    }
                }
        );
    }

    public void createInputActions( final Action action,
                                    final ActionArgument action_argument)
    {
        LinearLayout ll = (LinearLayout) findViewById(R.id
                .LinearLayoutInputActionElements);

        TextView tv = (TextView) (findViewById( R.id.InputActionTitle ));
        tv.setText( "Input Action" );

        Switch sw = new Switch(this);
        sw.setText(action.getName());
        sw.setTag( action.getName() );
        sw.setOnCheckedChangeListener( new CompoundButton
                .OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if ( isChecked ) {
                    input_value.add( true );
                    executeAction( upnpService, action.getService(), action,
                                   action_argument, input_value, true );
                    input_value.clear();
                } else {
                    input_value.add( false );
                    executeAction( upnpService, action.getService(), action,
                                   action_argument, input_value, true );
                    input_value.clear();
                }
            }
        });
        ll.addView(sw);
    }

    public void createOutPutActions( final Action action,
                                     final ActionArgument action_argument)
    {
        LinearLayout ll = (LinearLayout) findViewById(R.id
                .LinearLayoutOutPutActionElements);
        TextView tv = (TextView) findViewById( R.id.OutPutActionTitle );
        tv.setText( "Output Actions" );

        Button button = new Button(this);
        button.setText(action.getName());
        button.setTag(action.getName());
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeAction(upnpService, action.getService(), action,
                        action_argument, input_value, false);
            }
        });
        ll.addView(button);
    }

    public void createSeekBarActions(final Service service,
                                     final Action action,
                                     final ActionArgument action_argument)
    {
        int max_range = (int) service.getStateVariable( action_argument
                             .getRelatedStateVariableName()).getTypeDetails()
                .getAllowedValueRange().getMaximum();

        final LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutSeekBarElements);

        TextView titlev = (TextView) (findViewById( R.id.SeekBarActionTitle ));
        titlev.setText( "Seekbar Actions" );

        SeekBar sb = new SeekBar(this);
        sb.setTag(action.getName());
        sb.setMax(max_range);
        sb.setProgressDrawable( getResources().getDrawable(
                R.drawable.progressbar ) );

        sb.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener()
        {
            TextView tv;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser)
            {
                if (fromUser)
                {
                    tv = (TextView) ll.findViewWithTag(seekbar_process_tag);
                    tv.setText("Processing: " + progress + "%");
                }
                else
                {
                    tv = (TextView) ll.findViewWithTag(seekbar_process_tag);
                    tv.setText("Actual Process: " + progress + "%");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                tv = (TextView) ll.findViewWithTag(seekbar_process_tag);
                tv.setText("Actual Process: " + seekBar.getProgress() + "%");
                input_value.add( String.valueOf(seekBar.getProgress()) );
                executeAction( upnpService, action.getService(), action,
                               action_argument, input_value, true );
                input_value.clear();
            }
        } );

        TextView tv = new TextView(this);
        tv.setTag(seekbar_process_tag);

        ll.addView(tv);
        ll.addView(sb);
    }

    public void setSwitch(final boolean is_checked,final Action action)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                final View ll = findViewById(R.id
                        .LinearLayoutActionElements);

                Switch mySwitch;
                mySwitch = (Switch) ll.findViewWithTag(action.getName());
                mySwitch.setChecked(is_checked);
            }
        });
    }

    public void setSeekBar(final String value, final  Action action)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                final View ll = findViewById(R.id.LinearLayoutActionElements);

                SeekBar sb;
                sb = (SeekBar) ll.findViewWithTag( action.getName() );
                sb.setProgress(Integer.parseInt(value));
            }
        });
    }

    private void startEventlistening(StateVariable state_variable)
    {
            this.callback =
                    new SwitchPowerSubscriptionCallback (state_variable,
                        this, save_instance_state_counter);

            upnpService.getControlPoint().execute(callback);

        if(mChart == null)
        {
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            //initChartBoolean();
            mChart = ChartFactory.getCubeLineChartView(this, mDataSet,
                                                       mRenderer, 0.2f);

            // enable the chart click events
            mRenderer.setClickEnabled(true);
            mRenderer.setSelectableBuffer(50);
            mChart.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v) {
                    // handle the click event on the chart
                    SeriesSelection seriesSelection =
                            mChart.getCurrentSeriesAndPoint();

                    if (seriesSelection != null)
                    {
                        // display information of the clicked point
                        showToast("y-Wert: " + seriesSelection.getValue()
                                  + " x-Wert: " + seriesSelection.getXValue()
                                    ,false );
                    }
                    else
                    {
                        return;
                    }
                }
            });

            layout.addView(mChart);
        }
        else
        {
            mChart.repaint();
        }


    }

    private void generateUI(Service service, Action action)
    {

        for (ActionArgument action_argument : action.getInputArguments())
        {
            if(action_argument.getDatatype().getBuiltin().equals
                        (Datatype.Builtin.BOOLEAN))
            {
                    createInputActions(action, action_argument);
            }
            else if (action_argument.getDatatype().getBuiltin().equals
                        (Datatype.Builtin.UI1))
            {
                    createSeekBarActions(service, action, action_argument);
            }
        }

        for (ActionArgument action_argument : action.getOutputArguments())
        {
            createOutPutActions(action, action_argument);
        }

    }

    private void setSubscription(Service service)
    {
        for (StateVariable stateVariable : service.getStateVariables())
        {
            if (stateVariable.getEventDetails().isSendEvents())
            {
                initChartBoolean(stateVariable.getName());
                startEventlistening(stateVariable);
            }
        }
    }

    protected void showToast(final String msg, final boolean longLength)
    {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                        Switches.this,
                        msg,
                        longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    public void createUPnPServiceandActionInformations(Device upnp_device)
    {
        LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutDeviceInformation);
        TextView tv;

        tv = new TextView(this);
        tv.setText("Device Name: " + upnp_device.getDetails().getFriendlyName());
        tv.setTextSize(15);
        ll.addView(tv);

        tv = new TextView(this);
        tv.setText("Model Discription: " + upnp_device.getDetails()
                .getModelDetails().getModelDescription());
        tv.setTextSize(15);
        ll.addView(tv);

        tv = new TextView(this);
        tv.setText("Model Number: " + upnp_device.getDetails()
                .getModelDetails().getModelNumber());
        tv.setTextSize(15);
        ll.addView(tv);

        tv = new TextView(this);
        tv.setText("Model URI: " + upnp_device.getDetails()
                .getModelDetails().getModelURI());
        tv.setTextSize(15);
        ll.addView(tv);

        tv = new TextView(this);
        tv.setText("Model Name: " + upnp_device.getDetails()
                .getModelDetails().getModelName());
        tv.setTextSize(15);
        ll.addView(tv);

        tv = new TextView(this);
        tv.setText("Manufacturer: " + upnp_device.getDetails()
                .getManufacturerDetails().getManufacturer());
        tv.setTextSize(15);
        ll.addView(tv);

        tv = new TextView(this);
        tv.setText("Manufacturer URI: " + upnp_device.getDetails()
                .getManufacturerDetails().getManufacturerURI());
        tv.setTextSize(15);
        ll.addView(tv);

        tv = new TextView(this);
        tv.setText("Serial Number: " + upnp_device.getDetails()
                .getSerialNumber());
        tv.setTextSize(15);
        ll.addView(tv);

        tv = new TextView(this);
        tv.setText("PresentationURI: " + upnp_device.getDetails()
                .getPresentationURI());
        tv.setTextSize(15);

        ll.addView(tv);


    }

    public void addnewPoint(int x, int y){
        addBoolData( x,y );
        if (mChart != null){
            save_instance_state_counter = x;
            mChart.repaint();
        }
    }

}