import { apiFetch } from "./api";

async function bookingRequest(path, options = {}) {
  const response = await apiFetch(path, options);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    const error = new Error(data?.message || fallback(response.status));
    error.status = response.status;
    throw error;
  }
  return data;
}

export const createPassengerBooking = (payload) => bookingRequest("/api/passenger/bookings", {
  method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload),
});
export const listPassengerBookings = () => bookingRequest("/api/passenger/bookings");
export const getPassengerBooking = (reference) => bookingRequest(`/api/passenger/bookings/${encodeURIComponent(reference)}`);
export const cancelPassengerBooking = (reference) => bookingRequest(`/api/passenger/bookings/${encodeURIComponent(reference)}/cancel`, { method: "POST" });
export const payPassengerBookingWithWallet = (reference) => bookingRequest(`/api/passenger/bookings/${encodeURIComponent(reference)}/pay/wallet`, { method: "POST" });

export function handleBookingSession(error, navigate) {
  if (error.status === 401) { navigate("/login", { replace: true }); return true; }
  return false;
}
export function formatBookingDate(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-NP", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
export function formatNpr(value) { return `NPR ${Number(value || 0).toLocaleString()}`; }
function fallback(status) {
  if (status === 400) return "Please check the booking information.";
  if (status === 403) return "Passenger access is required.";
  if (status === 404) return "Booking not found.";
  if (status === 409) return "This booking request cannot be completed.";
  return "Unable to complete the request. Please try again.";
}
