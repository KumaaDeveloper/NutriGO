package com.example.pedulimakanan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class HeaderCircleView extends View {

    private Paint paint;

    public HeaderCircleView(Context context) {
        super(context);
        init();
    }

    public HeaderCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeaderCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#AED3E2"));
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float viewWidth = getWidth();

        /*
         * Oval dibuat lebih lebar dari layar,
         * supaya bentuknya melebar ke samping seperti desain.
         */
        float ovalWidth = viewWidth * 1.75f;

        /*
         * Tinggi dibuat cukup besar supaya bagian bawah
         * nutup area kotak cari makanan.
         */
        float ovalHeight = 680f;

        float left = (viewWidth - ovalWidth) / 2f;
        float top = -60f;
        float right = left + ovalWidth;
        float bottom = top + ovalHeight;

        RectF rectF = new RectF(left, top, right, bottom);
        canvas.drawOval(rectF, paint);
    }
}