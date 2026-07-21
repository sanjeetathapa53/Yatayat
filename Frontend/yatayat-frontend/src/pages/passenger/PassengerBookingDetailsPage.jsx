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
import { useLanguage } from "../../context/LanguageContext";
import { apiFetch } from "../../utils/api";
import {
  cancelPassengerBooking,
  formatBookingDate,
  formatNpr,
  getPassengerBooking,
  handleBookingSession,
  payPassengerBookingWithWallet,
} from "../../utils/passengerBookings";
import { getTicketByBooking } from "../../utils/passengerTickets";
import { tripStatusLabel, tripStatusTone } from "../../utils/driverTrips";

const PAYMENT_STEPS = {
  SUMMARY: "summary",
  PIN: "pin",
  SUCCESS: "success",
};

export default function PassengerBookingDetailsPage() {
  const { bookingReference } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useLanguage();
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
  const [ticketNumber, setTicketNumber] = useState("");
  const [ticketLoading, setTicketLoading] = useState(false);
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
    if (!response.ok) throw new Error(t("passenger.booking.walletLoadError"));
    const text = await response.text();
    setWalletBalance(Number(text) || 0);
    const pinResponse = await apiFetch(`/api/wallet/pin-status/${user.id}`);
    if (!pinResponse.ok) throw new Error(t("passenger.booking.pinStatusLoadError"));
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

  useEffect(() => {
    if (booking?.bookingStatus !== "CONFIRMED" || ticketNumber) return undefined;
    let active = true;
    setTicketLoading(true);
    getTicketByBooking(booking.bookingReference)
      .then((ticket) => { if (active) setTicketNumber(ticket.ticketNumber); })
      .catch(() => { if (active) setTicketNumber(""); })
      .finally(() => { if (active) setTicketLoading(false); });
    return () => { active = false; };
  }, [booking, ticketNumber]);

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
    [t("passenger.booking.passenger"), booking.passengerName],
    [t("passenger.booking.phone"), booking.passengerPhone],
    [t("passenger.booking.route"), `${booking.routeCode} - ${booking.routeName}`],
    [t("passenger.booking.tripType"), booking.tripType === "LOCAL" ? t("passenger.booking.local") : t("passenger.booking.outsideValley")],
    [t("passenger.booking.tripStatus"), formatTripStatus(booking.tripStatus, t)],
    [t("passenger.booking.journey"), t("passenger.booking.routeConnector", { origin: booking.origin, destination: booking.destination })],
    [t("passenger.booking.operator"), booking.operatorName],
    [t("passenger.booking.bus"), booking.busNumber],
    [t("passenger.booking.departure"), formatBookingDate(booking.departureAt)],
    [t("passenger.booking.estimatedArrival"), formatBookingDate(booking.estimatedArrivalAt)],
    [t("passenger.booking.selectedSeats"), booking.seatNumbers?.length ? booking.seatNumbers.join(", ") : t("passenger.booking.seatFallback", { count: booking.numberOfSeats })],
    [t("passenger.booking.farePerSeat"), formatNpr(booking.farePerSeat)],
    [t("passenger.booking.totalFare"), formatNpr(booking.totalFare)],
    [t("passenger.booking.paymentStatus"), booking.paymentStatus || (pendingPayment ? t("passenger.booking.pending") : t("passenger.booking.notPaid"))],
    [t("passenger.booking.paymentMethod"), booking.paymentMethod || t("passenger.booking.notPaid")],
    [t("passenger.booking.paidAmount"), booking.paidAmount ? formatNpr(booking.paidAmount) : t("passenger.booking.notPaid")],
    [t("passenger.booking.paidAt"), booking.paidAt ? formatBookingDate(booking.paidAt) : t("passenger.booking.notPaid")],
    [t("passenger.booking.paymentReference"), booking.transactionReference || t("passenger.booking.notGenerated")],
    [t("passenger.booking.bookedAt"), formatBookingDate(booking.bookedAt)],
    [t("passenger.booking.cancelledAt"), booking.cancelledAt ? formatBookingDate(booking.cancelledAt) : t("passenger.booking.notCancelled")],
    [t("passenger.booking.boardingNotes"), booking.boardingNotes || t("passenger.booking.noBoardingNotes")],
    [t("passenger.booking.tripStartedAt"), formatBookingDate(booking.actualDepartureAt)],
    [t("passenger.booking.tripCompletedAt"), formatBookingDate(booking.actualArrivalAt)],
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
      toast.success(t("passenger.booking.bookingCancelledSuccess"));
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
      if (result.ticketNumber) setTicketNumber(result.ticketNumber);
      await loadBooking();
      setWalletBalance(Number(result.walletBalance || 0));
      setWalletPin("");
      setPaymentStep(PAYMENT_STEPS.SUCCESS);
      toast.success(t("passenger.booking.walletPaymentCompleted"));
    } catch (paymentErrorResponse) {
      if (handleBookingSession(paymentErrorResponse, navigate)) return;
      const message = mapPaymentError(paymentErrorResponse.message, t);
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
    <button type="button" onClick={() => navigate("/passenger/bookings")} className="flex items-center gap-2 text-sm font-black text-[#08264a] transition hover:text-blue-700"><ArrowLeft size={17} /> {t("passenger.booking.backToMyBookings")}</button>
    {location.state?.created && <div className="rounded-3xl border border-amber-200 bg-gradient-to-r from-amber-50 to-orange-50 p-5 text-amber-900 shadow-sm"><div className="flex items-center gap-2 font-black"><CheckCircle2 size={20} /> {t("passenger.booking.seatsReserved")}</div><p className="mt-2 text-sm">{t("passenger.booking.payBeforeHoldExpires")}</p></div>}
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin text-[#08264a]" size={40} /></div> : booking && <>
      <header className="relative overflow-hidden rounded-[2rem] bg-[#08264a] p-7 text-white shadow-xl shadow-blue-950/10">
        <div className="absolute -right-16 -top-16 h-44 w-44 rounded-full bg-blue-400/20 blur-2xl" />
        <div className="relative flex flex-col justify-between gap-4 sm:flex-row">
          <div>
            <p className="text-sm font-bold text-blue-200">{t("passenger.booking.bookingReference")}</p>
            <h1 className="mt-1 break-all text-3xl font-black">{booking.bookingReference}</h1>
            <p className="mt-2 text-slate-300">{t("passenger.booking.routeConnector", { origin: booking.origin, destination: booking.destination })}</p>
          </div>
          <Status value={booking.bookingStatus} t={t} />
        </div>
      </header>
      {booking.tripStatus && <div className="flex flex-col gap-3 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.2em] text-slate-500">{t("passenger.booking.tripStatus")}</p>
          <p className="mt-1 text-lg font-black text-slate-900">{formatTripStatus(booking.tripStatus, t)}</p>
        </div>
        <span className={`self-start rounded-full px-4 py-2 text-xs font-black uppercase tracking-wide ${tripStatusTone(booking.tripStatus)}`}>
          {booking.tripStatus === "COMPLETED" ? t("passenger.booking.tripCompleted") : formatTripStatus(booking.tripStatus, t)}
        </span>
      </div>}
      {pendingPayment && <PaymentPanel booking={booking} walletBalance={walletBalance} walletError={walletError} pinStatus={pinStatus} remainingSeconds={remainingSeconds} insufficientBalance={insufficientBalance} paying={paying} onPay={openSummary} t={t} />}
      {booking.bookingStatus === "CONFIRMED" && <div className="flex flex-col gap-4 rounded-3xl border border-emerald-200 bg-gradient-to-r from-emerald-50 to-teal-50 p-5 text-emerald-900 shadow-sm sm:flex-row sm:items-center sm:justify-between"><div><div className="flex items-center gap-2 font-black"><CheckCircle2 size={20} /> {t("passenger.booking.bookingConfirmed")}</div><p className="mt-2 text-sm">{t("passenger.booking.bookingConfirmedHelp")}</p></div><button type="button" disabled={ticketLoading || !ticketNumber} onClick={() => navigate(`/passenger/tickets/${ticketNumber}`)} className="rounded-2xl bg-emerald-600 px-5 py-3 font-black text-white shadow-lg shadow-emerald-600/20 disabled:cursor-not-allowed disabled:opacity-60">{ticketLoading ? t("passenger.booking.preparingTicket") : t("passenger.booking.viewTicket")}</button></div>}
      <section className="grid grid-cols-1 gap-4 rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2">{rows.map(([label, value]) => <div key={label} className="rounded-2xl bg-slate-50 p-4 transition hover:bg-slate-100"><p className="text-xs font-black uppercase tracking-wide text-slate-500">{label}</p><p className="mt-2 break-words font-bold text-slate-800">{value}</p></div>)}</section>
      {cancellable && <div className="flex justify-end rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><button type="button" onClick={() => setShowCancel(true)} className="flex shrink-0 items-center justify-center gap-2 rounded-2xl bg-red-600 px-5 py-3 font-black text-white shadow-lg shadow-red-600/20 transition hover:bg-red-700"><XCircle size={17} /> {t("passenger.booking.cancelBooking")}</button></div>}
    </>}
    {showCancel && <CancelModal cancelling={cancelling} onClose={() => setShowCancel(false)} onConfirm={cancel} t={t} />}
    {paymentStep === PAYMENT_STEPS.SUMMARY && booking && <PaymentSummaryModal booking={booking} walletBalance={walletBalance} onClose={closePaymentFlow} onContinue={() => setPaymentStep(PAYMENT_STEPS.PIN)} t={t} />}
    {paymentStep === PAYMENT_STEPS.PIN && booking && <WalletPinModal booking={booking} walletPin={walletPin} setWalletPin={setWalletPin} paying={paying} paymentError={paymentError} pinVisible={pinVisible} setPinVisible={setPinVisible} pinShake={pinShake} inputRef={pinInputRef} onClose={closePaymentFlow} onConfirm={pay} t={t} />}
    {paymentStep === PAYMENT_STEPS.SUCCESS && booking && <PaymentSuccessModal booking={booking} paymentResult={paymentResult} ticketNumber={ticketNumber} ticketLoading={ticketLoading} onClose={() => setPaymentStep(null)} onTicket={() => ticketNumber && navigate(`/passenger/tickets/${ticketNumber}`)} onBookings={() => navigate("/passenger/bookings")} t={t} />}
  </div></PassengerLayout>;
}

function PaymentPanel({ booking, walletBalance, walletError, pinStatus, remainingSeconds, insufficientBalance, paying, onPay, t }) {
  const expired = booking.paymentHoldExpiresAt && remainingSeconds <= 0;
  const inactive = pinStatus === "PIN_NOT_SET";
  return <section className="overflow-hidden rounded-[2rem] border border-blue-100 bg-white shadow-xl shadow-blue-950/5">
    <div className="bg-gradient-to-r from-[#08264a] via-blue-900 to-blue-700 p-6 text-white">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.25em] text-blue-100"><Wallet size={16} /> {t("passenger.booking.walletCheckout")}</p>
          <h2 className="mt-3 text-4xl font-black">{formatNpr(booking.totalFare)}</h2>
          <p className="mt-2 text-sm font-semibold text-blue-100">{t("passenger.booking.confirmSeatsBeforeHold")}</p>
        </div>
        <button type="button" onClick={onPay} disabled={paying || inactive || insufficientBalance || expired || walletBalance === null || pinStatus !== "PIN_SET"} className="flex items-center justify-center gap-2 rounded-2xl bg-white px-6 py-4 font-black text-[#08264a] shadow-lg shadow-blue-950/20 transition hover:-translate-y-0.5 hover:bg-blue-50 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50"><CreditCard size={18} /> {t("passenger.booking.payWithWallet")}</button>
      </div>
    </div>
    <div className="grid gap-4 p-5 sm:grid-cols-3">
      <InfoPill label={t("passenger.booking.walletBalance")} value={walletError || (walletBalance === null ? t("passenger.booking.loading") : formatNpr(walletBalance))} />
      <InfoPill label={t("passenger.booking.walletStatus")} value={pinStatus === "PIN_SET" ? t("passenger.booking.active") : pinStatus === "PIN_NOT_SET" ? t("passenger.booking.activationRequired") : t("passenger.booking.checking")} tone={inactive ? "amber" : "blue"} />
      <InfoPill label={t("passenger.booking.holdTimeLeft")} value={booking.paymentHoldExpiresAt ? formatTime(remainingSeconds) : t("passenger.booking.notAvailable")} tone={expired ? "red" : "blue"} />
    </div>
    {(inactive || insufficientBalance || expired) && <div className="border-t border-slate-100 p-5">
      {inactive && <InlineNotice tone="amber" message={t("passenger.booking.activateWalletNotice")} action={<Link to="/wallet" className="font-black underline">{t("passenger.booking.activateWallet")}</Link>} />}
      {insufficientBalance && <InlineNotice tone="red" message={t("passenger.booking.insufficientBalanceNotice")} action={<Link to="/wallet" className="font-black underline">{t("passenger.booking.topUpWallet")}</Link>} />}
      {expired && <InlineNotice tone="red" message={t("passenger.booking.expiredHoldNotice")} />}
    </div>}
  </section>;
}

