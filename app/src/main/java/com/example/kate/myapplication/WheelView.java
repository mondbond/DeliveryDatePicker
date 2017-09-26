package com.example.kate.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private final double TEXT_OFFSET_INDEX = 0.05;
    private final double FADE_RECT_INDEX = 0.3;

    public static final String ALIGN_LEFT = "left";
    public static final String ALIGN_RIGHT = "right";

    private final String TIME_FORMAT = "HH:mm";

    public final static int DEFAULT_TEXT_SIZE = 20;
    public final static int MAX_TEXT_SIZE = 40;

    public final static String DEFAULT_UNABLE_TEXT = "unable";

    private final List<OnItemSelectedListener> listeners = new ArrayList<>();

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

    int mTotalScrollY;
    int mItemHeight;
    int mItemsListHeight;

    int mTopLineY;
    int mBottomLineY;

    private int mCurrentIndex;

    private int mMinimumY;
    private int mMaximumY;

// data
    private List<Interval> mIntervals;
    private List<WheelView.Time> timeItems;
    String mUnableText;
    private int mTextSize;

    private String mTextAlign;

    private StringBuilder mText;
    private Date mDate = new Date();
    SimpleDateFormat mDateFormat = new SimpleDateFormat(TIME_FORMAT);

//  instruments
    private final Paint mHighlightTextPaint = new Paint();
    private final Paint mUsualTextPaint = new Paint();
    private final Paint mUnableTextPaint = new Paint();
    private final Paint mLinePaint = new Paint();
    private final Paint mFadePaint = new Paint();

    Shader mShader = new LinearGradient(0, 0, 0, 40, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.MIRROR);

    private int mHighlightColor;
    private int mUnableColor;
    private int mTextColor;

    public static final int MSG_INVALIDATE = 1000;
    public static final int MSG_SCROLL_LOOP = 2000;
    public static final int MSG_SELECTED_ITEM = 3000;

    @NonNull
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduledFuture;

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
                    if(!timeItems.get(mCurrentIndex).isAble()) {
                        if(mTotalScrollY + getNearestAbleTimeDistance() < mMinimumY){
                            startScroll(getNearestBiggerAbleTimeDistanceFromPosition(mCurrentIndex));
                        }else {
                            startScroll(getNearestAbleTimeDistance());
                        }
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
        final TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.WheelView);

        try {
            if (null != array) {
                mHighlightColor = array.getColor(R.styleable.WheelView_highlightColor, 0xffafafaf);
                mTextColor = array.getColor(R.styleable.WheelView_textColor, 0xff313131);
                mUnableColor = array.getColor(R.styleable.WheelView_unableColor, 0xffc5c5c5);
            }
        } finally {
            if (null != array) {
                array.recycle();
            }
        }

        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        mCurrentIndex = 0;

        initData();
    }

    private void initData(){
        mTextSize = sp2px(getContext(), DEFAULT_TEXT_SIZE);

        mHighlightTextPaint.setStrokeWidth(5);
        mHighlightTextPaint.setColor(mHighlightColor);
        mHighlightTextPaint.setTextSize(mTextSize);
        mHighlightTextPaint.setAntiAlias(true);
        mHighlightTextPaint.setTypeface(Typeface.MONOSPACE);

        mUsualTextPaint.setStrokeWidth(5);
        mUsualTextPaint.setColor(mTextColor);
        mUsualTextPaint.setTextSize(mTextSize);
        mUsualTextPaint.setAntiAlias(true);
        mUsualTextPaint.setTypeface(Typeface.MONOSPACE);

        mUnableTextPaint.setStrokeWidth(5);
        mUnableTextPaint.setColor(mUnableColor);
        mUnableTextPaint.setTextSize(mTextSize);
        mUnableTextPaint.setAntiAlias(true);
        mUnableTextPaint.setTypeface(Typeface.MONOSPACE);

        mLinePaint.setStrokeWidth(7);
        mLinePaint.setColor(mHighlightColor);

        mFadePaint.setAntiAlias(true);

        gestureDetector = new GestureDetector(getContext(), onGestureListener);
        gestureDetector.setIsLongpressEnabled(false);

//        default interval
        if(mIntervals == null) {
            mIntervals = new ArrayList<>();
//            mIntervals.add(new Interval(0, 12 * 3600000, 120));
            mIntervals.add(new Interval(0, 100 * 3600000, 120));
        }
//          default timeset
        if(timeItems == null) {
            timeItems = new ArrayList<>();
            for(int i = 6; i <= 42; ++i ){
                timeItems.add(new Time(i* 1800000));
            }
        }

        mUnableText = DEFAULT_UNABLE_TEXT;

        mMaximumY = mItemsListHeight;

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        widgetWidth = getMeasuredWidth();
        widgetHeight = MeasureSpec.getSize(heightMeasureSpec);

        setDimensions();
        mItemHeight = contentHeight / 3;
        mItemsListHeight = mItemHeight * (timeItems.size() - 2);

        mTopLineY = paddingTop + mItemHeight;
        mBottomLineY = paddingTop + 2*mItemHeight;
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

        drawFade(canvas);
    }

    private void drawLines(Canvas canvas) {
        canvas.drawLine(paddingLeft, mTopLineY, contentWidth, mTopLineY, mLinePaint);
        canvas.drawLine(paddingLeft, mBottomLineY, contentWidth, mBottomLineY, mLinePaint);
    }

    private void drawTime(Canvas canvas) throws ParseException {

        for (int count = 0; count <= 35; ++count) {
            canvas.save();

            canvas.translate(0.0F, mTotalScrollY*-1);

            if(isInHighlightZone(calculateTextY(count))) {
                mCurrentIndex = count;
            }

            if(!timeItems.get(count).isAble) {
                if(TextUtils.equals(mTextAlign, ALIGN_RIGHT)){
                    mText = new StringBuilder(timeItems.get(count).getTime() + " "+ mUnableText);
                    canvas.drawText(mText.toString(),
                            getXCoordinate(timeItems.get(count).getTime() + " " + mUnableText), calculateTextY(count), mUnableTextPaint);
                } else {
                    mText = new StringBuilder(mUnableText + " " + timeItems.get(count).getTime());
                    canvas.drawText(mText.toString(),
                            getXCoordinate(timeItems.get(count).getTime() + " " + mUnableText), calculateTextY(count), mUnableTextPaint);
                }

            } else if(isInHighlightZone(calculateTextY(count))) {
                canvas.drawText(timeItems.get(count).getTime(), getXCoordinate(timeItems.get(count).getTime()), calculateTextY(count), mHighlightTextPaint);
            }
            else {
                canvas.drawText(timeItems.get(count).getTime(), getXCoordinate(timeItems.get(count).getTime()), calculateTextY(count), mUsualTextPaint);
            }
            canvas.restore();
        }
    }

    private boolean isUnable(int ml) {
        for(Interval interval : mIntervals) {
            if(interval.isInIntervalRange(ml)) {
                return false;
            }
        }
        return true;
    }

    private void drawFade(Canvas canvas) {
        mShader = new LinearGradient(0, 0, 0, (int)(contentHeight*FADE_RECT_INDEX), Color.argb(255, 255, 255,255), Color.argb(0, 255, 255,255), Shader.TileMode.CLAMP);
        mFadePaint.setShader(mShader);
        canvas.drawRect(paddingLeft, paddingTop, paddingLeft + contentWidth, paddingTop + (int)(contentHeight*FADE_RECT_INDEX), mFadePaint);
        mShader = new LinearGradient(0, paddingTop + contentHeight, 0,paddingTop + contentHeight - (int)(contentHeight*FADE_RECT_INDEX), Color.argb(255, 255, 255,255), Color.argb(0, 255, 255,255), Shader.TileMode.CLAMP);
        mFadePaint.setShader(mShader);
        canvas.drawRect(paddingLeft,
                paddingTop + contentHeight - (int)(contentHeight*FADE_RECT_INDEX),
                paddingLeft + contentWidth,
                paddingTop + contentHeight ,mFadePaint);
    }

