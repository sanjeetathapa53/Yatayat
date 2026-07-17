import { apiFetch } from "../utils/api";

const AUTH_STORAGE_KEYS = [
  "yatayatUser",
  "yatayatAdmin",
  "adminAuthenticated",
  "loginTime",
  "driverApplicationStatus",
  "operatorApplicationStatus",
];

export async function logoutUser({ admin = false } = {}) {
  try {
    const response = await apiFetch(
      admin ? "/api/admin/auth/logout" : "/api/auth/logout",
      { method: "POST" }
    );

    const contentType = response.headers.get("content-type") || "";
    const data = contentType.includes("application/json")
      ? await response.json().catch(() => null)
      : null;

    return { ok: response.ok, status: response.status, data };
  } catch (error) {
    return { ok: false, status: 0, error };
  } finally {
    AUTH_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
    window.dispatchEvent(new Event("yatayat-auth-cleared"));
  }
}
