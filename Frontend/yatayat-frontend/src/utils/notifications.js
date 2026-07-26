import { apiFetch } from "./api";

async function notificationRequest(path, options = {}) {
  const response = await apiFetch(path, options);
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(data?.message || "Unable to load notifications.");
    error.status = response.status;
    throw error;
  }
  return data;
}

export const getPassengerNotifications = () =>
  notificationRequest("/api/passenger/notifications");

export const getPassengerNotificationUnreadCount = () =>
  notificationRequest("/api/passenger/notifications/unread-count");

export const markPassengerNotificationRead = (notificationId) =>
  notificationRequest(`/api/passenger/notifications/${notificationId}/read`, {
    method: "PUT",
  });

export const markAllPassengerNotificationsRead = () =>
  notificationRequest("/api/passenger/notifications/read-all", {
    method: "PUT",
  });
