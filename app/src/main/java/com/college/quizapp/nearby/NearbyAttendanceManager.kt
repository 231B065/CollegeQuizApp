package com.college.quizapp.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class NearbyAttendanceManager(private val context: Context) {

    companion object {
        private const val TAG = "NearbyAttendance"
        private const val SERVICE_ID = "com.college.quizapp.attendance"
        private val STRATEGY = Strategy.P2P_STAR
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)

    // State for Teacher
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising

    // State for Student
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering
    
    private val _studentConnectionStatus = MutableStateFlow<String>("Not Connected")
    val studentConnectionStatus: StateFlow<String> = _studentConnectionStatus

    private val _studentMarkedPresent = MutableStateFlow(false)
    val studentMarkedPresent: StateFlow<Boolean> = _studentMarkedPresent

    // Called when teacher accepts student and receives data
    var onStudentConnectedAndDataReceived: ((studentId: String, studentName: String) -> Unit)? = null

    // TEACHER LOGIC
    fun startAdvertising(teacherName: String, sessionId: String) {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        // Provide sessionId embedded in the localized name so students know it's an attendance session
        val endpointName = "$teacherName|ATTENDANCE|$sessionId"

        connectionsClient.startAdvertising(
            endpointName, SERVICE_ID, teacherConnectionLifecycleCallback, advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Started Advertising")
            _isAdvertising.value = true
        }.addOnFailureListener { e: Exception ->
            Log.e(TAG, "Failed Advertising", e)
            _isAdvertising.value = false
        }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()
        _isAdvertising.value = false
    }

    private val teacherConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept incoming connection from students
            connectionsClient.acceptConnection(endpointId, teacherPayloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Student connected: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Student connection rejected")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "Student connection error")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Student disconnected: $endpointId")
        }
    }

    private val teacherPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val dataStr = payload.asBytes()?.let { String(it) } ?: return
                // Expecting JSON: {"studentId": "...", "studentName": "..."}
                try {
                    val json = JSONObject(dataStr)
                    val studentId = json.getString("studentId")
                    val studentName = json.getString("studentName")
                    
                    // Trigger callback to repository
                    onStudentConnectedAndDataReceived?.invoke(studentId, studentName)

                    // Send ACK back
                    val ackPayload = Payload.fromBytes("ACK".toByteArray())
                    connectionsClient.sendPayload(endpointId, ackPayload)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing student payload", e)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }


    // STUDENT LOGIC
    private var myStudentId = ""
    private var myStudentName = ""

    fun startDiscovering(studentId: String, studentName: String) {
        this.myStudentId = studentId
        this.myStudentName = studentName

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        
        _studentConnectionStatus.value = "Searching for teacher..."
        _studentMarkedPresent.value = false

        connectionsClient.startDiscovery(
            SERVICE_ID, endpointDiscoveryCallback, discoveryOptions
        ).addOnSuccessListener {
            _isDiscovering.value = true
            Log.d(TAG, "Started Discovering")
        }.addOnFailureListener { e: Exception ->
            Log.e(TAG, "Failed Discovering", e)
            _isDiscovering.value = false
            _studentConnectionStatus.value = "Failed to start discovery."
        }
    }

    fun stopDiscovering() {
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _isDiscovering.value = false
        _studentConnectionStatus.value = "Not Connected"
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // An endpoint was found
            if (info.endpointName.contains("ATTENDANCE")) {
                Log.d(TAG, "Teacher found! Requesting connection to: $endpointId")
                _studentConnectionStatus.value = "Teacher found, connecting..."
                // Initiate connection
                connectionsClient.requestConnection(
                    myStudentName,
                    endpointId,
                    studentConnectionLifecycleCallback
                )
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }

    private val studentConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, studentPayloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
             when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    _studentConnectionStatus.value = "Connected! Handshaking..."
                    // Send identity to teacher
                    val json = JSONObject()
                    json.put("studentId", myStudentId)
                    json.put("studentName", myStudentName)
                    val payload = Payload.fromBytes(json.toString().toByteArray())
                    connectionsClient.sendPayload(endpointId, payload)
                }
                else -> {
                    _studentConnectionStatus.value = "Failed to connect."
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (!_studentMarkedPresent.value) {
                _studentConnectionStatus.value = "Disconnected before marking."
            }
        }
    }

    private val studentPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
             if (payload.type == Payload.Type.BYTES) {
                val dataStr = payload.asBytes()?.let { String(it) } ?: return
                if (dataStr == "ACK") {
                    _studentMarkedPresent.value = true
                    _studentConnectionStatus.value = "You are marked present!"
                    Log.d(TAG, "Attendance marked successfully! Disconnecting.")
                    // Can optionally disconnect here to free up bandwidth
                    connectionsClient.disconnectFromEndpoint(endpointId)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}
