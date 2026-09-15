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
class CategoryGridActivity : FragmentActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val contentType = ContentType.valueOf(intent.getStringExtra(EXTRA_CONTENT_TYPE)!!)
            val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)!!
            val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME).orEmpty()
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, CategoryGridFragment.newInstance(contentType, categoryId, categoryName))
                .commitNow()
        }
    }

    companion object {
        private const val EXTRA_CONTENT_TYPE = "extra_content_type"
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_CATEGORY_NAME = "extra_category_name"

        fun newIntent(context: Context, contentType: ContentType, categoryId: String, categoryName: String): Intent =
            Intent(context, CategoryGridActivity::class.java)
                .putExtra(EXTRA_CONTENT_TYPE, contentType.name)
                .putExtra(EXTRA_CATEGORY_ID, categoryId)
                .putExtra(EXTRA_CATEGORY_NAME, categoryName)
    }
}
