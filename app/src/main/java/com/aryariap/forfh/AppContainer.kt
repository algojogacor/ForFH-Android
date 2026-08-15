package com.aryariap.forfh

import com.aryariap.forfh.data.db.AppDatabase

class AppContainer(private val app: ForfhApp) {
    val database: AppDatabase by lazy { AppDatabase.build(app) }
}
