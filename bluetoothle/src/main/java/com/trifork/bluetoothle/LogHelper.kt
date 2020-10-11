package com.trifork.bluetoothle

object LogHelper {
    fun d(logger: Logger?, TAG: String?, message: String?) {
        logger?.d(TAG, message)
    }

    fun i(logger: Logger?, TAG: String?, message: String?) {
        logger?.i(TAG, message)
    }

    fun w(logger: Logger?, TAG: String?, message: String?) {
        logger?.w(TAG, message)
    }

    fun e(logger: Logger?, TAG: String?, message: String?) {
        logger?.e(TAG, message)
    }
}