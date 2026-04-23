package com.aarab.prayertimessersheng.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom compass view.
 * Draws a circular compass rose and a gold needle pointing toward Qibla.
 * Call {@link #setQiblaAngle(float)} from the activity to rotate the needle.
 */
public class QiblaCompassView extends View {

    private float qiblaAngle = 0f;   // degrees: 0 = north, clockwise

    // ── Paints ────────────────────────────────────────────────────────────────
    private final Paint circlePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleTail   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    public QiblaCompassView(Context context) { super(context); init(); }
    public QiblaCompassView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public QiblaCompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        circlePaint.setColor(Color.parseColor("#162E4E"));
        circlePaint.setStyle(Paint.Style.FILL);

        ringPaint.setColor(Color.parseColor("#D4AF37"));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3f);

        needlePaint.setColor(Color.parseColor("#D4AF37"));
        needlePaint.setStyle(Paint.Style.FILL);

        needleTail.setColor(Color.parseColor("#1E3A5F"));
        needleTail.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.parseColor("#EAD9B8"));
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        dotPaint.setColor(Color.parseColor("#D4AF37"));
        dotPaint.setStyle(Paint.Style.FILL);

        tickPaint.setColor(Color.parseColor("#5A7A9A"));
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(2f);
    }

    /** Called by QiblaActivity whenever sensor data updates. */
    public void setQiblaAngle(float angle) {
        this.qiblaAngle = angle;
        invalidate();   // triggers redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) * 0.88f;

        // ── Background circle ─────────────────────────────────────────────
        canvas.drawCircle(cx, cy, r, circlePaint);
        canvas.drawCircle(cx, cy, r, ringPaint);

        // ── Tick marks every 30° ─────────────────────────────────────────
        for (int i = 0; i < 12; i++) {
            float deg = i * 30f;
            float rad = (float) Math.toRadians(deg);
            float innerR = r * 0.88f;
            float outerR = r * 0.97f;
            canvas.drawLine(
                    cx + (float) Math.sin(rad) * innerR,
                    cy - (float) Math.cos(rad) * innerR,
                    cx + (float) Math.sin(rad) * outerR,
                    cy - (float) Math.cos(rad) * outerR,
                    tickPaint);
        }

        // ── Cardinal letters ─────────────────────────────────────────────
        float labelR = r * 0.72f;
        String[] cardinals = {"ش", "ق", "ج", "غ"};  // ش=N, ق=E, ج=S, غ=W
        float[] angles     = {0, 90, 180, 270};
        for (int i = 0; i < 4; i++) {
            float rad = (float) Math.toRadians(angles[i]);
            canvas.drawText(cardinals[i],
                    cx + (float) Math.sin(rad) * labelR,
                    cy - (float) Math.cos(rad) * labelR + textPaint.getTextSize() / 3f,
                    textPaint);
        }

        // ── Needle (rotated to Qibla) ─────────────────────────────────────
        canvas.save();
        canvas.rotate(qiblaAngle, cx, cy);

        float needleLen = r * 0.65f;
        float needleW   = r * 0.07f;

        // Gold tip (toward Qibla)
        Path tip = new Path();
        tip.moveTo(cx, cy - needleLen);
        tip.lineTo(cx - needleW, cy);
        tip.lineTo(cx + needleW, cy);
        tip.close();
        canvas.drawPath(tip, needlePaint);

        // Blue tail (opposite direction)
        Path tail = new Path();
        tail.moveTo(cx, cy + needleLen * 0.45f);
        tail.lineTo(cx - needleW * 0.7f, cy);
        tail.lineTo(cx + needleW * 0.7f, cy);
        tail.close();
        canvas.drawPath(tail, needleTail);

        canvas.restore();

        // ── Centre dot ───────────────────────────────────────────────────
        canvas.drawCircle(cx, cy, r * 0.07f, dotPaint);

        // ── Kaaba emoji in centre ─────────────────────────────────────────
        Paint kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kaabaPaint.setTextSize(r * 0.13f);
        kaabaPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🕋", cx, cy + kaabaPaint.getTextSize() / 3f, kaabaPaint);
    }
}
