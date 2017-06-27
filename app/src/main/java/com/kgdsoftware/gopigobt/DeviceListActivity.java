package com.kgdsoftware.gopigobt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        // Get the list of bonded bluetooth devices
        // copy the devices to the adapter

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
             // device does not support bluetooth
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> deviceList = new ArrayList<>();
        deviceList.addAll(pairedDevices);
        DeviceAdapter adapter = new DeviceAdapter(this, R.layout.bluetooth_list_row, deviceList);

        ListView listView = (ListView)findViewById(R.id.device_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.v(TAG, "item clicked: " + i + " view: " + view.getId());
                ListView listView = (ListView)adapterView;
                BluetoothDevice device = (BluetoothDevice)listView.getItemAtPosition(i);
                Log.v(TAG, "  " + device.getName() + " at " + device.getAddress());

                Intent returnIntent = new Intent();
                returnIntent.putExtra("device", device);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

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
