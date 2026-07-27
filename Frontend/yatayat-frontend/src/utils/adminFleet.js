import { apiFetch } from "./api";

async function fleetRequest(path, signal) {
  const response = await apiFetch(path, { signal });
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json().catch(() => null)
    : null;
  if (!response.ok) {
    const error = new Error(data?.message || "Live monitoring data could not be loaded.");
    error.status = response.status;
    throw error;
  }
  return data;
}

export const getAdminLiveFleet = (signal) =>
  fleetRequest("/api/admin/live-fleet", signal);

export const getAdminLiveFleetTrip = (tripId, signal) =>
  fleetRequest(`/api/admin/live-fleet/${encodeURIComponent(tripId)}`, signal);

export const getAdminLiveMonitoring = (signal) =>
  fleetRequest("/api/admin/live-monitoring", signal);
