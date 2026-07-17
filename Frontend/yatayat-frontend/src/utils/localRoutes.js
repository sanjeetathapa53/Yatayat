import { apiFetch } from "./api";

export async function localRouteRequest(path) {
  const response = await apiFetch(path);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    const error = new Error(data?.message || fallback(response.status));
    error.status = response.status;
    throw error;
  }
  return data;
}

function fallback(status) {
  if (status === 400) return "Enter a valid origin and destination.";
  if (status === 401) return "Your session has expired. Please log in again.";
  if (status === 403) return "Passenger access is required.";
  if (status === 404) return "Local route not found.";
  return "The local route service is unavailable. Please try again.";
}
