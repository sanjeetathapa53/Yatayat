import { apiFetch } from "./api";

export const TRIP_STATUSES = ["SCHEDULED", "BOARDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

export async function tripRequest(path, options = {}) {
  const response = await apiFetch(path, options);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    const error = new Error(data?.message || defaultMessage(response.status));
    error.status = response.status;
    throw error;
  }
  return data;
}

export const getOperatorLiveTrips = () => tripRequest("/api/operator/trips/live");

export const assignOperatorTrip = (tripId, assignment) =>
  tripRequest(`/api/operator/trips/${tripId}/assignment`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      busId: Number(assignment.busId),
      driverId: Number(assignment.driverId),
    }),
  });

export function handleOperatorAccess(error, navigate) {
  if (error.status === 401) {
    navigate("/login", { replace: true });
    return true;
  }
  if (error.status === 403) {
    navigate("/operator/application-status", { replace: true });
    return true;
  }
  return false;
}

export function formatTripDate(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-NP", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

export function statusLabel(status) {
  return status?.replaceAll("_", " ") || "UNKNOWN";
}

export function statusTone(status) {
  return {
    SCHEDULED: "bg-blue-100 text-blue-700",
    BOARDING: "bg-amber-100 text-amber-700",
    IN_PROGRESS: "bg-emerald-100 text-emerald-700",
    COMPLETED: "bg-slate-200 text-slate-700",
    CANCELLED: "bg-red-100 text-red-700",
  }[status] || "bg-slate-100 text-slate-700";
}

export function tripFormPayload(form) {
  return {
    routeId: Number(form.routeId),
    busId: Number(form.busId),
    driverId: Number(form.driverId),
    departureAt: form.departureAt,
    estimatedArrivalAt: form.estimatedArrivalAt,
    fare: Number(form.fare),
    boardingNotes: form.boardingNotes.trim() || null,
  };
}

function defaultMessage(status) {
  if (status === 400) return "Please check the entered trip information.";
  if (status === 404) return "The requested trip or resource was not found.";
  if (status === 409) return "The trip conflicts with an existing schedule or an ineligible resource.";
  return "The request could not be completed. Please try again.";
}
