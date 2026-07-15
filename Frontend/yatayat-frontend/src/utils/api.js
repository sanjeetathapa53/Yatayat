export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export function apiFetch(path, options = {}) {
  return fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    ...options,
  });
}
