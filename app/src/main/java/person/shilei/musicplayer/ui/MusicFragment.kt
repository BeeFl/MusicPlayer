package person.shilei.chat.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import person.shilei.musicplayer.R
import person.shilei.musicplayer.databinding.FragmentMusicBinding
import person.shilei.musicplayer.databinding.ListItemMusicBinding
import person.shilei.musicplayer.model.Song

import person.shilei.musicplayer.service.BackGroundMusicService
import person.shilei.musicplayer.util.LocalMusicUtils
import person.shilei.musicplayer.util.ServiceObserver
import timber.log.Timber
import kotlin.random.Random

class MusicFragment : Fragment() {

    val requestWriteAndReadPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ){
            var grantedCount = 0
            it.forEach { (permission, isGranted) ->
                if (isGranted) {
                    Timber.i("$permission is Granted")
                    grantedCount++
                } else {
                    Timber.i("$permission is not Granted")
                }
            }
            if (grantedCount == 3){
                Toast.makeText(requireContext(),"您的权限够了",Toast.LENGTH_SHORT).show()
                viewModel.permissionGranted.value = true
            }else{
                Toast.makeText(requireContext(),"您的权限不够",Toast.LENGTH_SHORT).show()
            }
        }

    private lateinit var binding : FragmentMusicBinding

    private val viewModel: MusicViewModel by lazy {
        ViewModelProvider(this).get(MusicViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkWriteAndReadPermission()){
            Timber.i("checkStoragePermission: true")
            ServiceObserver.musics = LocalMusicUtils.getMusic(requireContext())
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_music,container,false)

        viewModel.adapter = MusicAdapter(MusicListener {song, position ->
            ServiceObserver.currentIndex.value = position
            Toast.makeText(requireContext(),"${song.name} clicked",Toast.LENGTH_SHORT).show()
            Timber.i("song's path: ${song.path}")

            if (ServiceObserver.mediaPlayerCreated.value == true){
                ServiceObserver.currentUri.value = song.path
            }else{
                val intent = Intent(activity,BackGroundMusicService::class.java)
                intent.putExtra("music_uri",song.path.toString())
                requireActivity().startService(intent)
            }

        })

        viewModel.permissionGranted.observe(viewLifecycleOwner){granted ->
            if (granted == true){
                //用户点击了运行权限，这时候请求音乐数据
                ServiceObserver.musics = LocalMusicUtils.getMusic(requireContext())
                viewModel.musicsPrepared.value = true
                viewModel.onPermissionGranted()
            }
        }

        binding.musicRecyclerview.adapter = viewModel.adapter

        if (ServiceObserver.musics.isNullOrEmpty()){
            viewModel.musicsPrepared.observe(viewLifecycleOwner){
                if (it == true){
                    ServiceObserver.musics.let {
                        viewModel.submitMusics2Adapter(it!!,activity)
                    }
                }
            }
        }else{
            ServiceObserver.musics.let {
                viewModel.submitMusics2Adapter(it!!,activity)
            }
        }


        binding.play.setOnClickListener {
            if (ServiceObserver.isPlaying.value == true){
                Timber.i("想要暂停音乐")
                ServiceObserver.actionPause.value = true
            }else if (ServiceObserver.isPlaying.value == false){
                Timber.i("想要播放音乐")
                ServiceObserver.actionPause.value = false
            }else{
                if (ServiceObserver.currentFlowMode == 0){
                    ServiceObserver.currentIndex.value = 0
                }else{
                    ServiceObserver.currentIndex.value = ServiceObserver.musics?.size?.let { it1 ->
                        Random.nextInt(
                            it1
                        )
                    }
                }

                val intent = Intent(activity,BackGroundMusicService::class.java)
                intent.putExtra("music_uri", ServiceObserver.musics?.get(0)?.path.toString())
                requireActivity().startService(intent)
            }
        }

        binding.top.setOnClickListener {
            if (!ServiceObserver.musics.isNullOrEmpty()){
                binding.musicRecyclerview.scrollToPosition(0)
            }
        }

        binding.musicRecyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    // Scrolling down
                    binding.top.visibility = View.GONE
                } else {
                    // Scrolling up
                    binding.top.visibility = View.VISIBLE
                }
            }
        })

        binding.musicName.isSelected = true
        binding.singer.isSelected = true

        val animator = ObjectAnimator.ofFloat(binding.albumImage, View.ROTATION, -360f, 0f)
        animator.duration = 3000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()

        ServiceObserver.isPlaying.observe(viewLifecycleOwner){
            Timber.i("歌曲是否正在播放：$it")
            if (it == true){
                binding.play.setBackgroundResource(R.drawable.ic_pause)
//                animator.repeatMode = ValueAnimator.INFINITE
                animator.start()
            }else if(it == false){
                binding.play.setBackgroundResource(R.drawable.ic_play)
                animator.pause()
//                binding.albumImage.clearAnimation()
            }else{
                //隐藏进度条
                binding.seekbar.visibility = View.INVISIBLE
            }
        }

        ServiceObserver.currentIndex.observe(viewLifecycleOwner){
            if (it != null){
                viewModel.adapter.selectItem(it)
                binding.musicRecyclerview.scrollToPosition(it)
                (activity as AppCompatActivity).supportActionBar?.title = "本地音乐(第${it+1}/${ServiceObserver.musics?.size}首歌)"
                val curSong = ServiceObserver.musics?.get(it)
                val index = curSong?.name?.lastIndexOf('.')
                val musicName = index?.let { curSong.name.substring(0, it) }
                binding.musicName.text = musicName
                if (curSong != null) {
                    binding.singer.text = curSong.singer
                }
                if (curSong != null) {
                    Glide.with(requireContext())
                        .load(LocalMusicUtils.getArtQuick(curSong.albumId))
                        .placeholder(R.drawable.ic_bx_album)
                        .error(R.drawable.ic_bx_album)
                        .centerCrop()
                        .into(binding.albumImage)
                }
            }
        }

        ServiceObserver.currentPosition.observe(viewLifecycleOwner){
//            Timber.i("current position: $it")
            if (ServiceObserver.musicDuration != 0){
                val time = ServiceObserver.musicDuration
                val max = binding.seekbar.max
                binding.seekbar.progress = it * max / time
            }
        }

        ServiceObserver.mediaPlayerCreated.observe(viewLifecycleOwner){
            if (it == true){
                binding.seekbar.visibility = View.VISIBLE
            }else{
                binding.seekbar.visibility = View.INVISIBLE
            }
        }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                ServiceObserver.seekToPosition.value = seekBar?.progress
            }

        })

        binding.next.setOnClickListener {
            ServiceObserver.nextSong()
        }

        binding.prev.setOnClickListener {
            ServiceObserver.prevSong()
        }

        binding.spinner.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Timber.i("你点击了：$position")
                if (position == 1){
                    ServiceObserver.currentFlowMode = 1
                    ServiceObserver.isLooping.value = false
                }else if(position == 2){
                    ServiceObserver.currentFlowMode = 2
                    ServiceObserver.isLooping.value = true
                }else{
                    ServiceObserver.currentFlowMode = 0
                    ServiceObserver.isLooping.value = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        return binding.root
    }



