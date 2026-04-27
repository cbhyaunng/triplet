package com.triplet.app

import android.app.Application
import com.triplet.app.travel.TripletPersistence

class TripletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TripletPersistence.initialize(this)
    }
}
