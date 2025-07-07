package com.aught.wakawaka.widget

import androidx.glance.action.ActionParameters

class WakaWidgetHelpers {
    companion object {
        val TIME_WINDOW_PROPORTION = 0.5f;
        val GRAPH_HEIGHT = 120;
        val GRAPH_WIDTH = 360;
        val GRAPH_BOTTOM_PADDING = 10;
        val DATE_TEXT_HEIGHT = 20;

        val WIDGET_INTENT_ID = ActionParameters.Key<String>("widget_intent_id");
    }
}
