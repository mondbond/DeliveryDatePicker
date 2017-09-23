package com.example.kate.myapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.text.ParseException;

/**
 * Created by kate on 22.09.17.
 */

public class DeliveryPeriodPicker extends LinearLayout {


    private int mCurrentStartPosition;
    private int mCurrentEndPosition;

    private WheelView.OnItemSelectedListener mStartWheelListener = new WheelView.OnItemSelectedListener() {
        @Override
        public void onLoopScrollFinish(@NonNull String item, int position) throws ParseException {
            mCurrentStartPosition = position;
            translateEndWheel(position);
        }
    };

    private WheelView.OnItemSelectedListener mEndWheelListener = new WheelView.OnItemSelectedListener() {
        @Override
        public void onLoopScrollFinish(@NonNull String item, int position) {
            mCurrentEndPosition = position;
        }
    };

    private int mMinimumIntervalInMinutes = 30;

    private int mStartUnablePosition;
    private int mEndUnablePosition;

    private View rootView;

    private WheelView mStartWheel;
    private WheelView mEndWheel;


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

        mStartWheel = (WheelView) rootView.findViewById(R.id.wv_start_period);
        mEndWheel = (WheelView) rootView.findViewById(R.id.wv_end_period);

        mStartWheel.setOnItemSelectedListener(mStartWheelListener);
        mEndWheel.setOnItemSelectedListener(mEndWheelListener);
    }

    private void translateEndWheel(int startWheelPosition) throws ParseException {
        mEndWheel.setmMinimumIndex(startWheelPosition + mMinimumIntervalInMinutes/30);
        if(mCurrentEndPosition - mCurrentStartPosition < mMinimumIntervalInMinutes/30) {
            mEndWheel.translateToPosition(calculateDistance(startWheelPosition));
        }
    }

    private int calculateDistance(int position) {
        return position + mMinimumIntervalInMinutes/30;
    }


    public void setMinimumIntervalInMinutes(int mMinimumIntervalInMinutes) {
        this.mMinimumIntervalInMinutes = mMinimumIntervalInMinutes;
    }

    public void setStartUnablePosition(int mStartUnablePositionInMl) {
        this.mStartUnablePosition = mStartUnablePosition;
    }

    public void setEndUnablePosition(int mEndUnablePositionInMl) {
        this.mEndUnablePosition = mEndUnablePosition;
    }
}
