package de.janschultke.alexablocker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.ToggleButton
import kotlinx.coroutines.*
import java.lang.Exception
import java.net.*
import java.util.*


@ExperimentalUnsignedTypes
class Ipv4(var bytes: Array<UByte>) : Cloneable {

    init {
        require(bytes.size == 4) { "IP must consist of 4 bytes" }
    }

    override fun equals(other: Any?): Boolean {
        return javaClass == other?.javaClass && bytes.contentEquals((other as Ipv4).bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for (i in bytes.indices) {
            builder.append(bytes[i])
            if (i + 1 != bytes.size) {
                builder.append('.')
            }
        }
        return builder.toString()
    }

    public override fun clone(): Ipv4 {
        return Ipv4(bytes.clone())
    }

}

@ExperimentalUnsignedTypes
@Throws(IllegalFormatException::class)
fun parseIpv4(str: String): Ipv4 {
    val result = Array<UByte>(4) { 0u }

    val splits = str.split('.')
    require(splits.size == 4) { "IP must have exactly 3 '.' separators" }

    for (i in 0..3) {
        result[i] = splits[i].toUByte()
    }

    return Ipv4(result)
}

@ExperimentalUnsignedTypes
fun hostIpv4(): Ipv4 {
    val address = Inet4Address.getLocalHost()
    return parseIpv4(address.hostAddress)
}

fun connectToSocket(address: String, port: Int, timeoutMillis: Int): Socket? {
    val socket = Socket()

    return try {
        socket.connect(InetSocketAddress(address, port), timeoutMillis)
        socket
    } catch (e: Exception) {
        //Log.v("ping", "$address:$port exception", e)
        null
    }
}

fun pingSocket(socket: Socket, timeoutMillis: Int): Boolean {
    socket.soTimeout = timeoutMillis
    val outStream = socket.getOutputStream()
    outStream.write('m'.toInt())

    val inStream = socket.getInputStream()
    val response = inStream.read()
    return response.toChar() == 'p'
}

@ExperimentalUnsignedTypes
fun pingIpWithMarco(address: String): Boolean {
    val timeoutMillis = 1000
    val port = 11444

    val socket = connectToSocket(address, port, timeoutMillis)
    socket.use {
        if (socket == null) {
            Log.d("ping", "$address:$port unreachable")
            return false
        }

        val result = pingSocket(socket, timeoutMillis)
        Log.d(
            "ping",
            "$address:$port ${if (result) "pinged with success" else "pinged with failure"}"
        )
        return result
    }
}

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    private var ip: Ipv4? = null

    private lateinit var toggleButton: ToggleButton
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findWidgets()
        initWidgets()

        Log.i("ping", "finding server in local network ...")

        GlobalScope.launch {
            ip = findServerInLocalNetwork()
            GlobalScope.launch(Dispatchers.Main) {
                if (ip == null) {
                    Log.e("ping", "could not establish connection")
                    onNoServerFound()
                } else {
                    Log.i("ping", "established connection with $ip")
                    onServerFound()
                }
            }

        }
    }

    private fun findWidgets() {
        toggleButton = findViewById(R.id.toggleButton)
        statusTextView = findViewById(R.id.statusTextView)
    }

    private fun initWidgets() {
        statusTextView.text = getString(R.string.status_looking)
        initToggleButton()
    }

    private fun onNoServerFound() {
        statusTextView.text = getString(R.string.status_no_server_found)
    }

    private fun onConnectionEstablishingFailed() {
        statusTextView.text = getString(R.string.status_establishing_failed)
    }

    private fun onServerFound() {
        val timeoutMillis = 1000

        statusTextView.text = getString(R.string.status_establishing_connection)

        GlobalScope.launch {
            Log.i("ping", "pinging $ip")

            val socket = connectToSocket(ip.toString(), 11444, timeoutMillis)

            if (socket == null || !pingSocket(socket, timeoutMillis)) {
                GlobalScope.launch(Dispatchers.Main) {
                    onConnectionEstablishingFailed()
                }
                return@launch
            }

            Log.i("ping", "connection with $ip established")
            GlobalScope.launch(Dispatchers.Main) {
                onConnect()
            }
        }
    }

    private fun onConnect() {
        toggleButton.isEnabled = true
        statusTextView.text = getString(R.string.status_connected)
    }

    @ExperimentalUnsignedTypes
    private suspend fun findServerInLocalNetwork(): Ipv4? {
        val jobs = mutableListOf<Job>()
        val results = Array(256) { false }

        val resultIp = Ipv4(arrayOf(192u, 168u, 0u, 0u))
        for (i in 0..255) {
            val ip = resultIp.clone()
            ip.bytes[3] = i.toUByte()

            val job = GlobalScope.launch(Dispatchers.IO) {
                results[i] = pingIpWithMarco(ip.toString())
            }
            jobs.add(job)
        }

        for (job in jobs) {
            job.join()
        }

        val firstSuccess = results.indexOfFirst { it }
        if (firstSuccess == -1) {
            return null
        }

        resultIp.bytes[3] = firstSuccess.toUByte()
        return resultIp
    }

    private fun sendBlockingCommand(isBlocked: Boolean): Int {
        val socket = connectToSocket(ip.toString(), 11444, 1000) ?: return -2

        socket.use {
            if (isBlocked) {
                Log.i("block", "sending 'a' to $ip")
                socket.getOutputStream().write('a'.toInt())
            } else {
                Log.i("block", "sending 'u' to $ip")
                socket.getOutputStream().write('u'.toInt())
            }

            return socket.getInputStream().read()
        }

    }

    private fun onConnectionLost() {
        statusTextView.text = getString(R.string.status_connection_lost)
        toggleButton.isEnabled = false
    }

    private fun onCommandResult(success: Boolean) {
        statusTextView.text = if (success) {
            getString(R.string.status_command_processed)
        } else {
            getString(R.string.command_failed)
        }
    }

    private fun initToggleButton() {
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            statusTextView.text = getString(R.string.status_waiting_for_response)

            GlobalScope.launch(Dispatchers.IO) {
                var connectSuccess = true
                var success = false

                when (val responseByte = sendBlockingCommand(isChecked)) {
                    -2 -> {
                        Log.w("block", "server unreachable")
                        connectSuccess = false
                    }

                    -1 -> {
                        Log.w("block", "empty response")
                    }

                    'g'.toInt() -> {
                        success = true
                        Log.i("block", "OK")
                    }

                    'f'.toInt() -> {
                        Log.w("block", "command failed")
                    }

                    'i'.toInt() -> {
                        Log.w("block", "command incorrect")
                    }

                    else -> {
                        Log.i("block", "unknown response: $responseByte")
                    }
                }

                GlobalScope.launch(Dispatchers.Main) {
                    if (!connectSuccess) {
                        onConnectionLost()
                    }
                    else {
                        onCommandResult(success)
                    }
                }
            }
        }
    }

}
