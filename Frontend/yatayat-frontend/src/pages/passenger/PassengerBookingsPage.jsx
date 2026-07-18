import { useCallback, useEffect, useState } from "react";
import { Bus, Eye, Loader2, Plus, RefreshCw, Ticket, XCircle } from "lucide-react";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
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
      toast.success("Booking cancelled successfully.");
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
      toast.error(ticketError.message || "Unable to load your ticket.");
    } finally {
      setTicketLoading("");
    }
  };

  return <PassengerLayout activePage="My Bookings"><div className="space-y-6">
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
      <div>
        <h1 className="text-3xl font-black text-slate-900">My Bookings</h1>
        <p className="mt-1 text-sm text-slate-500">View and manage your scheduled-trip bookings.</p>
      </div>
      <button onClick={() => navigate("/passenger/trips")} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white"><Plus size={18} /> New Booking</button>
    </header>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : bookings.length === 0
      ? <div className="rounded-3xl border border-slate-200 bg-white py-16 text-center"><Bus className="mx-auto text-slate-300" size={48} /><h2 className="mt-4 text-xl font-black">No bookings found</h2><p className="mt-2 text-sm text-slate-500">Search for a scheduled trip to create your first booking.</p></div>
      : <div className="space-y-4">{bookings.map((booking) => <BookingCard key={booking.bookingReference} booking={booking} ticketLoading={ticketLoading === booking.bookingReference} onView={() => navigate(`/passenger/bookings/${booking.bookingReference}`)} onTicket={() => viewTicket(booking)} onCancel={() => setSelected(booking)} />)}</div>}
    <button onClick={load} disabled={loading} className="flex items-center gap-2 text-sm font-black"><RefreshCw size={16} /> Refresh</button>
    {selected && <CancelModal booking={selected} cancelling={cancelling} onClose={() => setSelected(null)} onConfirm={cancel} />}
  </div></PassengerLayout>;
}

function BookingCard({ booking, ticketLoading, onView, onTicket, onCancel }) {
  const confirmed = booking.bookingStatus === "CONFIRMED";
  const cancellable = confirmed && new Date(booking.departureAt) > new Date();
  return <article className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
    <div className="flex flex-col justify-between gap-5 lg:flex-row">
      <div>
        <div className="flex flex-wrap items-center gap-3"><h2 className="text-xl font-black">{booking.origin} → {booking.destination}</h2><Status status={booking.bookingStatus} /></div>
        <p className="mt-1 font-bold text-slate-500">{booking.routeCode} · {booking.routeName}</p>
        <div className="mt-4 grid grid-cols-1 gap-2 text-sm sm:grid-cols-2 lg:grid-cols-3">
          <p><b>Reference:</b> {booking.bookingReference}</p>
          <p><b>Departure:</b> {formatBookingDate(booking.departureAt)}</p>
          <p><b>Operator:</b> {booking.operatorName}</p>
          <p><b>Bus:</b> {booking.busNumber}</p>
          <p><b>Seats:</b> {booking.numberOfSeats}</p>
          <p><b>Total:</b> {formatNpr(booking.totalFare)}</p>
          <p><b>Booked:</b> {formatBookingDate(booking.bookedAt)}</p>
          {booking.cancelledAt && <p><b>Cancelled:</b> {formatBookingDate(booking.cancelledAt)}</p>}
        </div>
      </div>
      <div className="flex shrink-0 flex-wrap gap-2">
        <button onClick={onView} className="flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-3 font-black"><Eye size={17} /> Details</button>
        {confirmed && <button disabled={ticketLoading} onClick={onTicket} className="flex items-center gap-2 rounded-xl bg-[#08264a] px-4 py-3 font-black text-white disabled:opacity-60"><Ticket size={17} /> {ticketLoading ? "Loading..." : "View Ticket"}</button>}
        {cancellable && <button onClick={onCancel} className="flex items-center gap-2 rounded-xl bg-red-600 px-4 py-3 font-black text-white"><XCircle size={17} /> Cancel</button>}
      </div>
    </div>
  </article>;
}

function Status({ status }) {
  return <span className={`rounded-full px-3 py-1 text-xs font-black ${status === "CONFIRMED" ? "bg-emerald-100 text-emerald-700" : status === "PENDING_PAYMENT" ? "bg-amber-100 text-amber-700" : "bg-red-100 text-red-700"}`}>{status}</span>;
}

function CancelModal({ booking, cancelling, onClose, onConfirm }) {
  return <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"><div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-xl"><h2 className="text-2xl font-black">Cancel this booking?</h2><p className="mt-2 text-sm text-slate-500">Booking {booking.bookingReference} will remain in your history with a cancelled status.</p><div className="mt-6 flex justify-end gap-3"><button disabled={cancelling} onClick={onClose} className="rounded-xl border border-slate-300 px-4 py-3 font-black">Keep Booking</button><button disabled={cancelling} onClick={onConfirm} className="rounded-xl bg-red-600 px-4 py-3 font-black text-white disabled:opacity-60">{cancelling ? "Cancelling..." : "Confirm Cancellation"}</button></div></div></div>;
}
