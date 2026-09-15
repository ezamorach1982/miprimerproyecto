package com.funtv.player.ui.browse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.funtv.player.R

/**
 * Extiende FragmentActivity (no AppCompatActivity): Theme.FunTV.Browse desciende de
 * Theme.Leanback, y AppCompatActivity exige un tema descendiente de Theme.AppCompat.
 */
class SectionBrowseActivity : FragmentActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentType = ContentType.valueOf(
            intent.getStringExtra(EXTRA_CONTENT_TYPE) ?: ContentType.LIVE.name
        )
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, SectionBrowseFragment.newInstance(contentType))
                .commitNow()
        }
    }

    companion object {
        private const val EXTRA_CONTENT_TYPE = "extra_content_type"

        fun newIntent(context: Context, contentType: ContentType): Intent =
            Intent(context, SectionBrowseActivity::class.java)
                .putExtra(EXTRA_CONTENT_TYPE, contentType.name)
    }
}
