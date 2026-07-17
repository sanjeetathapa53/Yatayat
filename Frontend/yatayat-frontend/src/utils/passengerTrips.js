import { apiFetch } from "./api";

export async function passengerTripRequest(path) {
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

export function handlePassengerSession(error, navigate) {
  if (error.status === 401) {
    navigate("/login", { replace: true });
    return true;
  }
  return false;
}

export function formatPassengerTripDate(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-NP", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function fallback(status) {
  if (status === 400) return "Enter a valid origin and destination.";
  if (status === 403) return "Passenger access is required.";
  if (status === 404) return "Scheduled trip not found.";
  return "Unable to load scheduled trips. Please try again.";
}
