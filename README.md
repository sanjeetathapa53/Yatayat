🚌 Yatayat — Public Transportation Management System

Yatayat is a full-stack public transportation management system designed for Nepal. It connects **passengers, drivers, transport operators, and administrators** through a centralized platform for managing routes, buses, trips, live tracking, bookings, payments, digital tickets, and QR-based ticket validation.

The system is designed to support **multiple transport operators and multiple active buses**, providing a more connected and manageable public transportation experience.


 📌 Project Overview

Public transportation can be difficult to manage when route information, bus operations, passenger services, and administrative processes are handled separately.

Yatayat addresses this problem by providing a centralized system where:

- 👤 Passengers can discover routes, track buses, book trips, and manage tickets.
- 🚌 Operators can manage buses, services, trips, and drivers.
- 👨‍✈️ Drivers can manage assigned trips, provide location data, and validate tickets.
- 🛠️ Administrators can manage routes, stops, operators, buses, approvals, and notifications.



✨ Key Features

👤 Passenger Features

- User registration and login
- Email OTP verification
- Google OAuth authentication
- Role-based access
- Passenger dashboard
- Local route discovery
- Search routes using intermediate bus stops
- View ordered route stops
- Live bus tracking
- Out-of-valley trip search
- Seat selection
- Trip booking
- Digital ticket generation
- Secure QR ticket generation
- Wallet management
- Payment integration
- eSewa payment support
- Khalti payment support
- Passenger notifications
- Booking and ticket history



🚌 Multi-Operator Management

Yatayat is designed to support multiple transportation operators within the same system.

Operators can:

- Register with the platform
- Manage their organization
- Register buses
- View bus approval status
- Manage approved buses
- Create/manage local services
- Manage scheduled trips
- Assign drivers
- Monitor transportation operations

Each operator's buses and services remain associated with the corresponding operator while administrators maintain centralized oversight.



👨‍✈️ Driver Features

Drivers have a dedicated dashboard for managing transportation operations.

Features include:

- Driver authentication
- Driver profile management
- Assigned trip management
- Trip status management
- Start/operate trips
- Device-based location tracking
- Live location updates
- QR ticket scanning
- Ticket validation
- Driver notifications
- Password management



🛠️ Administrator Features

Administrators have centralized control over the transportation network.

Features include:

- Admin dashboard
- Operator management
- Operator approval/rejection
- Bus management
- Bus approval/rejection
- Route management
- Bus stop management
- Local route creation
- Ordered route stops
- Trip management
- Approval notifications
- Transportation data management


 🗺️ Live Bus Tracking

One of the main features of Yatayat is live bus location tracking.

The driver's device can provide location information using the browser's Geolocation API.

The system:

1. Obtains the driver's current location.
2. Sends location information to the backend.
3. Stores the latest trip location.
4. Allows passenger-facing pages to retrieve active bus locations.
5. Displays active buses on a map.

The map interface is implemented using **React Leaflet** with OpenStreetMap.

Tracking Flow
Driver Device
     │
     │ GPS / Geolocation
     ▼
React Driver Application
     │
     │ Location API
     ▼
Spring Boot Backend
     │
     ▼
Trip Location Storage
     │
     ▼
Passenger Live Tracking
     │
     ▼
Interactive Map
