import { apiFetch } from "./api";

export const DRIVER_NOTIFICATIONS_CHANGED = "driver-notifications-changed";

export async function fetchDriverNotifications(unreadOnly = false) {
  const response = await apiFetch(`/api/driver/notifications?unreadOnly=${unreadOnly}`);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || "Unable to load notifications.");
  return Array.isArray(data) ? data : [];
}

export async function fetchDriverUnreadCount() {
  const response = await apiFetch("/api/driver/notifications/unread-count");
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || "Unable to load unread notifications.");
  return Number(data.unreadCount) || 0;
}

export async function markDriverNotificationRead(id) {
  const response = await apiFetch(`/api/driver/notifications/${id}/read`, { method: "PUT" });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || "Unable to mark notification as read.");
  notifyDriverNotificationsChanged();
  return data;
}

export async function markAllDriverNotificationsRead() {
  const response = await apiFetch("/api/driver/notifications/read-all", { method: "PUT" });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || "Unable to mark notifications as read.");
  notifyDriverNotificationsChanged();
  return data;
}

export function notifyDriverNotificationsChanged() {
  window.dispatchEvent(new Event(DRIVER_NOTIFICATIONS_CHANGED));
}
