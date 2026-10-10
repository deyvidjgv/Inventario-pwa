package com.deyvidjgv.inventario.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.deyvidjgv.inventario.domain.dto.ProductMarginReport;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;

import java.util.ArrayList;
import java.util.List;

public class ProductDistributionChartView extends View {

    private final List<ProductMarginReport> productList = new ArrayList<>();

    private final Paint barCostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barProfitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private static final int COLOR_PROFIT = 0xFF00E676;  // Verde ganancia
    private static final int COLOR_COST = 0xFF546E7A;    // Gris azulado costo
    private static final int COLOR_BAR_BG = 0xFF2A2A2A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_MUTED = 0xFFB0B0B0;

    public ProductDistributionChartView(Context context) {
        super(context);
        init();
    }

    public ProductDistributionChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProductDistributionChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barProfitPaint.setColor(COLOR_PROFIT);
        barCostPaint.setColor(COLOR_COST);
        bgBarPaint.setColor(COLOR_BAR_BG);
    }

    public void setData(List<ProductMarginReport> reports) {
        productList.clear();
        if (reports != null) {
            for (ProductMarginReport r : reports) {
                if (r.getTotalSales() > 0 || r.getUnitsSold() > 0) {
                    productList.add(r);
                }
            }
        }
        // Ordenar mayor venta primero
        productList.sort((a, b) -> Long.compare(b.getTotalSales(), a.getTotalSales()));
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int count = Math.max(1, productList.size());
        // Cada fila de producto mide ~56dp + padding superior
        int desiredHeight = (int) ((count * 52f + 30f) * density);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        float density = getResources().getDisplayMetrics().density;

        if (w <= 0 || productList.isEmpty()) {
            textPaint.setColor(COLOR_TEXT_MUTED);
            textPaint.setTextSize(13f * density);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Sin productos vendidos aún en este período", w / 2f, 30f * density, textPaint);
            return;
        }

        long maxSales = 1;
        for (ProductMarginReport r : productList) {
            if (r.getTotalSales() > maxSales) {
                maxSales = r.getTotalSales();
            }
        }

        float startY = 16f * density;
        float rowHeight = 52f * density;
        float barHeight = 16f * density;
        float maxBarWidth = w - 32f * density;

        for (int i = 0; i < productList.size(); i++) {
            ProductMarginReport r = productList.get(i);
            float currentY = startY + (i * rowHeight);

            // Título: Nombre y unidades
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(COLOR_TEXT);
            textPaint.setTextSize(14f * density);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            String title = r.getProductName() + "  (" + r.getUnitsSold() + " uds)";
            canvas.drawText(title, 16f * density, currentY + 12f * density, textPaint);

            // Derecha: Ganancia y Total
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(COLOR_PROFIT);
            textPaint.setTextSize(13f * density);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            String financialStr = "+" + CurrencyFormatter.formatCOP(r.getRealProfit()) + " / " + CurrencyFormatter.formatCOP(r.getTotalSales());
            canvas.drawText(financialStr, w - 16f * density, currentY + 12f * density, textPaint);

            // Barra horizontal
            float barTop = currentY + 20f * density;
            float barBottom = barTop + barHeight;
            float barTotalWidth = Math.max(12f * density, ((float) r.getTotalSales() / maxSales) * maxBarWidth);

            // Fondo de la barra
            rect.set(16f * density, barTop, 16f * density + maxBarWidth, barBottom);
            canvas.drawRoundRect(rect, 4f * density, 4f * density, bgBarPaint);

            // Segmento de Costo y Ganancia
            float profitRatio = r.getTotalSales() > 0 ? (float) r.getRealProfit() / r.getTotalSales() : 0f;
            float profitWidth = barTotalWidth * profitRatio;
            float costWidth = barTotalWidth - profitWidth;

            // Dibuja Costo
            if (costWidth > 0) {
                rect.set(16f * density, barTop, 16f * density + costWidth, barBottom);
                canvas.drawRoundRect(rect, 4f * density, 4f * density, barCostPaint);
            }

            // Dibuja Ganancia
            if (profitWidth > 0) {
                rect.set(16f * density + costWidth, barTop, 16f * density + barTotalWidth, barBottom);
                canvas.drawRoundRect(rect, 4f * density, 4f * density, barProfitPaint);
            }
        }
    }
}
