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

    /**
     * Updates the graph data from a DataPoint array.  This merely updates the data which the graph
     * is based upon.  It does not update the visual graph.
     * @param dataPointArray The array of DataPoints to update the graph data with.
     */
    public void updateGraphWithDataPoints(DataPoint[] dataPointArray) {
        if (dataPointArray != null) {
            for (DataPoint dataPoint : dataPointArray) {
                if (dataPoint != null) {
                    GraphPoint graphPoint = new GraphPoint(dataPoint);
                    mGraphPointArray[graphPoint.getAngle()] = graphPoint;
                }
            }
            createPointArray();
        }
    }

    /**
     * Returns the width of the chart in the view.
     * @return Width of the chart.
     */
    public int getChartWidth() {
        return chartWidth;
    }

    /**
     * Returns the height of the chart in the view.
     * @return Height of the chart.
     */
    public int getChartHeight() {
        return chartHeight;
    }

    /**
     * Returns true if the view is configured to draw lines from the center to the distance point.
     * @return
     */
    public boolean isDrawLines() {
        return drawLines;
    }

    /**
     * Pass true in order to configure the view to show lines from the center of the graph to the
     * point representing the distance value.
     * @param drawLines boolean value for turning on and off lines.
     */
    public void setDrawLines(boolean drawLines) {
        this.drawLines = drawLines;
    }

    /**
     * Get the color value used for drawing on the canvas.
     * @return The hex color value.
     */
    public String getHexColorValue() {
        return hexColorValue;
    }

    /**
     * Set the color value used for drawing on the canvas.
     * @param hexColorValue The hex color value.
     */
    public void setHexColorValue(String hexColorValue) {
        this.hexColorValue = hexColorValue;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        chartHeight = h;
        chartWidth = w;

        createPointArray();
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
     * Creates the point array used to draw on the canvas.
     */
    private void createPointArray() {

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

