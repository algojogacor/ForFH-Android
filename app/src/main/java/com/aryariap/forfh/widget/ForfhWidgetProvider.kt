package com.aryariap.forfh.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Receiver widget jadwal (terdaftar di manifest dengan BIND_APPWIDGET). */
class ForfhWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ForfhWidget()
}
