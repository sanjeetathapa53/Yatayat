import { apiFetch } from "./api";

async function driverTicketRequest(path, options = {}) {
  const response = await apiFetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : {};

  if (!response.ok) {
    const error = new Error(data.message || "Request could not be completed.");
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

export const validateDriverTicket = (qrPayload) =>
  driverTicketRequest("/api/driver/tickets/validate", {
    method: "POST",
    body: JSON.stringify({ qrPayload }),
  });

export async function getDriverTripManifest(scheduledTripId) {
  const response = await apiFetch(`/api/driver/trips/${encodeURIComponent(scheduledTripId)}/manifest`);
  const text = await response.text();
  const data = text ? JSON.parse(text) : {};

  if (!response.ok) {
    const error = new Error(data.message || "Unable to load trip manifest.");
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

export function manifestIdFromReference(reference) {
  const match = String(reference || "").match(/^TRIP-(\d+)$/);
  return match ? match[1] : "";
}
