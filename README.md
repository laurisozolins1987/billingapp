# Mana Nauda 💰

Personal finance tracker Android app for managing income and expenses with a modern Material Design UI.

**Personālā finanšu uzskaites lietotne Android platformai ar modernu Material Design saskarni.**

---

## Features

- **Dashboard** — overview of total balance, income & expenses at a glance
- **Add transactions** — income & expenses with amount, note, and date picker
- **Swipe to delete** — swipe left/right to remove transactions (with undo)
- **Date range filter** — filter transactions by custom date range via calendar
- **Biometric lock** — fingerprint / Face ID authentication for app security
- **Dark mode** — toggle dark theme from Settings
- **Currency selector** — choose between EUR (€), USD ($), GBP (£)
- **CSV export** — export all transaction data to a CSV file
- **Settings screen** — appearance, security, data management
- **Latvian UI** — full Latvian language interface
- **Empty state** — friendly placeholder when no transactions exist

## Screenshots

| Dashboard | Add Transaction | Settings |
|---|---|---|
| Modern gradient header with balance cards | Material toggle buttons for income/expense | Dark mode, biometric lock, currency, export |

## Tech Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Programming language |
| **MVVM** | Architecture pattern |
| **Room** | Local SQLite database |
| **LiveData** | Reactive UI updates |
| **BiometricPrompt** | Fingerprint / face authentication |
| **Material Design** | UI components, themes, date pickers |
| **View Binding** | Type-safe view access |
| **Coroutines** | Async database operations |
| **SharedPreferences** | User settings persistence |

## Project Structure

```
app/src/main/java/com/example/billingapp/
├── Transaction.kt           # Data model (Room Entity)
├── TransactionDao.kt        # Database Access Object
├── AppDatabase.kt           # Room database configuration
├── TransactionRepository.kt # Data layer
├── TransactionViewModel.kt  # MVVM ViewModel
├── TransactionAdapter.kt    # RecyclerView adapter
├── MainActivity.kt          # Main dashboard activity
└── SettingsActivity.kt      # Settings (dark mode, biometrics, export)
```

## Requirements

- Android Studio Ladybug or newer
- Min SDK: 24 (Android 7.0)
- Target SDK: 35

## Getting Started

1. Clone the repository
   ```bash
   git clone https://github.com/laurisozolins1987/billingapp.git
   ```
2. Open the project in Android Studio
3. Sync Gradle
4. Run on an emulator or device

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
