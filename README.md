# Mana Nauda 💰

Android personālā finanšu uzskaites lietotne, kas ļauj sekot līdzi ieņēmumiem un izdevumiem.

## Funkcionalitāte

- **Darījumu pievienošana** — ieņēmumi un izdevumi ar summu, piezīmi un datumu
- **Bilances aprēķins** — reāllaika kopsummas attēlošana
- **Datumu filtrēšana** — darījumu apskatīšana pa laika periodiem
- **Dzēšana ar vilkšanu** — velciet darījumu pa kreisi/labi lai dzēstu (ar atsaukšanas iespēju)
- **Tumšais režīms** — automātisks tumšā režīma atbalsts
- **Latviešu valodā** — pilns saskarnes tulkojums

## Tehnoloģijas

| Tehnoloģija | Lietojums |
|---|---|
| **Kotlin** | Programmēšanas valoda |
| **MVVM** | Arhitektūras modelis |
| **Room** | Lokālā SQLite datubāze |
| **LiveData** | Reaktīvi dati UI atjaunošanai |
| **Material Design** | UI komponentes un tēmas |
| **View Binding** | Drošs skatu piesaistīšanas veids |
| **Coroutines** | Asinhronie datubāzes izsaukumi |

## Projekta struktūra

```
app/src/main/java/com/example/billingapp/
├── Transaction.kt           # Datu modelis (Room Entity)
├── TransactionDao.kt        # Datubāzes piekļuves objekts
├── AppDatabase.kt           # Room datubāzes konfigurācija
├── TransactionRepository.kt # Datu slānis
├── TransactionViewModel.kt  # MVVM ViewModel
├── TransactionAdapter.kt    # RecyclerView adapteris
└── MainActivity.kt          # Galvenā aktivitāte
```

## Prasības

- Android Studio Ladybug vai jaunāks
- Min SDK: 24 (Android 7.0)
- Target SDK: 35

## Palaišana

1. Klonē repozitoriju
2. Atver projektu Android Studio
3. Sinhronizē Gradle
4. Palaid uz emulatora vai ierīces

## Licence

Šis projekts ir privāts.
