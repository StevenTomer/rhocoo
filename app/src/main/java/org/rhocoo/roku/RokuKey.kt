package org.rhocoo.roku

import androidx.annotation.DrawableRes
import org.rhocoo.R

/** Remote buttons, each mapped to its Roku External Control Protocol (ECP) key name. */
enum class RokuKey(val ecp: String, @param:DrawableRes val icon: Int, val label: String) {
    Up("Up", R.drawable.ic_key_up, "Up"),
    Down("Down", R.drawable.ic_key_down, "Down"),
    Left("Left", R.drawable.ic_key_left, "Left"),
    Right("Right", R.drawable.ic_key_right, "Right"),
    Select("Select", 0, "OK"),
    Back("Back", R.drawable.ic_key_back, "Back"),
    Home("Home", R.drawable.ic_key_home, "Home"),
    Play("Play", R.drawable.ic_key_play, "Play/Pause"),
    Rev("Rev", R.drawable.ic_key_rev, "Rewind"),
    Fwd("Fwd", R.drawable.ic_key_fwd, "Fast forward"),
    Replay("InstantReplay", R.drawable.ic_key_replay, "Instant replay"),
    VolumeUp("VolumeUp", R.drawable.ic_key_vol_up, "Volume up"),
    VolumeDown("VolumeDown", R.drawable.ic_key_vol_down, "Volume down"),
    Mute("VolumeMute", R.drawable.ic_key_mute, "Mute"),
    Power("Power", R.drawable.ic_key_power, "Power"),
}
