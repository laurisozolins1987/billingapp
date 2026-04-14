package com.example.billingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import com.example.billingapp.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_CURRENCY = "currency"

        fun isDarkMode(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false)
        }

        fun isBiometricEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_BIOMETRIC, false)
        }

        fun getCurrency(context: Context): String {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_CURRENCY, "EUR") ?: "EUR"
        }

        fun getCurrencySymbol(context: Context): String {
            return when (getCurrency(context)) {
                "USD" -> "$"
                "GBP" -> "£"
                else -> "€"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbarSettings.setNavigationOnClickListener { finish() }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        binding.switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK_MODE, false)
        binding.switchBiometric.isChecked = prefs.getBoolean(KEY_BIOMETRIC, false)

        val currency = prefs.getString(KEY_CURRENCY, "EUR") ?: "EUR"
        updateCurrencyLabel(currency)

        // Check biometric availability
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            binding.switchBiometric.isEnabled = false
            binding.tvBiometricDesc.text = getString(R.string.biometric_not_available)
        }
    }

    private fun setupListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_DARK_MODE, isChecked)
                .apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_BIOMETRIC, isChecked)
                .apply()
        }

        binding.layoutCurrency.setOnClickListener {
            showCurrencyPicker()
        }

        binding.layoutExport.setOnClickListener {
            exportData()
        }
    }

    private fun showCurrencyPicker() {
        val currencies = arrayOf("EUR (€)", "USD ($)", "GBP (£)")
        val currencyCodes = arrayOf("EUR", "USD", "GBP")
        val currentCurrency = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_CURRENCY, "EUR") ?: "EUR"
        val selectedIndex = currencyCodes.indexOf(currentCurrency)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.currency)
            .setSingleChoiceItems(currencies, selectedIndex) { dialog, which ->
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putString(KEY_CURRENCY, currencyCodes[which])
                    .apply()
                updateCurrencyLabel(currencyCodes[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun updateCurrencyLabel(currencyCode: String) {
        binding.tvCurrencyValue.text = when (currencyCode) {
            "USD" -> "USD ($)"
            "GBP" -> "GBP (£)"
            else -> "EUR (€)"
        }
    }

    private fun exportData() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "mana_nauda_export.csv")
        }
        exportLauncher.launch(intent)
    }

    private val exportLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val db = AppDatabase.getDatabase(this)
                    Thread {
                        val transactions = db.transactionDao().getAllTransactionsSync()
                        val csvContent = buildString {
                            appendLine("ID,Amount,Note,Category,Description,Date,CreatedAt,Type")
                            transactions.forEach { t ->
                                val type = if (t.isIncome) "Income" else "Expense"
                                appendLine("${t.id},${t.amount},\"${t.note}\",\"${t.category}\",\"${t.description}\",${t.date},${t.createdAt},$type")
                            }
                        }
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csvContent.toByteArray())
                        }
                        runOnUiThread {
                            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show()
                        }
                    }.start()
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.export_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
