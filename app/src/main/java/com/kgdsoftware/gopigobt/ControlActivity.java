package com.kgdsoftware.gopigobt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.R.attr.data;
import static android.R.attr.defaultValue;
import static java.lang.reflect.Array.getInt;


public class ControlActivity extends AppCompatActivity implements TouchPad.Listener {
    private static final String TAG = "GP";
    private static int IP_PORT = 33333;
    public static String gopigoAddress = null;
    public static Executor executor = Executors.newSingleThreadExecutor();

    private boolean autopilot = false;
    private boolean active = false;
    private View activeView = null;
    private TouchPad touchPad;

    private String pattern = "####.##";
    private DecimalFormat decimalFormat = new DecimalFormat(pattern);
    private boolean forward;
    private boolean left;
    private int speed = 100;

    private String address;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;

    private Receiver receiver = new Receiver();

    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static byte[] buffer = new byte[1024];

    private final int REQUEST_ENABLE_BT = 100;
    private final int REQUEST_MAC = 101;

    private final String BLUETOOTH_SOCKET = "com.kgdsoftware.gopigobt.SOCKET";
    private final String BLUETOOTH_CONNECT = "com.kgdsoftware.gopigobt.CONNECT";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        bluetoothDevice = (BluetoothDevice)intent.getParcelableExtra("device");

        Log.v(TAG, "ControlActivity.onCreate: " + bluetoothDevice.getName());

        touchPad = (TouchPad) findViewById(R.id.touch_pad);
        touchPad.setListener(this);

