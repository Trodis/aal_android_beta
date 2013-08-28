package com.example.aal_app;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;

/**
 * Created with IntelliJ IDEA.
 * User: trodis
 * Date: 28.08.13
 * Time: 03:22
 * To change this template use File | Settings | File Templates.
 */
public class UPnPUnknownDevice extends Activity {

    Device mydevice;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.unknowndevice);

        Bundle extras = getIntent().getExtras();
        int position = extras.getInt(MainActivity.EXTRA_MESSAGE);

        DeviceDisplay deviceDisplay = MainActivity.listAdapter.getItem(position);
        mydevice = deviceDisplay.getDevice();

        TableLayout outerTableLayout = (TableLayout) findViewById(R.id.tableLayout);

        TableRow tr1 = new TableRow(this);
        TextView tv1 = new TextView(this);

        tv1.setText("Service Types found for Device " + deviceDisplay.getDeviceName());
        tv1.setTextSize(20);
        tr1.addView(tv1);
        outerTableLayout.addView(tr1);

        int counter = 1;
        for (Service services : deviceDisplay.getServices()) {

            TableRow tr = new TableRow(this);
            TextView tv = new TextView(this);
            tv.setSingleLine(false);
            tv.setText("Service " + counter + ": "+ services.getServiceType().getType());
            tr.addView(tv);

            outerTableLayout.addView(tr);
            counter++;
        }


    }



}
