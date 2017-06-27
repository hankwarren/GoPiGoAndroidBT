package com.kgdsoftware.gopigobt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class AutoPilotActivity extends AppCompatActivity {
    int leftTicks = 18;
    int rightTicks = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_pilot);

        SeekBar leftMotorBar = (SeekBar) findViewById(R.id.left_motor_target);
        assert leftMotorBar != null;
        leftMotorBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar speedkBar, int progress, boolean fromUser) {
                updateLeftMotorLabel(progress);
                leftTicks = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        leftMotorBar.setProgress(leftTicks);

        updateLeftMotorLabel(leftTicks);

        SeekBar rightMotorBar = (SeekBar) findViewById(R.id.right_motor_target);
        assert rightMotorBar != null;
        rightMotorBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar speedkBar, int progress, boolean fromUser) {
                updateRightMotorLabel(progress);
                rightTicks = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        rightMotorBar.setProgress(rightTicks);
        updateRightMotorLabel(rightTicks);
    }

    private void updateLeftMotorLabel(int ticks) {
        TextView label = (TextView)findViewById(R.id.left_motor_label);
        label.setText(Integer.toString(ticks));
    }

    private void updateRightMotorLabel(int ticks) {
        TextView label = (TextView)findViewById(R.id.right_motor_label);
        label.setText(Integer.toString(ticks));
    }

    public void startClick(View view) {
        MainActivity.sendCommand("startauto");
    }

    public void stopClick(View view) {
        MainActivity.sendCommand("stopauto");
    }

    public void leftClick(View view) {
        SeekBar seekbar = (SeekBar)findViewById(R.id.left_motor_target);
        int ticks = seekbar.getProgress();
        MainActivity.sendCommand("lencoder " + ticks);
    }

    public void goClick(View view) {
        SeekBar leftSeekbar = (SeekBar)findViewById(R.id.left_motor_target);
        int leftTicks = leftSeekbar.getProgress();
        SeekBar rightSeekbar = (SeekBar)findViewById(R.id.right_motor_target);
        int rightTicks = rightSeekbar.getProgress();

        MainActivity.sendCommand("encoder " + leftTicks + " " + rightTicks);
    }

    public void rightClick(View view) {
        SeekBar seekbar = (SeekBar)findViewById(R.id.right_motor_target);
        int ticks = seekbar.getProgress();
        MainActivity.sendCommand("rencoder " + ticks);
    }
}
