import { apiFetch } from "./api";

async function seatRequest(path, options = {}) {
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

export const getTripSeats = (tripId) => seatRequest(`/api/passenger/trips/${tripId}/seats`);
export const holdTripSeats = (tripId, seatNumbers) => seatRequest(`/api/passenger/trips/${tripId}/seat-holds`, {
  method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ seatNumbers }),
});
export const releaseTripSeats = (tripId) => seatRequest(`/api/passenger/trips/${tripId}/seat-holds`, { method: "DELETE" });

function fallback(status) {
  if (status === 400) return "Check the selected seats.";
  if (status === 401) return "Your session has expired. Please log in again.";
  if (status === 403) return "Passenger access is required.";
  if (status === 404) return "This trip is no longer available.";
  if (status === 409) return "One or more selected seats are no longer available.";
  return "Unable to load seats. Please try again.";
}
