package net.opendasharchive.openarchive.publish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.services.Conduit
import timber.log.Timber

class UploadManagerActivity : BaseActivity() {

    companion object {
        const val PROJECT_ID = "PROJECT_ID"
    }

    var mFrag: MediaListFragment? = null
    private var mMenuEdit: MenuItem? = null
    private var projectId: Long = Project.EMPTY_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_upload_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.title_uploads)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        projectId = intent.getLongExtra(PROJECT_ID, Project.EMPTY_ID)
        mFrag = supportFragmentManager.findFragmentById(R.id.fragUploadManager) as? MediaListFragment
        mFrag?.projectId = projectId
    }

    override fun onResume() {
        super.onResume()
        mFrag?.refresh()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter(MainActivity.INTENT_FILTER_NAME)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Updating media")

            val status = intent.getIntExtra(Conduit.MESSAGE_KEY_STATUS, -1)

            if (status == Media.Status.Uploaded.id) {
                Handler(Looper.getMainLooper()).post {
                    val progressToolbarTitle: String = if (mFrag!!.getUploadingCounter() == 0) {
                        getString(R.string.title_uploads)
                    } else {
                        getString(R.string.title_uploading) + " (" + mFrag!!.getUploadingCounter() + " left)"
                    }
                    supportActionBar!!.title = progressToolbarTitle
                }
                mFrag!!.refresh()
            }
            else if (status == Media.Status.Queued.id) {
                Handler(Looper.getMainLooper()).post {
                    supportActionBar!!.title =
                        getString(R.string.title_uploading) + " (" + mFrag!!.getUploadingCounter() + " left)"
                }
            }
            else if (status == Media.Status.Uploading.id) {
                val mediaId = intent.getLongExtra(Conduit.MESSAGE_KEY_MEDIA_ID, -1)
                val progress = intent.getLongExtra(Conduit.MESSAGE_KEY_PROGRESS, -1)
                if (mediaId != -1L) {
                    mFrag!!.updateItem(mediaId, progress)
                }
            }
            else if (status == Media.Status.Error.id) {
                CleanInsightsManager.getConsent(this@UploadManagerActivity) {
                    // TODO: Record metadata. See iOS implementation.
                    CleanInsightsManager.measureEvent("upload", "upload_failed")
                }
            }
        }
    }

    private var mEditMode = false

    private fun toggleEditMode() {
        mEditMode = !mEditMode
        mFrag?.setEditMode(mEditMode)
        mFrag?.refresh()

        if (mEditMode) {
            mMenuEdit?.setTitle(R.string.menu_done)
            stopService(Intent(this, PublishService::class.java))
        }
        else {
            mMenuEdit?.setTitle(R.string.menu_edit)
            startService(Intent(this, PublishService::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_upload, menu)
        mMenuEdit = menu.findItem(R.id.menu_edit)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_edit -> {
                toggleEditMode()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}