function PaymentSummaryModal({ booking, walletBalance, onClose, onContinue, t }) {
  return <PremiumModal title={t("passenger.booking.confirmWalletPayment")} onClose={onClose} t={t}>
    <div className="text-center">
      <IconBubble><Wallet size={28} /></IconBubble>
      <h2 className="mt-4 text-2xl font-black text-slate-950">{t("passenger.booking.confirmWalletPayment")}</h2>
      <p className="mt-2 text-sm font-semibold text-slate-500">{t("passenger.booking.reviewBeforePin")}</p>
    </div>
    <div className="mt-6 rounded-3xl border border-slate-100 bg-slate-50 p-4">
      <div className="grid gap-3">
        <SummaryRow label={t("passenger.booking.bookingReference")} value={booking.bookingReference} icon={<ReceiptText size={17} />} t={t} />
        <SummaryRow label={t("passenger.booking.passenger")} value={booking.passengerName} t={t} />
        <SummaryRow label={t("passenger.booking.route")} value={t("passenger.booking.routeConnector", { origin: booking.origin, destination: booking.destination })} t={t} />
        <SummaryRow label={t("passenger.booking.travelDate")} value={formatBookingDate(booking.departureAt)} icon={<CalendarClock size={17} />} t={t} />
        <SummaryRow label={t("passenger.booking.selectedSeats")} value={booking.seatNumbers?.length ? booking.seatNumbers.join(", ") : t("passenger.booking.seatFallback", { count: booking.numberOfSeats })} icon={<Armchair size={17} />} t={t} />
        <SummaryRow label={t("passenger.booking.operator")} value={booking.operatorName} t={t} />
        <SummaryRow label={t("passenger.booking.bus")} value={booking.busNumber} icon={<Bus size={17} />} t={t} />
        <SummaryRow label={t("passenger.booking.paymentMethod")} value="Yatayat Wallet" icon={<Wallet size={17} />} t={t} />
      </div>
    </div>
    <div className="mt-5 rounded-3xl bg-[#08264a] p-5 text-white">
      <p className="text-xs font-black uppercase tracking-[0.22em] text-blue-200">{t("passenger.booking.totalAmount")}</p>
      <div className="mt-2 flex items-end justify-between gap-3">
        <p className="text-4xl font-black">{formatNpr(booking.totalFare)}</p>
        <p className="pb-1 text-xs font-bold text-blue-100">{t("passenger.booking.balance", { balance: walletBalance === null ? t("passenger.booking.loading") : formatNpr(walletBalance) })}</p>
      </div>
    </div>
    <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
      <button type="button" onClick={onClose} className="rounded-2xl border border-slate-200 px-5 py-3 font-black text-slate-700 transition hover:bg-slate-50">{t("passenger.booking.cancel")}</button>
      <button type="button" onClick={onContinue} className="rounded-2xl bg-[#08264a] px-5 py-3 font-black text-white shadow-lg shadow-blue-950/20 transition hover:-translate-y-0.5 hover:bg-blue-950">{t("passenger.booking.continue")}</button>
    </div>
  </PremiumModal>;
}

