package com.example.kate.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by kate on 22.09.17.
 */

public class WheelView extends View {
    private static final String TAG = WheelView.class.getSimpleName();

    private final List<OnItemSelectedListener> listeners = new ArrayList<>();

    // data
    private final String TIME_FORMAT = "HH:mm";

    private List<String> items = new ArrayList<String>(Arrays.asList(
            "06:00", "06:30",
            "07:00" , "07:30",
            "08:00" , "08:30",
            "09:00" , "09:30",
            "10:00" , "10:30",
            "11:00" , "11:30",
            "12:00" , "12:30",
            "13:00" , "13:30",
            "14:00" , "14:30",
            "15:00" , "15:30",
            "16:00" , "16:30",
            "17:00" , "17:30",
            "18:00" , "18:30",
            "19:00" , "19:30",
            "20:00" , "20:30",
            "21:00" , "21:30",
            "22:00"));

    private String startUnablePeriod = "12:56";
    private String finishUnablePeriod = "14:56";

    String mUnableText;

    SimpleDateFormat mDateFormat = new SimpleDateFormat(TIME_FORMAT);

    //    measure
    private int widgetHeight;
    private int widgetWidth;

    int paddingLeft;
    int paddingTop;
    int paddingRight;
    int paddingBottom;

    int contentWidth;
    int contentHeight;

    int centerX;
    int centerY;

    //
    int mTotalScrollY;
    int mItemHeight;
    int mItemsListHeight;

    int mTopLineY;
    int mBottomLineY;

    //    work
    public static final int MSG_INVALIDATE = 1000;
    public static final int MSG_SCROLL_LOOP = 2000;
    public static final int MSG_SELECTED_ITEM = 3000;

    @NonNull
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduledFuture;

    private int mCurrentIndex;

    private int mMinimumIndex;

    public Handler handler = new Handler(msg -> {
        switch (msg.what) {
            case MSG_INVALIDATE:
                invalidate();
                break;
            case MSG_SCROLL_LOOP:
                startSmoothScrollTo();
                break;
            case MSG_SELECTED_ITEM:
                try {
                    if(isUnableTime(items.get(mCurrentIndex))) {
                        startScroll(getNearestAbleTimeDistance());
                    }else {
                        itemSelected();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
        }

        return false;
    });


    @NonNull
    private final GestureDetector.SimpleOnGestureListener onGestureListener  = new WheelViewGestureListener();
    private GestureDetector gestureDetector;


//instruments

    private final Paint mHighlightTextPaint = new Paint();
    private final Paint mUsualTextPaint = new Paint();
    private final Paint mUnableTextPaint = new Paint();
    private final Paint mLinePaint = new Paint();

    //    constructors
    public WheelView(Context context) {
        super(context);
    }

    public WheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            initView(attrs);
        }
    }

    public WheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            initView(attrs);
        }
    }


    private void initView(@NonNull AttributeSet attrs) {
//        final TypedArray array = getContext().obtainStyledAttributes(attrs, Wheelview.R.styleable.WheelView);

        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        mHighlightTextPaint.setStrokeWidth(5);
        mHighlightTextPaint.setColor(Color.BLUE);
        mHighlightTextPaint.setTextSize(100);

        mUsualTextPaint.setStrokeWidth(5);
        mUsualTextPaint.setColor(Color.BLACK);
        mUsualTextPaint.setTextSize(100);

        mUnableTextPaint.setStrokeWidth(5);
        mUnableTextPaint.setColor(Color.GRAY);
        mUnableTextPaint.setTextSize(100);

        mLinePaint.setStrokeWidth(5);
        mLinePaint.setColor(Color.BLUE);

        gestureDetector = new GestureDetector(getContext(), onGestureListener);
        gestureDetector.setIsLongpressEnabled(false);

        mCurrentIndex = 0;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        widgetWidth = getMeasuredWidth();
        widgetHeight = MeasureSpec.getSize(heightMeasureSpec);

        setDimensions();
        mItemHeight = contentHeight / 3;
        mItemsListHeight = mItemHeight * (items.size() - 2);

        mTopLineY = paddingTop + mItemHeight;
        mBottomLineY = paddingTop + 2*mItemHeight;


//        Log.d("DIM!",
//                "contH = " + String.valueOf(contentHeight) +
//                "centerY = " + String.valueOf(centerY) +
//                "item = " + String.valueOf(mItemHeight)
//        );

    }

    private void setDimensions() {
        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();

        contentWidth = widgetWidth - paddingLeft - paddingRight;
        contentHeight = widgetHeight - paddingTop - paddingBottom;

        centerX = paddingLeft + contentWidth / 2;
        centerY = paddingTop + contentHeight / 2;
    }
