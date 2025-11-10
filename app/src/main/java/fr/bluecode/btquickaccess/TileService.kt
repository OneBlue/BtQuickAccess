package fr.bluecode.btquickaccess

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import java.util.concurrent.TimeUnit


class TileService : TileService() {

    private var bluetoothChangeReceiver: BroadcastReceiver? = null

    override fun onStartListening() {
        super.onStartListening()

        assert(bluetoothChangeReceiver == null)

        bluetoothChangeReceiver = object: BroadcastReceiver()
        {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED)
                {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    Log.d("TitleService", "Received bluetooth state change. New state: $state")

                    updateForBluetoothState(state)
                }
                else
                {
                    Log.e("TileService", "Received unexpected intent: ${intent.action}")
                }
            }
        }

        registerReceiver(bluetoothChangeReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        Log.i("TileService", "Tile service listening")
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?
        updateForBluetoothState(manager?.adapter?.state)
    }

    override fun onStopListening() {
        if (bluetoothChangeReceiver != null)
        {
            unregisterReceiver(bluetoothChangeReceiver)
            bluetoothChangeReceiver = null
        }

        Log.i("TileService", "Tile service no longer listening")
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        if (qsTile.state == Tile.STATE_ACTIVE)
        {
            Log.i("TileService", "Disabling bluetooth")
            setBlueToothState("disable")
        }
        else if (qsTile.state == Tile.STATE_INACTIVE)
        {
            Log.i("TileService", "Enabling bluetooth")
            setBlueToothState("enable")
        }
        else
        {
            Log.e("TileService", "Received onClick() from an unexpected state: ${qsTile.state}")
        }
    }

    fun updateForBluetoothState(state: Int?)
    {
        val (newState, label) = when(state)
        {
            BluetoothAdapter.STATE_ON -> Pair(Tile.STATE_ACTIVE, "on")
            BluetoothAdapter.STATE_OFF -> Pair(Tile.STATE_INACTIVE, "off")
            BluetoothAdapter.STATE_TURNING_ON -> Pair(Tile.STATE_ACTIVE, "turning on")
            BluetoothAdapter.STATE_TURNING_OFF -> Pair(Tile.STATE_INACTIVE, "turning off")
            else -> Pair(Tile.STATE_UNAVAILABLE, "Unexpected state: $state")
        }

        qsTile.state = newState
        qsTile.subtitle = label

        qsTile.updateTile()
    }

    fun setBlueToothState(state: String)
    {
        assert(state == "enable" || state == "disable")

        qsTile.state = Tile.STATE_UNAVAILABLE
        qsTile.updateTile()

        val cmd = listOf("su", "-c", "cmd bluetooth_manager $state")
        val builder = ProcessBuilder(cmd).redirectInput(
            ProcessBuilder.Redirect.PIPE).redirectInput(ProcessBuilder.Redirect.PIPE)

        try {
            val process = builder.start()

            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                Log.e("TileService", "Command: $cmd timed out")
                return
            }

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.inputStream.bufferedReader().readText()

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                Log.e(
                    "TileService",
                    "Command: $cmd, exited with code $exitCode. Stdout: '$stdout', Stderr: '$stderr'"
                )
            }
        } catch (e: Exception)
        {
            qsTile.subtitle = "Caught: ${e.javaClass.name}"
            qsTile.updateTile()
        }

        // Command ran successfully, tile state should be updated via 'bluetoothChangeReceiver'
    }
}