function WalletPinModal({ booking, walletPin, setWalletPin, paying, paymentError, pinVisible, setPinVisible, pinShake, inputRef, onClose, onConfirm, t }) {
  const digitRefs = useRef([]);
  const digits = Array.from({ length: 4 }, (_, index) => walletPin[index] || "");

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

  return <PremiumModal title={t("passenger.booking.enterWalletPin")} onClose={onClose} disableClose={paying} onEnter={onConfirm} t={t}>
    <div className="text-center">
      <IconBubble><ShieldCheck size={28} /></IconBubble>
      <h2 className="mt-5 text-2xl font-black text-slate-950">{t("passenger.booking.enterWalletPin")}</h2>
      <p className="mt-2 text-sm font-semibold text-slate-500">{t("passenger.booking.approveSecurePayment")}</p>
    </div>
    <div className="mt-7 rounded-[1.75rem] border border-blue-100 bg-gradient-to-br from-blue-50 to-slate-50 p-5">
      <div className="flex items-center justify-between gap-4">
        <div className="text-left">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-blue-700">{t("passenger.booking.amountToPay")}</p>
          <p className="mt-1 text-3xl font-black text-[#08264a]">{formatNpr(booking.totalFare)}</p>
        </div>
        <div className="rounded-2xl bg-white px-3 py-2 text-right shadow-sm">
          <p className="text-[10px] font-black uppercase tracking-wide text-slate-400">{t("passenger.booking.method")}</p>
          <p className="text-sm font-black text-slate-800">{t("passenger.booking.wallet")}</p>
        </div>
      </div>
      <div className="mt-3 flex items-center gap-2 rounded-2xl bg-white/80 px-3 py-2 text-xs font-bold text-slate-600">
        <ShieldCheck size={15} className="text-emerald-600" />
        {t("passenger.booking.pinSecureHelp")}
      </div>
    </div>
    <div className="mt-7">
      <div className="mb-3 flex items-center justify-between gap-3">
        <p className="text-sm font-black text-slate-700">{t("passenger.booking.secureTransactionPin")}</p>
        <button type="button" disabled={paying} onClick={() => { setPinVisible(!pinVisible); window.setTimeout(() => focusDigit(Math.min(walletPin.length, 3)), 0); }} aria-label={pinVisible ? t("passenger.booking.hideWalletPin") : t("passenger.booking.showWalletPin")} className="flex items-center gap-1 rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-black text-slate-600 shadow-sm transition hover:border-blue-200 hover:bg-blue-50 hover:text-[#08264a] disabled:opacity-50">
          {pinVisible ? <EyeOff size={15} /> : <Eye size={15} />}
          {pinVisible ? t("passenger.booking.hide") : t("passenger.booking.show")}
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
          aria-label={t("passenger.booking.walletPinDigit", { number: index + 1 })}
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
        <p className="text-xs font-semibold leading-relaxed text-slate-500">{t("passenger.booking.pinHelp")}</p>
        <p className="shrink-0 text-xs font-black text-slate-400">{walletPin.length}/4</p>
      </div>
    </div>
    {paymentError && <div id="wallet-pin-error" className="mt-4 rounded-2xl border border-red-100 bg-red-50 p-3 text-center text-sm font-black text-red-700">{paymentError}</div>}
    {paying && <div className="mt-5 rounded-3xl border border-blue-100 bg-blue-50 p-4 text-center">
      <Loader2 className="mx-auto animate-spin text-[#08264a]" size={32} />
      <p className="mt-3 font-black text-[#08264a]">{t("passenger.booking.processingPayment")}</p>
      <p className="mt-1 text-xs font-semibold text-blue-700">{t("passenger.booking.keepWindowOpen")}</p>
    </div>}
    <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
      <button type="button" disabled={paying} onClick={onClose} className="rounded-2xl border border-slate-200 px-5 py-3 font-black text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">{t("passenger.booking.cancel")}</button>
      <button type="button" disabled={paying || walletPin.length !== 4} onClick={onConfirm} className="flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-5 py-3 font-black text-white shadow-lg shadow-blue-950/20 transition hover:-translate-y-0.5 hover:bg-blue-950 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50">{paying && <Loader2 className="animate-spin" size={17} />} {paying ? t("passenger.booking.processing") : t("passenger.booking.payAmount", { amount: formatNpr(booking.totalFare) })}</button>
    </div>
  </PremiumModal>;
}

