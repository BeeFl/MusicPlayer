package person.shilei.musicplayer.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import person.shilei.chat.ui.MusicAdapter
import person.shilei.musicplayer.model.Song

class MusicViewModel : ViewModel() {

    lateinit var adapter: MusicAdapter

    var playOrPause = true
    fun playOrPauseToggle(){
        playOrPause = !playOrPause
    }

    val permissionGranted = MutableLiveData<Boolean?>()
    fun onPermissionGranted(){
        permissionGranted.value = null
    }

    val musicsPrepared = MutableLiveData<Boolean?>()
    fun onMusicsPrepared(){
        musicsPrepared.value = null
    }

    fun musicItemClicked(){

    }

    fun submitMusics2Adapter(it: List<Song>, activity: FragmentActivity?) {
        adapter.musics = it.toMutableList()
        adapter.notifyDataSetChanged()
        (activity as AppCompatActivity).supportActionBar?.title = "本地音乐(共${it.size}首歌)"
    }
}