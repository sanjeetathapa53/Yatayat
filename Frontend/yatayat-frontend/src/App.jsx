import { Routes, Route } from "react-router-dom";

import LandingPage from "./pages/LandingPage";
import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";
import AdminLoginPage from "./pages/auth/AdminLoginPage";
import GoogleSuccessPage from "./pages/auth/GoogleSuccessPage";

import PassengerDashboard from "./pages/passenger/PassengerDashboard";
import FarePassPage from "./pages/passenger/FarePassPage";
import WalletPage from "./pages/passenger/WalletPage";
import NotificationsPage from "./pages/passenger/NotificationsPage";
import ProfilePage from "./pages/passenger/ProfilePage";
import BookTicketPage from "./pages/passenger/ookTicketPage";
import SeatSelectionPage from "./pages/passenger/SeatSelectionPage";
import BookingSummaryPage from "./pages/passenger/BookingSummaryPage";
import PaymentPage from "./pages/passenger/PaymentPage";
import TicketPage from "./pages/passenger/TicketPage";
import MyBookingsPage from "./pages/passenger/MyBookingsPage";
import HistoryPage from "./pages/passenger/HistoryPage";
import SettingsPage from "./pages/passenger/SettingsPage";

import DriverApplicationPage from "./pages/driver/DriverApplicationPage";
import DriverApplicationStatusPage from "./pages/driver/DriverApplicationStatusPage";
import DriverDashboard from "./pages/driver/DriverDashboard";
import DriverScannerPage from "./pages/driver/DriverScannerPage";
import TripManagementPage from "./pages/driver/TripManagementPage";
import DriverPassengerListPage from "./pages/driver/DriverPassengerListPage";
import DriverNotificationsPage from "./pages/driver/DriverNotificationsPage";
import DriverProfilePage from "./pages/driver/DriverProfilePage";
import DriverSettingsPage from "./pages/driver/DriverSettingsPage";
import TripSummaryPage from "./pages/driver/TripSummaryPage";

import AdminDashboard from "./pages/admin/AdminDashboard";
import DriverApplicationsPage from "./pages/admin/DriverApplicationsPage";
import DriverApplicationDetailsPage from "./pages/admin/DriverApplicationDetailsPage";

import OperatorApplicationPage from "./pages/operator/OperatorApplicationPage";
import OperatorApplicationStatusPage from "./pages/operator/OperatorApplicationStatusPage";
import OperatorDashboard from "./pages/operator/OperatorDashboard";
import OperatorBusesPage from "./pages/operator/OperatorBusesPage";
import OperatorBusRegistrationPage from "./pages/operator/OperatorBusRegistrationPage";
import OperatorBusDetailsPage from "./pages/operator/OperatorBusDetailsPage";
import OperatorDriversPage from "./pages/operator/OperatorDriversPage";


import RoutesPage from "./pages/routes/RoutesPage";
import LiveTrackingPage from "./pages/tracking/LiveTrackingPage";

import ProtectedRoute from "./components/auth/ProtectedRoute";
import AdminProtectedRoute from "./components/auth/AdminProtectedRoute";
import TransportOperatorsPage from "./pages/admin/TransportOperatorsPage";
import AdminBusesPage from "./pages/admin/AdminBusesPage";

import ForgotPasswordPage from "./pages/auth/ForgotPasswordPage";

import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

function Page({ title }) {
  return (
    <div className="min-h-screen bg-slate-100 pt-28 text-center">
      <h1 className="text-4xl font-black text-[#08264a]">{title}</h1>
    </div>
  );
}

