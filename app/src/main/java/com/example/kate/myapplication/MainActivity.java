package com.example.kate.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    DeliveryPeriodPicker deliveryPeriodPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deliveryPeriodPicker = (DeliveryPeriodPicker) findViewById(R.id.dpp);
        deliveryPeriodPicker.setUnableInterval(54000000, 61200000);
        deliveryPeriodPicker.setMinimumIntervalInMin(120);
//        deliveryPeriodPicker.setTextSize(16);

    }
}
