package com.college.quizapp.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * BLE Beacon Scanner — used by STUDENTS to detect the teacher's quiz beacon.
 *
 * Continuously scans for the specific service UUID associated with a quiz session.
 * Reports proximity status and handles beacon loss detection (auto-submit trigger).
 */
class BLEBeaconScanner(private val context: Context) {

    companion object {
        private const val TAG = "BLEBeaconScanner"
        // Time in ms before declaring beacon lost
        const val BEACON_LOSS_TIMEOUT_MS = 15_000L
    }

    private var scanner: BluetoothLeScanner? = null

    private val _isInRange = MutableStateFlow(false)
    val isInRange: StateFlow<Boolean> = _isInRange

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var lastSeenTimestamp: Long = 0L
    private var currentCallback: ScanCallback? = null

    // Runnable to check for beacon timeout
    private val timeoutChecker = object : Runnable {
        override fun run() {
            if (_isScanning.value) {
                val elapsed = System.currentTimeMillis() - lastSeenTimestamp
                if (lastSeenTimestamp > 0 && elapsed > BEACON_LOSS_TIMEOUT_MS) {
                    Log.w(TAG, "Beacon lost! Last seen ${elapsed}ms ago")
                    _isInRange.value = false
                }
                android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(this, 5000L)
            }
        }
    }

    /**
     * Generate the same deterministic UUID as the advertiser.
     */
    fun getServiceUUID(bleSessionId: String): UUID {
        return UUID.nameUUIDFromBytes(bleSessionId.toByteArray())
    }

    /**
     * Start scanning for a specific quiz session beacon.
     */
    fun startScanning(bleSessionId: String): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            return false
        }

        scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BLE Scanner not available")
            return false
        }

        val serviceUuid = getServiceUUID(bleSessionId)

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        currentCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    lastSeenTimestamp = System.currentTimeMillis()
                    if (!_isInRange.value) {
                        Log.d(TAG, "Beacon found! RSSI: ${it.rssi}")
                        _isInRange.value = true
                    }
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { result ->
                    lastSeenTimestamp = System.currentTimeMillis()
                    if (!_isInRange.value) {
                        Log.d(TAG, "Beacon found (batch)! RSSI: ${result.rssi}")
                        _isInRange.value = true
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE Scan failed with error code: $errorCode")
                _isScanning.value = false
            }
        }

        try {
            scanner?.startScan(listOf(filter), settings, currentCallback)
            _isScanning.value = true
            lastSeenTimestamp = 0L

            // Start timeout checker
            android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(timeoutChecker, 5000L)

            Log.d(TAG, "BLE Scanning started for session: $bleSessionId")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for BLE scanning", e)
            return false
        }
    }

    /**
     * Stop scanning for beacons.
     */
    fun stopScanning() {
        try {
            currentCallback?.let { callback ->
                scanner?.stopScan(callback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when stopping scan", e)
        }
        _isScanning.value = false
        _isInRange.value = false
        currentCallback = null
        Log.d(TAG, "BLE Scanning stopped")
    }

    /**
     * Check if the beacon is currently considered in range.
     * Takes into account the timeout threshold.
     */
    fun isBeaconAlive(): Boolean {
        if (lastSeenTimestamp == 0L) return false
        val elapsed = System.currentTimeMillis() - lastSeenTimestamp
        return elapsed < BEACON_LOSS_TIMEOUT_MS
    }
}
