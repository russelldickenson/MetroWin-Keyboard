package com.metrowin.keyboard.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import com.metrowin.keyboard.KeyboardApp
import com.metrowin.keyboard.R

/** The settings: a home page (see [HomeFragment]) and, under it, one preference fragment per screen, under a Material top app bar. */
class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        Ui.applyInsets(findViewById<View>(R.id.settings_root))
        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportFragmentManager.addOnBackStackChangedListener { showArrow() }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.settings_container, HomeFragment()).commit()
            // The first time, a guided set-up comes first.
            if (!KeyboardApp.of(this).prefs.onboardingDone) startActivity(Intent(this, OnboardingActivity::class.java))
        }
        showArrow()
    }

    /** The arrow back is there on the screens under the home page, not on the home page itself. */
    private fun showArrow() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            toolbar.visibility = View.VISIBLE
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            toolbar.setNavigationContentDescription(R.string.back)
        } else {
            // The home page has a large title of its own instead of the bar (see HomeFragment).
            toolbar.navigationIcon = null
            toolbar.visibility = View.GONE
        }
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        if (::toolbar.isInitialized) { toolbar.title = title; Ui.fitTitle(toolbar) }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val name = pref.fragment ?: return false
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, name)
        supportFragmentManager.beginTransaction().replace(R.id.settings_container, fragment).addToBackStack(null).commit()
        return true
    }
}
