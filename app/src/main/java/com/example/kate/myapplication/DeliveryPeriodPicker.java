package com.example.kate.myapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.text.ParseException;
import java.util.List;

/**
 * Created by kate on 22.09.17.
 */

public class DeliveryPeriodPicker extends LinearLayout {

    public final static int DEFAULT_TEXT_SIZE = 20;

    private int mMinimumIntervalInMinutes = 30;

    private View rootView;

    private WheelView mStartWheel;
    private WheelView mEndWheel;

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

    private void draw(){
        rootView = LayoutInflater.from(getContext()).inflate(R.layout.delivery_picker, this, true);

        mStartWheel = (WheelView) rootView.findViewById(R.id.wv_start_period);
        mEndWheel = (WheelView) rootView.findViewById(R.id.wv_end_period);

        mStartWheel.setOnItemSelectedListener(mStartWheelListener);
        mEndWheel.setOnItemSelectedListener(mEndWheelListener);

        mStartWheel.setAlign(WheelView.ALIGN_LEFT);
        mEndWheel.setAlign(WheelView.ALIGN_RIGHT);
    }

    private void translateEndWheel(int startWheelPosition) throws ParseException {
        mEndWheel.setMinimumY(startWheelPosition + mMinimumIntervalInMinutes/30);
        mStartWheel.setMaximumY(mMinimumIntervalInMinutes);
        if(mCurrentEndPosition - mCurrentStartPosition < mMinimumIntervalInMinutes/30) {
            mEndWheel.translateToPosition(calculateDistance(startWheelPosition));
        }
    }

    private int calculateDistance(int position) {
        return position + mMinimumIntervalInMinutes/30;
    }

    public void setMinimumIntervalInMin(int mMinimumIntervalInMinutes) {
        this.mMinimumIntervalInMinutes = mMinimumIntervalInMinutes;
    }

    public void setIntervals(List<WheelView.Interval> intervals) {
        mStartWheel.setIntervals(intervals);
    }

    public void setTextSize(int size) {
        mStartWheel.setTextSize(size);
        mEndWheel.setTextSize(size);
    }

    public void setUnableText(String unableText) {
        mStartWheel.setUnableText(unableText);
        mEndWheel.setUnableText(unableText);
    }

    public Period getSelectedPeriod() {
        return new Period(mStartWheel.getSelectedPeriod(), mEndWheel.getSelectedPeriod());
    }

    public class Period {

        private int start;
        private int end;

        public Period(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }
    }
}
