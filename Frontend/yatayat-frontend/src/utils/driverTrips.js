import { apiFetch } from "./api";

async function driverTripRequest(path, options = {}) {
  const response = await apiFetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
  });
  if (response.status === 204) return null;
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    const error = new Error(data?.message || "Trip operation could not be completed.");
    error.status = response.status;
    throw error;
  }
  return data;
}

export const getCurrentDriverTrip = () => driverTripRequest("/api/driver/trips/current");
export const beginDriverTripBoarding = (scheduledTripId) =>
  driverTripRequest(`/api/driver/trips/${encodeURIComponent(scheduledTripId)}/boarding`, { method: "POST" });
export const startDriverTrip = (scheduledTripId) =>
  driverTripRequest(`/api/driver/trips/${encodeURIComponent(scheduledTripId)}/start`, { method: "POST" });
export const finishDriverTrip = (scheduledTripId) =>
  driverTripRequest(`/api/driver/trips/${encodeURIComponent(scheduledTripId)}/finish`, { method: "POST" });
export const updateDriverTripLocation = (scheduledTripId, location, options = {}) =>
  driverTripRequest(`/api/driver/trips/${encodeURIComponent(scheduledTripId)}/location`, {
    method: "PUT",
    body: JSON.stringify(location),
    signal: options.signal,
  });

export function tripStatusLabel(status) {
  return {
    SCHEDULED: "Scheduled",
    BOARDING: "Boarding",
    IN_PROGRESS: "On the way",
    COMPLETED: "Completed",
    CANCELLED: "Cancelled",
  }[status] || "Not started";
}

export function tripStatusTone(status) {
  return {
    SCHEDULED: "bg-blue-100 text-blue-700",
    BOARDING: "bg-amber-100 text-amber-700",
    IN_PROGRESS: "bg-emerald-100 text-emerald-700",
    COMPLETED: "bg-slate-200 text-slate-700",
    CANCELLED: "bg-red-100 text-red-700",
  }[status] || "bg-slate-100 text-slate-700";
}
