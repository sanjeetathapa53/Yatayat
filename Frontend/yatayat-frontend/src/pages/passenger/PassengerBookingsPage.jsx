import { useCallback, useEffect, useState } from "react";
import { Bus, Eye, Loader2, Plus, RefreshCw, Ticket, XCircle } from "lucide-react";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useLanguage } from "../../hooks/useLanguage";
import {
  cancelPassengerBooking,
  formatBookingDate,
  formatNpr,
  handleBookingSession,
  listPassengerBookings,
} from "../../utils/passengerBookings";
import { getTicketByBooking } from "../../utils/passengerTickets";

export default function PassengerBookingsPage() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [ticketLoading, setTicketLoading] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setBookings(await listPassengerBookings());
    } catch (loadError) {
      if (!handleBookingSession(loadError, navigate)) setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => { Promise.resolve().then(load); }, [load]);

  const cancel = async () => {
    setCancelling(true);
    try {
      const updated = await cancelPassengerBooking(selected.bookingReference);
      setBookings((current) => current.map((item) => item.bookingReference === updated.bookingReference
        ? { ...item, bookingStatus: updated.bookingStatus, cancelledAt: updated.cancelledAt } : item));
      setSelected(null);
      toast.success(t("passenger.tickets.bookingCancelledSuccess"));
    } catch (cancelError) {
      if (!handleBookingSession(cancelError, navigate)) {
        setError(cancelError.message);
        setSelected(null);
      }
    } finally {
      setCancelling(false);
    }
  };

  const viewTicket = async (booking) => {
    setTicketLoading(booking.bookingReference);
    try {
      const ticket = await getTicketByBooking(booking.bookingReference);
      navigate(`/passenger/tickets/${ticket.ticketNumber}`);
    } catch (ticketError) {
      toast.error(ticketError.message || t("passenger.tickets.unableToLoadTicket"));
    } finally {
      setTicketLoading("");
    }
  };

  return <PassengerLayout activePage="My Bookings" title={t("passenger.tickets.myBookings")} subtitle={t("passenger.tickets.pageSubtitle")}><div className="space-y-6">
    <div className="flex justify-end">
      <button type="button" onClick={() => navigate("/passenger/trips")} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white"><Plus size={18} /> {t("passenger.tickets.newBooking")}</button>
    </div>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : bookings.length === 0
      ? <div className="rounded-3xl border border-slate-200 bg-white py-16 text-center"><Bus className="mx-auto text-slate-300" size={48} /><h2 className="mt-4 text-xl font-black">{t("passenger.tickets.noBookingsFound")}</h2><p className="mt-2 text-sm text-slate-500">{t("passenger.tickets.noBookingsDescription")}</p></div>
      : <div className="space-y-4">{bookings.map((booking) => <BookingCard key={booking.bookingReference} booking={booking} ticketLoading={ticketLoading === booking.bookingReference} onView={() => navigate(`/passenger/bookings/${booking.bookingReference}`)} onTicket={() => viewTicket(booking)} onCancel={() => setSelected(booking)} t={t} />)}</div>}
    <button type="button" onClick={load} disabled={loading} className="flex items-center gap-2 text-sm font-black"><RefreshCw size={16} /> {t("passenger.tickets.refresh")}</button>
    {selected && <CancelModal booking={selected} cancelling={cancelling} onClose={() => setSelected(null)} onConfirm={cancel} t={t} />}
  </div></PassengerLayout>;
}

