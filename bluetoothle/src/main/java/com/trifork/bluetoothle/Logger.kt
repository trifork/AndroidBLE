package com.trifork.bluetoothle

interface Logger {
    fun d(TAG: String, message: String)
    fun i(TAG: String, message: String)
    fun w(TAG: String, message: String)
    fun e(TAG: String, message: String)
}