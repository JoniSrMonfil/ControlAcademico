package com.example.controlacademico

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.example.controlacademico.databinding.ActivitySettingsBinding
import com.example.controlacademico.prefs.Prefs

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        binding.switchTema.isChecked = prefs.isTemaOscuro()

        binding.switchTema.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTemaOscuro(isChecked)

            // Cambiar el tema dinámicamente
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
}