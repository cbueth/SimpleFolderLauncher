package me.robbyblue.mylauncher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WidgetPickerActivity extends AppCompatActivity {

    public static final String EXTRA_APPWIDGET_ID = "appWidgetId";
    private static final int REQUEST_CONFIGURE = 100;

    private AppWidgetManager appWidgetManager;
    private AppWidgetHost appWidgetHost;
    private ExpandableListView expandableList;

    private List<WidgetAppItem> appsWithWidgets;
    private Map<String, List<WidgetPreviewData>> widgetsByApp;
    private int pendingAppWidgetId = -1;
    private AppWidgetProviderInfo pendingInfo;
    private String folder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_picker);

        // Get folder from intent
        android.content.Intent intent = getIntent();
        this.folder = intent.getStringExtra("folder");

        appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetHost = new AppWidgetHost(this, MainActivity.APPWIDGET_HOST_ID);

        expandableList = findViewById(R.id.widgets_expandable_list);
        expandableList.setGroupIndicator(null);
        expandableList.setChildIndicator(null);

        findViewById(R.id.cancel_button).setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
        findViewById(R.id.done_button).setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        loadWidgets();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CONFIGURE) {
            if (resultCode == RESULT_OK && pendingAppWidgetId != -1) {
                // Config succeeded, widget should be bound now
                onWidgetPicked(pendingAppWidgetId);
            } else {
                // Config cancelled or failed
                if (pendingAppWidgetId != -1) {
                    appWidgetHost.deleteAppWidgetId(pendingAppWidgetId);
                }
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private void loadWidgets() {
        List<AppWidgetProviderInfo> providers = appWidgetManager.getInstalledProvidersForProfile(null);
        
        appsWithWidgets = new ArrayList<>();
        widgetsByApp = new HashMap<>();
        
        Map<String, WidgetAppItem> appsMap = new HashMap<>();
        
        for (AppWidgetProviderInfo info : providers) {
            if (info.widgetCategory != AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) {
                continue;
            }
            
            String pkg = info.provider.getPackageName();
            
            if (!appsMap.containsKey(pkg)) {
                Drawable icon = getAppIcon(pkg);
                String appLabel = getAppLabel(pkg);
                WidgetAppItem appItem = new WidgetAppItem(pkg, appLabel, icon);
                appsMap.put(pkg, appItem);
                appsWithWidgets.add(appItem);
                widgetsByApp.put(pkg, new ArrayList<>());
            }
            
            String sizeStr = info.minWidth + "x" + info.minHeight;
            WidgetPreviewData widgetData = new WidgetPreviewData(info, info.label != null ? info.label.toString() : "Widget", sizeStr);
            widgetsByApp.get(pkg).add(widgetData);
        }

        WidgetExpandableAdapter adapter = new WidgetExpandableAdapter(appsWithWidgets, widgetsByApp, this);
        expandableList.setAdapter(adapter);
        
        expandableList.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            WidgetPreviewData widget = widgetsByApp.get(appsWithWidgets.get(groupPosition).packageName).get(childPosition);
            onWidgetSelected(widget.info);
            return true;
        });
    }

    private Drawable getAppIcon(String packageName) {
        try {
            return getPackageManager().getApplicationIcon(packageName);
        } catch (Exception e) {
            return getPackageManager().getDefaultActivityIcon();
        }
    }

    private String getAppLabel(String packageName) {
        try {
            return getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private void onWidgetSelected(AppWidgetProviderInfo info) {
        pendingInfo = info;
        
        // Allocate widget ID
        int widgetId = appWidgetHost.allocateAppWidgetId();
        
        // Check if binding is allowed
        boolean allowed = appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, info.provider);
        
        if (allowed) {
            pendingAppWidgetId = widgetId;
            onWidgetPicked(widgetId);
        } else {
            // Use system bind flow - this triggers the permission picker
            pendingAppWidgetId = widgetId;
            
            Intent bindIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
            
            startActivityForResult(bindIntent, REQUEST_CONFIGURE);
        }
    }

    private void onWidgetPicked(int appWidgetId) {
        if (pendingInfo == null) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }
        
        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    static class WidgetAppItem {
        String packageName;
        String appLabel;
        Drawable icon;

        WidgetAppItem(String packageName, String appLabel, Drawable icon) {
            this.packageName = packageName;
            this.appLabel = appLabel;
            this.icon = icon;
        }
    }

    static class WidgetPreviewData {
        AppWidgetProviderInfo info;
        String label;
        String size;

        WidgetPreviewData(AppWidgetProviderInfo info, String label, String size) {
            this.info = info;
            this.label = label;
            this.size = size;
        }
    }

    class WidgetExpandableAdapter extends BaseExpandableListAdapter {
        private List<WidgetAppItem> apps;
        private Map<String, List<WidgetPreviewData>> widgetsByApp;
        private Activity activity;
        private LayoutInflater inflater;

        WidgetExpandableAdapter(List<WidgetAppItem> apps, Map<String, List<WidgetPreviewData>> widgetsByApp, Activity activity) {
            this.apps = apps;
            this.widgetsByApp = widgetsByApp;
            this.activity = activity;
            this.inflater = activity.getLayoutInflater();
        }

        @Override
        public int getGroupCount() {
            return apps.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return widgetsByApp.get(apps.get(groupPosition).packageName).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return apps.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return widgetsByApp.get(apps.get(groupPosition).packageName).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_widget_app_header, parent, false);
            }

            WidgetAppItem app = apps.get(groupPosition);
            ImageView icon = convertView.findViewById(R.id.app_icon);
            TextView label = convertView.findViewById(R.id.app_label);
            TextView count = convertView.findViewById(R.id.widget_count);

            icon.setImageDrawable(app.icon);
            label.setText(app.appLabel);
            
            int widgetCount = widgetsByApp.get(app.packageName).size();
            count.setText(widgetCount + " " + (widgetCount == 1 ? "widget" : "widgets"));

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_widget_preview, parent, false);
            }

            WidgetPreviewData widget = widgetsByApp.get(apps.get(groupPosition).packageName).get(childPosition);
            ImageView icon = convertView.findViewById(R.id.widget_icon);
            TextView label = convertView.findViewById(R.id.widget_label);
            TextView size = convertView.findViewById(R.id.widget_size);

            // Just use the app icon - actual widget previews require complex rendering
            try {
                icon.setImageDrawable(activity.getPackageManager().getApplicationIcon(widget.info.provider.getPackageName()));
            } catch (Exception e) {
                icon.setImageDrawable(activity.getPackageManager().getDefaultActivityIcon());
            }
            
            label.setText(widget.label);
            size.setText(widget.size);

            return convertView;
        }

        private Drawable loadWidgetPreview(AppWidgetProviderInfo info) {
            try {
                // First try previewLayout (Android 12+)
                if (info.previewLayout != 0) {
                    RemoteViews rv = new RemoteViews(activity.getPackageName(), info.previewLayout);
                    return createDrawableFromRemoteViews(rv, info.minWidth, info.minHeight);
                }
                // Then try previewImage (static image)
                if (info.previewImage != 0) {
                    return activity.getResources().getDrawable(info.previewImage, activity.getTheme());
                }
            } catch (Exception e) {
                // Ignore and return null
            }
            return null;
        }

        private Drawable createDrawableFromRemoteViews(RemoteViews rv, int width, int height) {
            try {
                // Create a bitmap to render the RemoteViews
                int w = Math.max(width, 64);
                int h = Math.max(height, 64);
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                
                // Create a simple AppWidgetHostView to render
                AppWidgetHostView hostView = new AppWidgetHostView(activity);
                hostView.setAppWidget(0, null);
                hostView.updateAppWidget(rv);
                hostView.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                hostView.layout(0, 0, w, h);
                hostView.draw(canvas);
                
                return new android.graphics.drawable.BitmapDrawable(bitmap);
            } catch (Exception e) {
                return null;
            }
        }
    }
}