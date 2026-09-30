package dev.jaypee.anotherpomodoroapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {

    private Paint paint;
    private RectF rectF;
    private int progress = 100;
    private int max = 100;
    private int strokeWidth;
    private int color;

    public CircularProgressView(Context context) {
        super(context);
        init(context);
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        rectF = new RectF();

        // Get the primary color from theme using obtainStyledAttributes
        int[] attrs = new int[]{android.R.attr.colorPrimary};
        android.content.res.TypedArray a = context.getTheme().obtainStyledAttributes(attrs);
        color = a.getColor(0, 0xFFC62828); // fallback to pomodoro red
        a.recycle();

        // 8dp stroke width to match the grey ring
        strokeWidth = (int) (8 * getResources().getDisplayMetrics().density);

        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }

    public void setMax(int max) {
        this.max = max;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());
        // Inset by half the stroke width so the stroke's outer edge
        // aligns with the container edge (matching the grey ring)
        int offset = strokeWidth / 2;
        rectF.set(offset, offset, size - offset, size - offset);

        float sweepAngle = 360f * progress / max;
        canvas.drawArc(rectF, -90, sweepAngle, false, paint);
    }
}
