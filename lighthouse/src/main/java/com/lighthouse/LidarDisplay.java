package com.lighthouse;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.lighthouse.Data.DataPoint;
import com.lighthouse.Data.GraphPoint;


public class LidarDisplay extends View {

    private final Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final GraphPoint[] mGraphPointArray = new GraphPoint[360];

    private float[] mPointArray;

    private String hexColorValue = "#212121";

    private boolean drawLines = false;

    private int chartWidth, chartHeight;

    /**
     * Constructor
     */
    public LidarDisplay(Context context) {
        super(context);
        init(context, null);
    }

    /**
     * Constructor
     */
    public LidarDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * Constructor
     */
    public LidarDisplay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Constructor
     */
    @TargetApi(21)
    public LidarDisplay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public void updateGraphWithDataPoints(DataPoint[] dataPointArray) {
        if (dataPointArray != null) {
            for (DataPoint dataPoint : dataPointArray) {
                if (dataPoint != null) {
                    GraphPoint graphPoint = new GraphPoint(dataPoint);
                    mGraphPointArray[graphPoint.getAngle()] = graphPoint;
                }
            }
            createSectionsPath();
        }
    }

    public int getChartWidth() {
        return chartWidth;
    }

    public int getChartHeight() {
        return chartHeight;
    }

    public boolean isDrawLines() {
        return drawLines;
    }

    public void setDrawLines(boolean drawLines) {
        this.drawLines = drawLines;
    }

    public String getHexColorValue() {
        return hexColorValue;
    }

    public void setHexColorValue(String hexColorValue) {
        this.hexColorValue = hexColorValue;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        chartHeight = h;
        chartWidth = w;

        createSectionsPath();
    }

    @Override
    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawLines) {
            canvas.drawLines(mPointArray, shapePaint);
        } else {
            canvas.drawPoints(mPointArray, shapePaint);
        }
    }

    /**
     * Init
     *
     * @param context
     */
    private void init(Context context, AttributeSet attrs) {
        shapePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        shapePaint.setColor(Color.parseColor(hexColorValue));
        shapePaint.setStrokeWidth(5);
    }

    /**
     * Create sections path
     */
    private void createSectionsPath() {

        if (drawLines) {
            mPointArray = new float[1440];
        } else {
            mPointArray = new float[720];
        }

        float currentSectionX, currentSectionY;
        GraphPoint[] graphPointsArray = mGraphPointArray;
        int pointArrayCursor = 0;
        for (GraphPoint graphPoint : graphPointsArray) {
            // Check to make sure that we have logged a value for that angle.
            if (graphPoint != null) {
                float xValue = graphPoint.getxCoordinate();
                currentSectionX = (chartWidth / 2) + xValue;

                float yValue = graphPoint.getyCoordinate();
                currentSectionY = (chartHeight / 2) - yValue;


            } else {
                currentSectionX = (chartWidth / 2);
                currentSectionY = (chartHeight / 2);
            }

            if (drawLines) {
                mPointArray[pointArrayCursor] = chartWidth / 2;
                mPointArray[pointArrayCursor + 1] = chartHeight / 2;
                mPointArray[pointArrayCursor + 2] = currentSectionX;
                mPointArray[pointArrayCursor + 3] = currentSectionY;
                pointArrayCursor = pointArrayCursor + 4;
            } else {
                mPointArray[pointArrayCursor] = currentSectionX;
                mPointArray[pointArrayCursor + 1] = currentSectionY;
                pointArrayCursor = pointArrayCursor + 2;
            }
        }

    }
}

