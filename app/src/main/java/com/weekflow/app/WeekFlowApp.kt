package com.weekflow.app

import android.app.Application
import com.weekflow.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WeekFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@WeekFlowApp)
            modules(appModule)
        }
    }
}