        SeekBar speedBar = (SeekBar) findViewById(R.id.speedBar);
        assert speedBar != null;
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar speedkBar, int progress, boolean fromUser) {
                updateSpeedLabel(progress);
                speed = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // It was too noisy to send the command in onProgressChanged.
                sendCommand("speed " + speed);
            }
        });

        updateSpeedLabel(speed);

        // The onResume will continue the setup process...
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
        setTitle("Pause...");
        unregisterReceiver(receiver);
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.v(TAG, "Could not close socket: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");

        Log.v(TAG, "use address: " + address + " name: " + getDeviceName());

        setTitle(getDeviceName());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLUETOOTH_SOCKET);
        intentFilter.addAction(BLUETOOTH_CONNECT);
        registerReceiver(receiver, intentFilter);

        executor.execute(new GetBluetoothSocket());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gopigo_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.kill_item:
                sendCommand("kill");
                return true;

            case R.id.say_ip_address_item:
                sendCommand("sayip");
                return true;

            case R.id.show_lidar_item:
                sendCommand("showlidar");
                return true;

            case R.id.auto_pilot_item:
                //Intent autoPilotIntent = new Intent(this, AutoPilotActivity.class);
                //startActivity(autoPilotIntent);
                if (autopilot) {
                    sendCommand("stopauto");
                } else {
                    sendCommand("startauto");
                }
                autopilot = !autopilot;
                return true;

            case R.id.show_device_list:
                Intent showDeviceList = new Intent(this, DeviceListActivity.class);
                startActivityForResult(showDeviceList, REQUEST_MAC);
                return true;

            case R.id.exit_item:
                finish();
                return true;
        }
        return false;
    }

    private String getDeviceName() {
        if (bluetoothDevice == null) {
            return "Waiting...";
        } else {
            return bluetoothDevice.getName();
        }
    }



    private void updateSpeedLabel(int speed) {
        TextView textView = (TextView) findViewById(R.id.speedView);
        assert textView != null;
        textView.setText(Integer.toString(speed));

        final SeekBar speedBar = (SeekBar) findViewById(R.id.speedBar);
        assert speedBar != null;
        speedBar.setProgress(speed);
    }

    public void increaseClick(View view) {
        speed += 10;
        if (speed > 255)
            speed = 255;
        sendCommand("incspeed");
        updateSpeedLabel(speed);
    }

    public void decreaseClick(View view) {
        speed -= 10;
        if (speed < 35)
            speed = 35;
        sendCommand("decspeed");
        updateSpeedLabel(speed);
    }

    public void forwardClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("forward");
            active = true;
        }
        activeView = view;
        forward = true;
    }

    public void backwardClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("backward");
            active = true;
        }
        activeView = view;
        forward = false;
    }

    public void stopClick(View view) {
        sendCommand("stop");
        active = false;
        activeView = view;
    }

    public void leftClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("left");
            active = true;
        }
        activeView = view;
    }

    public void rightClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("right");
            active = true;
        }
        activeView = view;
    }

    public void rotateLeftClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("rotl");
            active = true;
        }
        activeView = view;
    }

    public void rotateRightClick(View view) {
        if (activeView == view && active) {
            sendCommand("stop");
            active = false;
        } else {
            sendCommand("rotr");
            active = true;
        }
        activeView = view;
    }

    public void autoClick(View view) {
        Button button = (Button) view;
        if (autopilot) {
            sendCommand("stopauto");
            button.setText("Start Auto");
        } else {
            sendCommand("startauto");
            button.setText("Stop Auto");
        }
        autopilot = !autopilot;
    }

    public void distanceClick(View view) {
        sendCommand("distance");
    }

    public void lookLeftClick(View view) {
        sendCommand("sleft");
    }

    public void homeClick(View view) {
        sendCommand("home");
    }

    public void lookRightClick(View view) {
        sendCommand("sright");
    }

    // TouchPad listener ----------------------------------------------

    // It would be nice to use the length of the vector to control the speed.
    
    @Override
    public void onUp() {
        Log.v(TAG, "onUp");
        sendCommand("stop");
        active = false;
//        if (this.forward) {
//            sendCommand("forward");
//        } else {
//            sendCommand("backward");
//        }
//        active = true;
        activeView = null;
    }

    @Override
    public void onDown() {
        Log.v(TAG, "onDown");
        // need to start the thing moving
    }

    @Override
    public void onMove(double angle, boolean forward, boolean left, double dx, double dy) {
        double length = Math.sqrt(dx * dx + dy * dy);
        Log.v(TAG, "onMove " + decimalFormat.format(angle)
                + " --> " + decimalFormat.format(length));
        if (left != this.left) {
            if (left) {
                sendCommand("left");
            } else {
                sendCommand("right");
            }
            this.left = left;
            active = true;
        }
//        if(forward != this.forward) {
//            if(forward) {
//                executor.execute(new WriteCommand("forward", gopigoAddress));
//            } else {
//                executor.execute(new WriteCommand("backward", gopigoAddress));
//            }
//            this.forward = forward;
//            active = true;
//        }
    }

    public static void sendCommand(String command) {
        executor.execute(new WriteCommand(command));
    }

    private static class WriteCommand implements Runnable {
        private String command;

        public WriteCommand(String command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                outputStream.write(command.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.v(TAG, "encoding error: " + e.getMessage());
            } catch (IOException e) {
                Log.v(TAG, "ioexception: " + e.getMessage());
            }

            try {
                int len = inputStream.read(buffer);
                buffer[len] = 0;
                String str = new String(buffer, 0, len, "UTF-8");
                Log.v(TAG, "read: " + len + " : " + str);
            } catch (IOException e) {
                Log.v(TAG, "read failed: " + e.getMessage());
            }
        }
    }


    private class GetBluetoothSocket implements Runnable {
        @Override
        public void run() {
            try {
                Log.v(TAG, "GetBluetoothSocket");
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    // SPP
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Intent intent = new Intent();
                intent.setAction(BLUETOOTH_SOCKET);
                sendBroadcast(intent);
            } catch (IOException e) {
                Log.v(TAG, e.getMessage());
            }
        }
    }

    private class ConnectSocket implements Runnable {
        @Override
        public void run() {
            try {
                bluetoothSocket.connect();
                Intent intent = new Intent();
                intent.setAction(BLUETOOTH_CONNECT);
                sendBroadcast(intent);
            } catch (IOException e) {
                try {
                    bluetoothSocket.close();
                    Log.v(TAG, "try connect: " + e.getMessage());
                } catch (IOException closeException) {
                    Log.v(TAG, "close exception: " + closeException.getMessage());
                }
            }
        }
    }

    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BLUETOOTH_SOCKET)) {
                Log.v(TAG, "Receive: the bluetooth socket has been set");
                executor.execute(new ConnectSocket());
            } else if (intent.getAction().equals(BLUETOOTH_CONNECT)) {
                setTitle(bluetoothDevice.getName() + " connected");

                try {
                    inputStream = bluetoothSocket.getInputStream();
                    outputStream = bluetoothSocket.getOutputStream();
                } catch (IOException e) {
                    Log.v(TAG, "get streams failed: " + e.getMessage());
                }
            }
        }
    }
}
