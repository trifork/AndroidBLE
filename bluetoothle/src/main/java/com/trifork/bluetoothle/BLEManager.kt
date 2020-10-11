package com.trifork.bluetoothle

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import java.util.*

class BLEManager @JvmOverloads constructor(context: Context, listener: BLEManagerCallbacks? = null, val mLogger: Logger? = null) : BLEGattManagerCallbacks {
    private val TAG = "BLEManager"
    private var bleManagerScanCallbacks: BLEManagerScanCallbacks? = null
    private val mListeners: MutableList<BLEManagerCallbacks> = ArrayList()
    private val mBluetoothAdapter: BluetoothAdapter?
    private val mBLEGattManager: BLEGattManager?
    private val mainThread = Handler(Looper.getMainLooper())
    private var scanning = false
    private val mScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (scanning) {
                LogHelper.d(mLogger, TAG, "onScanResult() called with: callbackType = [" + callbackType + "], result = [" + result.device.address + "]")
                mainThread.post {
                    if (bleManagerScanCallbacks != null) {
                        bleManagerScanCallbacks!!.onScanResult(result)
                    }
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            mainThread.post {
                for (result in results) {
                    if (bleManagerScanCallbacks != null) {
                        bleManagerScanCallbacks!!.onScanResult(result)
                    }
                    LogHelper.i(mLogger, TAG, result.toString())
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            if (bleManagerScanCallbacks != null) {
                bleManagerScanCallbacks!!.onScanFailed(errorCode)
            }
            when (errorCode) {
                1 -> LogHelper.e(mLogger, TAG, "Scan failed: SCAN_FAILED_ALREADY_STARTED")
                2 -> LogHelper.e(mLogger, TAG, "Scan failed: SCAN_FAILED_APPLICATION_REGISTRATION_FAILED")
                3 -> LogHelper.e(mLogger, TAG, "Scan failed: SCAN_FAILED_INTERNAL_ERROR")
                4 -> LogHelper.e(mLogger, TAG, "Scan failed: SCAN_FAILED_FEATURE_UNSUPPORTED")
                5 -> LogHelper.e(mLogger, TAG, "Scan failed: SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES")
                else -> LogHelper.e(mLogger, TAG, "Scan failed: $errorCode")
            }
        }
    }

    @Synchronized
    fun setScanListener(bleManagerScanCallbacks: BLEManagerScanCallbacks?) {
        LogHelper.d(mLogger, TAG, "Added listener")
        this.bleManagerScanCallbacks = bleManagerScanCallbacks
    }

    @Synchronized
    fun addListener(listener: BLEManagerCallbacks?) {
        LogHelper.d(mLogger, TAG, "Added listener")
        if (listener != null) {
            mainThread.post { mListeners.add(listener) }
        }
    }

    @Synchronized
    fun removeListener(listener: BLEManagerCallbacks?) {
        LogHelper.d(mLogger, TAG, "Removed listener")
        if (listener != null) {
            mainThread.post { mListeners.remove(listener) }
        }
    }

    fun getBondedDevice(macAddress: String): BluetoothDevice? {
        LogHelper.d(mLogger, TAG, "getBondedDevice() called with: macAddress = [$macAddress]")
        if (mBluetoothAdapter != null) {
            for (device in mBluetoothAdapter.bondedDevices) {
                if (device.address == macAddress) {
                    LogHelper.d(mLogger, TAG, "getBondedDevice() found device with with: macAddress = [$macAddress]")
                    return device
                }
            }
        }
        LogHelper.w(mLogger, TAG, "getBondedDevice: mBluetoothAdapter == null")
        return null
    }

    fun startScan(filters: List<ScanFilter>) {
        if (scanning) {
            LogHelper.w(mLogger, TAG, "startScan: cancel, already scanning")
            return
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.startDiscovery()
            if (mBluetoothAdapter.bluetoothLeScanner != null) {
                scanning = true
                LogHelper.d(mLogger, TAG, "startScan() called with: filters = [$filters]")
                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                mainThread.post { mBluetoothAdapter.bluetoothLeScanner.startScan(filters, settings, mScanCallback) }
            } else {
                LogHelper.w(mLogger, TAG, "startScan: BluetoothLeScanner == null")
            }
        } else {
            LogHelper.w(mLogger, TAG, "startScan: mBluetoothAdapter == null")
        }
    }

    @JvmOverloads
    fun startScan(serviceUuid: UUID, macAddress: String? = null) {
        val filters: MutableList<ScanFilter> = ArrayList()
        filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serviceUuid.toString())).build())
        if (macAddress != null) {
            filters.add(ScanFilter.Builder().setDeviceAddress(macAddress).build())
        }
        startScan(filters)
    }

    fun readRemoteRssi() {
        if (mBLEGattManager != null) {
            LogHelper.d(mLogger, TAG, "readRemoteRssi()")
            mBLEGattManager.readRemoteRssi()
        } else {
            LogHelper.w(mLogger, TAG, "writeCharacteristic: mBLEGattManager == null")
        }
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        if (mBLEGattManager != null) {
            LogHelper.d(mLogger, TAG, "writeCharacteristic() called with: characteristic = [$characteristic], data = [$data]")
            mBLEGattManager.writeCharacteristic(characteristic, data)
        } else {
            LogHelper.w(mLogger, TAG, "writeCharacteristic: mBLEGattManager == null")
        }
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic?, enabled: Boolean) {
        if (mBLEGattManager != null) {
            mBLEGattManager.setCharacteristicNotification(characteristic, enabled)
        } else {
            LogHelper.w(mLogger, TAG, "setCharacteristicNotification: mBLEGattManager == null")
        }
    }

    fun stopScan() {
        scanning = false
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery()
            if (mBluetoothAdapter.bluetoothLeScanner != null) {
                mBluetoothAdapter.bluetoothLeScanner.stopScan(mScanCallback)
            } else {
                LogHelper.w(mLogger, TAG, "stopScan: getBluetoothLeScanner == null")
            }
        } else {
            LogHelper.w(mLogger, TAG, "stopScan: mBluetoothAdapter == null")
        }
    }

    fun connect(bluetoothDevice: BluetoothDevice) {
        mBluetoothAdapter?.cancelDiscovery()
        LogHelper.d(mLogger, TAG, "connect() called with: bluetoothDevice = [" + bluetoothDevice.address + "]")
        mainThread.postDelayed({ mBLEGattManager!!.connect(bluetoothDevice) }, 500)
    }

    fun discoverServices() {
        LogHelper.d(mLogger, TAG, "discoverServices() called")
        if (mBLEGattManager != null) {
            mBLEGattManager.discoverServices()
        } else {
            LogHelper.w(mLogger, TAG, "discoverServices: mBLEGattManager == null")
        }
    }

    fun createBond(device: BluetoothDevice) {
        LogHelper.d(mLogger, TAG, "createBond() called with: device = [" + device.address + "]")
        if (mBLEGattManager != null) {
            mBLEGattManager.createBond(device)
        } else {
            LogHelper.w(mLogger, TAG, "createBond: mBLEGattManager == null")
        }
    }

    fun removeBond(device: BluetoothDevice) {
        if (mBluetoothAdapter != null) {
            LogHelper.d(mLogger, TAG, "removeBond() called with: device = [" + device.address + "]")
            for (bt in mBluetoothAdapter.bondedDevices) {
                if (bt.address.contains(device.address)) {
                    unpairDevice(bt)
                }
            }
        } else {
            LogHelper.w(mLogger, TAG, "removeBond: mBluetoothAdapter == null")
        }
    }

    private fun unpairDevice(device: BluetoothDevice) {
        mainThread.post {
            try {
                val m = device.javaClass.getMethod("removeBond", *(null as Array<Class<*>?>))
                m.invoke(device, *null as Array<Any?>)
            } catch (e: Exception) {
                LogHelper.e(mLogger, TAG, e.message)
            }
        }
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        LogHelper.d(mLogger, TAG, "readCharacteristic() called with: characteristic = [" + characteristic.uuid + "]")
        if (mBLEGattManager != null) {
            mBLEGattManager.readCharacteristic(characteristic)
        } else {
            LogHelper.w(mLogger, TAG, "readCharacteristic: mBLEGattManager == null")
        }
    }

    @Synchronized
    override fun onDisconnected(device: BluetoothDevice, status: Int) {
        LogHelper.d(mLogger, TAG, "onDisconnected() called with: device = [" + device.address + "], status = [" + status + "]")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onDisconnected(device, status)
            }
        }
    }

    @Synchronized
    override fun onConnected(device: BluetoothDevice) {
        LogHelper.d(mLogger, TAG, "onConnected() called with: device = [" + device.address + "]")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onConnected(device)
            }
        }
    }

    @Synchronized
    override fun onServicesDiscovered(services: List<BluetoothGattService?>) {
        LogHelper.d(mLogger, TAG, "onServicesDiscovered() called with: services = [" + services.size + "]")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onServicesDiscovered(services)
            }
        }
    }

    @Synchronized
    override fun onMtuChanged(mtu: Int) {
        LogHelper.d(mLogger, TAG, "onMtuChanged() called")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onMtuChanged(mtu)
            }
        }
    }

    @Synchronized
    override fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        LogHelper.d(mLogger, TAG, "onCharacteristicChanged() called with: characteristic = [" + characteristic.uuid + "], data = [" + data + "]")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onCharacteristicChanged(characteristic, data)
            }
        }
    }

    @Synchronized
    override fun onCharacteristicWrite(characteristic: BluetoothGattCharacteristic, status: Int) {
        LogHelper.d(mLogger, TAG, "onCharacteristicWrite() called with: characteristic = [" + characteristic.uuid + "], status = [" + status + "]")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onCharacteristicWrite(characteristic, status)
            }
        }
    }

    override fun onReadRemoteRssi(rssi: Int) {
        LogHelper.d(mLogger, TAG, "onReadRemoteRssi() called with: rssi=$rssi")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onReadRemoteRssi(rssi)
            }
        }
    }

    @Synchronized
    override fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        LogHelper.d(mLogger, TAG, "onCharacteristicRead() called with: characteristic = [" + characteristic.uuid + "], data = [" + data + "]")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onCharacteristicRead(characteristic, data)
            }
        }
    }

    @Synchronized
    override fun onDescriptorWrite(descriptor: BluetoothGattDescriptor, status: Int) {
        LogHelper.d(mLogger, TAG, "onDescriptorWrite() called with: bluetoothGattDescriptor = [" + descriptor.uuid + "], status = [" + status + "]")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onDescriptorWrite(descriptor, status)
            }
        }
    }

    @Synchronized
    override fun onBondStateChanged(state: Int, device: BluetoothDevice) {
        LogHelper.d(mLogger, TAG, "onBondStateChanged() called with: state = [" + state + "], device = [" + device.address + "]")
        mainThread.post {
            val it: Iterator<BLEManagerCallbacks> = mListeners.iterator()
            while (it.hasNext()) {
                val mListener = it.next()
                mListener.onBondStateChanged(state, device)
            }
        }
    }

    fun changeMtu(mtu: Int) {
        LogHelper.d(mLogger, TAG, "changeMtu() called with: mtu = [$mtu]")
        if (mBLEGattManager != null) {
            mBLEGattManager.changeMtu(mtu)
        } else {
            LogHelper.w(mLogger, TAG, "changeMtu: mBLEGattManager == null")
        }
    }

    fun refreshCache() {
        mBLEGattManager?.refreshCache()
    }

    fun disconnect() {
        LogHelper.d(mLogger, TAG, "disconnect() called")
        if (mBLEGattManager != null) {
            mBLEGattManager.disconnect()
        } else {
            LogHelper.w(mLogger, TAG, "disconnect: mBLEGattManager == null")
        }
    }

    init {
        if (listener != null) {
            mainThread.post { mListeners.add(listener) }
        }
        val bluetoothManager = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
        mBluetoothAdapter = bluetoothManager.adapter
        mBLEGattManager = BLEGattManager(this, context, mLogger)
    }
}