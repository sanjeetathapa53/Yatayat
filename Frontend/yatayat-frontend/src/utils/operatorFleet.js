import { apiFetch } from "./api";

async function fleetRequest(path, signal) {
  const response = await apiFetch(path, { signal });
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json().catch(() => null)
    : null;
  if (!response.ok) {
    const error = new Error(data?.message || "Live fleet information could not be loaded.");
    error.status = response.status;
    throw error;
  }
  return data;
}

export const getOperatorFleetLocations = (signal) =>
  fleetRequest("/api/operator/live-fleet", signal);

export const getOperatorFleetTrip = (tripId, signal) =>
  fleetRequest(`/api/operator/live-fleet/${encodeURIComponent(tripId)}`, signal);
