package com.aryariap.forfh.alarm

import android.app.Activity
import android.os.Bundle

class FullScreenAlarmActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // diisi penuh di T9
    }
}
