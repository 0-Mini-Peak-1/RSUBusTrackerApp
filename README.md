# RSU Bus Tracker (Rangsit University Shuttle System)

**RSU Bus Tracker** is a comprehensive "Mega Project" designed to modernize the shuttle bus operations at Rangsit University. This system replaces legacy hardware with a modern, stable solution that provides real-time tracking, estimated passenger density via Computer Vision, and route optimization data.

## Project Overview

This repository contains the source code for the **Driver-Side Mobile Application**. It serves as the primary data sender, utilizing Android Foreground Services to broadcast precise GPS coordinates to the central server while the bus is in operation.

### Key Features
* **Real-Time Tracking:** High-frequency GPS updates (1Hz - 3Hz) sent to a PostgreSQL/PostGIS backend.
* **Modern UI/UX:** Built entirely with **Kotlin Jetpack Compose**, featuring bouncy animations and gradient aesthetics.
* **Driver Authentication:** Secure login system linking specific vehicles to active trips.
* **Background Operation:** Robust `ForegroundService` implementation ensures tracking continues even when the screen is locked.
* **Passenger Counting (Planned):** Integration with on-board cameras and **OpenCV** to count passengers boarding and alighting.

---

## Tech Stack

### Mobile Application (Android)
* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material3)
* **Build System:** Gradle (Kotlin DSL)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Key Libraries:**
    * `androidx.compose`: Modern UI development.
    * `com.google.android.gms:play-services-location`: Fused Location Provider.
    * `Retrofit`: Networking & API calls.
    * `Coroutines`: Asynchronous programming.

### Backend & Infrastructure
* **Database:** PostgreSQL with **PostGIS** extension (for spatial queries).
* **Hardware:** Raspberry Pi / Arduino (integrated with GPS modules).
* **Computer Vision:** OpenCV (Python) for passenger density analysis.

---

## Database Schema

The system is built on a relational database designed to separate static schedule data from high-volume telemetry.

```mermaid
erDiagram
    USERS {
        uuid id PK
        varchar username UK
        varchar password_hash
        timestamp created_at
    }

    VEHICLES }|--o| ROUTES : "assigned to"
    VEHICLES ||--o{ TRIPS : has
    
    VEHICLES {
        varchar id PK "VH001"
        varchar name "Bus A1"
        varchar type "1-Section"
        varchar assigned_route_id FK
        varchar status "active/inactive"
        timestamp created_at
    }

    ROUTES ||--o{ TRIPS : has
    ROUTES ||--o{ ROUTE_STOPS : contains
    
    ROUTES {
        varchar id PK "R01"
        varchar name "Campus Loop"
        varchar color "#FF0000"
        varchar status
        timestamp created_at
    }

    STOPS ||--o{ ROUTE_STOPS : "part of"
    
    STOPS {
        varchar id PK "ST001"
        varchar name_th "Stop A"
        varchar name_en "Stop A"
        geography location "PostGIS Point"
        varchar image_url
        varchar status
        timestamp created_at
    }

    ROUTE_STOPS {
        uuid id PK
        varchar route_id FK
        varchar stop_id FK
        int stop_order "1, 2, 3..."
    }

    TRIPS ||--o{ GPS_TRACKS : generates
    
    TRIPS {
        uuid id PK
        varchar vehicle_id FK
        varchar route_id FK
        timestamp start_time
        timestamp end_time
        varchar status "in_progress"
        timestamp created_at
    }

    GPS_TRACKS {
        bigserial id PK
        uuid trip_id FK
        varchar vehicle_id FK
        geography location "PostGIS Point"
        decimal speed "km/h"
        timestamp recorded_at
    }

    FEEDBACK {
        uuid id PK
        varchar type
        text message
        inet ip_address
        timestamp created_at
    }

```

---

## Screenshots

| Login Screen | Tracker Dashboard |
| --- | --- |
| Currently we have no picture :/ <img src="docs/login_screen.png" width="300" /> | Currently we have no picture :/ <img src="docs/tracker_screen.png" width="300" /> |
| *Clean gradient UI with vehicle authentication* | *Real-time telemetry stats with slide animations* |

---

## Getting Started

### Prerequisites

* Android Studio Panda 1 or newer.
* JDK 17+.
* An Android device with GPS capabilities (or Emulator).

### Installation

1. **Clone the repository:**
```bash
git clone [https://github.com/0-Mini-Peak-1/RSUBusTrackerApp](https://github.com/0-Mini-Peak-1/RSUBusTrackerApp)

```


2. **Open in Android Studio:**
Select `Open` and navigate to the cloned folder. Wait for Gradle Sync to finish.
3. **Configure API Keys:**
* Create a `local.properties` file if it doesn't exist.
* Add your backend URL: `BASE_URL="https://api.your-backend.com/"`.


4. **Run the App:**
Connect your device via USB and click the **Run** (▶) button.

---

## Contributors

* **Chawagorn Toomma** - *Mobile Developer & IoT*
* **Rangsit University** - *Project Host*

---
