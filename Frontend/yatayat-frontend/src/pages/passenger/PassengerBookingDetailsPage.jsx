import { useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowLeft,
  Armchair,
  Bus,
  CalendarClock,
  CheckCircle2,
  CreditCard,
  Eye,
  EyeOff,
  Loader2,
  ReceiptText,
  ShieldCheck,
  Wallet,
  X,
  XCircle,
} from "lucide-react";
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

const PAYMENT_STEPS = {
  SUMMARY: "summary",
  PIN: "pin",
  SUCCESS: "success",
};

export default function PassengerBookingDetailsPage() {
  const { bookingReference } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const pinInputRef = useRef(null);
  const user = useMemo(() => {
    const stored = localStorage.getItem("yatayatUser");
    return stored ? JSON.parse(stored) : {};
  }, []);
  const [booking, setBooking] = useState(null);
  const [walletBalance, setWalletBalance] = useState(null);
  const [pinStatus, setPinStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [walletError, setWalletError] = useState("");
  const [showCancel, setShowCancel] = useState(false);
  const [paymentStep, setPaymentStep] = useState(null);
  const [walletPin, setWalletPin] = useState("");
  const [paymentError, setPaymentError] = useState("");
  const [pinVisible, setPinVisible] = useState(false);
  const [pinShake, setPinShake] = useState(false);
  const [paymentResult, setPaymentResult] = useState(null);
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
    const pinResponse = await apiFetch(`/api/wallet/pin-status/${user.id}`);
    if (!pinResponse.ok) throw new Error("Unable to load wallet PIN status.");
    setPinStatus(await pinResponse.text());
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

  useEffect(() => {
    if (paymentStep === PAYMENT_STEPS.PIN) {
      window.setTimeout(() => pinInputRef.current?.focus(), 120);
    }
  }, [paymentStep]);

  useEffect(() => {
    if (paymentStep !== PAYMENT_STEPS.SUCCESS) return undefined;
    const timer = window.setTimeout(() => setPaymentStep(null), 6500);
    return () => window.clearTimeout(timer);
  }, [paymentStep]);

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

  const closePaymentFlow = () => {
    if (paying) return;
    setPaymentStep(null);
    setWalletPin("");
    setPaymentError("");
    setPinVisible(false);
  };

  const openSummary = () => {
    setPaymentError("");
    setPaymentResult(null);
    setPaymentStep(PAYMENT_STEPS.SUMMARY);
  };

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
    if (paying || insufficientBalance || walletPin.length !== 4) return;
    setPaying(true);
    setPaymentError("");
    try {
      const result = await payPassengerBookingWithWallet(bookingReference, walletPin);
      setPaymentResult(result);
      await loadBooking();
      setWalletBalance(Number(result.walletBalance || 0));
      setWalletPin("");
      setPaymentStep(PAYMENT_STEPS.SUCCESS);
      toast.success("Wallet payment completed.");
    } catch (paymentErrorResponse) {
      if (handleBookingSession(paymentErrorResponse, navigate)) return;
      const message = mapPaymentError(paymentErrorResponse.message);
      setPaymentError(message);
      setWalletPin("");
      setPinShake(true);
      toast.error(message);
      window.setTimeout(() => setPinShake(false), 420);
      window.setTimeout(() => pinInputRef.current?.focus(), 80);
    } finally {
      setPaying(false);
    }
  };

  return <PassengerLayout activePage="My Bookings"><div className="mx-auto max-w-5xl space-y-6">
    <button onClick={() => navigate("/passenger/bookings")} className="flex items-center gap-2 text-sm font-black text-[#08264a] transition hover:text-blue-700"><ArrowLeft size={17} /> Back to My Bookings</button>
    {location.state?.created && <div className="rounded-3xl border border-amber-200 bg-gradient-to-r from-amber-50 to-orange-50 p-5 text-amber-900 shadow-sm"><div className="flex items-center gap-2 font-black"><CheckCircle2 size={20} /> Seats reserved. Payment is pending.</div><p className="mt-2 text-sm">Pay with your Yatayat Wallet before the hold expires to confirm these seats.</p></div>}
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin text-[#08264a]" size={40} /></div> : booking && <>
      <header className="relative overflow-hidden rounded-[2rem] bg-[#08264a] p-7 text-white shadow-xl shadow-blue-950/10">
        <div className="absolute -right-16 -top-16 h-44 w-44 rounded-full bg-blue-400/20 blur-2xl" />
        <div className="relative flex flex-col justify-between gap-4 sm:flex-row">
          <div>
            <p className="text-sm font-bold text-blue-200">Booking reference</p>
            <h1 className="mt-1 break-all text-3xl font-black">{booking.bookingReference}</h1>
            <p className="mt-2 text-slate-300">{booking.origin} to {booking.destination}</p>
          </div>
          <Status value={booking.bookingStatus} />
        </div>
      </header>
      {pendingPayment && <PaymentPanel booking={booking} walletBalance={walletBalance} walletError={walletError} pinStatus={pinStatus} remainingSeconds={remainingSeconds} insufficientBalance={insufficientBalance} paying={paying} onPay={openSummary} />}
      {booking.bookingStatus === "CONFIRMED" && <div className="rounded-3xl border border-emerald-200 bg-gradient-to-r from-emerald-50 to-teal-50 p-5 text-emerald-900 shadow-sm"><div className="flex items-center gap-2 font-black"><CheckCircle2 size={20} /> Booking confirmed</div><p className="mt-2 text-sm">Wallet payment is recorded and your selected seats are confirmed.</p></div>}
      <section className="grid grid-cols-1 gap-4 rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2">{rows.map(([label, value]) => <div key={label} className="rounded-2xl bg-slate-50 p-4 transition hover:bg-slate-100"><p className="text-xs font-black uppercase tracking-wide text-slate-500">{label}</p><p className="mt-2 break-words font-bold text-slate-800">{value}</p></div>)}</section>
      {cancellable && <div className="flex justify-end rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><button onClick={() => setShowCancel(true)} className="flex shrink-0 items-center justify-center gap-2 rounded-2xl bg-red-600 px-5 py-3 font-black text-white shadow-lg shadow-red-600/20 transition hover:bg-red-700"><XCircle size={17} /> Cancel Booking</button></div>}
    </>}
    {showCancel && <CancelModal cancelling={cancelling} onClose={() => setShowCancel(false)} onConfirm={cancel} />}
    {paymentStep === PAYMENT_STEPS.SUMMARY && booking && <PaymentSummaryModal booking={booking} walletBalance={walletBalance} onClose={closePaymentFlow} onContinue={() => setPaymentStep(PAYMENT_STEPS.PIN)} />}
    {paymentStep === PAYMENT_STEPS.PIN && booking && <WalletPinModal booking={booking} walletPin={walletPin} setWalletPin={setWalletPin} paying={paying} paymentError={paymentError} pinVisible={pinVisible} setPinVisible={setPinVisible} pinShake={pinShake} inputRef={pinInputRef} onClose={closePaymentFlow} onConfirm={pay} />}
    {paymentStep === PAYMENT_STEPS.SUCCESS && booking && <PaymentSuccessModal booking={booking} paymentResult={paymentResult} onClose={() => setPaymentStep(null)} onBookings={() => navigate("/passenger/bookings")} />}
  </div></PassengerLayout>;
}

function PaymentPanel({ booking, walletBalance, walletError, pinStatus, remainingSeconds, insufficientBalance, paying, onPay }) {
  const expired = booking.paymentHoldExpiresAt && remainingSeconds <= 0;
  const inactive = pinStatus === "PIN_NOT_SET";
  return <section className="overflow-hidden rounded-[2rem] border border-blue-100 bg-white shadow-xl shadow-blue-950/5">
    <div className="bg-gradient-to-r from-[#08264a] via-blue-900 to-blue-700 p-6 text-white">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.25em] text-blue-100"><Wallet size={16} /> Wallet checkout</p>
          <h2 className="mt-3 text-4xl font-black">{formatNpr(booking.totalFare)}</h2>
          <p className="mt-2 text-sm font-semibold text-blue-100">Confirm your reserved seats before the hold expires.</p>
        </div>
        <button onClick={onPay} disabled={paying || inactive || insufficientBalance || expired || walletBalance === null || pinStatus !== "PIN_SET"} className="flex items-center justify-center gap-2 rounded-2xl bg-white px-6 py-4 font-black text-[#08264a] shadow-lg shadow-blue-950/20 transition hover:-translate-y-0.5 hover:bg-blue-50 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50"><CreditCard size={18} /> Pay with Wallet</button>
      </div>
    </div>
    <div className="grid gap-4 p-5 sm:grid-cols-3">
      <InfoPill label="Wallet balance" value={walletError || (walletBalance === null ? "Loading..." : formatNpr(walletBalance))} />
      <InfoPill label="Wallet status" value={pinStatus === "PIN_SET" ? "Active" : pinStatus === "PIN_NOT_SET" ? "Activation required" : "Checking..."} tone={inactive ? "amber" : "blue"} />
      <InfoPill label="Hold time left" value={booking.paymentHoldExpiresAt ? formatTime(remainingSeconds) : "Not available"} tone={expired ? "red" : "blue"} />
    </div>
    {(inactive || insufficientBalance || expired) && <div className="border-t border-slate-100 p-5">
      {inactive && <InlineNotice tone="amber" message="Activate your wallet from the existing Wallet page before paying for this booking." action={<Link to="/wallet" className="font-black underline">Activate wallet</Link>} />}
      {insufficientBalance && <InlineNotice tone="red" message="Your wallet balance is not enough for this booking." action={<Link to="/wallet" className="font-black underline">Top up wallet</Link>} />}
      {expired && <InlineNotice tone="red" message="This seat hold has expired. Select seats again before payment." />}
    </div>}
  </section>;
}

