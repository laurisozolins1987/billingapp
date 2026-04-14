package com.example.billingapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all bill reminders after device reboots
            Thread {
                val db = AppDatabase.getDatabase(context)
                val bills = db.billDao().getBillsWithReminders()
                bills.forEach { bill ->
                    BillReminderManager.scheduleReminder(context, bill)
                }
            }.start()
        }
    }
}
