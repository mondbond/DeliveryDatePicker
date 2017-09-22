package com.example.kate.myapplication;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * Created by kate on 22.09.17.
 */

public class DeliveryPeriodPicker extends LinearLayout {

    private View rootView;

    public DeliveryPeriodPicker(Context context) {
        super(context);
        draw();
    }

    public DeliveryPeriodPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        draw();
    }

    public DeliveryPeriodPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        draw();
    }

    public DeliveryPeriodPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        draw();
    }

    private WheelView mStartPeriod;
    private WheelView mEndPeriodPeriod;


    private void draw(){

        rootView = LayoutInflater.from(getContext()).inflate(R.layout.delivery_picker, this, true);

    }
}
