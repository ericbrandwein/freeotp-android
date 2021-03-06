package org.fedorahosted.freeotp.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import org.fedorahosted.freeotp.*;

/**
 * Created by root on 13/04/17.
 */
public class OtpListWidgetProvider extends AppWidgetProvider {

    /**
     * Should be called whenever the {@link Token}s in the {@link TokenPersistence} are changed.
     */
    public static void notifyWidgetDataChanged(final Context context) {
        final AppWidgetManager manager = AppWidgetManager.getInstance(context);
        final ComponentName providerComponentName = new ComponentName(context, OtpListWidgetProvider.class);
        final int[] widgetIds = manager.getAppWidgetIds(providerComponentName);
        manager.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_widget);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (final int widgetId : appWidgetIds) {
            final RemoteViews widget = getFirstWidget(context, widgetId);
            appWidgetManager.updateAppWidget(widgetId, widget);
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        final String action = intent.getAction();
        if (OtpListWidgetService.ACTION_SHOW_CODE.equals(action) ||
                OtpListWidgetService.ACTION_HIDE_CODE.equals(action)) {
            int widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            final String tokenId = intent.getStringExtra(OtpListWidgetService.EXTRA_TOKEN_ID);
            final OtpListWidgetViewModel model = OtpListWidgetViewModel.getInstance(widgetId);
            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            if (OtpListWidgetService.ACTION_SHOW_CODE.equals(action)) {
                model.addTokenIdToShow(tokenId);
                final ClipboardManager clipboardManager =
                        (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                final String code = getCodeForTokenId(context, tokenId);
                ClipboardManagerUtil.copyToClipboard(context, clipboardManager, code);
            } else {
                model.removeTokenIdToShow(tokenId);
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.list_widget);
        }
    }

    private RemoteViews getFirstWidget(Context context, int widgetId) {
        final Intent serviceIntent = new Intent(context, OtpListWidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        final RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.list_widget);
        widget.setRemoteAdapter(R.id.list_widget, serviceIntent);
        widget.setEmptyView(R.id.list_widget, android.R.id.empty);

        setTitleIntent(context, widget);
        setCodeClickPendingIntentTemplate(context, widget, widgetId);
        return widget;
    }

    private void setTitleIntent(final Context context, final RemoteViews widget) {
        final Intent intent = new Intent(context, MainActivity.class)
                .setAction(Intent.ACTION_MAIN);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        widget.setOnClickPendingIntent(R.id.widget_title_container, pendingIntent);
    }

    private void setCodeClickPendingIntentTemplate(Context context, RemoteViews widget, int widgetId) {
        final Intent codeClickIntent = new Intent(context, OtpListWidgetProvider.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        codeClickIntent.setData(Uri.parse(codeClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent showCodeIntentTemplate =
                PendingIntent.getBroadcast(context, 0, codeClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setPendingIntentTemplate(R.id.list_widget, showCodeIntentTemplate);
    }

    private String getCodeForTokenId(final Context context, final String id) {
        final TokenPersistence persistence = new TokenPersistence(context);
        for (int i = 0; i <= persistence.length(); i++) {
            final Token token = persistence.get(i);
            if (token.getID().equals(id)) {
                return token.generateCodes().getCurrentCode();
            }
        }
        return null;
    }
}
