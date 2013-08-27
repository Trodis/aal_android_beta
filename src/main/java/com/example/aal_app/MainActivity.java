package com.example.aal_app;

import android.os.Bundle;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

public class MainActivity extends ListActivity {

    public final static String EXTRA_MESSAGE = "Device Object";

    // private static final Logger log = Logger.getLogger(BrowseActivity.class.getName());

    private ArrayAdapter<DeviceDisplay> listAdapter;

    private ListView list;

    private BrowseRegistryListener registryListener = new BrowseRegistryListener();

    private AndroidUpnpService upnpService;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            upnpService = (AndroidUpnpService) service;

            // Refresh the list with all known devices
            listAdapter.clear();
            for (Device device : upnpService.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // Getting ready for future device advertisements
            upnpService.getRegistry().addListener(registryListener);

            // Search asynchronously for all devices
            upnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {

            upnpService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);

        list = getListView();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //To change body of implemented methods use File | Settings | File Templates.
                //final String item = parent.getItemAtPosition(position).toString();

                final ArrayList<String> upnp_device_informations = new ArrayList<String>();

                upnp_device_informations.add(listAdapter.getItem(position).getDeviceName());
                upnp_device_informations.add(listAdapter.getItem(position).getDeviceDescription());
                String serviceType_of_device = listAdapter.getItem(position).getServiceTypeOfDevice();


                if (serviceType_of_device.equals("SwitchPower")) {

                    Intent intent = new Intent(MainActivity.this, Switches.class);
                    intent.putExtra(EXTRA_MESSAGE, (Serializable) listAdapter.getItem(position).getDevice());
                    startActivity(intent);
                }

                showToast(upnp_device_informations.get(0) + " selected!", true);
            }
        });
    }

    @Override
    protected void onStart() {

        super.onStart();
        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }

        getApplicationContext().unbindService(serviceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(0, 0, 0, R.string.search_lan);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case 0:
                searchNetwork();
                break;
        }

        return false;
    }

    protected void searchNetwork() {

        if (upnpService == null) return;
        Toast.makeText(this, R.string.search_lan, Toast.LENGTH_SHORT).show();
        upnpService.getRegistry().removeAllRemoteDevices();
        upnpService.getControlPoint().search();
    }

    protected class BrowseRegistryListener extends DefaultRegistryListener {

        /* Discovery performance optimization for very slow Android devices! */

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {

            showToast(
                    "Discovery failed of '" + device.getDisplayString() + "': " +
                            (ex != null ? ex.toString() : "Couldn't retrieve device/service descriptors"),
                    true
            );
            deviceRemoved(device);
        }
        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {

            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {

            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {

            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {

            deviceRemoved(device);
        }

        public void deviceAdded(final Device device) {

            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceDisplay d = new DeviceDisplay(device);

                    int position = listAdapter.getPosition(d);
                    if (position >= 0) {
                        // Device already in the list, re-set new value at same position
                        listAdapter.remove(d);
                        listAdapter.insert(d, position);
                    } else {
                        listAdapter.add(d);
                    }

                    // Sort it?
                    // listAdapter.sort(DISPLAY_COMPARATOR);
                    // listAdapter.notifyDataSetChanged();
                }
            });
        }

        public void deviceRemoved(final Device device) {

            runOnUiThread(new Runnable() {
                public void run() {
                    listAdapter.remove(new DeviceDisplay(device));
                }
            });
        }

    }

    protected void showToast(final String msg, final boolean longLength) {

        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                        MainActivity.this,
                        msg,
                        longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                ).show();
            }
        });
    }


    static final Comparator<DeviceDisplay> DISPLAY_COMPARATOR =
            new Comparator<DeviceDisplay>() {
                public int compare(DeviceDisplay a, DeviceDisplay b) {
                    return a.toString().compareTo(b.toString());
                }
            };

}