//    ==============================================================================
//    methods

    private int calculateTextY(int count) {
        return mItemHeight + ++count*mItemHeight - getTextYOffset();
    }

    private int getTextWeight(String word) {
        final Rect rect = new Rect();
        mUnableTextPaint.getTextBounds(word, 0, word.length(), rect);

        return rect.width();
    }

    private boolean isInHighlightZone(int distance) {
        return distance - mTotalScrollY > mTopLineY && distance - mTotalScrollY <+ mBottomLineY;
    }

    private int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    private void cancelSchedule() {
        if (null != scheduledFuture && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    private void itemSelected() {
        postDelayed(this::onItemSelected, 0L);
    }

    private void onItemSelected() {
        for (OnItemSelectedListener onItemSelectedListener : listeners) {
            try {
                onItemSelectedListener.onLoopScrollFinish(timeItems.get(mCurrentIndex).getTime(), timeItems.indexOf(timeItems.get(mCurrentIndex)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private int getNearestAbleTimeDistance() throws ParseException {
        for(int i = 0; i <= timeItems.size()/3; ++i) {
            if(mCurrentIndex + i <= timeItems.size() && timeItems.get(mCurrentIndex + i).isAble()) {
                return i*(int)mItemHeight;
            }else if(mCurrentIndex - i >= 0 && timeItems.get(mCurrentIndex - i).isAble()){
                return -i*(int)mItemHeight;
            }
        }
        return 0;
    }

    private int getNearestBiggerAbleTimeDistanceFromPosition(int position) throws ParseException {
        for(int i = position; i <= timeItems.size(); ++i) {
            if(timeItems.get(i).isAble()) {
                mMinimumY = (i -1) * mItemHeight;
                return mMinimumY - mCurrentIndex * mItemHeight;
            }
        }
        return 0;
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

//  open method
    public void translateToPosition(int position) throws ParseException {
        if(timeItems.get(position).isAble()) {
            startScroll(getNearestBiggerAbleTimeDistanceFromPosition(position));
        }else {
            startScroll((position - mCurrentIndex) * mItemHeight);
        }
    }

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
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mTotalScrollY = (int) ((float) mTotalScrollY + distanceY);
            if(mTotalScrollY < 0) {
                mTotalScrollY = 0;
            }else if(mTotalScrollY < mMinimumY){
                mTotalScrollY = mMinimumY;
            } else if(mTotalScrollY > mMaximumY) {
                mTotalScrollY = mMaximumY;
            }else if(mTotalScrollY > mItemsListHeight){
                mTotalScrollY = mItemsListHeight;
            }

            invalidate();

            return true;
        }
    }

//    run
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
            } else if(mTotalScrollY <= mMinimumY){
                velocity = 0F;
                mTotalScrollY = mMinimumY;
            } else if(mTotalScrollY >= mMaximumY){
                mTotalScrollY = mMaximumY;
            } else if (mTotalScrollY >= mItemsListHeight) {
                mTotalScrollY = mItemsListHeight;
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

    //    listeners
    public interface OnItemSelectedListener {
        void onLoopScrollFinish(@NonNull String item, int position) throws ParseException;
    }

//    classes

    public class Interval {

        private int mBegin;
        private int mEnd;

        public int getPeriodInMin() {
            return mPeriodInMin;
        }

        public void setPeriodInMin(int mPeriodInMin) {
            this.mPeriodInMin = mPeriodInMin;
        }

        private int mPeriodInMin;

        public Interval(int mBegin, int mEnd, int interval) {
            this.mBegin = mBegin;
            this.mEnd = mEnd;
            this.mPeriodInMin = interval;
        }

        public boolean isInIntervalRange(int ml) {
            return ml > mBegin && ml < mEnd;
        }

        public int getBegin() {
            return mBegin;
        }

        public void setBegin(int mBegin) {
            this.mBegin = mBegin;
        }

        public int getEnd() {
            return mEnd;
        }

        public void setEnd(int mEnd) {
            this.mEnd = mEnd;
        }
    }

    public class Time {

        private String time;
        private int timeInMl;

        public boolean isAble() {
            return isAble;
        }

        public void setAble(boolean able) {
            isAble = able;
        }

        private boolean isAble;

        public Time(int timeInMl) {
            this.timeInMl = timeInMl;
            setTimeInMl(timeInMl);
            isAble = !isUnable(timeInMl);
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public int getTimeInMl() {
            return timeInMl;
        }

        public void setTimeInMl(int timeInMl) {
            mDate.setTime(timeInMl);
            time = mDateFormat.format(mDate);
            this.timeInMl = timeInMl;
        }
    }

//    getters setters
//    =================================================================
    public String getUnableText() {
        return mUnableText;
    }

    public void setUnableText(String mUnableText) {
        this.mUnableText = mUnableText;
    }

    public void setMinimumY(int position) {
        mMinimumY = (position - 1)*mItemHeight;
        mMaximumY = mItemsListHeight;
    }

    public void setIntervals(List<WheelView.Interval> intervals) {
        mIntervals = intervals;
        initData();
    }

    public void setAlign(String align) {
        if(TextUtils.equals(align, ALIGN_LEFT) || TextUtils.equals(align, ALIGN_RIGHT)) {
            mTextAlign = align;
        } else {
            mTextAlign = ALIGN_LEFT;
        }
    }

    private int getXCoordinate(String word){
        if(TextUtils.equals(mTextAlign, ALIGN_RIGHT)){
            return (int)(paddingLeft + contentWidth*TEXT_OFFSET_INDEX);
        } else {
            return paddingLeft + contentWidth - (int)(contentWidth*TEXT_OFFSET_INDEX) - getTextWeight(word);
        }
    }

    private int getTextYOffset() {
        return (mItemHeight - mTextSize) / 2;
    }

    public int getMaximumY() {
        return mMaximumY;
    }

    public void setMaximumY(int interval) {
        this.mMaximumY = mItemsListHeight - (interval/30 - 1) * mItemHeight;
    }

    public int getSelectedPeriod() {
        return timeItems.get(mCurrentIndex).getTimeInMl();
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener loopScrollListener) {
        listeners.add(loopScrollListener);
        invalidate();
    }

    public final void setTextSize(float size) {
        if (size > 0 && size < MAX_TEXT_SIZE) {
            mTextSize = sp2px(getContext(), size);
        } else {
            mTextSize = sp2px(getContext(), MAX_TEXT_SIZE);
        }
    }
}
