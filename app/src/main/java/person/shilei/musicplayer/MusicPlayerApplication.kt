package person.shilei.musicplayer

import android.app.Application
import timber.log.Timber

class MusicPlayerApplication:Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}