//    ======================== draw



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //draw top and bottom line
        drawLines(canvas);

        try {
            drawTime(canvas);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void drawLines(Canvas canvas) {
        canvas.drawLine(paddingLeft, mTopLineY, contentWidth, mTopLineY, mLinePaint);
        canvas.drawLine(paddingLeft, mBottomLineY, contentWidth, mBottomLineY, mLinePaint);
    }

    private void drawTime(Canvas canvas) throws ParseException {

        int count = 1;
        while (count < items.size()) {
            canvas.save();

            canvas.translate(0.0F, mTotalScrollY*-1);

            if(isInHighlightZone(calculateTextY(count))) {
                mCurrentIndex = count;
            }

            if(isUnableTime(items.get(count))){
                canvas.drawText((String) items.get(count) + "unable", 50, calculateTextY(count), mUnableTextPaint);
            } else if(isInHighlightZone(calculateTextY(count))) {
                canvas.drawText((String) items.get(count), 50, calculateTextY(count), mHighlightTextPaint);
            }
            else {
                canvas.drawText((String) items.get(count), 50, calculateTextY(count), mUsualTextPaint);
            }
            canvas.restore();
            count++;
        }
    }

    private int calculateTextY(int count) {
        return ++count*mItemHeight - 50;
    }

    private boolean isInHighlightZone(int distance) {
        return distance - mTotalScrollY > mTopLineY && distance - mTotalScrollY <+ mBottomLineY;
    }

//==================================================================================

//    listener

    @Override
    public boolean onTouchEvent(MotionEvent motionevent) {
        switch (motionevent.getAction()) {
            case MotionEvent.ACTION_UP:
            default:
                if (!gestureDetector.onTouchEvent(motionevent)) {
                    startSmoothScrollTo();
                }
        }

        return true;
    }


    class WheelViewGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public final boolean onDown(MotionEvent motionevent) {
            cancelSchedule();
            return true;
        }

        @Override
        public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            startSmoothScrollTo(velocityY);
//            Log.i(TAG, "WheelViewGestureListener -> onFling " + String.valueOf(velocityY));
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//            Log.i(TAG, "WheelViewGestureListener -> onScroll");
            mTotalScrollY = (int) ((float) mTotalScrollY + distanceY);

            if(mTotalScrollY < 0) {
                mTotalScrollY = 0;
            }else if(mTotalScrollY < mMinimumIndex){
                mTotalScrollY = mMinimumIndex;
            } else if(mTotalScrollY > mItemsListHeight){
                mTotalScrollY = mItemsListHeight;
            }

//            Log.d("SCROLL!", " is = " + String.valueOf(mTotalScrollY));

            invalidate();
            return true;
        }
    }
    class FlingRunnable implements Runnable {

        float velocity;
        final float velocityY;

        FlingRunnable(float velocityY) {
            this.velocityY = velocityY;
            velocity = Integer.MAX_VALUE;
        }

        @Override
        public void run() {
            if (velocity == Integer.MAX_VALUE) {
                if (Math.abs(velocityY) > 2000F) {
                    velocity = (velocityY > 0.0F) ? 2000F : - 2000F;
                } else {
                    velocity = velocityY;
                }
            }

            if (Math.abs(velocity) >= 0.0F && Math.abs(velocity) <= 20F) {
                cancelSchedule();
                handler.sendEmptyMessage(MSG_SCROLL_LOOP);
                return;
            }

            int i = (int) ((velocity * 10F) / 1000F);
            mTotalScrollY = mTotalScrollY - i;


            if (mTotalScrollY <= 0) {
                velocity = 0F;
                mTotalScrollY = 0;
            } else if(mTotalScrollY <= mMinimumIndex){
                velocity = 0F;
                mTotalScrollY = mMinimumIndex;
            } else if (mTotalScrollY >= mItemsListHeight) {
                mTotalScrollY = mItemHeight;
            }

            velocity = (velocity < 0.0F) ? velocity + 20F : velocity - 20F;
            handler.sendEmptyMessage(MSG_INVALIDATE);
        }
    }

    class HalfHeightRunnable implements Runnable {
        int realTotalOffset;
        int realOffset;
        int offset;

        HalfHeightRunnable(int offset) {
            this.offset = offset;
            realTotalOffset = Integer.MAX_VALUE;
            realOffset = 0;
        }

        @Override
        public void run() {
            //first in
            if (realTotalOffset == Integer.MAX_VALUE) {

                if ((float) offset > mItemHeight / 2.0F) {
                    //move to next item
                    realTotalOffset = (int) (mItemHeight - (float) offset);
                } else {
                    //move to pre item
                    realTotalOffset = -offset;
                }
            }

            realOffset = (int) ((float) realTotalOffset * 0.1F);

            if (realOffset == 0) {
                realOffset = (realTotalOffset < 0) ? -1 : 1;
            }

            if (Math.abs(realTotalOffset) <= 0) {
                cancelSchedule();
                handler.sendEmptyMessage(MSG_SELECTED_ITEM);
            } else {
                mTotalScrollY = mTotalScrollY + realOffset;
                handler.sendEmptyMessage(MSG_INVALIDATE);
                realTotalOffset = realTotalOffset - realOffset;
            }
        }
    }


    class ScrollToTargetRunnable implements Runnable {
        int offset;
        int speed = 20;

        boolean vector;

        ScrollToTargetRunnable(int distance) {
            this.offset = distance;

            if(distance < 0) {
                vector = false;
                offset = offset*-1;
            }else {
                vector = true;
            }
        }

        @Override
        public void run() {

            if(speed >= offset) {
                speed = offset;
            }
            offset -= speed;

            if(vector) {
                mTotalScrollY += speed;
            }else {
                mTotalScrollY -= speed;
            }

            //first in
            if (offset <= 0) {
                cancelSchedule();
                handler.sendEmptyMessage(MSG_SELECTED_ITEM);
            } else {
                handler.sendEmptyMessage(MSG_INVALIDATE);
            }
        }
    }

