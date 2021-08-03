package person.shilei.musicplayer.ui

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import person.shilei.musicplayer.R
import person.shilei.musicplayer.adapter.SpinnerItemSelectedListener
import person.shilei.musicplayer.adapter.StopTrackingTouchListener
import person.shilei.musicplayer.databinding.FragmentMusicBinding
import person.shilei.musicplayer.databinding.ListItemMusicBinding
import person.shilei.musicplayer.model.Song
import person.shilei.musicplayer.service.BackGroundMusicService
import person.shilei.musicplayer.util.LocalMusicUtils
import person.shilei.musicplayer.util.ServiceObserver
import person.shilei.musicplayer.util.SortedMode
import timber.log.Timber
import kotlin.random.Random

class MusicFragment : Fragment() {

    private val requestWriteAndReadPermissionLauncher =
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
            if (grantedCount == 2){
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
        //检查读写权限，若有，则请求本地音乐列表数据
        if (checkWriteAndReadPermission()){
            Timber.i("checkStoragePermission: true")
            ServiceObserver.sortedMusics.value = LocalMusicUtils.getMusic(requireContext())
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_music,container,false)

        //给音乐列表定义适配器，并传入自定义的item点击监听器
        initMusicAdapter()

        //viewModel里面变量的观察者
        observeMethods()

        //observe the media player entity whether created
        observeMediaPlayerCreated()

        //观察是否正在播放音乐
        observeIsPlaying()

        //observe current song's index change for update ui
        observeCurrentIndexUpdate()

        //observe song's playing progress to update the seekbar
        observeCurrentPositionUpdate()

        //observe song's sort mode
        observeSortedModeUpdate()

        //observe sortedlist to update the recyclerview
        //observe the datasource - musics
        observeSortedMusics2UpdateRCView()

        return binding.root
    }

    private fun observeSortedMusics2UpdateRCView() {
        ServiceObserver.sortedMusics.observe(viewLifecycleOwner){
            viewModel.submitSortedMusics2Adapter(it)
        }
    }

    private fun observeSortedModeUpdate() {
        ServiceObserver.sortMode.observe(viewLifecycleOwner){
            ServiceObserver.updateSortedMusics(it)
        }
    }

    private fun observeMediaPlayerCreated() {
        ServiceObserver.mediaPlayerCreated.observe(viewLifecycleOwner){
            if (it == true){
                binding.seekbar.visibility = View.VISIBLE
            }else{
                binding.seekbar.visibility = View.INVISIBLE
            }
        }
    }

    private fun observeCurrentPositionUpdate() {
        ServiceObserver.currentPosition.observe(viewLifecycleOwner){
            if (ServiceObserver.musicDuration != 0){
                val time = ServiceObserver.musicDuration
                val max = binding.seekbar.max
                binding.seekbar.progress = it * max / time
            }
        }
    }

    private fun observeCurrentIndexUpdate() {
        ServiceObserver.currentIndex.observe(viewLifecycleOwner){ currentIndx ->
            if (currentIndx != null){
                //lock the wrong change
                if (!ServiceObserver.sortModeChangeBringCurrentIndexLock){
                    viewModel.adapter.selectItem(currentIndx)
                    binding.musicRecyclerview.scrollToPosition(currentIndx)
                    (activity as AppCompatActivity).supportActionBar?.title =
                        "本地音乐(第${currentIndx+1}/${ServiceObserver.sortedMusics.value?.size}首歌)"
                    val curSong = ServiceObserver.sortedMusics.value?.get(currentIndx)
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
                }else{
                    viewModel.adapter.selectItem(currentIndx)
                    binding.musicRecyclerview.scrollToPosition(currentIndx)
                    (activity as AppCompatActivity).supportActionBar?.title =
                        "本地音乐(第${currentIndx+1}/${ServiceObserver.sortedMusics.value?.size}首歌)"
                    //free the lock
                    ServiceObserver.freeLock()
                }
            }
        }
    }

