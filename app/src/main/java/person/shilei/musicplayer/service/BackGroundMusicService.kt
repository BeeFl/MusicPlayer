package person.shilei.musicplayer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import person.shilei.musicplayer.MainActivity
import person.shilei.musicplayer.R
import person.shilei.musicplayer.util.ServiceObserver
import timber.log.Timber
import java.util.*


class BackGroundMusicService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, LifecycleOwner{


    private val mLifecycleRegistry = LifecycleRegistry(this)

    var mediaPlayer: MediaPlayer? = null
    var audioManager: AudioManager? = null
    var volume = 0
     var timer: Timer? = null

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        initDataObserver()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val musicUri = intent?.getStringExtra("music_uri")?.toUri()
        Timber.i("service：开始播放${musicUri}")
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes
                    .Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                setWakeMode(applicationContext,PowerManager.PARTIAL_WAKE_LOCK)
                if (musicUri != null) {
                    setDataSource(applicationContext,musicUri)
                }
                setOnPreparedListener(this@BackGroundMusicService)

                prepareAsync()
            }
            ServiceObserver.mediaPlayerCreated.value = true

        }catch (e:Exception){
            Timber.i(e)
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val CHANNEL_ID = "person.shilei.chat.playMusic"
        val CHANNEL_NAME = "playMusic"
        var notificationChannel: NotificationChannel? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }


        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("播放音乐")
            .setContentText("目前正在播放的音乐")
            .setSmallIcon(R.drawable.ic_bx_album)
            .setContentIntent(pendingIntent)
            .setTicker("ticker")
            .build()
        startForeground(1,notification)
        return START_NOT_STICKY
//        return super.onStartCommand(intent, flags, startId)
    }

    private fun initDataObserver() {
        ServiceObserver.actionPause.observe(this){
            Timber.i("actionPause: $it")
            if (it){
                mediaPlayer?.pause()
                ServiceObserver.isPlaying.value = false
            }else{
                mediaPlayer?.start()
                ServiceObserver.isPlaying.value = true
            }
        }

        ServiceObserver.currentUri.observe(this){
            if (it != null){
                try {
                    mediaPlayer?.apply {
                        setOnCompletionListener({})
                        reset()//不加报java.lang.IllegalStateException
                        ServiceObserver.isPlaying.value = false
                        setDataSource(applicationContext,it)
                        Timber.i("service：开始播放$it")
                        prepare()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        ServiceObserver.nextMusicEvent.observe(this){
            Timber.i("播放下一首：$it")
            if (it){
                if (ServiceObserver.currentFlowMode != 2){
                    ServiceObserver.nextSong()
                }

            }
        }

        ServiceObserver.currentIndex.observe(this){
            if (it != null){
                if (!ServiceObserver.sortModeChangeBringCurrentIndexLockForService){
                    if (ServiceObserver.mediaPlayerCreated.value == true){
                        ServiceObserver.currentUri.value = ServiceObserver.sortedMusics.value?.get(it)?.path
                    }
                }else{
                    ServiceObserver.freeLockForService()
                }

            }
        }

        ServiceObserver.seekToPosition.observe(this){
            if (it != null){
                mediaPlayer?.seekTo((mediaPlayer?.duration?.times(it) ?: 0) / 1000)
            }
        }

        ServiceObserver.isLooping.observe(this){
            if (it == true){
                mediaPlayer?.isLooping = true
            }else if (it == false){
                mediaPlayer?.isLooping = false
            }
        }
    }


    override fun stopService(name: Intent?): Boolean {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        return super.stopService(name)
    }

    override fun onDestroy() {
        stopForeground(true)
        Timber.i("结束服务")
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
        mediaPlayer!!.stop()
        mediaPlayer!!.release()
        mediaPlayer = null
        ServiceObserver.mediaPlayerCreated.value = false
//        Timber.i("onDestroy(),保存当前歌曲")
//        ServiceObserver.setCurrentUri(this)
        super.onDestroy()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.setOnCompletionListener(this@BackGroundMusicService)
        mp?.start()
        Timber.i("status: ${mp?.isPlaying}")
        ServiceObserver.musicDuration = mp?.duration ?: 0
        ServiceObserver.isPlaying.value = mp?.isPlaying

        Timber.i("duration: ${ServiceObserver.musicDuration}")

        //开启定时器
        timer = Timer(true)
        val timerTask = object : TimerTask(){
            override fun run() {
//            Timber.i("timer:${mediaPlayer?.currentPosition}")
                ServiceObserver.currentPosition.postValue(mp?.currentPosition)
            }

        }
        timer?.schedule(timerTask,0,1000)

    }

    override fun onCompletion(mp: MediaPlayer?) {
        timer?.cancel()
        timer?.purge()
        timer = null
        Timber.i("歌结束了，默认下一首")
        ServiceObserver.isPlaying.value = false
        ServiceObserver.nextMusicEvent.value = true
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }
}