//    ======== methods

    private void cancelSchedule() {
        if (null != scheduledFuture && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    private void startSmoothScrollTo() {
        int offset = (int) (mTotalScrollY % (mItemHeight));
        cancelSchedule();
        scheduledFuture = executorService.scheduleWithFixedDelay(new HalfHeightRunnable(offset), 0, 10, TimeUnit.MILLISECONDS);
    }

    private void startSmoothScrollTo(float velocityY) {
        cancelSchedule();
        int velocityFling = 20;
        scheduledFuture = executorService.scheduleWithFixedDelay(new WheelView.FlingRunnable(velocityY), 0, velocityFling, TimeUnit.MILLISECONDS);
    }

    private void startScroll(int distance){
        cancelSchedule();
        scheduledFuture = executorService.scheduleWithFixedDelay(new ScrollToTargetRunnable(distance),
                0, 10, TimeUnit.MILLISECONDS);
    }

    private void itemSelected() {
        postDelayed(this::onItemSelected, 0L);
    }

    private void onItemSelected() {
        for (OnItemSelectedListener onItemSelectedListener : listeners) {
            try {
                onItemSelectedListener.onLoopScrollFinish(items.get(mCurrentIndex), items.indexOf(items.get(mCurrentIndex)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }
    }
    private long getMilisecounds(String format) throws ParseException {
        Date date = mDateFormat.parse(format);
        return date.getTime();
    }

    private int getNearestAbleTimeDistance() throws ParseException {
        for(int i = 0; i <= items.size()/2; ++i){
            if(!isUnableTime(items.get(mCurrentIndex + i))){
                return i*(int)mItemHeight;
            }else if(!isUnableTime(items.get(mCurrentIndex - i))){
                return -i*(int)mItemHeight;
            }
        }
        return 0;
    }

    private int getNearestBiggerAbleTimeDistanceFromPosition(int position) throws ParseException {
        for(int i = position; i <= items.size(); ++i) {
            if(!isUnableTime(items.get(i))) {
                mMinimumIndex = (i -1) * mItemHeight;
                return mMinimumIndex - mCurrentIndex * mItemHeight;
            }
        }
        return 0;
    }

    private boolean isUnableTime(String itemTime) throws ParseException {
        return getMilisecounds(itemTime) > getMilisecounds(startUnablePeriod)
                && getMilisecounds(itemTime) < getMilisecounds(finishUnablePeriod);
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener loopScrollListener) {
        listeners.add(loopScrollListener);
        invalidate();
    }

//    listeners
    public interface OnItemSelectedListener {
        void onLoopScrollFinish(@NonNull String item, int position) throws ParseException;
    }

    public void translateToPosition(int position) throws ParseException {
        if(isUnableTime(items.get(position))) {
            startScroll(getNearestBiggerAbleTimeDistanceFromPosition(position));
        }else {
            startScroll((position - mCurrentIndex) * mItemHeight);
        }
    }

    public void setmMinimumIndex(int position) {
        mMinimumIndex = (position - 1)*mItemHeight;
    }
}
