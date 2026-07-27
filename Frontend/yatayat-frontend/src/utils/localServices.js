import { apiFetch } from "./api";

export const LOCAL_SERVICE_STATUSES = ["PLANNED", "READY", "IN_SERVICE", "COMPLETED", "CANCELLED"];

export async function localServiceRequest(path, options = {}) {
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

export const getCurrentDriverLocalService = () =>
  localServiceRequest("/api/driver/local-services/current");

export const startDriverLocalService = (id) =>
  localServiceRequest(`/api/driver/local-services/${encodeURIComponent(id)}/start`, {
    method: "POST",
  });

export const finishDriverLocalService = (id) =>
  localServiceRequest(`/api/driver/local-services/${encodeURIComponent(id)}/finish`, {
    method: "POST",
  });

export const updateDriverLocalServiceLocation = (id, location, options = {}) =>
  localServiceRequest(`/api/driver/local-services/${encodeURIComponent(id)}/location`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(location),
    signal: options.signal,
  });

export function localServicePayload(form) {
  return {
    routeId: Number(form.routeId),
    busId: Number(form.busId),
    driverId: Number(form.driverId),
    serviceDate: form.serviceDate,
    plannedStartTime: form.plannedStartTime,
    plannedEndTime: form.plannedEndTime,
    notes: form.notes.trim() || null,
  };
}

export function handleOperatorLocalAccess(error, navigate) {
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

export function handleDriverLocalAccess(error, navigate) {
  if (error.status === 401) {
    navigate("/login", { replace: true });
    return true;
  }
  return false;
}

export function serviceStatusLabel(status) {
  return status?.replaceAll("_", " ") || "UNKNOWN";
}

export function serviceStatusTone(status) {
  return {
    PLANNED: "bg-blue-100 text-blue-700",
    READY: "bg-amber-100 text-amber-700",
    IN_SERVICE: "bg-emerald-100 text-emerald-700",
    COMPLETED: "bg-slate-200 text-slate-700",
    CANCELLED: "bg-red-100 text-red-700",
  }[status] || "bg-slate-100 text-slate-700";
}

export function formatServiceDate(date, start, end) {
  if (!date) return "—";
  const formatted = new Intl.DateTimeFormat("en-NP", { dateStyle: "medium" }).format(new Date(`${date}T00:00:00`));
  return `${formatted} · ${start || "--"} - ${end || "--"}`;
}

function defaultMessage(status) {
  if (status === 400) return "Please check the local service details.";
  if (status === 404) return "Local service or selected resource was not found.";
  if (status === 409) return "This local service conflicts with an existing assignment.";
  return "Local service request could not be completed.";
}
