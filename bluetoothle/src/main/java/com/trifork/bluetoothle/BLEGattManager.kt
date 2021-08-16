package com.trifork.bluetoothle

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import java.util.*

internal class BLEGattManager(
    private val mListener: BLEGattManagerCallbacks,
    private val mContext: Context,
    private val mLogger: Logger?
) {
    companion object {
        private val TAG = BLEGattManager::class.java.simpleName
        private val CCCD_ID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val ACTION_CONNECT = "ACTION_CONNECT"
        private const val ACTION_DISCOVER_SERVICES = "ACTION_DISCOVER_SERVICES"
        private const val ACTION_CREATE_BOND = "ACTION_CREATE_BOND"
        private const val ACTION_CHANGE_MTU = "ACTION_CHANGE_MTU"
        private const val ACTION_WRITE_CHARACTERISTIC = "ACTION_WRITE_CHARACTERISTIC"
        private const val ACTION_READ_CHARACTERISTIC = "ACTION_READ_CHARACTERISTIC"
        private const val ACTION_SET_CHARACTERISTIC_NOTIFICATION =
            "ACTION_SET_CHARACTERISTIC_NOTIFICATION"
        private const val ACTION_READ_REMOTE_RSSI = "ACTION_READ_REMOTE_RSSI"
    }

    private var mBluetoothGatt: BluetoothGatt? = null
    private var mDevice: BluetoothDevice? = null
    private val bleHandler = Handler(Looper.getMainLooper())
    private val actionQueue = ArrayDeque<Runnable>()
    private val actionNames = HashMap<Runnable, String>()
    private var actionStartedAt: Date? = null
    private var retries = 0

    private val mBluetoothBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)!!
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val state =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                LogHelper.d(mLogger, TAG, "onReceive: Bond state: $state")
                mListener.onBondStateChanged(state, device)
            } else {
                LogHelper.d(mLogger, TAG, "onReceive() device state " + device.bondState)
            }
            executeNext()
        }
    }

    @Synchronized
    private fun addAction(action: Runnable, actionName: String) {
        actionQueue.add(action)
        actionNames[action] = actionName
        LogHelper.d(mLogger, TAG, "New action queue size: " + actionQueue.size)
        when {
            actionStartedAt == null -> {
                actionStartedAt = Date()
                LogHelper.d(mLogger, TAG, "Execute first action: " + actionNames[action])
                action.run()
                //bleHandler.post(action);
            }
            actionStartedAt!!.time + 60000 < Date().time -> {
                try {
                    LogHelper.d(
                        mLogger,
                        TAG,
                        "Action blocks(" + actionNames[actionQueue.first] + ") executing next action after 60 seconds wait time"
                    )
                } catch (e: NoSuchElementException) {
                    LogHelper.e(mLogger, TAG, "No such element: " + e.message)
                }
                clearActionQueue()
                executeNext()
            }
            else -> {
                try {
                    LogHelper.d(
                        mLogger,
                        TAG,
                        "Already executing task: " + actionNames[actionQueue.first]
                    )
                } catch (e: NoSuchElementException) {
                    LogHelper.e(mLogger, TAG, "No such element: " + e.message)
                }
            }
        }
    }

    @Synchronized
    private fun executeNext() {
        if (actionQueue.isEmpty()) {
            LogHelper.d(mLogger, TAG, "Completed all actions!")
            actionStartedAt = null
            return
        }
        if (retries in 1..2) {
            val retryAction = actionQueue.first
            LogHelper.d(mLogger, TAG, "Retrying action: " + actionNames[retryAction])
            bleHandler.post(retryAction)
            return
        }
        retries = 0
        val previousAction = actionQueue.removeFirst()
        LogHelper.d(mLogger, TAG, "Completed action: " + actionNames[previousAction])
        actionNames.remove(previousAction)
        if (actionQueue.isEmpty()) {
            actionStartedAt = null
        } else {
            actionStartedAt = Date()
            val nextAction = actionQueue.first
            LogHelper.d(mLogger, TAG, "Execute next action: " + actionNames[nextAction])
            bleHandler.post(nextAction)
        }
    }

    fun connect(device: BluetoothDevice) {
        clearActionQueue()
        actionStartedAt = null
        addAction({
            mDevice = device
            LogHelper.d(
                mLogger,
                TAG,
                "Connecting to device: " + device.address + " bond state: " + device.bondState
            )
            mBluetoothGatt = mDevice!!.connectGatt(mContext, false, mBluetoothGattCallback)
        }, ACTION_CONNECT)
    }

    fun discoverServices() {
        addAction({
            mBluetoothGatt?.let {
                LogHelper.d(mLogger, TAG, "Discovering services.")
                if (!it.discoverServices()) {
                    LogHelper.e(mLogger, TAG, "Failed to discover services.")
                }
            } ?: run {
                LogHelper.w(mLogger, TAG, "discoverServices: mBluetoothGatt == null")
            }
        }, ACTION_DISCOVER_SERVICES)
    }

    fun createBond(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            LogHelper.d(mLogger, TAG, "Already paired")
            mListener.onBondStateChanged(BluetoothDevice.BOND_BONDED, device)
        } else {
            addAction({
                LogHelper.d(mLogger, TAG, "Starting pairing")
                mContext.registerReceiver(mBluetoothBroadcastReceiver, broadcastIntentFilter)
                device.createBond()
            }, ACTION_CREATE_BOND)
        }
    }

    fun changeMtu(mtu: Int) {
        addAction({
            mBluetoothGatt?.let {
                LogHelper.d(mLogger, TAG, "Changing MTU size to: $mtu")
                if (!it.requestMtu(mtu)) {
                    LogHelper.e(mLogger, TAG, "Failed to request MTU size: $mtu")
                }
            } ?: run {
                LogHelper.w(mLogger, TAG, "requestMtu: mBluetoothGatt == null")
            }
        }, ACTION_CHANGE_MTU)
    }

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt != mBluetoothGatt) {
                return
            }
            LogHelper.w(
                mLogger,
                TAG,
                "onConnectionStateChange status: $status newState: " + getStateString(
                    newState
                )
            )
            if (gatt != mBluetoothGatt) {
                executeNext()
                LogHelper.e(mLogger, TAG, "Incorrect GATT! status: $status newState: $newState")
                clearActionQueue()
                gatt.close()
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    LogHelper.d(mLogger, TAG, "Connected to GATT server!")
                    executeNext()
                    mListener.onConnected(gatt.device)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    LogHelper.d(mLogger, TAG, "Disconnected from GATT server status = [$status]")
                    refreshDeviceCache(gatt)
                    gatt.disconnect()
                    gatt.close()
                    mDevice = null
                    mBluetoothGatt = null
                    clearActionQueue()
                    mListener.onDisconnected(gatt.device, status)
                }
                else -> {
                    LogHelper.e(mLogger, TAG, "Unknown state: $newState status: $status")
                }
            }
        }

        private fun getStateString(newState: Int): String {
            when (newState) {
                BluetoothProfile.STATE_DISCONNECTED -> return "STATE_DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> return "STATE_CONNECTING"
                BluetoothProfile.STATE_CONNECTED -> return "STATE_CONNECTED"
                BluetoothProfile.STATE_DISCONNECTING -> return "STATE_DISCONNECTING"
            }
            return "STATE_UNKNOWN"
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = ArrayList<String>()
                for (service in gatt.services) {
                    services.add(service.uuid.toString())
                }
                LogHelper.d(mLogger, TAG, "onServicesDiscovered received: $services")
                mListener.onServicesDiscovered(gatt.services)
            } else {
                LogHelper.w(mLogger, TAG, "onServicesDiscovered received: $status")
            }
            executeNext()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogHelper.d(mLogger, TAG, "onCharacteristicRead: " + characteristic.uuid.toString())
                mListener.onCharacteristicRead(characteristic, characteristic.value)
            } else {
                retries++
                LogHelper.w(mLogger, TAG, "onCharacteristicRead status: $status")
            }
            executeNext()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            LogHelper.d(mLogger, TAG, "onCharacteristicChanged")
            mListener.onCharacteristicChanged(characteristic, characteristic.value)
            executeNext()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogHelper.d(mLogger, TAG, "onCharacteristicWrite")
                mListener.onCharacteristicWrite(characteristic, status)
            } else {
                retries++
                LogHelper.w(mLogger, TAG, "onCharacteristicWrite status: $status")
            }
            executeNext()
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogHelper.d(mLogger, TAG, "onDescriptorWrite")
                mListener.onDescriptorWrite(descriptor, status)
            } else {
                retries++
                LogHelper.d(mLogger, TAG, "onDescriptorWrite status: $status")
            }
            executeNext()
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogHelper.d(mLogger, TAG, "MTU size is now: $mtu")
                mListener.onMtuChanged(mtu)
            } else {
                LogHelper.e(mLogger, TAG, "MTU size could not be changed, status: $status")
            }
            executeNext()
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogHelper.d(mLogger, TAG, "onReadRemoteRssi: $rssi")
                mListener.onReadRemoteRssi(rssi)
            } else {
                retries++
                LogHelper.w(mLogger, TAG, "onReadRemoteRssi status: $status")
            }
            executeNext()
        }
    }
    private val broadcastIntentFilter: IntentFilter
        get() = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        addAction({
            LogHelper.d(
                mLogger,
                TAG,
                "writeCharacteristic() called with: service = [" + characteristic.service?.uuid + "], characteristicUuid = [" + characteristic.uuid + "]"
            )
            mBluetoothGatt?.let {
                characteristic.value = data
                if (!it.writeCharacteristic(characteristic)) {
                    LogHelper.e(
                        mLogger,
                        TAG,
                        "writeCharacteristic on mBluetoothGatt failed"
                    )
                }
            } ?: run {
                LogHelper.w(mLogger, TAG, "writeCharacteristic: mBluetoothGatt == null")
            }
        }, ACTION_WRITE_CHARACTERISTIC)
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        LogHelper.d(mLogger, TAG, "addAction - readCharacteristic")
        addAction({
            mBluetoothGatt?.let {
                LogHelper.d(
                    mLogger,
                    TAG,
                    "readCharacteristic(" + characteristic.uuid + ", " + characteristic.service?.uuid + ")"
                )
                try {
                    if (!it.readCharacteristic(characteristic)) {
                        LogHelper.e(
                            mLogger,
                            TAG,
                            "readCharacteristic on mBluetoothGatt failed"
                        )
                    }
                } catch (npe: NullPointerException) {
                    LogHelper.e(
                        mLogger,
                        TAG,
                        "readCharacteristic(" + characteristic.uuid + ", " + characteristic.service?.uuid + ") throws NullPointerException"
                    )
                }
            } ?: run {
                LogHelper.w(
                    mLogger,
                    TAG,
                    "readCharacteristic(" + characteristic.uuid + ") failed, mBluetoothGatt is null"
                )
            }
        }, ACTION_READ_CHARACTERISTIC)
    }

    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        addAction({
            LogHelper.d(
                mLogger,
                TAG,
                "setCharacteristicNotification(" + characteristic.uuid + ", " + characteristic.service?.uuid.toString() + ")"
            )
            mBluetoothGatt?.let {
                it.setCharacteristicNotification(characteristic, enabled)
                val descriptor = characteristic.getDescriptor(CCCD_ID)
                descriptor.value =
                    if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                if (!it.writeDescriptor(descriptor)) {
                    LogHelper.e(
                        mLogger,
                        TAG,
                        "writeDescriptor on mBluetoothGatt failed"
                    )
                }
            } ?: run {
                LogHelper.w(
                    mLogger,
                    TAG,
                    "setCharacteristicNotification: mBluetoothGatt == null"
                )
            }
        }, ACTION_SET_CHARACTERISTIC_NOTIFICATION)

    }

    fun disconnect() {
        LogHelper.d(mLogger, TAG, "disconnect()")
        clearActionQueue()
        mBluetoothGatt?.disconnect()
    }

    fun refreshCache(): Boolean {
        return mBluetoothGatt?.let {
            refreshDeviceCache(it)
        } ?: run {
            false
        }
    }

    private fun refreshDeviceCache(gatt: BluetoothGatt): Boolean {
        try {
            val localMethod = gatt.javaClass.getMethod("refresh", *arrayOfNulls(0))
            return (localMethod.invoke(gatt, *arrayOfNulls(0)) as Boolean)
        } catch (localException: Exception) {
            LogHelper.e(mLogger, TAG, "An exception occurred while refreshing device")
        }
        return false
    }

    private fun clearActionQueue() {
        retries = 0
        actionQueue.clear()
        actionNames.clear()
    }

    fun readRemoteRssi() {
        if (!actionNames.values.contains(ACTION_READ_REMOTE_RSSI)) {
            addAction({
                LogHelper.d(mLogger, TAG, "readRemoteRssi()")
                mBluetoothGatt?.readRemoteRssi() ?: run {
                    LogHelper.w(mLogger, TAG, "readRemoteRssi: mBluetoothGatt == null")
                }
            }, ACTION_READ_REMOTE_RSSI)
        }
    }
}