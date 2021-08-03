package person.shilei.musicplayer.util

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import person.shilei.musicplayer.R
import person.shilei.musicplayer.model.Song

import java.util.*
import kotlin.random.Random

object ServiceObserver {
//    var musics:List<Song>? = null
    val sortedMusics = MutableLiveData<MutableList<Song>>()

    val sortMode = MutableLiveData(SortedMode.ADDED_DATE)
    //flag to avoid the wrong change with the currentIndex change
    var sortModeChangeBringCurrentIndexLock = false
    fun freeLock(){
        sortModeChangeBringCurrentIndexLock = false
    }

    var sortModeChangeBringCurrentIndexLockForService = false
    fun freeLockForService(){
        sortModeChangeBringCurrentIndexLockForService = false
    }

    //观察音乐是否在播放
    val isPlaying = MutableLiveData<Boolean>()
    var mediaPlayerCreated = MutableLiveData(false)
    val nextMusicEvent = MutableLiveData<Boolean>()
    //观察音乐进度
    var musicDuration = 0

    //歌曲进度
    val currentPosition = MutableLiveData<Int>()

    val actionPause = MutableLiveData<Boolean>()
    val currentUri = MutableLiveData<Uri?>()
    //当前歌曲索引
    val currentIndex = MutableLiveData<Int>()
    val seekToPosition = MutableLiveData<Int>()

    var currentFlowMode = 0
    var isLooping = MutableLiveData<Boolean>()

    fun updateSortedMusics(sm:SortedMode){
        val smlist = sortedMusics.value
        val currentSongId = currentIndex.value?.let { smlist?.get(it)?.id }

        when (sm) {
            SortedMode.ADDED_DATE_REVERSE -> {
                sortedMusics.value = smlist?.sortedBy {
                    it.addedDate
                }?.toMutableList()
            }
            SortedMode.ADDED_DATE -> sortedMusics.value = smlist?.sortedByDescending {
                it.addedDate
            }?.toMutableList()
            SortedMode.SINGER_NAME -> sortedMusics.value = smlist?.sortedBy {
                it.singer
            }?.toMutableList()
        }

        if (currentIndex.value != null){
            sortModeChangeBringCurrentIndexLock = true
            sortModeChangeBringCurrentIndexLockForService = true
            currentIndex.value = sortedMusics.value?.indexOf(
                sortedMusics.value?.find {
                    it.id == currentSongId
                }
            )
        }


    }

    fun nextSong(){
        if (currentFlowMode == 0 || currentFlowMode == 2){
            currentIndex.value = sortedMusics.value?.size?.let { currentIndex.value?.plus(1)?.rem(it) }
        }else if (currentFlowMode == 1){
            currentIndex.value = Random.nextInt(sortedMusics.value?.size!!)
        }
    }

    fun prevSong(){
        if (currentFlowMode == 0 || currentFlowMode == 2){
            if (currentIndex.value == 0){
                currentIndex.value = sortedMusics.value?.size!!
            }
            currentIndex.value = currentIndex.value?.minus(1)
        }else if (currentFlowMode == 1){
            currentIndex.value = Random.nextInt(sortedMusics.value?.size!!)
        }
    }

    fun getCurrentUri(context: Context?): Uri? {
        val activity = (context as AppCompatActivity)
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return null
        val tmp = sharedPref.getString(activity.getString(R.string.shared_pref_current_uri),"")
        if (tmp == ""){
            return null
        }else{
            return tmp?.toUri()
        }

    }

    fun setCurrentUri(context: Context?){
//        ServiceObserver.currentUri.value = currentUri
        val activity = (context as AppCompatActivity)
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        if (sharedPref != null) {
            with (sharedPref.edit()) {
                putString(activity.getString(R.string.shared_pref_current_uri), currentUri.value.toString())
                apply()
            }
        }
    }

}