import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, CheckCircle2, CreditCard, Loader2, Wallet, XCircle } from "lucide-react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { apiFetch } from "../../utils/api";
import {
  cancelPassengerBooking,
  formatBookingDate,
  formatNpr,
  getPassengerBooking,
  handleBookingSession,
  payPassengerBookingWithWallet,
} from "../../utils/passengerBookings";

export default function PassengerBookingDetailsPage() {
  const { bookingReference } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useMemo(() => {
    const stored = localStorage.getItem("yatayatUser");
    return stored ? JSON.parse(stored) : {};
  }, []);
  const [booking, setBooking] = useState(null);
  const [walletBalance, setWalletBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [walletError, setWalletError] = useState("");
  const [showCancel, setShowCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [paying, setPaying] = useState(false);
  const [now, setNow] = useState(Date.now());

  const loadBooking = async () => {
    const data = await getPassengerBooking(bookingReference);
    setBooking(data);
    return data;
  };

  const loadWallet = async () => {
    if (!user?.id) return;
    const response = await apiFetch(`/api/wallet/balance/${user.id}`);
    if (!response.ok) throw new Error("Unable to load wallet balance.");
    const text = await response.text();
    setWalletBalance(Number(text) || 0);
  };

  useEffect(() => {
    let active = true;
    loadBooking()
      .then(() => loadWallet().catch((loadError) => {
        if (!active) return;
        if (!handleBookingSession(loadError, navigate)) setWalletError(loadError.message);
      }))
      .catch((loadError) => {
        if (active && !handleBookingSession(loadError, navigate)) {
          setError(loadError.message);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [bookingReference, navigate]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const remainingSeconds = Math.max(
    0,
    booking?.paymentHoldExpiresAt
      ? Math.floor((new Date(booking.paymentHoldExpiresAt).getTime() - now) / 1000)
      : 0
  );
  const pendingPayment = booking?.bookingStatus === "PENDING_PAYMENT";
  const insufficientBalance = pendingPayment && walletBalance !== null && walletBalance < Number(booking.totalFare || 0);
  const cancellable = ["PENDING_PAYMENT", "CONFIRMED"].includes(booking?.bookingStatus)
    && new Date(booking.departureAt) > new Date();
  const rows = booking ? [
    ["Passenger", booking.passengerName],
    ["Phone", booking.passengerPhone],
    ["Route", `${booking.routeCode} - ${booking.routeName}`],
    ["Trip type", booking.tripType === "LOCAL" ? "Local" : "Outside Valley"],
    ["Journey", `${booking.origin} to ${booking.destination}`],
    ["Operator", booking.operatorName],
    ["Bus", booking.busNumber],
    ["Departure", formatBookingDate(booking.departureAt)],
    ["Estimated arrival", formatBookingDate(booking.estimatedArrivalAt)],
    ["Selected seats", booking.seatNumbers?.length ? booking.seatNumbers.join(", ") : `${booking.numberOfSeats} seat(s)`],
    ["Fare per seat", formatNpr(booking.farePerSeat)],
    ["Total fare", formatNpr(booking.totalFare)],
    ["Payment status", booking.paymentStatus || (pendingPayment ? "PENDING" : "Not paid")],
    ["Payment method", booking.paymentMethod || "Not paid"],
    ["Paid amount", booking.paidAmount ? formatNpr(booking.paidAmount) : "Not paid"],
    ["Paid at", booking.paidAt ? formatBookingDate(booking.paidAt) : "Not paid"],
    ["Payment reference", booking.transactionReference || "Not generated"],
    ["Booked at", formatBookingDate(booking.bookedAt)],
    ["Cancelled at", booking.cancelledAt ? formatBookingDate(booking.cancelledAt) : "Not cancelled"],
    ["Boarding notes", booking.boardingNotes || "No boarding notes provided"],
  ] : [];

  const cancel = async () => {
    setCancelling(true);
    try {
      setBooking(await cancelPassengerBooking(bookingReference));
      setShowCancel(false);
      toast.success("Booking cancelled successfully.");
    } catch (cancelError) {
      if (!handleBookingSession(cancelError, navigate)) {
        setError(cancelError.message);
        setShowCancel(false);
      }
    } finally {
      setCancelling(false);
    }
  };

  const pay = async () => {
    if (paying || insufficientBalance) return;
    setPaying(true);
    setError("");
    try {
      const result = await payPassengerBookingWithWallet(bookingReference);
      toast.success(result.bookingStatus === "CONFIRMED" ? "Payment successful." : "Payment already completed.");
      await loadBooking();
      setWalletBalance(Number(result.walletBalance || 0));
    } catch (paymentError) {
      if (!handleBookingSession(paymentError, navigate)) setError(paymentError.message);
    } finally {
      setPaying(false);
    }
  };

  return <PassengerLayout activePage="My Bookings"><div className="mx-auto max-w-5xl space-y-6">
    <button onClick={() => navigate("/passenger/bookings")} className="flex items-center gap-2 text-sm font-black"><ArrowLeft size={17} /> Back to My Bookings</button>
    {location.state?.created && <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-amber-900"><div className="flex items-center gap-2 font-black"><CheckCircle2 size={20} /> Seats reserved. Payment is pending.</div><p className="mt-2 text-sm">Pay with your Yatayat Wallet before the hold expires to confirm these seats.</p></div>}
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : booking && <>
      <header className="flex flex-col justify-between gap-4 rounded-3xl bg-[#08264a] p-7 text-white sm:flex-row"><div><p className="text-sm font-bold text-blue-200">Booking reference</p><h1 className="mt-1 break-all text-3xl font-black">{booking.bookingReference}</h1><p className="mt-2 text-slate-300">{booking.origin} to {booking.destination}</p></div><Status value={booking.bookingStatus} /></header>
      {pendingPayment && <PaymentPanel booking={booking} walletBalance={walletBalance} walletError={walletError} remainingSeconds={remainingSeconds} insufficientBalance={insufficientBalance} paying={paying} onPay={pay} />}
      {booking.bookingStatus === "CONFIRMED" && <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-900"><div className="flex items-center gap-2 font-black"><CheckCircle2 size={20} /> Booking confirmed</div><p className="mt-2 text-sm">Wallet payment is recorded and your selected seats are confirmed.</p></div>}
      <section className="grid grid-cols-1 gap-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2">{rows.map(([label, value]) => <div key={label} className="rounded-2xl bg-slate-50 p-4"><p className="text-xs font-black uppercase tracking-wide text-slate-500">{label}</p><p className="mt-2 break-words font-bold text-slate-800">{value}</p></div>)}</section>
      {cancellable && <div className="flex justify-end rounded-2xl border border-slate-200 bg-white p-5"><button onClick={() => setShowCancel(true)} className="flex shrink-0 items-center justify-center gap-2 rounded-xl bg-red-600 px-5 py-3 font-black text-white"><XCircle size={17} /> Cancel Booking</button></div>}
    </>}
    {showCancel && <CancelModal cancelling={cancelling} onClose={() => setShowCancel(false)} onConfirm={cancel} />}
  </div></PassengerLayout>;
}

function PaymentPanel({ booking, walletBalance, walletError, remainingSeconds, insufficientBalance, paying, onPay }) {
  const expired = booking.paymentHoldExpiresAt && remainingSeconds <= 0;
  return <section className="rounded-3xl border border-blue-200 bg-blue-50 p-6">
    <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
      <div>
        <p className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-blue-700"><Wallet size={16} /> Wallet payment</p>
        <h2 className="mt-2 text-2xl font-black text-[#08264a]">{formatNpr(booking.totalFare)}</h2>
        <p className="mt-1 text-sm font-semibold text-blue-900">Balance: {walletError || (walletBalance === null ? "Loading..." : formatNpr(walletBalance))}</p>
        <p className={`mt-1 text-sm font-bold ${expired ? "text-red-700" : "text-blue-900"}`}>Hold time left: {booking.paymentHoldExpiresAt ? formatTime(remainingSeconds) : "Not available"}</p>
      </div>
      <div className="flex flex-col gap-3 sm:flex-row lg:flex-col">
        <button onClick={onPay} disabled={paying || insufficientBalance || expired || walletBalance === null} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white disabled:opacity-50"><CreditCard size={17} /> {paying ? "Processing..." : "Pay with Wallet"}</button>
        {insufficientBalance && <Link to="/wallet" className="rounded-xl border border-blue-300 bg-white px-6 py-3 text-center font-black text-[#08264a]">Top up wallet</Link>}
      </div>
    </div>
    {insufficientBalance && <p className="mt-4 text-sm font-bold text-red-700">Your wallet balance is not enough for this booking.</p>}
    {expired && <p className="mt-4 text-sm font-bold text-red-700">This seat hold has expired. Select seats again before payment.</p>}
  </section>;
}

function Status({ value }) {
  const style = value === "CONFIRMED" ? "bg-emerald-400/20 text-emerald-100" : value === "PENDING_PAYMENT" ? "bg-amber-400/20 text-amber-100" : "bg-red-400/20 text-red-100";
  return <span className={`self-start rounded-full px-4 py-2 text-xs font-black ${style}`}>{value}</span>;
}
function CancelModal({ cancelling, onClose, onConfirm }) {
  return <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"><div className="w-full max-w-md rounded-3xl bg-white p-6"><h2 className="text-2xl font-black">Cancel this booking?</h2><p className="mt-2 text-sm text-slate-500">It will remain in your history with a cancelled status and its seats will be released.</p><div className="mt-6 flex justify-end gap-3"><button disabled={cancelling} onClick={onClose} className="rounded-xl border border-slate-300 px-4 py-3 font-black">Keep Booking</button><button disabled={cancelling} onClick={onConfirm} className="rounded-xl bg-red-600 px-4 py-3 font-black text-white disabled:opacity-60">{cancelling ? "Cancelling..." : "Confirm Cancellation"}</button></div></div></div>;
}
function formatTime(seconds) {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${minutes}:${String(rest).padStart(2, "0")}`;
}
