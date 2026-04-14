package com.example.billingapp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.*

class BillReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_BILL_ID = "bill_id"
        const val EXTRA_BILL_TITLE = "bill_title"
        const val EXTRA_BILL_AMOUNT = "bill_amount"
        const val CHANNEL_ID = "bill_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val billId = intent.getIntExtra(EXTRA_BILL_ID, -1)
        val billTitle = intent.getStringExtra(EXTRA_BILL_TITLE) ?: return
        val billAmount = intent.getDoubleExtra(EXTRA_BILL_AMOUNT, 0.0)

        createNotificationChannel(context)

        val currencySymbol = SettingsActivity.getCurrencySymbol(context)
        val amountText = String.format("%,.2f %s", billAmount, currencySymbol)

        // Open BillDetailActivity when notification is tapped
        val detailIntent = Intent(context, BillDetailActivity::class.java).apply {
            putExtra(BillDetailActivity.EXTRA_BILL_ID, billId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, billId, detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.bill_reminder_title))
            .setContentText(context.getString(R.string.bill_reminder_text, billTitle, amountText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(billId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.bill_reminder_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.bill_reminder_channel_desc)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

object BillReminderManager {

    fun scheduleReminder(context: Context, bill: Bill) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Calculate reminder time: dueDate - reminderDaysBefore at 9:00 AM
        val reminderCalendar = Calendar.getInstance().apply {
            timeInMillis = bill.dueDate
            add(Calendar.DAY_OF_MONTH, -bill.reminderDaysBefore)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Don't schedule if the reminder time has already passed
        if (reminderCalendar.timeInMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            putExtra(BillReminderReceiver.EXTRA_BILL_ID, bill.id)
            putExtra(BillReminderReceiver.EXTRA_BILL_TITLE, bill.title)
            putExtra(BillReminderReceiver.EXTRA_BILL_AMOUNT, bill.amount)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, bill.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderCalendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if exact alarm permission is not granted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderCalendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, billId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BillReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, billId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
