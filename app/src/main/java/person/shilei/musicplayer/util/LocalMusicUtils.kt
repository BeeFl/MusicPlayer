package person.shilei.musicplayer.util


import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import person.shilei.musicplayer.model.Song

import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


object LocalMusicUtils {
    //定义一个集合，存放从本地读取到的内容
    private lateinit var list: MutableList<Song>
    private var name: String? = null
    private var singer: String? = null
    private var uri: Uri? = null
    private var duration = 0
    private var size: Long = 0
    private var albumId: Long = 0
    private var id: Long = 0

    //获取专辑封面的Uri
    private val albumArtUri: Uri = Uri.parse("content://media/external/audio/albumart")

    private val albumArtString = "content://media/external/audio/albumart/"

    fun getMusic(context: Context): MutableList<Song> {
        list = mutableListOf()
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {

                name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))

                uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id)

                singer =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))

                duration =
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                albumId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                //list.add(song);

                //把歌曲名字和歌手切割开,如果文件大于800KB，则为音乐文件
                if (size > 1000 * 800) {
                    if (name!!.contains(" - ")) {
                        val str = name!!.split(" - ".toRegex()).toTypedArray()
                        if (str.size == 2){
                            singer = str[0].trim()
                            name = str[1].trim()
                        }
                    }
                    val song = Song(name,singer,size,duration,uri,albumId,id)
                    list.add(song)
                }

            }
        }
        cursor?.close()
        return list
    }

    //    转换歌曲时间的格式
    fun formatTime(time: Int): String {
        return if (time / 1000 % 60 < 10) {
            (time / 1000 / 60).toString() + ":0" + (time / 1000 % 60)
        } else {
            (time / 1000 / 60).toString() + ":" + (time / 1000 % 60)
        }
    }

    fun getArtQuick(album_id: Long): Uri {
        val artworkUri = Uri.parse("content://media/external/audio/albumart")
        return ContentUris.withAppendedId(artworkUri, album_id)
    }

}