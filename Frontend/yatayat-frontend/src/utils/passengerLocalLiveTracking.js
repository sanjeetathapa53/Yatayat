import { apiFetch } from "./api";

async function localTrackingRequest(path, options = {}) {
  const response = await apiFetch(path, options);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json().catch(() => null)
    : null;
  if (!response.ok) {
    const error = new Error(data?.message || "Local live bus information could not be loaded.");
    error.status = response.status;
    throw error;
  }
  return data;
}

export const getActiveLocalServices = (signal, routeId) => {
  const query = routeId == null ? "" : `?routeId=${encodeURIComponent(routeId)}`;
  return localTrackingRequest(`/api/passenger/local-live-services${query}`, { signal });
};

export const getActiveLocalService = (runId, signal) =>
  localTrackingRequest(
    `/api/passenger/local-live-services/${encodeURIComponent(runId)}`,
    { signal },
  );
