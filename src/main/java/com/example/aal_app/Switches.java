package com.example.aal_app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.*;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
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
 * In dieser Klassen wird das ausgew&auml;hlte UPnP Ger&auml;t,
 * vollst&auml;ndig ausgewertet mit seinen Services, Statevariablen, Actions und Action Argumenten. Anschlie&szlig;end wird die
 * entsprechende GUI aufgebaut und auf dem Tablet dargestellt. Zudem wird die relevante Statevariable angemeldet,
 * um auf Events zu reagieren, um die GUI, mit den neuen Werten vom UPnP Ger&auml;t zu aktualisieren.
 * @author Ferhat &Ouml;zmen
 * @version 0.1
 */


public class Switches extends Activity{

    private String EXTRA_MESSAGE = "UPNP Device";
    private static final String TAG = "AAL LOG: ";
    private final static String seekbar_process_tag = "SeekBar Process";
    private final static String gena_status_textview = "genau_status_textview";

    private Device upnp_device;
    private ArrayList input_value;

    private AndroidUpnpService upnpService;
    private LinkedList<SubscriptionCallback> subscription_list;
    private LinkedList<StateVariable> stateVariables_list;
    private SubscriptionCallback callback;
    private String unique_device_identifier;
    private boolean statevariable_gone_offline = false;

    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataSet = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

    private TimeSeries mCurrentSeries;
    private XYSeriesRenderer mCurrentRenderer;

    private int save_instance_state_counter = 0;

