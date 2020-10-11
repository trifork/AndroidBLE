package com.trifork.bluetoothle

import android.bluetooth.le.ScanResult

interface BLEManagerScanCallbacks {
    fun onScanResult(sr: ScanResult)
    fun onScanFailed(errorCode: Int)
}