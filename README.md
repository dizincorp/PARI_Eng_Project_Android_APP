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
* **Precise Date & Time Selection (RRRR-MM-DD GG:mm:ss)**:
  * Beautiful Material dialog (`DialogDateTimePickerBinding` / `dialog_date_time_picker.xml`) with rounded corners, pill toggle for Start vs End, date wheel spinner, and side-by-side NumberPickers for Hour (00-23), Minute (00-59), and Second (00-59).
  * Real-time preview pill and validation ensuring start time is strictly earlier than end time and falls within owner access hours.
  * Separate clickable start and end datetime cards on the reservation screen.
* **Vehicle Selector & Deduplicated Vehicle Management**: Select existing vehicle or add a new vehicle via `POST /api/vehicles` and `VehicleRepository` without duplicate records.
* **Real REST Reservations (`POST /api/reservations`)**: Reserving a spot dispatches ISO-8601 UTC timestamps to backend via `ReservationRepository`, updates status to occupied, marks it on the map, and creates an active reservation in Room.
* **HTTP 409 Conflict Handling**: Detects concurrent booking conflicts (HTTP 409) and alerts the user with a dialog to select another time slot.

### 3. 📅 Active & Past Reservations (`ReservationsActivity`)
* **REST Synchronization (`GET /api/reservations/incoming` & `GET /api/reservations`)**:
  * Seamlessly fetches incoming / active reservations and full user history from backend endpoints via `ReservationRepository`.
  * Synchronizes responses into the local Room Database cache (`ReservationDao`) acting as Single Source of Truth.
  * Real-time refresh button (`btn_refresh_reservations`) and auto-refresh on activity start/resume.
* **Modern `RecyclerView` List Display (`ReservationsAdapter` / `item_reservation.xml`)**:
  * **Active Tab (`Aktywne`)**:
    * Displays incoming and active bookings with color-coded status badges (**AKTYWNA TERAZ**, **OCZEKUJE NA PŁATNOŚĆ**), exact schedule, vehicle info, and entry PIN codes.
    * Live ticking countdown timer banner (`HH:mm:ss`) calculating real-time remaining or upcoming duration.
    * **Nawiguj do miejsca** button launching Google Maps navigation directly to the parking spot.
    * Tapping any reservation opens a detailed modal dialog with PIN code, exact dates, confirmation download, and cancellation options.
    * Empty state with a quick link back to search on map if no active reservations exist.
  * **History Tab (`Historia`)**:
    * Lists all past and canceled reservations with vehicle details, formatted dates, status badges (**ZAKOŃCZONA**, **ANULOWANA**), and total amounts paid.

### 4. 🌐 Robust ISO-8601 Gson Date Adapters (`IsoDateTimeAdapter`)
* **Multi-Format Deserialization**:
  * Handles standard ISO-8601 with milliseconds (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`), without milliseconds (`yyyy-MM-dd'T'HH:mm:ss'Z'`), timezone offsets (`+02:00`, `+00:00`), SQL timestamps, and epoch timestamps.
  * Registered in `ApiClient` for `Date`, `LocalDateTime`, and `ZonedDateTime`.
  * `ReservationResponse` utilizes `Date` for all timestamp fields (`startsAt`, `endsAt`, `createdAt`, `checkInAt`, `checkOutAt`) with millis helper methods.

### 5. 🅿️ Owner Dashboard & Spot Management (`PlacesActivity` & `AddPlaceActivity`)
* **Dashboard Subtitle**: Displays dynamic user greeting and current month/year (`Dashboard • [Aktualny Miesiąc i Rok]`).
* **Moje Miejsca**: Lists owned parking spots with status badges (**Wolne** / **Zajęte**).
* **Separate Spot Availability Fragment**: Standalone `SpotAvailabilityFragment` managing availability time ranges.
* **Geocoding via Google Maps**: Coordinates (`lat`, `lng`) are obtained dynamically using Android Google Geocoding API (`Geocoder`) in background thread from street, postal code, and city.
* **Photo Picker with Permissions**: Custom photo selection from gallery (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`) with preview and removal option; default fallback photo used when owner uploads no custom photo.
* **Real REST Spot Creation**: Dispatches `POST /api/spots` through `SpotRepository`, validating constraints (country: `PL`, ISO 2-letter code, positive price, ownership confirmation).
* **Spot Editing & Deletion**: Tapping any spot opens `AddPlaceActivity` with pre-filled fields to edit spot details, pricing, amenities, and **Czas dostępu**.

### 6. 👤 Profile & Profile Details (`ProfileActivity` & `ProfileDetailsActivity`)
* **Minimalist Profile Header**: Displays user avatar, first and last name, and user rating (no extraneous membership badges).
* **Dedicated Profile Details Screen (`ProfileDetailsActivity`)**:
  * Profile avatar image picker with runtime permission handling.
  * Separate editing of first and last name via `DialogEditNameBinding`.
  * Phone number editing via `DialogEditPhoneBinding`.
  * Display of user role (Kierowca / Właściciel) and rating.
* **Driver Vehicles Management (`VehicleApiService`)**:
  * POJO Models: `CreateVehicleRequest` (`model`, `plateNumber`), `VehicleResponse` (`id`, `model`, `plateNumber`, `createdAt`).
  * Endpoints: `POST /api/vehicles`, `GET /api/vehicles`, `DELETE /api/vehicles/{id}`.
  * Single Source of Truth caching in Room (`VehicleDao`) with plate-based deduplication preventing repeated entries.

