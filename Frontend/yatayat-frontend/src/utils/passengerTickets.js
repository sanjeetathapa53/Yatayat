import { apiFetch } from "./api";

async function ticketRequest(path, options = {}) {
  const response = await apiFetch(path, options);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    const error = new Error(data?.message || fallback(response.status));
    error.status = response.status;
    throw error;
  }
  return data;
}

export const getTicketByBooking = (bookingReference) =>
  ticketRequest(`/api/passenger/bookings/${encodeURIComponent(bookingReference)}/ticket`);

export const getTicketByNumber = (ticketNumber) =>
  ticketRequest(`/api/passenger/tickets/${encodeURIComponent(ticketNumber)}`);

export const sendTicketEmail = (ticketNumber) =>
  ticketRequest(`/api/passenger/tickets/${encodeURIComponent(ticketNumber)}/email`, { method: "POST" });

export async function downloadTicketPdf(ticketNumber) {
  const response = await apiFetch(`/api/passenger/tickets/${encodeURIComponent(ticketNumber)}/pdf`);
  if (!response.ok) throw new Error(fallback(response.status));
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `Yatayat-Ticket-${ticketNumber}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export function handleTicketSession(error, navigate) {
  if (error.status === 401) {
    window.dispatchEvent(new Event("yatayat-auth-cleared"));
    navigate("/login", {
      replace: true,
      state: { message: "Session expired. Please log in again." },
    });
    return true;
  }
  return false;
}

function fallback(status) {
  if (status === 401) return "Please log in to view your ticket.";
  if (status === 403) return "Passenger access is required.";
  if (status === 404) return "Ticket not found.";
  if (status === 409) return "Complete payment before viewing the ticket.";
  return "Unable to load your ticket.";
}
