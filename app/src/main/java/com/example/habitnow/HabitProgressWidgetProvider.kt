package com.example.habitnow

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

class HabitProgressWidgetProvider : AppWidgetProvider() {

    companion object {
        const val PREFS_NAME = "habit_prefs"
        const val KEY_COMPLETED = "habits_completed"
        const val KEY_TOTAL = "habits_total"
        const val KEY_DISPLAY_MODE = "widget_display_mode" // percent|fraction

        fun requestUpdateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(
                    context,
                    HabitProgressWidgetProvider::class.java
                )
            )
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val completed = prefs.getInt(KEY_COMPLETED, 0)
            val total = prefs.getInt(KEY_TOTAL, 0)
            val mode = prefs.getString(KEY_DISPLAY_MODE, "percent") ?: "percent"
            val percent = if (total == 0) 0 else (100f * completed / total).toInt()

            val views = RemoteViews(context.packageName, R.layout.widget_habit_progress)
            views.setProgressBar(R.id.widget_progress, 100, percent, false)
            val text = if (mode == "percent") "$percent%" else "$completed/$total"
            views.setTextViewText(R.id.widget_text, text)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}