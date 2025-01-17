package net.opendasharchive.openarchive.features.media.batch

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityBatchReviewMediaBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListViewModel
import net.opendasharchive.openarchive.features.media.review.ReviewMediaActivity
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import java.io.File

class BatchReviewMediaActivity : BaseActivity() {

    private lateinit var mBinding: ActivityBatchReviewMediaBinding
    private lateinit var viewModel: BatchReviewMediaViewModel
    private lateinit var previewMediaListViewModel: PreviewMediaListViewModel

    private var mediaList: ArrayList<Media> = arrayListOf()
    private var mPicasso: Picasso? = null
    private var menuPublish: MenuItem? = null
    private var menuDelete: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityBatchReviewMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        viewModel = ViewModelProvider(this)[BatchReviewMediaViewModel::class.java]
        initLayout()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        if (mPicasso == null) {
            val videoRequestHandler = VideoRequestHandler(this)

            mPicasso = Picasso.Builder(this)
                .addRequestHandler(videoRequestHandler)
                .build()
        }

        previewMediaListViewModel = PreviewMediaListViewModel.getInstance(this, application)
        previewMediaListViewModel.observeValuesForWorkState(this)
    }

    private fun updateFlagState(media: Media) {
        if (media.flag) {
            mBinding.archiveMetadataLayout.flagIndicator.setImageResource(R.drawable.ic_flag_selected)
        } else {
            mBinding.archiveMetadataLayout.flagIndicator.setImageResource(R.drawable.ic_flag_unselected)
        }

        if (media.flag) {
            mBinding.archiveMetadataLayout.tvFlagLbl.setText(R.string.status_flagged)
        } else {
            mBinding.archiveMetadataLayout.tvFlagLbl.setText(R.string.hint_flag)
        }

    }

    private fun bindMedia() {
        bindMedia(mediaList[0])
        mBinding.itemDisplay.removeAllViews()
        for (media in mediaList) {
            showThumbnail(media)
        }
    }

    private fun bindMedia(media: Media) {
        mBinding.apply {
            archiveMetadataLayout.apply {
                tvTitleLbl.setText(media.title)

                if (media.description.isNotEmpty()) {
                    tvDescriptionLbl.setText(media.description)
                    descIndicator.setImageResource(R.drawable.ic_edit_selected)
                } else {
                    tvDescriptionLbl.setText("")
                    descIndicator.setImageResource(R.drawable.ic_edit_unselected)
                }

                if (media.location.isNotEmpty()) {
                    tvLocationLbl.setText(media.location)
                    locationIndicator.setImageResource(R.drawable.ic_location_selected)
                } else {
                    tvLocationLbl.setText("")
                    locationIndicator.setImageResource(R.drawable.ic_location_unselected)
                }

                if (media.tags.isNotEmpty()) {
                    tvTagsLbl.setText(media.tags)
                    tagsIndicator.setImageResource(R.drawable.ic_tag_selected)
                } else {
                    tvTagsLbl.setText("")
                    tagsIndicator.setImageResource(R.drawable.ic_tag_unselected)
                }

                if (media.sStatus != Media.Status.Uploaded) {
                    mBinding.archiveMetadataLayout.flagIndicator.show()
                    mBinding.archiveMetadataLayout.tvFlagLbl.show()
                } else {
                    mBinding.archiveMetadataLayout.flagIndicator.hide()
                    mBinding.archiveMetadataLayout.tvFlagLbl.hide()
                }

                tvAuthorLbl.setText(media.author)
            }

            if (media.sStatus != Media.Status.Local && media.sStatus != Media.Status.New) {
                when (media.sStatus) {
                    Media.Status.Uploaded, Media.Status.Published -> {
                        // NO-OP
                    }
                    Media.Status.Queued -> {
                        tvUrl.text = getString(R.string.batch_waiting_for_upload)
                        tvUrl.show()
                    }
                    Media.Status.Uploading -> {
                        tvUrl.text = getString(R.string.batch_uploading_now)
                        tvUrl.show()
                    }
                    else -> {}
                }

                archiveMetadataLayout.apply {
//                    tvTitleLbl.isEnabled = false
//                    tvDescriptionLbl.isEnabled = false

                    if (media.description.isEmpty()) {
                        tvDescriptionLbl.hint = ""
                    }
//                    tvAuthorLbl.isEnabled = false
//                    tvLocationLbl.isEnabled = false

                    if (TextUtils.isEmpty(media.location)) {
                        tvLocationLbl.hint = ""
                    }
//                    tvTagsLbl.isEnabled = false

                    if (media.tags.isEmpty()) {
                        tvTagsLbl.hint = ""
                    }
//                    tvCcLicense.isEnabled = false
                }
            }
            else {
                archiveMetadataLayout.rowFlag.setOnClickListener {
                    mediaList.forEach { media ->
                        media.flag = !media.flag
                    }
                    updateFlagState(media)
                }
            }

            updateFlagState(media)
        }
    }

    private fun saveMedia() {
        for (media in mediaList) saveMedia(media)
    }

    private fun saveMedia(media: Media?) {
        //if deleted
        if (media == null) return

        mBinding.archiveMetadataLayout.let { metaDataLayout ->

            val title = if (metaDataLayout.tvTitleLbl.text.isNotEmpty())
                metaDataLayout.tvTitleLbl.text.toString() else {
                File(media.originalFilePath).name
            }

            viewModel.saveMedia(
                media,
                title,
                metaDataLayout.tvDescriptionLbl.text.toString(),
                metaDataLayout.tvAuthorLbl.text.toString(),
                metaDataLayout.tvLocationLbl.text.toString(),
                metaDataLayout.tvTagsLbl.text.toString()
            )
        }
    }

    override fun onPause() {
        super.onPause()
        saveMedia()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_review_media, menu)
        menuPublish = menu.findItem(R.id.menu_upload)
        menuDelete = menu.findItem(R.id.menu_delete)
        menuPublish?.isVisible = true
        menuDelete?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menu_upload -> uploadMedia()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun uploadMedia() {
        for (media in mediaList) {
            media.sStatus = Media.Status.Queued
            media.save()
        }

        val operation = previewMediaListViewModel.applyMedia()

        print(operation.result.get())
    }

    private fun init() {
        mediaList.clear()

        intent.getLongArrayExtra(ReviewMediaActivity.EXTRA_CURRENT_MEDIA_ID)?.forEach {
            val media = Media.get(it) ?: return@forEach
            mediaList.add(media)
        }

        bindMedia()
    }

    private fun showThumbnail(media: Media) {
        val ivMedia = ImageView(this)
        val margin = 3
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        lp.setMargins(margin, margin, margin, margin)
        lp.height = 600
        lp.width = 800
        ivMedia.layoutParams = lp
        ivMedia.scaleType = ImageView.ScaleType.CENTER_CROP
        if (media.mimeType.startsWith("image")) {
            Glide.with(ivMedia.context).load(Uri.parse(media.originalFilePath)).into(ivMedia)
        } else if (media.mimeType.startsWith("video")) {
            mPicasso?.load(VideoRequestHandler.SCHEME_VIDEO + ":" + media.originalFilePath)?.fit()
                ?.centerCrop()?.into(ivMedia)
        } else if (media.mimeType.startsWith("audio")) {
            ivMedia.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.audio_waveform))
        } else ivMedia.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.no_thumbnail))

        ivMedia.setOnClickListener {
            saveMedia()
            bindMedia(media)
        }

        mBinding.itemDisplay.addView(ivMedia)
    }

    override fun onResume() {
        super.onResume()
        init()
        bindMedia()
    }
}