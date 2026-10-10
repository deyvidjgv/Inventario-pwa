package com.deyvidjgv.inventario.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;

public class FinancialChartView extends View {

    private long totalSales = 0;
    private long totalCost = 0;
    private long totalProfit = 0;
    private String subtitle = "";

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private static final int COLOR_SALES = 0xFF00E676;   // Verde neón
    private static final int COLOR_COST = 0xFFFF5252;    // Rojo coral
    private static final int COLOR_PROFIT = 0xFFFFD54F;  // Dorado cálido
    private static final int COLOR_GRID = 0xFF2C2C2C;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_MUTED = 0xFF9E9E9E;

    public FinancialChartView(Context context) {
        super(context);
        init();
    }

    public FinancialChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FinancialChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        linePaint.setColor(COLOR_GRID);
        linePaint.setStrokeWidth(2f);
    }

    public void setData(long totalSales, long totalCost, long totalProfit, String subtitle) {
        this.totalSales = Math.max(0, totalSales);
        this.totalCost = Math.max(0, totalCost);
        this.totalProfit = Math.max(0, totalProfit);
        this.subtitle = subtitle != null ? subtitle : "";
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) (230 * getResources().getDisplayMetrics().density);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float density = getResources().getDisplayMetrics().density;

        if (w <= 0 || h <= 0) return;

        // Subtítulo / Encabezado
        textPaint.setColor(COLOR_TEXT_MUTED);
        textPaint.setTextSize(12f * density);
        textPaint.setTypeface(Typeface.DEFAULT);
        if (!subtitle.isEmpty()) {
            canvas.drawText(subtitle, w / 2f, 20f * density, textPaint);
        }

        float chartTop = 38f * density;
        float chartBottom = h - 45f * density;
        float chartHeight = chartBottom - chartTop;

        // Línea base del gráfico
        canvas.drawLine(20f * density, chartBottom, w - 20f * density, chartBottom, linePaint);

        if (totalSales == 0 && totalCost == 0 && totalProfit == 0) {
            textPaint.setColor(COLOR_TEXT_MUTED);
            textPaint.setTextSize(14f * density);
            canvas.drawText("Sin ventas registradas en este período", w / 2f, chartTop + chartHeight / 2f, textPaint);
            return;
        }

        long maxVal = Math.max(totalSales, Math.max(totalCost, totalProfit));
        if (maxVal <= 0) maxVal = 1;

        // 3 Barras: Ventas, Costos, Ganancias
        float barWidth = Math.min(64f * density, (w - 80f * density) / 4f);
        float spacing = (w - (barWidth * 3)) / 4f;

        long[] values = {totalSales, totalCost, totalProfit};
        int[] colors = {COLOR_SALES, COLOR_COST, COLOR_PROFIT};
        String[] labels = {"Ventas", "Costo", "Ganancia"};

        for (int i = 0; i < 3; i++) {
            float left = spacing * (i + 1) + barWidth * i;
            float right = left + barWidth;
            float valRatio = (float) values[i] / (float) maxVal;
            float barH = Math.max(6f * density, valRatio * (chartHeight - 30f * density));
            float top = chartBottom - barH;

            barRect.set(left, top, right, chartBottom);
            barPaint.setColor(colors[i]);
            canvas.drawRoundRect(barRect, 8f * density, 8f * density, barPaint);

            // Monto encima de la barra
            textPaint.setColor(COLOR_TEXT);
            textPaint.setTextSize(11f * density);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            String formattedVal = CurrencyFormatter.formatCOP(values[i]);
            canvas.drawText(formattedVal, left + barWidth / 2f, Math.max(chartTop + 10f * density, top - 6f * density), textPaint);

            // Etiqueta debajo de la barra
            textPaint.setColor(colors[i]);
            textPaint.setTextSize(13f * density);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(labels[i], left + barWidth / 2f, chartBottom + 18f * density, textPaint);

            // Porcentaje respecto a ventas debajo de la etiqueta
            if (i > 0 && totalSales > 0) {
                float pct = (values[i] * 100f) / totalSales;
                textPaint.setColor(COLOR_TEXT_MUTED);
                textPaint.setTextSize(10f * density);
                textPaint.setTypeface(Typeface.DEFAULT);
                canvas.drawText(String.format(java.util.Locale.US, "%.1f%%", pct), left + barWidth / 2f, chartBottom + 32f * density, textPaint);
            }
        }
    }
}
