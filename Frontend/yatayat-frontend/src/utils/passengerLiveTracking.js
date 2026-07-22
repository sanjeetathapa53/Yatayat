import { apiFetch } from "./api";

async function trackingRequest(path, options = {}) {
  const response = await apiFetch(path, options);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json().catch(() => null)
    : null;

  if (!response.ok) {
    const error = new Error(data?.message || "Live bus information could not be loaded.");
    error.status = response.status;
    throw error;
  }
  return data;
}

export const getActiveTripLocations = (signal) =>
  trackingRequest("/api/passenger/live-trips", { signal });

export const getTripLocation = (tripId, signal) =>
  trackingRequest(`/api/passenger/live-trips/${encodeURIComponent(tripId)}`, { signal });
