package com.example.aal_app;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.androidplot.Plot;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.XYChart;
import org.achartengine.model.*;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.*;
import org.achartengine.renderer.XYSeriesRenderer;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.*;
import java.util.*;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import com.androidplot.Plot;
import com.androidplot.xy.*;

import java.util.Observable;
import java.util.Observer;


/**
 * @author Ferhat Özmen
 */



public class Switches extends Activity{

    private class MyPlotUpdater implements Observer {
        Plot plot;
        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }
        @Override
        public void update(Observable o, Object arg) {
            plot.redraw();
        }
    }

    private XYPlot mySimpleXYPlot;
    private XYPlot staticPlot;
    private MyPlotUpdater plotUpdater;
    DynamicXYDatasource data;
    private Thread myThread;

    //private Service service_from_upnp_device;
    private String EXTRA_MESSAGE = "UPNP Device";
    private static final String TAG = "AAL LOG: ";

    private Device upnp_device;
    private ArrayList input_value;
    private ArrayAdapter<DeviceDisplay> listAdapter;

    private AndroidUpnpService upnpService;
    private SubscriptionCallback callback;
    private String unique_device_identifier;
    private Bundle savedInstanceState;
    ArrayList numSightings = new ArrayList();
    ArrayList years = new ArrayList();
    SimpleXYSeries series2;

    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataSet = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

    private org.achartengine.model.XYSeries mCurrentSeries;
    private org.achartengine.renderer.XYSeriesRenderer mCurrentRenderer;

    private void initChart(){
        mCurrentSeries = new XYSeries("Sample Data");
        mDataSet.addSeries(mCurrentSeries);
        mCurrentRenderer = new XYSeriesRenderer();
        mRenderer.addSeriesRenderer(mCurrentRenderer);
        mRenderer.setPointSize(10);
    }

    private void addSampleData(){
        mCurrentSeries.add(1, 2);
        mCurrentSeries.add(2, 3);
        mCurrentSeries.add(3, 2);
        mCurrentSeries.add(4, 5);
        mCurrentSeries.add(5, 4);

    }


    private final static String seekbar_process_tag = "SeekBar Process";

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);

        Bundle extras = getIntent().getExtras();

        this.unique_device_identifier = extras.getString(EXTRA_MESSAGE);
        this.savedInstanceState = savedInstanceState;
        input_value = new ArrayList();
        //plotting();

    }

    @Override
    protected void onResume()
    {
        //myThread = new Thread(data);
        //myThread.start();
        super.onResume();
        if (upnpService != null && savedInstanceState == null)
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
            }
            createUPnPServiceandActionInformations(upnp_device);
        }
        else
        {
            this.savedInstanceState = null;
            onCreate(savedInstanceState);
        }

        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        if(mChart == null){
            initChart();
            addSampleData();
            mChart = ChartFactory.getCubeLineChartView(this, mDataSet, mRenderer, 0.3f);
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }
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

    @Override protected void onDestroy()
    {
        super.onDestroy();
        if (upnpService != null)
        {
            getApplicationContext().unbindService(serviceConnection);
            this.listAdapter    = null;
            this.upnp_device = null;
            this.upnpService = null;
            this.input_value = null;
        }

        if (callback != null)
        {
            callback.end();
        }

    }

    @Override
    protected void onPause()
    {
        //data.stopThread();
        super.onPause();
        onDestroy();
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
                    //repaint();
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
        } );
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
                                     final  Action action,
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
                input_value.add( String.valueOf( progress ) );
                executeAction( upnpService, action.getService(), action,
                               action_argument, input_value, true );
                input_value.clear();

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
                sb.setProgress( Integer.parseInt(value) );
            }
        });
    }

    public void setSeekBarRange(final int max_value, final Action action)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                final View ll = findViewById(R.id.LinearLayoutActionElements);

                SeekBar sb;
                sb = (SeekBar) ll.findViewWithTag( action.getName() );
                sb.setMax(max_value);
            }
        });

    }

    private void startEventlistening(Action action,
                                     StateVariable state_variable)
    {
            this.callback = new SwitchPowerSubscriptionCallback (action,
                            state_variable, this);

            upnpService.getControlPoint().execute(callback);

    }

    public void createSwitchPowerSubscription(Action action)
    {
        if(action.getService().hasStateVariables())
        {
            for (StateVariable state_variable :
                    action.getService().getStateVariables())
            {
                if(state_variable.getEventDetails().isSendEvents())
                {
                    startEventlistening(action, state_variable);
                    Log.v(TAG, "STATEVARIABLE: " + state_variable);

                }
            }
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
                    createSwitchPowerSubscription(action);
            }
            else if (action_argument.getDatatype().getBuiltin().equals
                        (Datatype.Builtin.UI1))
            {
                    createSeekBarActions(service, action, action_argument);
                    createSwitchPowerSubscription(action);
            }
        }

        for (ActionArgument action_argument : action.getOutputArguments())
        {
                createOutPutActions(action, action_argument);
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

        for (Service services : upnp_device.getServices())
        {
            tv = new TextView(this);
            tv.setText("Service " + services.getServiceType().getType() + ":");
            tv.setHighlightColor(1);
            tv.setTextColor(Color.rgb(50, 205, 50));
            tv.setTextSize(17);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            ll.addView(tv, p);

            for(Action actions : services.getActions())
            {
                tv = new TextView(this);
                tv.setText(actions.getName());
                tv.setTextSize( 15 );
                ll.addView(tv, p);
            }
        }
    }

    public void createUPnPGeneralInformations(Service service)
    {
        LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutActionElements);

        TextView tv;

        tv = new TextView(this);
        tv.setText("Device Name: " + service.getDevice().getDetails()
                .getFriendlyName());
        tv.setTextSize(15);

        tv = new TextView(this);
        tv.setText("Device Discription: " +service.getDevice().getDetails()
                .getManufacturerDetails());
        tv.setTextSize(15);

        ll.addView(tv);
    }

    public void plotting(){

       // mySimpleXYPlot = (XYPlot) findViewById(R.id.testPlot);
        Number[] numSightings = {1, 2, 3, 4, 5};
        Number[] years =        {12, 22, 13, 54, 11};

        // create our series from our array of nums:
        /*this.series2 = new SimpleXYSeries(
                Arrays.asList(years),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                "Sightings in USA");
            */

        DynamicSeries series2 = new DynamicSeries("Series");
        mySimpleXYPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
        mySimpleXYPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        mySimpleXYPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        mySimpleXYPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
        mySimpleXYPlot.getBorderPaint().setStrokeWidth(3);
        mySimpleXYPlot.getBorderPaint().setAntiAlias(true);
        mySimpleXYPlot.getBorderPaint().setColor(Color.WHITE);

        // Create a formatter to use for drawing a series using LineAndPointRenderer:
        LineAndPointFormatter series1Format = new LineAndPointFormatter(
                Color.rgb(0, 100, 0),                   // line color
                Color.rgb(0, 100, 0),                   // point color
                Color.rgb(100, 200, 0), null);          // fill color

        // setup our line fill paint to be a slightly transparent gradient:
        Paint lineFill = new Paint();
        lineFill.setAlpha(200);
        lineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.GREEN, Shader.TileMode.MIRROR));

        LineAndPointFormatter formatter  = new LineAndPointFormatter(Color.rgb(0, 0,0), Color.BLUE, Color.RED, null);
        formatter.setFillPaint(lineFill);
        mySimpleXYPlot.getGraphWidget().setPaddingRight(2);
        mySimpleXYPlot.addSeries(series2, formatter);

        // draw a domain tick for each year:
        //mySimpleXYPlot.setDomainBoundaries(0, 24, BoundaryMode.FIXED);
        //mySimpleXYPlot.setRangeBoundaries(0, 1, BoundaryMode.FIXED);

        // customize our domain/range labels
        mySimpleXYPlot.setDomainLabel("Year");
        mySimpleXYPlot.setRangeLabel("# of Sightings");
        mySimpleXYPlot.setDomainStepMode(XYStepMode.SUBDIVIDE);
        mySimpleXYPlot.setDomainStepValue(series2.size());
        //mySimpleXYPlot.setDomainValueFormat(new DecimalFormat("0"));
        //mySimpleXYPlot.setDomainStep(XYStepMode.SUBDIVIDE, series2.size());
        //mySimpleXYPlot.setRangeStep(XYStepMode.SUBDIVIDE, series2.size());
        mySimpleXYPlot.setRangeValueFormat(new DecimalFormat("0"));
        //mySimpleXYPlot.setRangeStepMode(XYStepMode.SUBDIVIDE);
        mySimpleXYPlot.setRangeBoundaries(0, 1, BoundaryMode.FIXED);

        //mySimpleXYPlot.setR

        // get rid of decimal points in our range labels:
        mySimpleXYPlot.setRangeValueFormat(new DecimalFormat("0"));

      mySimpleXYPlot.setDomainValueFormat(new Format() {

            // create a simple date format that draws on the year portion of our timestamp.
            // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
            // for a full description of SimpleDateFormat.
            private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                // because our timestamps are in seconds and SimpleDateFormat expects milliseconds
                // we multiply our timestamp by 1000:
                long timestamp = ((Number) obj).longValue();
                Date date = new Date(timestamp);
                return dateFormat.format(date, toAppendTo, pos);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;

            }
        });

        // by default, AndroidPlot displays developer guides to aid in laying out your plot.
        // To get rid of them call disableAllMarkup():

    }

    public void achart(){

    }

    public void repaint(){
       /*
        Number[] numSightings = {1,0,1,0,1};
        ArrayList test = new ArrayList();
        test.add(33);
        test.add(42);
        test.add(544);

        series2.setModel(test, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
        series2.setXY(23,45, 0);
         */
        mySimpleXYPlot.redraw();
    }

    private class ANFormat extends Format {
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            Number num = (Number) obj;

            // using num.intValue() will floor the value, so we add 0.5 to round instead:
            int roundNum = (int) (num.floatValue() + 0.5f);
            switch(roundNum) {
                case 0:
                    toAppendTo.append("Aus");
                    break;
                case 1:
                    toAppendTo.append("An");
                default:
                    toAppendTo.append("Unknown");
            }
            return toAppendTo;
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return null;  // We don't use this so just return null for now.
        }
    }

}