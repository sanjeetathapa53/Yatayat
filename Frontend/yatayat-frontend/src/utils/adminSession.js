const ADMIN_STORAGE_KEYS = ["adminAuthenticated", "yatayatAdmin"];
const SESSION_EXPIRED_MESSAGE = "Session expired. Please log in again.";

export function expireAdminSession(navigate) {
  ADMIN_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
  sessionStorage.setItem("adminLoginMessage", SESSION_EXPIRED_MESSAGE);
  navigate("/admin/login", { replace: true });
}

export function consumeAdminLoginMessage() {
  const message = sessionStorage.getItem("adminLoginMessage") || "";
  sessionStorage.removeItem("adminLoginMessage");
  return message;
}
