# ParkShare - Smart Parking System Android Application

ParkShare is a modern Android application written in **Java** following the **MVVM (Model-View-ViewModel)** architectural pattern. The app provides a smart parking management system supporting drivers (who search, reserve, and pay for parking spots) and parking owners (who list, edit, and manage availability for their spots).

---

## 🚀 Key Features

### 1. 🔍 Search & Interactive Map (`MainActivity`)
* **Google Maps Integration**: Displays available parking spots as interactive markers.
* **Geocoding & Location Search**: Asynchronous location search via `Geocoder` on a background thread.
* **Spot Preview Card**: Clicking a spot marker displays spot details (title, hourly rate, rating, tags/amenities, and a **Rezerwuj teraz** button).

### 2. 📝 Reservation Process (`ReservePlaceActivity`)
* **Spot Details & Pricing**: Full pricing breakdown (hourly rate, service fee, total).
* **Schedule Selector**: Time interval schedule selector.
* **Vehicle Selector & Vehicle Management**: Select existing vehicle or add a new vehicle (car make/model, license plate, color) via dialog.
* **Booking & Payment**: Reserving a spot updates its status to occupied, marks it on the map, and creates an active reservation.

### 3. 📅 Active & Past Reservations (`ReservationsActivity`)
* **Active Tab (`Aktywne`)**:
  * Displays active reservation card with countdown timer (`HH:mm:ss`), spot location, and **Nawiguj do miejsca** button launching Google Maps navigation.
  * Interactive details dialog showing entry PIN code, entry/exit schedule, **Pobierz potwierdzenie**, and **Anuluj rezerwację** button.
  * Canceling a reservation sets spot status back to free and restores it on the map.
  * Empty state with a quick link back to search on map if no active reservation exists.
* **History Tab (`Historia`)**:
  * Lists all past and canceled reservations with vehicle details, dates, and total amount paid.

### 4. 🅿️ Owner Dashboard & Spot Management (`PlacesActivity` & `AddPlaceActivity`)
* **Dashboard Subtitle**: Displays dynamic user greeting and current month/year (`Dashboard • [Aktualny Miesiąc i Rok]`).
* **Moje Miejsca**: Lists owned parking spots with status badges (**Wolne** / **Zajęte**).
* **Spot Editing**: Tapping any spot opens `AddPlaceActivity` with pre-filled fields to edit spot details, pricing, amenities, and **Czas dostępu** (availability window).
* **New Spot Creation**: Spots created by the owner are initially marked as free (**Wolne**) and immediately rendered on the map.

### 5. 🗓️ Availability Calendar (`AvailabilityCalendarActivity`)
* **Interactive Calendar**: Dynamic calendar view allowing owners to check availability for any date.
* **Spot & Time Range Selection**: Dropdown to select spot and input custom time range.
* **Block Slot**: Ability to block slots for maintenance or private use.
* **Quick Navigation**: Button to return back to the Places dashboard.

### 6. 💰 Earnings & Transaction Logs (`EarningsActivity`)
* **Monthly Summary**: Displays overall earnings summary card.
* **Transaction History**: Itemized list of earnings per reservation with vehicle, duration, date, and price.

---

## 🛠️ Tech Stack & Architecture

* **Language**: Java 11
* **Architecture**: MVVM (Model-View-ViewModel) + Single Source of Truth Repository Pattern
* **Networking**: Retrofit2 + Gson (`SpotApiService`, `AuthInterceptor` with Bearer JWT token)
* **Local Database**: Room Database (`AppDatabase`, `ParkingPlaceDao` with batch inserts)
* **UI Layer**: View Binding, Material Design Components 3, `ConstraintLayout`, `RecyclerView`
* **Maps**: Google Maps SDK for Android (`SupportMapFragment`)
* **Reactivity**: `LiveData`, `MutableLiveData`
* **Asynchronous Execution**: `ExecutorService`, `Handler`, `Looper`
* **Package Structure**: Package-by-layer (`.ui`, `.viewmodel`, `.model`, `.adapter`, `.repository`, `.network`, `.database`)

---

## 📁 Package Structure

```
com.example.project/
├── adapter/
│   ├── PlacesAdapter.java
│   ├── ReservationsAdapter.java
│   └── TransactionsAdapter.java
├── model/
│   ├── ParkingPlace.java
│   ├── Reservation.java
│   ├── Transaction.java
│   └── Vehicle.java
├── repository/
│   └── AppRepository.java
├── ui/
│   ├── AddPlaceActivity.java
│   ├── AvailabilityCalendarActivity.java
│   ├── BaseActivity.java
│   ├── EarningsActivity.java
│   ├── MainActivity.java
│   ├── PlacesActivity.java
│   ├── ProfileActivity.java
│   ├── ReservationsActivity.java
│   └── ReservePlaceActivity.java
└── viewmodel/
    ├── AddPlaceViewModel.java
    ├── AvailabilityCalendarViewModel.java
    ├── PlacesViewModel.java
    ├── ProfileViewModel.java
    ├── ReservationsViewModel.java
    └── SearchViewModel.java
```

---

## ⚙️ Build & Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   ```
2. **Open in Android Studio**:
   Open the project folder in Android Studio Jellyfish / Ladybug or newer.
3. **Configure Google Maps API Key**:
   In `app/src/main/res/values/strings.xml`, replace `YOUR_API_KEY_HERE` with a valid Google Maps API Key:
   ```xml
   <string name="google_maps_key" translatable="false">YOUR_ACTUAL_API_KEY</string>
   ```
4. **Build & Run**:
   Sync project with Gradle files and run `app:assembleDebug` or launch on an Android device/emulator (API level 24+).
