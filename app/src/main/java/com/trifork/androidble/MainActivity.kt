package com.trifork.androidble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.trifork.bluetoothle.BLEManager
import com.trifork.bluetoothle.BLEManagerCallbacks
import com.trifork.bluetoothle.BLEManagerScanCallbacks
import com.trifork.bluetoothle.Logger

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var bleManager: BLEManager
    private lateinit var scanResultAdapter: ScanResultAdapter

    private val deviceMap = mutableMapOf<String, ScanResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = DefaultItemAnimator()
        scanResultAdapter = ScanResultAdapter()
        recyclerView.adapter = scanResultAdapter
        bleManager = BLEManager(this, bleManagerCallbacks, object : Logger {
            override fun d(TAG: String, message: String) {
                Log.d(TAG, message)
            }

            override fun i(TAG: String, message: String) {
                Log.i(TAG, message)
            }

            override fun w(TAG: String, message: String) {
                Log.w(TAG, message)
            }

            override fun e(TAG: String, message: String) {
                Log.e(TAG, message)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                42
            )
            return
        }
        Log.d(TAG, "Starting scan")
        bleManager.setScanListener(object : BLEManagerScanCallbacks {
            override fun onScanResult(sr: ScanResult) {
                if (!deviceMap.containsKey(sr.device.address)) {
                    deviceMap[sr.device.address] = sr
                    updateListView()
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with errorCode:$errorCode")
            }

        })
        deviceMap.clear()
        bleManager.startScan()
    }

    private fun updateListView() {
        val devices = deviceMap.values.sortedBy { it.device.name }.toMutableList()
        val items = devices.map {
            ScanResultItem(
                it.device.name,
                it.device.address,
                it.scanRecord?.serviceUuids
            )
        }.toMutableList()
        scanResultAdapter.setItems(items)
    }

    private val bleManagerCallbacks: BLEManagerCallbacks = object : BLEManagerCallbacks {
        override fun onDisconnected(device: BluetoothDevice, status: Int) {
            TODO("Not yet implemented")
        }

        override fun onConnected(device: BluetoothDevice) {
            TODO("Not yet implemented")
        }

        override fun onServicesDiscovered(services: List<BluetoothGattService>) {
            TODO("Not yet implemented")
        }

        override fun onCharacteristicRead(
            characteristic: BluetoothGattCharacteristic,
            data: ByteArray
        ) {
            TODO("Not yet implemented")
        }

        override fun onDescriptorWrite(descriptor: BluetoothGattDescriptor, status: Int) {
            TODO("Not yet implemented")
        }

        override fun onBondStateChanged(state: Int, device: BluetoothDevice) {
            TODO("Not yet implemented")
        }

        override fun onMtuChanged(mtu: Int) {
            TODO("Not yet implemented")
        }

        override fun onCharacteristicChanged(
            characteristic: BluetoothGattCharacteristic,
            data: ByteArray
        ) {
            TODO("Not yet implemented")
        }

        override fun onCharacteristicWrite(
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            TODO("Not yet implemented")
        }

        override fun onReadRemoteRssi(rssi: Int) {
            TODO("Not yet implemented")
        }

    }
}
