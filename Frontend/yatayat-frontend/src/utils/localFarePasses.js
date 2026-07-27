import { apiFetch } from "./api";

async function request(path, options = {}) {
  const response = await apiFetch(path, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {}),
    },
  });
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json()
    : null;
  if (!response.ok) {
    const error = new Error(data?.message || "Local fare request could not be completed.");
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

export const getLocalFarePasses = () =>
  request("/api/passenger/local-fare-passes");

export const getLocalFareQuote = (selection) =>
  request("/api/passenger/local-fare-passes/quote", {
    method: "POST",
    body: JSON.stringify(selection),
  });

export const purchaseLocalFarePass = (selection) =>
  request("/api/passenger/local-fare-passes", {
    method: "POST",
    body: JSON.stringify(selection),
  });

export const validateDriverLocalFarePass = (qrPayload) =>
  request("/api/driver/local-fare-passes/validate", {
    method: "POST",
    body: JSON.stringify({ qrPayload }),
  });