function BookingCard({ booking, ticketLoading, onView, onTicket, onCancel, t }) {
  const confirmed = booking.bookingStatus === "CONFIRMED";
  const cancellable = confirmed && new Date(booking.departureAt) > new Date();
  return <article className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
      <div className="min-w-0 flex-1">
        <div className="flex min-w-0 flex-wrap items-center gap-2"><h2 className="min-w-0 break-words text-xl font-black">{booking.origin} → {booking.destination}</h2><Status status={booking.bookingStatus} t={t} /></div>
        <p className="mt-1 min-w-0 break-words text-sm font-bold text-slate-500">{booking.routeCode} · {booking.routeName}</p>
        <div className="mt-3 grid min-w-0 grid-cols-1 gap-x-5 gap-y-1.5 text-sm sm:grid-cols-2 lg:grid-cols-3">
          <p className="min-w-0 break-words leading-5 [overflow-wrap:anywhere]"><span className="font-semibold">{t("passenger.tickets.reference")}:</span> {booking.bookingReference}</p>
          <p className="min-w-0 leading-5"><span className="font-semibold">{t("passenger.tickets.departure")}:</span> <span className="whitespace-nowrap">{formatBookingDate(booking.departureAt)}</span></p>
          <p className="min-w-0 break-words leading-5" title={booking.operatorName || undefined}><span className="font-semibold">{t("passenger.tickets.operator")}:</span> {booking.operatorName}</p>
          <p className="min-w-0 leading-5"><span className="font-semibold">{t("passenger.tickets.bus")}:</span> <span className="whitespace-nowrap">{booking.busNumber}</span></p>
          <p className="min-w-0 leading-5"><span className="font-semibold">{t("passenger.tickets.seats")}:</span> <span className="whitespace-nowrap">{booking.numberOfSeats}</span></p>
          <p className="min-w-0 leading-5"><span className="font-semibold">{t("passenger.tickets.total")}:</span> <span className="whitespace-nowrap font-semibold">{formatNpr(booking.totalFare)}</span></p>
          <p className="min-w-0 whitespace-nowrap leading-5"><span className="font-semibold">{t("passenger.tickets.booked")}:</span> <span className="whitespace-nowrap">{formatBookingDate(booking.bookedAt)}</span></p>
          {booking.cancelledAt && <p className="min-w-0 leading-5"><span className="font-semibold">{t("passenger.tickets.cancelled")}:</span> <span className="whitespace-nowrap">{formatBookingDate(booking.cancelledAt)}</span></p>}
        </div>
      </div>
      <div className="flex shrink-0 flex-wrap items-center gap-2 self-start lg:justify-end lg:self-center">
        <button type="button" onClick={onView} className="inline-flex h-11 w-auto items-center justify-center gap-2 whitespace-nowrap rounded-xl border border-slate-300 px-4 font-black"><Eye size={17} /> {t("passenger.tickets.details")}</button>
        {confirmed && <button type="button" disabled={ticketLoading} onClick={onTicket} className="inline-flex h-11 w-auto items-center justify-center gap-2 whitespace-nowrap rounded-xl bg-[#08264a] px-4 font-black text-white disabled:opacity-60"><Ticket size={17} /> {ticketLoading ? t("passenger.tickets.loading") : t("passenger.tickets.viewTicket")}</button>}
        {cancellable && <button type="button" onClick={onCancel} className="inline-flex h-11 w-auto items-center justify-center gap-2 whitespace-nowrap rounded-xl bg-red-600 px-4 font-black text-white"><XCircle size={17} /> {t("passenger.tickets.cancel")}</button>}
      </div>
    </div>
  </article>;
}

function Status({ status, t }) {
  const label = status === "CONFIRMED" ? t("passenger.tickets.statusConfirmed") : status === "PENDING_PAYMENT" ? t("passenger.tickets.statusPendingPayment") : status === "CANCELLED" ? t("passenger.tickets.statusCancelled") : status;
  return <span className={`rounded-full px-3 py-1 text-xs font-black ${status === "CONFIRMED" ? "bg-emerald-100 text-emerald-700" : status === "PENDING_PAYMENT" ? "bg-amber-100 text-amber-700" : "bg-red-100 text-red-700"}`}>{label}</span>;
}

function CancelModal({ booking, cancelling, onClose, onConfirm, t }) {
  return <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"><div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-xl"><h2 className="text-2xl font-black">{t("passenger.tickets.cancelThisBooking")}</h2><p className="mt-2 text-sm text-slate-500">{t("passenger.tickets.cancelBookingDescription", { reference: booking.bookingReference })}</p><div className="mt-6 flex justify-end gap-3"><button type="button" disabled={cancelling} onClick={onClose} className="rounded-xl border border-slate-300 px-4 py-3 font-black">{t("passenger.tickets.keepBooking")}</button><button type="button" disabled={cancelling} onClick={onConfirm} className="rounded-xl bg-red-600 px-4 py-3 font-black text-white disabled:opacity-60">{cancelling ? t("passenger.tickets.cancelling") : t("passenger.tickets.confirmCancellation")}</button></div></div></div>;
}