    /**
     * Der UPnP Serivce wird hier seperat angelegt. Um unabh&auml;ngig von anderen Klassen,
     * das UPnP Ger&auml;t auszuwerten zu k&ouml;nnen. Anschlie&szlig;end wird die onResume() Methode explizit aufgerufen um
     * sicherzustellen, dass onCreate() nicht aufgerufen wird, bevor ein upnpservice agefordert wurde.
     *
     * @param className F&uuml;r welche Klasse der Service gestartet werden soll
     * @param service Der eigentliche Service, der gebunden und im Hintergrund
     *                laufen soll
     */
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            upnpService = (AndroidUpnpService) service;
            onResume();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            upnpService = null;
        }
    };

    /**
     * In dieser Methode wird das Diagramm initialisiert. Das hei&szlig;t alle Objekte, die n&ouml;tig sind um mit der Bibliothek
     * acharts ein Diagramm anzufertigen, werden erzeugt. Zu dem werden die optischen Eigenschaften,
     * des Diagrammes festgelegt und kann auch nach eigenen W&uuml;nschen umgestaltet werden.
     *
     * @param state_variable die Statevariable wird, welches im Diagramm &uuml;berwacht werden soll, wird erwartet.
     */
    private void initChartBoolean(String state_variable)
    {
        if (mCurrentRenderer == null && mCurrentSeries == null)
        {
            mCurrentRenderer = new XYSeriesRenderer();
            mCurrentSeries = new TimeSeries("Monitoring State Variable: " + state_variable);
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

    /**
     * Diese Methode f&uuml;gt auf dem Diagramm einen neuen Punkt an der Stelle x,y ein mit einem Zeitstempel.
     *
     * @param x Die x Koordinate, in diesem Falle die Anzahl der Event Zyklen.
     * @param y Die y Koordinate, in diesem Falle der Wert, welcher dem UPnP Ger&auml;t gesendet wurde.
     */
    private void addBoolData(long x, int y)
    {
        mCurrentSeries.add(x, y);
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat( "HH:mm:ss" );
        mCurrentSeries.addAnnotation(format.format(date) + " Uhr", x, y);
    }

    /**
     * Android Framework onDestroy() Methode (siehe Android Lifecycle auf google).
     * onCreate Methode vom Android Framework (siehe Android Lifecycle auf google). Diese Methode wurde &uuml;berschrieben
     * um die Auswertung des UPnP Ger&auml;tes zu bewerkstelligen. Das hei&szlig;t hier werden die Services,
     * Statevariablen  und Actions ausgewertet und die GUI entsprechend aufgebaut. Anschlie&szlig;end wird noch die
     * Statevariable als GENAEvent angemeldet.
     *
     * @param savedInstanceState Ein mapping von String Werten
     *                           und Elementen die vom Typ Parcelable sind.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);

        Bundle extras = getIntent().getExtras();

        this.unique_device_identifier = extras.getString(EXTRA_MESSAGE);
        input_value = new ArrayList();
        subscription_list = new LinkedList<SubscriptionCallback>();
        stateVariables_list = new LinkedList<StateVariable>();

        if (upnpService != null)
        {
            upnp_device = upnpService.getRegistry().getDevice(UDN.valueOf(unique_device_identifier), true);
        }

        if (this.upnp_device != null)
        {
            createUPnPServiceandActionInformations(upnp_device);
            for (Service service : upnp_device.getServices())
            {
                for(Action action : service.getActions())
                {
                    generateUI(service, action);
                }
                setSubscription(service);
            }
        }
    }

    /**
     * Android Framework onResume() Methode (siehe Android Lifecycle auf google).
     * Die onResume() Methode wird aufgerufen, um genau die Reihenfolge festzulegen. Es darf also onCreate() nicht
     * aufgerufen werden, bevor der upnpservice angefordert wurde.
     *
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        onCreate(null);
    }

    /**
     * Android Framework onStart() Methode (siehe Android Lifecycle auf google).
     * Die onStart Methode wird noch vor onCreate() aufgerufen. Der eigentlich upnpservice,
     * wird also hier f&uuml;r diese Klasse gebunden und gestartet.
     *
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        getApplicationContext().bindService(new Intent(this, AndroidUpnpServiceImpl.class), serviceConnection,
                                                                                            Context.BIND_AUTO_CREATE);
    }

    /**
     * Diese Methode speichert, die Werte vom Diagramm, falls das Tablet gedreht wurde. Also die Orientierung sich
     * ge&auml;ndert hat. Denn bei einer neuen Orientierung, werden alle Objekte zerst&ouml;rt und wieder neu aufgebaut. Jedoch
     * muss man sich selber um die speicherung der relevanten Daten k&uuml;mmern.
     *
     * @param outStat variable vom Typ Bundle wird &uuml;bergeben. Die Klasse Bundle hat viele Hilfsmethoden um Daten zu
     *                speichern.
     */
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

    /**
     * Diese Methode wird aufgerufen, um die zuvor gespeicherten Werte wieder abzurufen und die objekte neu zu
     * initialiseren.
     *
     * @param savedInstanceState variable vom Typ Bundle wird &uuml;bergeben, Die Klasse Bundle hat viele Hilfsmethoden
     *                           um die Daten wieder abzrufen.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        mDataSet = (XYMultipleSeriesDataset) savedInstanceState.getSerializable("dataset");
        mRenderer = (XYMultipleSeriesRenderer) savedInstanceState.getSerializable("renderer");
        mCurrentSeries = (TimeSeries) savedInstanceState.getSerializable("current_series");
        mCurrentRenderer = (XYSeriesRenderer) savedInstanceState.getSerializable("current_renderer");
        save_instance_state_counter = savedInstanceState.getInt("xAxisCounter");
    }

    /**
     * Diese Methode wird aufgerufen wenn die App geschlossen wird bzw. beendet. Die Statevariablen werden komplett
     * abgemeldet, um unnötige Ressourcen nicht zu verschwenden.
     *
     */
    @Override protected void onDestroy()
    {
        super.onDestroy();
        if (!subscription_list.isEmpty())
        {
            while(!subscription_list.isEmpty())
            {
                subscription_list.poll().end();
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //onDestroy();
    }

    /**
     * Die executeAction Methode, ruft die Methode der Klasse SetTargetActionInvocation auf,
     * um dem UPnP Ger&auml;t die Argumente bzw. Werte zu senden. Es wird gepr&uuml;ft ob es sich um einen Input oder Output
     * Argument handelt, dass mit neuen Werten versorgt werden soll.
     *
     * @param upnpService das upnpservice des jeweiligen UPnP Ger&auml;tes wird erwartet.
     * @param action die action vom jeweiligen UPnP Ger&auml;t wird erwartet.
     * @param action_argument das Action Argument vom UPnP Ger&auml;t wird erwartet.
     * @param input_value der eigentliche Wert der an das UPnP Ger&auml;t gesendet werden soll, wird erwartet.
     * @param isInput es wird gepr&uuml;ft ob es sich um einen Input oder Output Wert handelt.
     */
    protected void executeAction(AndroidUpnpService upnpService, final Action action,
                                 final ActionArgument action_argument, ArrayList input_value, boolean isInput)
    {

        ActionInvocation setTargetInvocation = new SetTargetActionInvocation (action, action_argument, input_value,
                                                                              isInput);

        // Executes asynchronous in the background
        upnpService.getControlPoint().execute(new ActionCallback(setTargetInvocation)
                {
                    @Override
                    public void success(ActionInvocation invocation)
                    {
                        if (action.hasOutputArguments())
                        {
                            ActionArgumentValue value  = invocation.getOutput(action_argument.getName());
                            showToast("Received Value: " + value.getValue().toString(), false);
                        }
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg)
                    {
                        showToast(defaultMsg, true);
                    }
                }
        );
    }

    /**
     * In dieser Methode werden die Buttons erzeugt f&uuml;r die InputActions. Die Buttons bekommen einen Tag,
     * um sp&auml;ter zuordnen zu k&ouml;nnen zur welcher Action das jewelige Button geh&ouml;rt. Die Methode executeAction wird
     * aufgerufen und die entsprechenden boolean Werte &uuml;bergeben. Abh&auml;ngig davon in welcher Stellung der Button ist
     * (AN oder AUS).
     *
     * @param action die action vom jeweiligen UPnP Ger&auml;t wird erwartet.
     * @param action_argument das Action Argument vom UPnP Ger&auml;t wird erwartet.
     */
    public void createInputActions(final Action action, final ActionArgument action_argument)
    {
        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutInputActionElements);

        TextView tv = (TextView) (findViewById( R.id.InputActionTitle ));
        tv.setText("Input Action");

        Switch sw = new Switch(this);
        sw.setText(action.getName());
        sw.setTag(action.getName());
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked)
            {

                if (isChecked)
                {
                    input_value.add(true);
                    executeAction(upnpService, action, action_argument, input_value, true );
                    input_value.clear();
                }
                else
                {
                    input_value.add(false);
                    executeAction(upnpService, action, action_argument, input_value, true );
                    input_value.clear();
                }
            }
        });
        ll.addView(sw);
    }

    /**
     * In dieser Methode werden die Buttons erzeugt f&uuml;r die OutputActions. Die Buttons bekommen einen Tag,
     * um sp&auml;ter zuordnen zu k&ouml;nnen zur welcher Action das jewelige Button geh&ouml;rt. Sobald ein OutputButton bet&auml;tigt
     * wurde, wir die Methode setOnClickListener aufgerufen und daraufhin die executeAction Methode um dem UPnP Ger&auml;t
     * den neuen Wert zu senden.
     *
     * @param action die action vom jeweiligen UPnP Ger&auml;t wird erwartet.
     * @param action_argument das Action Argument vom UPnP Ger&auml;t wird erwartet.
     */
    public void createOutPutActions(final Action action, final ActionArgument action_argument)
    {
        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutOutPutActionElements);
        TextView tv = (TextView) findViewById( R.id.OutPutActionTitle );
        tv.setText( "Output Actions" );

        Button button = new Button(this);
        button.setText(action.getName());
        button.setTag(action.getName());
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                executeAction(upnpService, action, action_argument, input_value, false);
            }
        });
        ll.addView(button);
    }

    /**
     * In dieser Methode werden die SeekBar erzeugt f&uuml;r die InputActions. Das jeweilige SeekBar bekommt einen Tag,
     * um sp&auml;ter zuordnen zu k&ouml;nnen zur welcher Action das SeekBar geh&ouml;rt. Die Methode pr&uuml;ft ob und wann der
     * Schiebeschalter bet&auml;tigt und wieder losgelassen wurde. Dementsprechend werden die Informationen &uuml;ber die
     * SeekBar aktualisiert und der neue Wert an die Methode executeAction() &uuml;bergeben.
     *
     * @param service der Service vom UPnP Ger&auml;t wird erwartet.
     * @param action die action vom jeweiligen UPnP Ger&auml;t wird erwartet.
     * @param action_argument das Action Argument vom UPnP Ger&auml;t wird erwartet.
     */
    public void createSeekBarActions(final Service service, final Action action, final ActionArgument action_argument)
    {
        int max_range = (int) service.getStateVariable(action_argument.getRelatedStateVariableName()).getTypeDetails()
                                                                        .getAllowedValueRange().getMaximum();

        final LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutSeekBarElements);

        TextView titlev = (TextView) (findViewById( R.id.SeekBarActionTitle ));
        titlev.setText( "Seekbar Actions" );

        SeekBar sb = new SeekBar(this);
        sb.setTag(action.getName());
        sb.setMax(max_range);
        sb.setProgressDrawable( getResources().getDrawable(R.drawable.progressbar));

        sb.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener()
        {
            TextView tv;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
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
                input_value.add( String.valueOf(seekBar.getProgress()));
                executeAction(upnpService, action, action_argument, input_value, true );
                input_value.clear();
            }
        } );

        TextView tv = new TextView(this);
        tv.setTag(seekbar_process_tag);

        ll.addView(tv);
        ll.addView(sb);
    }

    /**
     * Diese Methode wird aufgerufen, wenn ein GENA Event eingegangen ist. Nachdem das GENA Event ausgewertet wurde,
     * in der Klasse SwitchPowerSubscriptionCallback, wird diese Methode aufgerufen um den Switch Status zu
     * aktualisieren.
     *
     * @param is_checked welchen Wert der Switch bekommen soll.
     * @param action durch den action parameter wird der der Tag name des Switches ermittelt.
     */
    public void setSwitch(final boolean is_checked,final Action action)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                final View ll = findViewById(R.id.LinearLayoutActionElements);

                Switch mySwitch;
                mySwitch = (Switch) ll.findViewWithTag(action.getName());
                mySwitch.setChecked(is_checked);
            }
        });
    }

    /**
     * Diese Methode wird aufgerufen, wenn ein GENA Event eingegangen ist. Nachdem das GENA Event ausgewertet wurde,
     * in der Klasse SwitchPowerSubscriptionCallback, wird diese Methode aufgerufen um den SeekBar Status zu
     * aktualisieren.
     *
     * @param value neuer Wert f&uuml;r das SeekBar.
     * @param action durch den action parameter wird der der Tag name des Switches ermittelt.
     */
    public void setSeekBar(final String value, final  Action action)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                final View ll = findViewById(R.id.LinearLayoutActionElements);

                SeekBar sb;
                sb = (SeekBar) ll.findViewWithTag(action.getName());
                sb.setProgress(Integer.parseInt(value));
            }
        });
    }

    /**
     * In dieser Methode wird die Statevariable angemeldet und das horchen nach neuen GENA Events gestartet.
     * Zusätzlich wird das Diagramm vorbereitet für die endgültige Darstellung.
     * @param state_variable
     */
    private void startEventlistening(StateVariable state_variable)
    {
        subscription_list.add(new SwitchPowerSubscriptionCallback(state_variable, this,
                                                                  save_instance_state_counter));
        upnpService.getControlPoint().execute(subscription_list.getLast());
        if(mChart == null)
        {
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            //initChartBoolean();
            mChart = ChartFactory.getCubeLineChartView(this, mDataSet, mRenderer, 0.01f);

            // enable the chart click events
            mRenderer.setClickEnabled(true);
            mRenderer.setSelectableBuffer(50);
            mChart.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v) {
                    // handle the click event on the chart
                    SeriesSelection seriesSelection = mChart.getCurrentSeriesAndPoint();

                    if (seriesSelection != null)
                    {
                        // display information of the clicked point
                        showToast("Statevariable-Wert: " + seriesSelection.getValue() + " Zyklus-Wert: " +
                                  seriesSelection.getXValue(), false );
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

    /**
     * In dieser Methode wird der Service vom UPnP Ger&auml;t genauer untersucht. Je nachdem um was f&uuml;r eine Action es
     * sich handelt, wir das entsprechende GUI Element erzeugt. Dabei ist wichtig zu untersuchen,
     * um welchen Typ es sich handelt bei der Action. Es werden nur Actions vom Typ Boolean oder UI1 durchgelassen.
     *
     * @param service der Service vom UPnP Ger&auml;t wird erwartet.
     * @param action die action vom jeweiligen UPnP Ger&auml;t wird erwartet.
     */
    private void generateUI(Service service, Action action)
    {

        for (ActionArgument action_argument : action.getInputArguments())
        {
            if(action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.BOOLEAN))
            {
                    createInputActions(action, action_argument);
            }
            else if (action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.UI1))
            {
                    createSeekBarActions(service, action, action_argument);
            }
        }

        for (ActionArgument action_argument : action.getOutputArguments())
        {
            if(action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.BOOLEAN) ||
               action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.UI1))
            {
                createOutPutActions(action, action_argument);
            }
        }

    }

    /**
     * Die Statevariablen vom UPnP Ger&auml;t, werden in dieser Methode ausgewertet und gefilter. Es werden Statevariablen
     * nur vom Typ Boolean oder UI1 verarbeitet. Anschlie&szlig;end wird die Statevariable, die eine der Bedingung erf&uuml;llt,
     * einer Liste hinzugef&uuml;gt und anschlie&szlig;end wir diese Liste mit den Statevariablen an die Methode
     * startEventlistening() weiter gereicht, um die GENA Anmeldung zu starten.
     *
     * @param service der Service vom UPnP Ger&auml;t wird erwartet.
     */
    private void setSubscription(Service service)
    {
        for (StateVariable stateVariable : service.getStateVariables())
        {
            Datatype.Builtin stateVariable_dataType = stateVariable.getTypeDetails().getDatatype().getBuiltin();
            if (stateVariable.getEventDetails().isSendEvents() &&
                        (stateVariable_dataType.equals( Datatype.Builtin.BOOLEAN) ||
                            stateVariable_dataType.equals(Datatype.Builtin.UI1)))
            {
                initChartBoolean(stateVariable.getName());
                startEventlistening(stateVariable);
                stateVariables_list.add(stateVariable);
            }
        }
    }

    /**
     * Diese Methode hat einfach nur die Aufgabe, eine entsprechende Meldung auf dem Tablet anzuzeigen. Es kann f&uuml;r
     * jeden Zweck genutzt werden. Sei es Fehlermeldung, oder eine Information die man dem Nutzer mitteilen m&ouml;chte.
     *
     * @param msg die Nachricht die angezeigt werden soll.
     * @param longLength wie lange es angezeigt werden soll.
     */
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

    /**
     * Diese Methode wird durchlaufen um die allgemeinen Informationen &uuml;ber das UPnP Ger&auml;t anzuzeigen. Zudem soll
     * angezeigt werden, ob die Statevariable noch angemeldet ist oder nicht.
     *
     * @param upnp_device die Referenz vom UPnP Ger&auml;t wird erwartet. um die Informationen &uuml;ber das Ger&auml;t einzholen.
     */
    public void createUPnPServiceandActionInformations(Device upnp_device)
    {
        LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutDeviceInformation);
        TextView tv;
        TextView gena_status_tv;

        gena_status_tv = new TextView(this);
        gena_status_tv.setText("GENA Subscription Status: not activated! ");
        gena_status_tv.setTextSize(15);
        gena_status_tv.setTextColor(Color.MAGENTA);
        gena_status_tv.setTag(gena_status_textview);
        ll.addView(gena_status_tv);

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

    /**
     * Diese Methode wird aufgerufen, wenn man einen neuen Punkt im Diagramm eintragen m&ouml;chte. Zudem hat diese
     * Methode die Aufgabe, die Zyklen der GENA Events aufzuzeichnen.
     *
     * @param x Die x Koordinate, in diesem Falle die Anzahl der Event Zyklen.
     * @param y Die y Koordinate, in diesem Falle der Wert, welcher dem UPnP Ger&auml;t gesendet wurde.
     */
    public void addnewPoint(int x, int y){
        addBoolData( x,y );
        if (mChart != null){
            save_instance_state_counter = x;
            mChart.repaint();
        }
    }

    /**
     * Diese Methode wird von der Klasse SwitchPowerSubscriptionCallback aufgerufen,
     * um den Status der Statevariable aktuell zu halten. Ob die Statevariable noch angemeldet ist oder nicht.
     */
    public void updateGENAStatusTextView()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                LinearLayout ll;
                ll = (LinearLayout) findViewById(R.id.LinearLayoutDeviceInformation);
                TextView gena_status_tv;
                gena_status_tv = (TextView) ll.findViewWithTag(gena_status_textview);

                if (getStateVariableStatus())
                {
                    gena_status_tv.setText( "GENA Subscription Status: offline" );
                    gena_status_tv.setTextColor( Color.RED );
                }
                else
                {
                    gena_status_tv.setText( "GENA Subscription Status: online" );
                    gena_status_tv.setTextColor( Color.GREEN );
                }
            }
        });
    }

    /**
     /**
     * Das Standard Men&uuml; der Activity wird initialisiert, wenn der User w&uuml;nscht, eine manuelle Suche zu starten. Kann
     * der User &uuml;ber das Android Standard Activity Men&uuml;, eine neue Suche starten.
     * @param menu das Men&uuml; Parameter wird automatisch
     *             vom Framework zur Laufzeit &uuml;bergeben.
     * @return Diese Methode muss true zur&uuml;ck liefern, damit das Men&uuml; angezeigt wird.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, 0, 0, R.string.establish_subscription);
        return true;
    }

    /**
     * Wenn aus dem Men&uuml; das Element ausgew&auml;hlt wurde, eine manuelle Suche zu starten.
     * Wird diese Methode vom Framework aufgerufen. Um anschlie&szlig;end die Methode searchNetwork() aufzurufen.
     *
     * @param item Welches men&uuml; Element ausgew&auml;hlt wurde.
     *             Anhand der ID kann festgestellt welches men&uuml; Element ausgew&auml;hlt wurde.
     * @return Der R&uuml;ckgabewert ist false, sofern die ID nicht gefunden werden kann.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case 0:
                reestablish_gena();
                break;
        }
        return false;
    }

    /**
     * In dieser &ouml;ffentlichen Methode wird der Klasse SwitchPowerSubscriptionCallback die M&ouml;glichkeit geboten,
     * den Status der Statevariable zu aktualisieren.
     *
     * @param status der neue Status als Boolean.
     */
    public void setStateVariableStatus(boolean status)
    {
        statevariable_gone_offline = status;
    }

    /**
     * &Ouml;ffentliche Methode um den Status der Statevariable abzufragen.
     *
     * @return boolean Wert der Statevariable
     */
    public boolean getStateVariableStatus()
    {
        return this.statevariable_gone_offline;
    }

    /**
     * Diese Methode dient dazu, um ein UPnP Ger&auml;t, dessen GENA Event verloren ging, wiederherzustellen.
     * der Zyklus Counter wird hochgez&auml;hlt, damit die Kurve im Diagramm sich nicht &uuml;berlagert,
     * sondern genau dort weiter macht, wo das GENA Event zuletzt abgebrochen wurde.
     *
     */
    private void reestablish_gena()
    {
        if(statevariable_gone_offline)
        {
            save_instance_state_counter++;
            startEventlistening(stateVariables_list.getLast());

        }
        else
        {
            showToast("Subscription already online", false);
        }

    }

}