package com.example.usbchatapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants.USB_DIR_IN
import android.hardware.usb.UsbConstants.USB_DIR_OUT
import android.hardware.usb.UsbConstants.USB_TYPE_VENDOR
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.usbchatapp.ui.theme.USBChatAppTheme
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity(), SerialInputOutputManager.Listener {
    private lateinit var usbManager: UsbManager
    private lateinit var permissionIntent: PendingIntent
    private lateinit var device: UsbDevice

    private var serialPort: UsbSerialPort? = null
    private var connection: UsbDeviceConnection? = null

    private val usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d(this@MainActivity::class.simpleName, "attached usb")
                    Log.d(this@MainActivity::class.simpleName, "${usbManager.deviceList.values}")
                    device = usbManager.deviceList.values.first()
                    usbManager.requestPermission(device, permissionIntent)
                }

                ACTION_USB_PERMISSION -> {
                    Log.d(this@MainActivity::class.simpleName, "ACTION USB PERMISSION")
                    val result = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    Log.d(this@MainActivity::class.simpleName, "result: $result")
                    if (result) {
                        openSerialPort()
                        getProtocol()
                        sendManufacturerInfo()
                        requestSwitchMode()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_MUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION).apply {
            addAction(ACTION_USB_DEVICE_ATTACHED)
        }
        registerReceiver(usbReceiver, filter)

        setContent {
            USBChatAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    private fun openSerialPort() {
        val driver = CdcAcmSerialDriver(device)
        val connection =
            usbManager.openDevice(device) ?: throw IOException("not open connection for usb device")
        serialPort = driver.ports[0]
        serialPort?.run {
            open(connection)
//            val baudRate = 115200
//            setParameters(
//                baudRate,
//                UsbSerialPort.DATABITS_8,
//                UsbSerialPort.STOPBITS_1,
//                UsbSerialPort.PARITY_NONE
//            )
        }

        Log.d(this::class.simpleName, "serialPort Open")

        val ioManager = SerialInputOutputManager(serialPort, this)
        Executors.newSingleThreadExecutor().submit(ioManager)
    }

    private fun getProtocol() {
        val reqType = USB_DIR_IN or USB_TYPE_VENDOR
        val request = ACCESSORY_GET_PROTOCOL
        val value = 0
        val index = 0
        val data = byteArrayOf(-1)
        val bytes: ByteArray = byteArrayOf(
            reqType.toByte(),
            request.toByte(),
            value.toByte(),
            index.toByte(),
            -1,
            1
        )
        serialPort?.write(bytes, TIMEOUT)

        connection?.controlTransfer(
            reqType,
            request,
            value,
            index,
            data,
            data.size,
            TIMEOUT
        )

        Log.d(this::class.simpleName, "getProtocol")
    }

    private fun sendManufacturerInfo() {
        sendString(0, "test\u0000")
        sendString(1, "test\u0000")
        sendString(2, "description\u0000")
        sendString(3, "1.0.0\u0000")
        sendString(4, "https://example.com/\u0000")
        sendString(5, "0123456789\u0000")
    }

    private fun sendString(index: Int, string: String) {
        val reqType = USB_DIR_OUT or USB_TYPE_VENDOR
        val request = ACCESSORY_SEND_STRING
        val value = 0
        val data = string.toByteArray()

        var bytes: ByteArray = ByteBuffer.allocate(1 + 1 + 1 + data.size + 1)
//        serialPort?.write(bytes, TIMEOUT)

        connection?.controlTransfer(
            reqType,
            request,
            value,
            index,
            data,
            data.size,
            TIMEOUT
        )
    }

    private fun requestSwitchMode() {
        val reqType = USB_DIR_OUT or USB_TYPE_VENDOR
        val request = ACCESSORY_SEND_STRING
        val value = 0
        val index = 0
        val data = null

//        var bytes: ByteArray
//        serialPort?.write(bytes, TIMEOUT)

        connection?.controlTransfer(
            reqType,
            request,
            value,
            index,
            data,
            0,
            TIMEOUT
        )
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        private const val TIMEOUT = 0

        private const val USB_SETUP_HOST_TO_DEVICE = 0x00
        private const val USB_SETUP_DEVICE_TO_HOST = 0x80
        private const val USB_SETUP_TYPE_STANDARD = 0x00
        private const val USB_SETUP_TYPE_CLASS = 0x20
        private const val USB_SETUP_TYPE_VENDOR = 0x40
        private const val USB_SETUP_RECIPIENT_DEVICE = 0x00
        private const val USB_SETUP_RECIPIENT_INTERFACE = 0x01
        private const val USB_SETUP_RECIPIENT_ENDPOINT = 0x02
        private const val USB_SETUP_RECIPIENT_OTHER = 0x03

        private const val ACCESSORY_GET_PROTOCOL = 51
        private const val ACCESSORY_SEND_STRING = 52
        private const val ACCESSORY_START = 53
    }

    override fun onNewData(data: ByteArray?) {
        data?.let { Log.d(this::class.simpleName, data.toString()) }
    }

    override fun onRunError(e: Exception?) {
        e?.let { throw e }
    }
}