export default function App() {
  return (
    <>
      <Routes>
        {/* PUBLIC ROUTES */}

        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/admin/login" element={<AdminLoginPage />} />
        <Route path="/google-success" element={<GoogleSuccessPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />

        {/* PASSENGER ROUTES */}

        <Route
          path="/passenger/dashboard"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <PassengerDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/profile"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <ProfilePage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/wallet"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <WalletPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/notifications"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <NotificationsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/fare-pass"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <FarePassPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/book-ticket"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <BookTicketPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/seat-selection"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <SeatSelectionPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/booking-summary"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <BookingSummaryPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/payment"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <PaymentPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/ticket"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <TicketPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/my-bookings"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <MyBookingsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/history"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <HistoryPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/settings"
          element={
            <ProtectedRoute allowedRole="PASSENGER">
              <SettingsPage />
            </ProtectedRoute>
          }
        />

        {/* DRIVER APPLICATION ROUTES */}

        <Route
          path="/driver/application"
          element={<DriverApplicationPage />}
        />

        <Route
          path="/driver/application-status"
          element={<DriverApplicationStatusPage />}
        />

        {/* PROTECTED DRIVER ROUTES */}

        <Route
          path="/driver/dashboard"
          element={
            <ProtectedRoute allowedRole="DRIVER">
              <DriverDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/driver/trip"
          element={
            <ProtectedRoute allowedRole="DRIVER">
              <TripManagementPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/driver/scanner"
          element={
            <ProtectedRoute allowedRole="DRIVER">
              <DriverScannerPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/driver/passengers"
          element={
            <ProtectedRoute allowedRole="DRIVER">
              <DriverPassengerListPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/driver/notifications"
          element={
            <ProtectedRoute allowedRole="DRIVER">
              <DriverNotificationsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/driver/profile"
          element={
            <ProtectedRoute allowedRole="DRIVER">
              <DriverProfilePage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/driver/settings"
          element={
            <ProtectedRoute allowedRole="DRIVER">
              <DriverSettingsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/driver/trip-summary"
          element={
            <ProtectedRoute allowedRole="DRIVER">
              <TripSummaryPage />
            </ProtectedRoute>
          }
        />

        {/* PROTECTED ADMIN ROUTES */}

        <Route element={<AdminProtectedRoute />}>
          <Route
            path="/admin/dashboard"
            element={<AdminDashboard />}
          />
            <Route
    path="/admin/driver-applications"
    element={<DriverApplicationsPage />}
  />
          <Route
    path="/admin/driver-applications/:id"
    element={<DriverApplicationDetailsPage />}
  />
  <Route
  path="/admin/operators"
  element={<TransportOperatorsPage />}
/>
  <Route
    path="/admin/buses"
    element={<AdminBusesPage />}
  />
        </Route>

        {/* PUBLIC ROUTE AND TRACKING PAGES */}

        <Route path="/routes" element={<RoutesPage />} />
        <Route path="/track-bus" element={<LiveTrackingPage />} />
        <Route path="/track-bus/:id" element={<LiveTrackingPage />} />

        <Route
  path="/operator/application"
  element={<OperatorApplicationPage />}
/>

<Route
  path="/operator/application-status"
  element={<OperatorApplicationStatusPage />}
/>

<Route
  path="/operator/dashboard"
  element={
    <ProtectedRoute allowedRole="OPERATOR">
      <OperatorDashboard />
    </ProtectedRoute>
  }
/>

{[
  ["/operator/buses", <OperatorBusesPage />],
  ["/operator/buses/register", <OperatorBusRegistrationPage />],
  ["/operator/buses/:id", <OperatorBusDetailsPage />],
  ["/operator/drivers", <OperatorDriversPage />],
].map(([path, page]) => (
  <Route key={path} path={path} element={<ProtectedRoute allowedRole="OPERATOR">{page}</ProtectedRoute>} />
))}

        <Route
          path="/bookings"
          element={<Page title="Out-of-Valley Bookings" />}
        />

        <Route path="/help" element={<Page title="Help Center" />} />
        <Route path="/privacy" element={<Page title="Privacy Policy" />} />
        <Route path="/terms" element={<Page title="Terms of Service" />} />

        {/* FALLBACK */}

        <Route path="*" element={<Page title="Page Not Found" />} />
      </Routes>

      <ToastContainer
        position="top-right"
        autoClose={2500}
      />
    </>
  );
}
