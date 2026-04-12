package me.robbyblue.mylauncher.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import java.util.HashMap;

public class WidgetSystem {

    static boolean showOutlines;

    public static HashMap<WidgetLayout, LinearLayout> createLayout(WidgetList widgets, LinearLayout container, boolean hasOutlines) {
        showOutlines = hasOutlines;

        container.removeAllViewsInLayout();
        HashMap<WidgetLayout, LinearLayout> layouts = new HashMap<>();

        for (WidgetLayout widget : widgets.getChildren()) {
            if (widget instanceof WidgetElement) {
                LinearLayout layout = addTopLevelWidget((WidgetElement) widget, container);
                layouts.put(widget, layout);
            }
            if (widget instanceof WidgetList) {
                addRow((WidgetList) widget, container, layouts);
            }
        }

        return layouts;
    }

    private static LinearLayout addTopLevelWidget(WidgetElement widget, LinearLayout container) {
        Context ctx = container.getContext();
        LinearLayout childLayout = new LinearLayout(ctx);

        // Get available space - use container's parent dimensions if needed
        View parent = container.getParent() instanceof View ? (View) container.getParent() : container;
        int screenWidth = parent.getWidth();
        int screenHeight = parent.getHeight();
        
        // Fallback if not measured yet
        if (screenWidth <= 0) screenWidth = 720;
        if (screenHeight <= 0) screenHeight = 1280;

        if (showOutlines) {
            childLayout.setBackground(createOutline(Color.MAGENTA));
        }

        // Use the stored size percentage (30 = 30%)
        double sizePercent = widget.getSize() / 100.0;
        // Clamp to reasonable range (5% to 100%)
        if (sizePercent < 0.05) sizePercent = 0.05;
        if (sizePercent > 1.0) sizePercent = 1.0;
        
        int height = (int) (screenHeight * sizePercent);
        if (height < 50) height = 50;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(screenWidth, height);
        childLayout.setLayoutParams(layoutParams);
        childLayout.setGravity(Gravity.CENTER);

        // Add the actual widget view if available
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
        AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(widget.getAppWidgetId());
        
        if (appWidgetInfo != null) {
            // The widget view is handled by the host in the parent
        }

        container.addView(childLayout);
        return childLayout;
    }

    private static void addRow(WidgetList widget, LinearLayout container, HashMap<WidgetLayout, LinearLayout> layouts) {
        Context ctx = container.getContext();
        LinearLayout childLayout = new LinearLayout(ctx);

        int screenWidth = container.getWidth();
        if (screenWidth <= 0) screenWidth = 720;
        
        // getSize() stored as integer (30 = 30%), convert
        double sizeFraction = widget.getSize() / 100.0;
        if (sizeFraction < 0.05) sizeFraction = 0.05;
        if (sizeFraction > 1.0) sizeFraction = 1.0;
        
        int height = (int) (screenWidth * sizeFraction);
        if (height < 50) height = 50;
        
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(screenWidth, height);
        childLayout.setLayoutParams(layoutParams);

        if (showOutlines) {
            childLayout.setBackground(createOutline(Color.BLUE));
        }

        for (WidgetLayout child : widget.getChildren()) {
            addWidgetInRow(child, childLayout, layouts, screenWidth, height);
        }

        container.addView(childLayout);
    }

    private static void addWidgetInRow(WidgetLayout widget, LinearLayout container, HashMap<WidgetLayout, LinearLayout> layouts, int parentWidth, int parentHeight) {
        Context ctx = container.getContext();
        LinearLayout childLayout = new LinearLayout(ctx);

        // getSize() is stored as integer (30 = 30%), convert to decimal
        double sizeFraction = widget.getSize() / 100.0;
        if (sizeFraction < 0.01) sizeFraction = 0.01;
        if (sizeFraction > 1.0) sizeFraction = 1.0;
        
        int width = (int) (parentWidth * sizeFraction);
        if (width < 20) width = 20;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, parentHeight);
        childLayout.setLayoutParams(layoutParams);

        if (showOutlines) {
            childLayout.setBackground(createOutline(Color.GREEN));
        }

        container.addView(childLayout);
        layouts.put(widget, childLayout);
    }

    private static GradientDrawable createOutline(int color) {
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.TRANSPARENT);
        border.setStroke(4, color);
        border.setCornerRadius(16f);
        return border;
    }

}