function PaymentSummaryModal({ booking, walletBalance, onClose, onContinue }) {
  return <PremiumModal title="Confirm Wallet Payment" onClose={onClose}>
    <div className="text-center">
      <IconBubble><Wallet size={28} /></IconBubble>
      <h2 className="mt-4 text-2xl font-black text-slate-950">Confirm Wallet Payment</h2>
      <p className="mt-2 text-sm font-semibold text-slate-500">Review your journey and payment amount before entering your wallet PIN.</p>
    </div>
    <div className="mt-6 rounded-3xl border border-slate-100 bg-slate-50 p-4">
      <div className="grid gap-3">
        <SummaryRow label="Booking Reference" value={booking.bookingReference} icon={<ReceiptText size={17} />} />
        <SummaryRow label="Passenger" value={booking.passengerName} />
        <SummaryRow label="Route" value={`${booking.origin} to ${booking.destination}`} />
        <SummaryRow label="Travel Date" value={formatBookingDate(booking.departureAt)} icon={<CalendarClock size={17} />} />
        <SummaryRow label="Seats" value={booking.seatNumbers?.length ? booking.seatNumbers.join(", ") : `${booking.numberOfSeats} seat(s)`} icon={<Armchair size={17} />} />
        <SummaryRow label="Operator" value={booking.operatorName} />
        <SummaryRow label="Bus" value={booking.busNumber} icon={<Bus size={17} />} />
        <SummaryRow label="Payment Method" value="Yatayat Wallet" icon={<Wallet size={17} />} />
      </div>
    </div>
    <div className="mt-5 rounded-3xl bg-[#08264a] p-5 text-white">
      <p className="text-xs font-black uppercase tracking-[0.22em] text-blue-200">Total amount</p>
      <div className="mt-2 flex items-end justify-between gap-3">
        <p className="text-4xl font-black">{formatNpr(booking.totalFare)}</p>
        <p className="pb-1 text-xs font-bold text-blue-100">Balance: {walletBalance === null ? "Loading..." : formatNpr(walletBalance)}</p>
      </div>
    </div>
    <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
      <button onClick={onClose} className="rounded-2xl border border-slate-200 px-5 py-3 font-black text-slate-700 transition hover:bg-slate-50">Cancel</button>
      <button onClick={onContinue} className="rounded-2xl bg-[#08264a] px-5 py-3 font-black text-white shadow-lg shadow-blue-950/20 transition hover:-translate-y-0.5 hover:bg-blue-950">Continue</button>
    </div>
  </PremiumModal>;
}

