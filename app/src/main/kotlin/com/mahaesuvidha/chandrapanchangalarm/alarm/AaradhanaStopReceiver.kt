package com.mahaesuvidha.chandrapanchangalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Receives notification swipe/delete events and cancels only the matching Aaradhana session. */
class AaradhanaStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOP) return
        val id = intent.getIntExtra(EXTRA_ID, -1)
        AaradhanaVoiceSession.stop(id.takeIf { it >= 0 })
    }

    companion object {
        const val ACTION_STOP = "com.mahaesuvidha.chandrapanchangalarm.STOP_AARADHANA"
        const val EXTRA_ID = "aaradhanaId"
    }
}
