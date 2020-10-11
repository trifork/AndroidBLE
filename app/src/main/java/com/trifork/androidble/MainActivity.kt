package com.trifork.androidble

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.trifork.bluetoothle.BLEManager
import com.trifork.bluetoothle.BLEManagerCallbacks
import com.trifork.bluetoothle.Logger

class MainActivity : AppCompatActivity() {
    lateinit var bleManager ;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bleManager = BLEManager(this, bleManagerCallbacks, object : Logger {
            override fun d(TAG: String?, message: String?) {
                Log.d(TAG, message)
            }

            override fun i(TAG: String?, message: String?) {
                Log.i(TAG, message)
            }

            override fun w(TAG: String?, message: String?) {
                Log.w(TAG, message)
            }

            override fun e(TAG: String?, message: String?) {
                Log.e(TAG, message)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        blem
    }


    fun requestLocationPermission(cb: () -> Unit) {
        val requestCode = incrementalRequestCode.generateRequestCode()
        requestCallbacks[requestCode] = cb
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
    }

    private val bleManagerCallbacks: BLEManagerCallbacks = object : BLEManagerCallbacks {
        override fun onDisconnected(device: BluetoothDevice, status: Int) {
            TODO("Not yet implemented")
        }

        override fun onConnected(device: BluetoothDevice) {
            TODO("Not yet implemented")
        }

        override fun onServicesDiscovered(services: List<BluetoothGattService?>) {
            TODO("Not yet implemented")
        }

        override fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
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

        override fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
            TODO("Not yet implemented")
        }

        override fun onCharacteristicWrite(characteristic: BluetoothGattCharacteristic, status: Int) {
            TODO("Not yet implemented")
        }

        override fun onReadRemoteRssi(rssi: Int) {
            TODO("Not yet implemented")
        }

    }
}