function WalletPinModal({ booking, walletPin, setWalletPin, paying, paymentError, pinVisible, setPinVisible, pinShake, inputRef, onClose, onConfirm }) {
  const digitRefs = useRef([]);
  const digits = walletPin.padEnd(4, "").slice(0, 4).split("");

  useEffect(() => {
    digitRefs.current[0]?.focus();
  }, []);

  const focusDigit = (index) => {
    const safeIndex = Math.max(0, Math.min(3, index));
    digitRefs.current[safeIndex]?.focus();
    digitRefs.current[safeIndex]?.select();
  };

  const updateDigit = (index, value) => {
    const numeric = value.replace(/\D/g, "");
    if (!numeric) {
      const nextDigits = [...digits];
      nextDigits[index] = "";
      setWalletPin(nextDigits.join("").trim());
      return;
    }
    if (numeric.length > 1) {
      const pastedDigits = numeric.slice(0, 4).split("");
      setWalletPin(pastedDigits.join(""));
      focusDigit(Math.min(pastedDigits.length, 3));
      return;
    }
    const nextDigits = [...digits];
    nextDigits[index] = numeric;
    setWalletPin(nextDigits.join("").trim());
    if (index < 3) focusDigit(index + 1);
  };

  const handleKeyDown = (event, index) => {
    if (event.key === "Enter") {
      event.preventDefault();
      onConfirm();
      return;
    }
    if (event.key === "ArrowLeft") {
      event.preventDefault();
      focusDigit(index - 1);
      return;
    }
    if (event.key === "ArrowRight") {
      event.preventDefault();
      focusDigit(index + 1);
      return;
    }
    if (event.key === "Backspace" && !digits[index] && index > 0) {
      event.preventDefault();
      focusDigit(index - 1);
    }
  };

  const handlePaste = (event) => {
    event.preventDefault();
    const pasted = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, 4);
    if (!pasted) return;
    setWalletPin(pasted);
    focusDigit(Math.min(pasted.length, 3));
  };

  return <PremiumModal title="Enter Wallet PIN" onClose={onClose} disableClose={paying} onEnter={onConfirm}>
    <div className="text-center">
      <IconBubble><ShieldCheck size={28} /></IconBubble>
      <h2 className="mt-5 text-2xl font-black text-slate-950">Enter Wallet PIN</h2>
      <p className="mt-2 text-sm font-semibold text-slate-500">Approve this secure wallet payment.</p>
    </div>
    <div className="mt-7 rounded-[1.75rem] border border-blue-100 bg-gradient-to-br from-blue-50 to-slate-50 p-5">
      <div className="flex items-center justify-between gap-4">
        <div className="text-left">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-blue-700">Amount to pay</p>
          <p className="mt-1 text-3xl font-black text-[#08264a]">{formatNpr(booking.totalFare)}</p>
        </div>
        <div className="rounded-2xl bg-white px-3 py-2 text-right shadow-sm">
          <p className="text-[10px] font-black uppercase tracking-wide text-slate-400">Method</p>
          <p className="text-sm font-black text-slate-800">Wallet</p>
        </div>
      </div>
      <div className="mt-3 flex items-center gap-2 rounded-2xl bg-white/80 px-3 py-2 text-xs font-bold text-slate-600">
        <ShieldCheck size={15} className="text-emerald-600" />
        Your PIN is verified securely and never shown.
      </div>
    </div>
    <div className="mt-7">
      <div className="mb-3 flex items-center justify-between gap-3">
        <p className="text-sm font-black text-slate-700">Secure transaction PIN</p>
        <button type="button" disabled={paying} onClick={() => { setPinVisible(!pinVisible); window.setTimeout(() => focusDigit(Math.min(walletPin.length, 3)), 0); }} aria-label={pinVisible ? "Hide wallet PIN" : "Show wallet PIN"} className="flex items-center gap-1 rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-black text-slate-600 shadow-sm transition hover:border-blue-200 hover:bg-blue-50 hover:text-[#08264a] disabled:opacity-50">
          {pinVisible ? <EyeOff size={15} /> : <Eye size={15} />}
          {pinVisible ? "Hide" : "Show"}
        </button>
      </div>
      <div className={`grid grid-cols-4 gap-3 sm:gap-4 ${pinShake ? "yatayat-pin-shake" : ""}`}>
        {[0, 1, 2, 3].map((index) => <input
          key={index}
          ref={(element) => {
            digitRefs.current[index] = element;
            if (index === 0) inputRef.current = element;
          }}
          type={pinVisible ? "text" : "password"}
          inputMode="numeric"
          pattern="[0-9]*"
          autoComplete="one-time-code"
          maxLength={1}
          disabled={paying}
          aria-label={`Wallet PIN digit ${index + 1}`}
          aria-invalid={Boolean(paymentError)}
          aria-describedby={paymentError ? "wallet-pin-error" : undefined}
          value={digits[index]}
          onChange={(event) => updateDigit(index, event.target.value)}
          onKeyDown={(event) => handleKeyDown(event, index)}
          onPaste={handlePaste}
          onFocus={(event) => event.target.select()}
          className={`aspect-square w-full rounded-2xl border bg-white text-center text-3xl font-black text-slate-950 shadow-lg shadow-slate-950/5 outline-none transition duration-150 placeholder:text-slate-300 focus:-translate-y-0.5 focus:scale-[1.03] focus:border-[#0b5ed7] focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400 ${paymentError ? "border-red-300 ring-4 ring-red-100" : "border-slate-200"}`}
        />)}
      </div>
      <div className="mt-4 flex items-start justify-between gap-4">
        <p className="text-xs font-semibold leading-relaxed text-slate-500">Enter the wallet PIN you created when activating your wallet.</p>
        <p className="shrink-0 text-xs font-black text-slate-400">{walletPin.length}/4</p>
      </div>
    </div>
    {paymentError && <div id="wallet-pin-error" className="mt-4 rounded-2xl border border-red-100 bg-red-50 p-3 text-center text-sm font-black text-red-700">{paymentError}</div>}
    {paying && <div className="mt-5 rounded-3xl border border-blue-100 bg-blue-50 p-4 text-center">
      <Loader2 className="mx-auto animate-spin text-[#08264a]" size={32} />
      <p className="mt-3 font-black text-[#08264a]">Processing your payment...</p>
      <p className="mt-1 text-xs font-semibold text-blue-700">Please keep this window open.</p>
    </div>}
    <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
      <button disabled={paying} onClick={onClose} className="rounded-2xl border border-slate-200 px-5 py-3 font-black text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">Cancel</button>
      <button disabled={paying || walletPin.length !== 4} onClick={onConfirm} className="flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-5 py-3 font-black text-white shadow-lg shadow-blue-950/20 transition hover:-translate-y-0.5 hover:bg-blue-950 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50">{paying && <Loader2 className="animate-spin" size={17} />} {paying ? "Processing..." : `Pay ${formatNpr(booking.totalFare)}`}</button>
    </div>
  </PremiumModal>;
}