function PaymentSuccessModal({ booking, paymentResult, ticketNumber, ticketLoading, onClose, onTicket, onBookings, t }) {
  const paidAt = paymentResult?.paidAt || booking.paidAt || new Date().toISOString();
  return <PremiumModal title={t("passenger.booking.paymentSuccessful")} onClose={onClose} t={t}>
    <div className="text-center">
      <div className="yatayat-success-check mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-emerald-500 text-white shadow-2xl shadow-emerald-500/30">
        <CheckCircle2 size={54} />
      </div>
      <h2 className="mt-5 text-3xl font-black text-slate-950">{t("passenger.booking.paymentSuccessful")}</h2>
      <p className="mt-2 text-sm font-semibold text-slate-500">{paymentResult?.ticketEmailMessage || t("passenger.booking.paymentSuccessEmail")}</p>
    </div>
    <div className="mt-6 grid gap-3 rounded-3xl border border-emerald-100 bg-emerald-50 p-4">
      <SummaryRow label={t("passenger.booking.bookingReference")} value={booking.bookingReference} t={t} />
      <SummaryRow label={t("passenger.booking.selectedSeats")} value={booking.seatNumbers?.length ? booking.seatNumbers.join(", ") : t("passenger.booking.seatFallback", { count: booking.numberOfSeats })} t={t} />
      <SummaryRow label={t("passenger.booking.amountPaid")} value={formatNpr(paymentResult?.paidAmount || booking.paidAmount || booking.totalFare)} t={t} />
      <SummaryRow label={t("passenger.booking.method")} value={paymentResult?.paymentMethod || booking.paymentMethod || t("passenger.booking.wallet")} t={t} />
      <SummaryRow label={t("passenger.booking.paidAt")} value={formatBookingDate(paidAt)} t={t} />
    </div>
    <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
      <button type="button" onClick={onBookings} className="rounded-2xl border border-slate-200 px-5 py-3 font-black text-slate-700 transition hover:bg-slate-50">{t("passenger.booking.backToMyBookings")}</button>
      <button type="button" disabled={ticketLoading || !ticketNumber} onClick={onTicket || onClose} className="rounded-2xl bg-emerald-600 px-5 py-3 font-black text-white shadow-lg shadow-emerald-600/20 transition hover:-translate-y-0.5 hover:bg-emerald-700 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60">{ticketLoading ? t("passenger.booking.preparingTicket") : t("passenger.booking.viewTicket")}</button>
    </div>
  </PremiumModal>;
}

