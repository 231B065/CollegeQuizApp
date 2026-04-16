package com.college.quizapp.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * BLE Beacon Advertiser — used by TEACHERS to broadcast a quiz session beacon.
 *
 * The teacher's phone acts as a BLE beacon, broadcasting a unique service UUID
 * derived from the quiz's bleSessionId. Students must detect this beacon to
 * start taking the exam.
 */
class BLEBeaconAdvertiser(private val context: Context) {

    companion object {
        private const val TAG = "BLEBeaconAdvertiser"
        // Base UUID namespace for our app — last 4 bytes will be overridden by session
        const val BASE_SERVICE_UUID = "0000FFE0-0000-1000-8000-00805F9B34FB"
    }

    private var advertiser: BluetoothLeAdvertiser? = null

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising

    private var currentCallback: AdvertiseCallback? = null

    /**
     * Generate a deterministic UUID from the quiz's BLE session ID.
     * This ensures both teacher (advertiser) and student (scanner) use the same UUID.
     */
    fun getServiceUUID(bleSessionId: String): UUID {
        return UUID.nameUUIDFromBytes(bleSessionId.toByteArray())
    }

    /**
     * Start advertising the BLE beacon for a quiz session.
     */
    fun startAdvertising(bleSessionId: String): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            return false
        }

        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(TAG, "BLE Advertising not supported on this device")
            return false
        }

        val serviceUuid = getServiceUUID(bleSessionId)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0) // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(serviceUuid))
            .build()

        currentCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "BLE Advertising started successfully for session: $bleSessionId")
                _isAdvertising.value = true
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "BLE Advertising failed with error code: $errorCode")
                _isAdvertising.value = false
            }
        }

        try {
            advertiser?.startAdvertising(settings, data, currentCallback)
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for BLE advertising", e)
            return false
        }
    }

    /**
     * Stop advertising the BLE beacon.
     */
    fun stopAdvertising() {
        try {
            currentCallback?.let { callback ->
                advertiser?.stopAdvertising(callback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when stopping advertising", e)
        }
        _isAdvertising.value = false
        currentCallback = null
        Log.d(TAG, "BLE Advertising stopped")
    }
}
