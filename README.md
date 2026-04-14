# Mana Nauda 💰

Personal finance tracker Android app for managing income and expenses.

**Personālā finanšu uzskaites lietotne Android platformai.**

---

## Features / Funkcionalitāte

- **Add transactions** — income & expenses with amount, note, and date
- **Balance calculation** — real-time total balance display
- **Date range filter** — view transactions for a specific period
- **Swipe to delete** — swipe left/right to remove a transaction (with undo)
- **Dark mode** — automatic dark theme support
- **Latvian UI** — full Latvian language interface

## Tech Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Programming language |
| **MVVM** | Architecture pattern |
| **Room** | Local SQLite database |
| **LiveData** | Reactive UI updates |
| **Material Design** | UI components & theming |
| **View Binding** | Type-safe view access |
| **Coroutines** | Async database operations |

## Project Structure

```
app/src/main/java/com/example/billingapp/
├── Transaction.kt           # Data model (Room Entity)
├── TransactionDao.kt        # Database Access Object
├── AppDatabase.kt           # Room database configuration
├── TransactionRepository.kt # Data layer
├── TransactionViewModel.kt  # MVVM ViewModel
├── TransactionAdapter.kt    # RecyclerView adapter
└── MainActivity.kt          # Main activity
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
