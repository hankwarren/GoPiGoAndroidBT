package com.kgdsoftware.gopigobt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
    private static final String TAG = "DL";
    public static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        // Get the list of bonded bluetooth devices
        // copy the devices to the adapter

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth not supported")
                    .setMessage("BluetoothAdapter is null.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // See if we have already selected a device sometime in the past
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        String saveMacAddress = getString(R.string.saved_mac_address);
        String macAddress = preferences.getString(saveMacAddress, "notpaired");

        if (!macAddress.equals("notpaired")) {
            BluetoothDevice bluetoothDevice = findDeviceByMac(bluetoothAdapter, macAddress);
            if (bluetoothDevice != null) {
                showControl(bluetoothDevice);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth device found")
                        .setMessage("Looked for " + macAddress)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        populateList(bluetoothAdapter);
    }

    private BluetoothDevice findDeviceByMac(BluetoothAdapter bluetoothAdapter, String macAddress) {
        Set<BluetoothDevice>pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (macAddress.equals(device.getAddress())) {
                return device;
            }
        }
        return null;
    }

    private void populateList(BluetoothAdapter bluetoothAdapter) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> deviceList = new ArrayList<>();
        deviceList.addAll(pairedDevices);
        DeviceAdapter adapter = new DeviceAdapter(this, R.layout.bluetooth_list_row, deviceList);

        ListView listView = (ListView) findViewById(R.id.device_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.v(TAG, "item clicked: " + i + " view: " + view.getId());
                ListView listView = (ListView) adapterView;
                showControl((BluetoothDevice) listView.getItemAtPosition(i));
            }
        });
    }

    private void saveMacAddress(String macAddress) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String saveMacAddress = getString(R.string.saved_mac_address);
        editor.putString(saveMacAddress, macAddress);
        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Log.v(TAG, "User enabled bluetooth");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            populateList(bluetoothAdapter);
        } else if (resultCode == RESULT_CANCELED) {
            finish();
        }
    }

    private void showControl(BluetoothDevice device) {
        Log.v(TAG, "  " + device.getName() + " at " + device.getAddress());
        saveMacAddress(device.getAddress());
        Intent intent = new Intent(this, ControlActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
    }

    public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
        public DeviceAdapter(Context context,int textViewResourceId) {
            super(context, textViewResourceId);
        }
        public DeviceAdapter(Context context, int resource, List<BluetoothDevice> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater inflater;
                inflater = LayoutInflater.from(getContext());
                v = inflater.inflate(R.layout.bluetooth_list_row, null);
            }

            BluetoothDevice d = getItem(position);

            if (d != null) {
                TextView name = (TextView)v.findViewById(R.id.device_name);
                TextView mac = (TextView)v.findViewById(R.id.device_mac);

                if (name != null) {
                    name.setText(d.getName());
                }

                if (mac != null) {
                    mac.setText(d.getAddress());
                }
            }
            return v;
        }
    }
}
