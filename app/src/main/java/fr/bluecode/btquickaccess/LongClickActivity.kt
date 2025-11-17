/*
 * This activity only exists to open the bluetooth settings on tile long press.
 */

package fr.bluecode.btquickaccess

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity


class LongClickActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentOpenBluetoothSettings = Intent()
        intentOpenBluetoothSettings.action = Settings.ACTION_BLUETOOTH_SETTINGS
        startActivity(intentOpenBluetoothSettings)
        finish()
    }
}