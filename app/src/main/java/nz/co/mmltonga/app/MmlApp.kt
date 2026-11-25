package nz.co.mmltonga.app

import android.app.Application
import nz.co.mmltonga.app.data.Repository
import nz.co.mmltonga.app.data.Session

class MmlApp : Application() {
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        val gson = Repository.gson()
        repo = Repository(Session(this, gson), gson)
    }
}
