package com.lentikr.reminder

import android.app.Application
import com.lentikr.reminder.data.AppContainer
import com.lentikr.reminder.data.DefaultAppContainer

class ReminderApplication : Application() {

    /**
     * AppContainer instance used by the rest of classes to obtain dependencies
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