function PaymentSuccessModal({ booking, paymentResult, onClose, onBookings }) {
  const paidAt = paymentResult?.paidAt || booking.paidAt || new Date().toISOString();
  return <PremiumModal title="Payment Successful" onClose={onClose}>
    <div className="text-center">
      <div className="yatayat-success-check mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-emerald-500 text-white shadow-2xl shadow-emerald-500/30">
        <CheckCircle2 size={54} />
      </div>
      <h2 className="mt-5 text-3xl font-black text-slate-950">Payment Successful</h2>
      <p className="mt-2 text-sm font-semibold text-slate-500">Your wallet payment has been recorded and your seats are confirmed.</p>
    </div>
    <div className="mt-6 grid gap-3 rounded-3xl border border-emerald-100 bg-emerald-50 p-4">
      <SummaryRow label="Booking Reference" value={booking.bookingReference} />
      <SummaryRow label="Seats" value={booking.seatNumbers?.length ? booking.seatNumbers.join(", ") : `${booking.numberOfSeats} seat(s)`} />
      <SummaryRow label="Amount Paid" value={formatNpr(paymentResult?.paidAmount || booking.paidAmount || booking.totalFare)} />
      <SummaryRow label="Method" value={paymentResult?.paymentMethod || booking.paymentMethod || "Wallet"} />
      <SummaryRow label="Paid At" value={formatBookingDate(paidAt)} />
    </div>
    <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
      <button onClick={onBookings} className="rounded-2xl border border-slate-200 px-5 py-3 font-black text-slate-700 transition hover:bg-slate-50">Back to My Bookings</button>
      <button onClick={onClose} className="rounded-2xl bg-emerald-600 px-5 py-3 font-black text-white shadow-lg shadow-emerald-600/20 transition hover:-translate-y-0.5 hover:bg-emerald-700">View Ticket</button>
    </div>
  </PremiumModal>;
}

