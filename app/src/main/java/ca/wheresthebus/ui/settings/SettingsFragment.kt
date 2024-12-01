package ca.wheresthebus.ui.settings

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import ca.wheresthebus.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("appearance")?.setOnPreferenceChangeListener {
                _, newValue ->
            val themeValue = newValue as String
            updateTheme(themeValue) // Change the theme dynamically
            true
        }
    }

    private fun updateTheme(themeValue: String) {
        val themeMode = when (themeValue) {
            getString(R.string.preference_appearance_light_value)  -> AppCompatDelegate.MODE_NIGHT_NO
            getString(R.string.preference_appearance_dark_value) -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}