### 7. 🗓️ Availability Calendar (`AvailabilityCalendarActivity`)
* **Interactive Calendar**: Dynamic calendar view allowing owners to check availability for any date.
* **Spot & Time Range Selection**: Dropdown to select spot and input custom time range.
* **Block Slot**: Ability to block slots for maintenance or private use.
* **Quick Navigation**: Button to return back to the Places dashboard.

### 8. 💰 Earnings & Transaction Logs (`EarningsActivity`)
* **Monthly Summary**: Displays overall earnings summary card.
* **Transaction History**: Itemized list of earnings per reservation with vehicle, duration, date, and price.

---

## 🛠️ Tech Stack & Architecture

* **Language**: Java 11
* **Architecture**: MVVM (Model-View-ViewModel) + Single Source of Truth Repository Pattern
* **Networking**: Retrofit2 + Gson (`SpotApiService`, `ReservationApiService`, `VehicleApiService`, `AuthInterceptor` with Bearer JWT token, `IsoDateTimeAdapter`)
* **Local Database**: Room Database (`AppDatabase`, `ParkingPlaceDao`, `ReservationDao`, `VehicleDao`, `UserDao`)
* **UI Layer**: View Binding, Material Design Components 3, `ConstraintLayout`, `RecyclerView`
* **Maps**: Google Maps SDK for Android (`SupportMapFragment`), Google Maps Geocoding API (`Geocoder`)
* **Reactivity**: `LiveData`, `MutableLiveData`, `MediatorLiveData`
* **Asynchronous Execution**: `ExecutorService`, `Handler`, `Looper`
* **Package Structure**: Package-by-layer (`.ui`, `.viewmodel`, `.model`, `.adapter`, `.repository`, `.network`, `.database`, `.utils`)

---

## 📁 Package Structure

```
com.example.project/
├── adapter/
│   ├── PlacesAdapter.java
│   ├── ReservationsAdapter.java
│   ├── TransactionsAdapter.java
│   └── VehiclesAdapter.java
├── database/
│   ├── dao/
│   │   ├── ParkingPlaceDao.java
│   │   ├── ReservationDao.java
│   │   ├── TransactionDao.java
│   │   ├── UserDao.java
│   │   └── VehicleDao.java
│   ├── entity/
│   │   ├── ParkingPlaceEntity.java
│   │   ├── ReservationEntity.java
│   │   ├── TransactionEntity.java
│   │   ├── UserEntity.java
│   │   └── VehicleEntity.java
│   ├── AppDatabase.java
│   └── Converters.java
├── model/
│   ├── ParkingPlace.java
│   ├── Reservation.java
│   ├── Transaction.java
│   ├── UserProfile.java
│   └── Vehicle.java
├── network/
│   ├── model/
│   │   ├── CreateSpotRequest.java
│   │   ├── CreateVehicleRequest.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── ParkingSpot.java
│   │   ├── RegisterRequest.java
│   │   ├── RegisterResponse.java
│   │   ├── ReservationRequest.java
│   │   ├── ReservationResponse.java
│   │   ├── UserProfileResponse.java
│   │   └── VehicleResponse.java
│   ├── ApiClient.java
│   ├── ApiService.java
│   ├── AuthInterceptor.java
│   ├── IsoDateTimeAdapter.java
│   ├── ReservationApiService.java
│   ├── Resource.java
│   ├── SpotApiService.java
│   └── VehicleApiService.java
├── repository/
│   ├── AppRepository.java
│   ├── AuthRepository.java
│   ├── ReservationRepository.java
│   ├── SpotRepository.java
│   └── VehicleRepository.java
├── ui/
│   ├── AddPlaceActivity.java
│   ├── AvailabilityCalendarActivity.java
│   ├── BaseActivity.java
│   ├── EarningsActivity.java
│   ├── LoginActivity.java
│   ├── MainActivity.java
│   ├── OnboardingActivity.java
│   ├── PaymentMethodsActivity.java
│   ├── PlacesActivity.java
│   ├── ProfileActivity.java
│   ├── ProfileDetailsActivity.java
│   ├── RegisterActivity.java
│   ├── ReservationsActivity.java
│   ├── ReservePlaceActivity.java
│   ├── SettingsActivity.java
│   └── SpotAvailabilityFragment.java
├── utils/
│   ├── AccessTimeParser.java
│   ├── NotificationHelper.java
│   └── SessionManager.java
└── viewmodel/
    ├── AddPlaceViewModel.java
    ├── AuthViewModel.java
    ├── AvailabilityCalendarViewModel.java
    ├── PlacesViewModel.java
    ├── ProfileViewModel.java
    ├── ReservationsViewModel.java
    ├── ReservePlaceViewModel.java
    ├── SearchViewModel.java
    └── SettingsViewModel.java
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
   In `app/src/main/res/values/strings.xml`, check or replace the Google Maps API Key:
   ```xml
   <string name="google_maps_key" translatable="false">YOUR_ACTUAL_API_KEY</string>
   ```
4. **Build & Run**:
   Sync project with Gradle files in Android Studio and run `:app:assembleDebug` or launch on an Android device/emulator (API level 24+).