    private fun observeIsPlaying() {
        val animator = ObjectAnimator.ofFloat(binding.albumImage, View.ROTATION, -360f, 0f)
        animator.duration = 3000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()

        ServiceObserver.isPlaying.observe(viewLifecycleOwner){
            Timber.i("歌曲是否正在播放：$it")
            when (it) {
                true -> {
                    binding.play.setBackgroundResource(R.drawable.ic_pause)
                    animator.start()
                }
                false -> {
                    binding.play.setBackgroundResource(R.drawable.ic_play)
                    animator.pause()
                }
                else -> {
                    //隐藏进度条
                    binding.seekbar.visibility = View.INVISIBLE
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //播放、暂停音乐
        playButtonClickListener()
        //向上按钮
        fabTopClickListener()
        //recognize scroll up or down
        recyclerviewScrollListener()
        //realize text marquee effect
        textviewMarquee()
        //change seekbar progress to control the song's progress
        seekbarChangeListener()
        //next song
        nextSongButtonClickListener()
        //next song
        prevSongButtonClickListener()
        //choose the play mode
        spinnerItemSelectedListener()
        //sort the music list
        sortButtonClickListener()
        //location current song
        locationBtnClickListener()
    }

    private fun locationBtnClickListener() {
        binding.location.setOnClickListener {
            ServiceObserver.currentIndex.value?.let {
                binding.musicRecyclerview.scrollToPosition(it)
            }
        }
    }

    private fun sortButtonClickListener() {
        binding.sort.setOnClickListener {
            findNavController().navigate(R.id.action_musicFragment_to_sortBottomSheetDialog)
        }
    }

    private fun spinnerItemSelectedListener() {
        binding.spinner.onItemSelectedListener = object : SpinnerItemSelectedListener(){
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Timber.i("你点击了：$position")
                when (position) {
                    1 -> {
                        ServiceObserver.currentFlowMode = 1
                        ServiceObserver.isLooping.value = false
                    }
                    2 -> {
                        ServiceObserver.currentFlowMode = 2
                        ServiceObserver.isLooping.value = true
                    }
                    else -> {
                        ServiceObserver.currentFlowMode = 0
                        ServiceObserver.isLooping.value = false
                    }
                }
            }
        }
    }

    private fun prevSongButtonClickListener() {
        binding.prev.setOnClickListener {
            ServiceObserver.prevSong()
        }
    }

    private fun nextSongButtonClickListener() {
        binding.next.setOnClickListener {
            ServiceObserver.nextSong()
        }
    }

    private fun seekbarChangeListener() {
        binding.seekbar.setOnSeekBarChangeListener(object : StopTrackingTouchListener() {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                ServiceObserver.seekToPosition.value = seekBar?.progress
            }
        })
    }

    private fun textviewMarquee() {
        binding.musicName.isSelected = true
        binding.singer.isSelected = true
    }

    private fun recyclerviewScrollListener() {
        binding.musicRecyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    // Scrolling down
                    binding.top.visibility = View.GONE
                    if (ServiceObserver.currentIndex.value != null){
                        binding.location.visibility = View.VISIBLE
                    }else{
                        binding.location.visibility = View.GONE
                    }

                } else if (dy < 0){
                    // Scrolling up
                    binding.top.visibility = View.VISIBLE
                    binding.location.visibility = View.GONE
                }
            }
        })
    }

    private fun fabTopClickListener() {
        binding.top.setOnClickListener {
            if (!ServiceObserver.sortedMusics.value.isNullOrEmpty()){
                binding.musicRecyclerview.scrollToPosition(0)
            }
        }
    }

    private fun playButtonClickListener() {
        binding.play.setOnClickListener {
            when (ServiceObserver.isPlaying.value) {
                true -> {
                    Timber.i("想要暂停音乐")
                    ServiceObserver.actionPause.value = true
                }
                false -> {
                    Timber.i("想要播放音乐")
                    ServiceObserver.actionPause.value = false
                }
                else -> {
                    if (ServiceObserver.currentFlowMode == 0){
                        //ordered play
                        ServiceObserver.currentIndex.value = 0
                    }else{
                        //random play
                        ServiceObserver.currentIndex.value = ServiceObserver.sortedMusics.value?.size?.let { it1 ->
                            Random.nextInt(
                                it1
                            )
                        }
                    }
                    val intent = Intent(activity,BackGroundMusicService::class.java)
                    intent.putExtra("music_uri", ServiceObserver.sortedMusics.value?.get(0)?.path.toString())
                    requireActivity().startService(intent)
                }
            }
        }
    }

    private fun observeMethods() {
        //观察用户是否给了读写权限
        observePemissionGranted()
    }

    private fun observePemissionGranted() {
        viewModel.permissionGranted.observe(viewLifecycleOwner){granted ->
            if (granted == true){
                //用户点击了运行权限，这时候请求音乐数据
                ServiceObserver.sortedMusics.value = LocalMusicUtils.getMusic(requireContext())
                viewModel.musicsPrepared.value = true
                viewModel.onPermissionGranted()
            }
        }
    }

    private fun initMusicAdapter() {
        viewModel.adapter = MusicAdapter(MusicListener { song, position ->
            //点击的item的position对应音乐列表的索引，索引改变会触发观察者模式
            ServiceObserver.currentIndex.value = position

    //            Toast.makeText(requireContext(),"${song.name} clicked",Toast.LENGTH_SHORT).show()
            Timber.i("song's path: ${song.path}")

            if (ServiceObserver.mediaPlayerCreated.value == true) {
                //如果mediaplayer实例已经被创造了，直接将被点击的item代表的Uri提交给currentUri
                ServiceObserver.currentUri.value = song.path
            } else {
                //如果mediaplayer实例还没被创造，说明前台服务还没有开始，遂启动服务
                val intent = Intent(activity, BackGroundMusicService::class.java)
                intent.putExtra("music_uri", song.path.toString())
                requireActivity().startService(intent)
            }
        })

        binding.musicRecyclerview.adapter = viewModel.adapter
    }


    private fun checkWriteAndReadPermission(): Boolean {

        val result1 = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (result1 && result2){
            return true
        }else{
            requestWriteAndReadPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
            return false
        }
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