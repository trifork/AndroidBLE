package com.trifork.bluetoothle

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService

interface BLEGattManagerCallbacks {
    fun onDisconnected(device: BluetoothDevice, status: Int)
    fun onConnected(device: BluetoothDevice)
    fun onServicesDiscovered(services: List<BluetoothGattService>)
    fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic, data: ByteArray)
    fun onDescriptorWrite(descriptor: BluetoothGattDescriptor, status: Int)
    fun onBondStateChanged(state: Int, device: BluetoothDevice)
    fun onMtuChanged(mtu: Int)
    fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic, data: ByteArray)
    fun onCharacteristicWrite(characteristic: BluetoothGattCharacteristic, status: Int)
    fun onReadRemoteRssi(rssi: Int)
}