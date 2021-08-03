package person.shilei.musicplayer.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import person.shilei.musicplayer.model.Song

object LocalMusicUtils {

    /**
     * 从MediaStore里获取本地音乐的信息，包括Uri路径等
     *
     * @param context
     * @return 本地音乐列表
     */
    fun getMusic(context: Context): MutableList<Song> {
        val list = mutableListOf<Song>()
        //从本地数据库中获取外部存储的音乐文件信息，并已添加日期倒序排列
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {
                var name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))

                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id)

                var singer =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))

                var duration = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    duration =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                }

                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                val albumId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))

                val addedDate =
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))

                //把歌曲名字和歌手切割开,如果文件大于800KB，则为音乐文件
                if (size > 1000 * 800) {
                    //将音乐文件名分割为歌曲名和歌手名
                    val pair = splitNameSinger(name, singer)
                    name = pair.first
                    singer = pair.second
//                    Timber.i("$name : $singer")
                    val song = Song(name,singer,size,duration,uri,albumId,id,addedDate)
                    list.add(song)
                }

            }
        }
        cursor?.close()
        return list
    }

    /**
     * 例："周杰伦 - 七里香"
     * 分离歌手和歌名
     *
     * @param name
     * @param singer
     * @return 返回分割后的歌手和歌名
     */
    fun splitNameSinger(
        name: String?,
        singer: String?
    ): Pair<String?, String?> {
        var name1 = name
        var singer1 = singer
        if (name1!!.contains(" - ")) {
            val str = name1.split(" - ".toRegex()).toTypedArray()
            if (str.size == 2) {
                if (singer1 == "<unknown>"){
                    singer1 = str[0].trim()
                }
                name1 = str[1].trim()
            }
        }
        return Pair(name1, singer1)
    }

    /**
     * 转换歌曲时间的格式
     *
     * @param time 单位为毫秒
     * @return 返回格式为‘x:xx’格式的时间字符串
     */
    fun formatTime(time: Int): String {
        return if (time / 1000 % 60 < 10) {
            (time / 1000 / 60).toString() + ":0" + (time / 1000 % 60)
        } else {
            (time / 1000 / 60).toString() + ":" + (time / 1000 % 60)
        }
    }

    /**
     * 获取歌曲的专辑图片
     * 大部分mp3歌曲是没有专辑图片的，flac格式大概率会有
     *
     * @param album_id 歌曲的专辑id
     * @return 专辑图片的Uri地址
     */
    fun getArtQuick(album_id: Long): Uri {
        val artworkUri = Uri.parse("content://media/external/audio/albumart")
        return ContentUris.withAppendedId(artworkUri, album_id)
    }

}