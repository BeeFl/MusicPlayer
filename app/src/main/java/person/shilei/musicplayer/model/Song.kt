package person.shilei.musicplayer.model

import android.net.Uri

data class Song(
    val name:String?,
    val singer:String?,
    val size:Long,
    val duration:Int,
    val path: Uri?,
    val albumId:Long,
    val id:Long
)