//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        val currentUri = ServiceObserver.getCurrentUri(requireContext())
//        if (currentUri != null){
//            val intent = Intent(activity,BackGroundMusicService::class.java)
//            intent.putExtra("music_uri", currentUri.toString())
//            requireActivity().startService(intent)
//        }
//    }

    // checking camera permissions
    private fun checkWriteAndReadPermission(): Boolean {

        val result1 = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val result3 = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_MEDIA_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (result1 && result2 && result3){
            return true
        }else{
            requestWriteAndReadPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION)
            )
            return false
        }
    }

}

class MusicViewModel : ViewModel() {

    lateinit var adapter:MusicAdapter

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

class MusicAdapter(val clickListener: MusicListener):RecyclerView.Adapter<MusicAdapter.ViewHolder>(){

    var musics = mutableListOf<Song>()

    var currentIdx = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = musics[position]
        var selectStatus = false
        if (currentIdx == position){
            selectStatus = true
        }
        holder.bind(item, clickListener, position, selectStatus)
    }

    override fun getItemCount() = musics.size

    fun selectItem(position:Int){
        //记录上一首歌
        val lastPosition = currentIdx

        currentIdx = position

        if (lastPosition != -1){
            notifyItemChanged(lastPosition)
        }

        notifyItemChanged(position)
    }

    class ViewHolder private constructor(val binding: ListItemMusicBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item: Song, clickListener: MusicListener, position: Int, selectStatus: Boolean) {
            binding.song = item
            binding.position = position
            binding.musicListener = clickListener

            val name = item.name
            val index = name?.lastIndexOf('.')
            val musicName = index?.let { name.substring(0, it) }
            val extName = index?.let { name.substring(it+1, name.length) }
            binding.singer.text = item.singer
            binding.musicName.text = musicName
            binding.musicTime.text = LocalMusicUtils.formatTime(item.duration)
            binding.musicFormat.text = extName

            if (selectStatus){
                binding.background.setBackgroundResource(R.color.teal_200)
            }else{
                binding.background.setBackgroundResource(R.color.white)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemMusicBinding.inflate(layoutInflater,parent,false)
                return ViewHolder(binding)
            }
        }
    }
}

class MusicListener(val clickListener: (song:Song, position:Int) -> Unit) {
    fun onClick(song:Song, position:Int) = clickListener(song, position)
}