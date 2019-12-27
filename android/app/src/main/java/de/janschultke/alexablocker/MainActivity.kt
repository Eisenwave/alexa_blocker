package de.janschultke.alexablocker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
        Log.v("ping", "$address:$port exception", e)
        null
    }
}

fun pingSocket(socket: Socket, timeoutMillis: Int): Boolean {
    socket.soTimeout = timeoutMillis
    socket.use {
        val outStream = socket.getOutputStream()
        outStream.write('m'.toInt())

        val inStream = socket.getInputStream()
        val response = inStream.read()
        return response.toChar() == 'p'
    }
}

@ExperimentalUnsignedTypes
fun pingIpWithMarco(address: String): Boolean {
    val timeoutMillis = 1000
    val port = 11444

    val socket = connectToSocket(address, port, timeoutMillis)
    if (socket == null) {
        Log.println(Log.DEBUG, "ping", "$address:$port unreachable")
        return false
    }

    val result = pingSocket(socket, timeoutMillis)
    Log.println(
        Log.DEBUG,
        "ping",
        "$address:$port ${if (result) "pinged with success" else "pinged with failure"}"
    )
    return result
}

@ExperimentalUnsignedTypes
suspend fun establishConnection(): Ipv4? {
    val jobs = mutableListOf<Job>()
    val results = Array(256) { false }

    val resultIp = Ipv4(arrayOf(192u, 168u, 0u, 0u))
    for (i in 0..255) {
        val ip = resultIp.clone()
        ip.bytes[3] = i.toUByte()

        val job = GlobalScope.launch(Dispatchers.IO) { results[i] = pingIpWithMarco(ip.toString()) }
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

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initToggleButton()

        GlobalScope.launch {
            Log.println(Log.DEBUG, "ping", "establishing connection ...")
            val ip = establishConnection()
            if (ip == null) {
                Log.println(Log.ERROR, "ping", "could not establish connection")
            } else {
                Log.println(Log.ERROR, "ping", "established connection with $ip")
            }
        }
    }

    fun initToggleButton() {
        val toggle = findViewById<ToggleButton>(R.id.toggleButton)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                println("enabled")
            } else {
                println("disabled")
            }
        }
    }

}
