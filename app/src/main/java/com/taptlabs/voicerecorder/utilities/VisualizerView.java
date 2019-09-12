package com.taptlabs.voicerecorder.utilities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class VisualizerView extends View {
    private int scrWidth = 0;

    ///////////////////
    public static ArrayList<Float> amplitudesList;

    private static final int LINE_WIDTH = 10; // width of visualizer lines
    private static final int LINE_SCALE = 30; // scales visualizer lines
    private static List<Float> amplitudes; // amplitudes for line lengths

    private int width; // width of this View
    private int height; // height of this View
    private Paint linePaint; // specifies line drawing characteristics

    // constructor
    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs); // call superclass constructor
        linePaint = new Paint(); // create Paint for lines
        linePaint.setColor(Color.WHITE); // set color to green
        linePaint.setStrokeWidth(LINE_WIDTH); // set stroke width
    }

    // called when the dimensions of the View change
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        scrWidth = w;
        width = (w / 2) - 5; // new width of this View
        height = h; // new height of this View
//        amplitudes = new ArrayList<Float>(width / LINE_WIDTH);
        amplitudes = new ArrayList<Float>(0);
    }

    // clear all amplitudes to prepare for a new visualization
    public void clear() {
        amplitudes.clear();
    }


    // add the given amplitude to the amplitudes ArrayList
    public void addAmplitude(float amplitude) {

        if (amplitudes == null){
            amplitudes = new ArrayList<>();
        }

        if (amplitudesList == null){
            amplitudesList = new ArrayList<>();
        }

        amplitudesList.add(amplitude);

        amplitudes.add(amplitude); // add newest to the amplitudes ArrayList

        // if the power lines completely fill the VisualizerView
        if (amplitudes.size() * LINE_WIDTH >= width) {
            //clear();
            amplitudes.remove(0); // remove oldest power value
        }
    }

    // draw the visualizer with scaled lines representing the amplitudes
    @Override
    public void onDraw(Canvas canvas) {
        int middle = height;//      / 2; // get the middle of the View
        float curX = 0; // start curX at zero
        // for each item in the amplitudes ArrayList
        for (float power : amplitudes) {
            float scaledHeight = power / LINE_SCALE; // scale the power
            curX += LINE_WIDTH*2; // increase X by LINE_WIDTH

            //draw a line representing this item in the amplitudes ArrayList
            canvas.drawLine(curX, middle + scaledHeight / 2, curX, middle
                    - scaledHeight /2, linePaint);
        }
    }

}