function PremiumModal({ title, children, onClose, disableClose = false, onEnter }) {
  const panelRef = useRef(null);
  const onCloseRef = useRef(onClose);
  const onEnterRef = useRef(onEnter);
  const disableCloseRef = useRef(disableClose);

  useEffect(() => {
    onCloseRef.current = onClose;
    onEnterRef.current = onEnter;
    disableCloseRef.current = disableClose;
  }, [disableClose, onClose, onEnter]);

  useEffect(() => {
    const panel = panelRef.current;
    if (!panel) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === "Escape" && !disableCloseRef.current) {
        event.preventDefault();
        onCloseRef.current();
      }
      if (event.key === "Enter" && onEnterRef.current) {
        const target = event.target;
        if (target?.tagName !== "BUTTON") {
          event.preventDefault();
          onEnterRef.current();
        }
      }
      const focusable = panel.querySelectorAll("button:not([disabled]), input:not([disabled]), a[href], [tabindex]:not([tabindex='-1'])");
      if (event.key !== "Tab" || focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, []);

  return <div className="fixed inset-0 z-[80] flex h-dvh items-end justify-center overflow-hidden bg-slate-950/60 p-0 backdrop-blur-sm sm:items-center sm:p-4" role="presentation">
    <div ref={panelRef} role="dialog" aria-modal="true" aria-label={title} className="yatayat-modal-panel max-h-[calc(100dvh-1rem)] w-full max-w-[480px] overflow-y-auto overscroll-contain rounded-t-[2rem] bg-white p-6 shadow-2xl shadow-slate-950/30 outline-none sm:max-h-[calc(100dvh-2rem)] sm:rounded-[2rem]">
      <div className="flex justify-end">
        <button disabled={disableClose} onClick={onClose} aria-label="Close modal" className="rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"><X size={18} /></button>
      </div>
      {children}
    </div>
  </div>;
}

function InfoPill({ label, value, tone = "blue" }) {
  const tones = {
    blue: "bg-blue-50 text-blue-900",
    amber: "bg-amber-50 text-amber-800",
    red: "bg-red-50 text-red-700",
  };
  return <div className={`rounded-3xl p-4 ${tones[tone]}`}>
    <p className="text-xs font-black uppercase tracking-wide opacity-70">{label}</p>
    <p className="mt-2 text-lg font-black">{value}</p>
  </div>;
}

function InlineNotice({ tone, message, action }) {
  const styles = tone === "red" ? "border-red-100 bg-red-50 text-red-700" : "border-amber-100 bg-amber-50 text-amber-700";
  return <p className={`rounded-2xl border p-4 text-sm font-bold ${styles}`}>{message} {action}</p>;
}

function IconBubble({ children }) {
  return <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-blue-50 text-[#08264a] shadow-inner">{children}</div>;
}

function SummaryRow({ label, value, icon }) {
  return <div className="flex items-start justify-between gap-4 rounded-2xl bg-white px-4 py-3 shadow-sm">
    <p className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-slate-500">{icon}{label}</p>
    <p className="max-w-[55%] break-words text-right text-sm font-black text-slate-900">{value || "Not available"}</p>
  </div>;
}

function Status({ value }) {
  const style = value === "CONFIRMED" ? "bg-emerald-400/20 text-emerald-100" : value === "PENDING_PAYMENT" ? "bg-amber-400/20 text-amber-100" : "bg-red-400/20 text-red-100";
  return <span className={`self-start rounded-full px-4 py-2 text-xs font-black ${style}`}>{value}</span>;
}

function CancelModal({ cancelling, onClose, onConfirm }) {
  return <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"><div className="w-full max-w-md rounded-3xl bg-white p-6"><h2 className="text-2xl font-black">Cancel this booking?</h2><p className="mt-2 text-sm text-slate-500">It will remain in your history with a cancelled status and its seats will be released.</p><div className="mt-6 flex justify-end gap-3"><button disabled={cancelling} onClick={onClose} className="rounded-xl border border-slate-300 px-4 py-3 font-black">Keep Booking</button><button disabled={cancelling} onClick={onConfirm} className="rounded-xl bg-red-600 px-4 py-3 font-black text-white disabled:opacity-60">{cancelling ? "Cancelling..." : "Confirm Cancellation"}</button></div></div></div>;
}

function mapPaymentError(message = "") {
  const normalized = message.toLowerCase();
  if (normalized.includes("incorrect") && normalized.includes("pin")) return "Incorrect Wallet PIN";
  if (normalized.includes("insufficient") || normalized.includes("balance")) return "Insufficient wallet balance. Please top up your wallet and try again.";
  if (normalized.includes("expired") || normalized.includes("hold")) return "This booking hold has expired. Please select seats again.";
  if (normalized.includes("inactive") || normalized.includes("activate") || normalized.includes("pin")) return "Wallet is not active. Please activate your wallet before paying.";
  if (normalized.includes("not found")) return "Booking not found or no longer available.";
  return "Unable to process payment. Please try again.";
}

function formatTime(seconds) {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${minutes}:${String(rest).padStart(2, "0")}`;
}