function PremiumModal({ title, children, onClose, disableClose = false, onEnter, t }) {
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
        <button type="button" disabled={disableClose} onClick={onClose} aria-label={t("passenger.booking.closeModal")} className="rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"><X size={18} /></button>
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

function SummaryRow({ label, value, icon, t }) {
  return <div className="flex items-start justify-between gap-4 rounded-2xl bg-white px-4 py-3 shadow-sm">
    <p className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-slate-500">{icon}{label}</p>
    <p className="max-w-[55%] break-words text-right text-sm font-black text-slate-900">{value || t("passenger.booking.notAvailable")}</p>
  </div>;
}

function Status({ value, t }) {
  const style = value === "CONFIRMED" ? "bg-emerald-400/20 text-emerald-100" : value === "PENDING_PAYMENT" ? "bg-amber-400/20 text-amber-100" : "bg-red-400/20 text-red-100";
  const label = value === "CONFIRMED" ? t("passenger.booking.statusConfirmed") : value === "PENDING_PAYMENT" ? t("passenger.booking.statusPendingPayment") : value === "CANCELLED" ? t("passenger.booking.statusCancelled") : value;
  return <span className={`self-start rounded-full px-4 py-2 text-xs font-black ${style}`}>{label}</span>;
}

function formatTripStatus(status, t) {
  const labels = {
    SCHEDULED: t("passenger.booking.statusScheduled"),
    BOARDING: t("passenger.booking.statusBoarding"),
    IN_PROGRESS: t("passenger.booking.statusInProgress"),
    COMPLETED: t("passenger.booking.statusCompleted"),
    CANCELLED: t("passenger.booking.statusCancelled"),
  };
  return labels[status] || tripStatusLabel(status);
}

function CancelModal({ cancelling, onClose, onConfirm, t }) {
  return <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"><div className="w-full max-w-md rounded-3xl bg-white p-6"><h2 className="text-2xl font-black">{t("passenger.booking.cancelThisBooking")}</h2><p className="mt-2 text-sm text-slate-500">{t("passenger.booking.cancelThisBookingDesc")}</p><div className="mt-6 flex justify-end gap-3"><button type="button" disabled={cancelling} onClick={onClose} className="rounded-xl border border-slate-300 px-4 py-3 font-black">{t("passenger.booking.keepBooking")}</button><button type="button" disabled={cancelling} onClick={onConfirm} className="rounded-xl bg-red-600 px-4 py-3 font-black text-white disabled:opacity-60">{cancelling ? t("passenger.booking.cancelling") : t("passenger.booking.confirmCancellation")}</button></div></div></div>;
}

function mapPaymentError(message = "", t) {
  const normalized = message.toLowerCase();
  if (normalized.includes("incorrect") && normalized.includes("pin")) return t("passenger.booking.incorrectPin");
  if (normalized.includes("insufficient") || normalized.includes("balance")) return t("passenger.booking.insufficientBalanceError");
  if (normalized.includes("expired") || normalized.includes("hold")) return t("passenger.booking.holdExpiredError");
  if (normalized.includes("inactive") || normalized.includes("activate") || normalized.includes("pin")) return t("passenger.booking.walletInactiveError");
  if (normalized.includes("not found")) return t("passenger.booking.bookingNotFound");
  return t("passenger.booking.paymentFailed");
}

function formatTime(seconds) {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${minutes}:${String(rest).padStart(2, "0")}`;
}
