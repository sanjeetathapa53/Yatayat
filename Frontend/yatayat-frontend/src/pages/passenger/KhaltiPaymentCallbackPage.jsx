import { useEffect, useState } from "react";
import { CheckCircle2, Clock3, Loader2, ShieldAlert, XCircle } from "lucide-react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { handleBookingSession, verifyKhaltiBookingPayment } from "../../utils/passengerBookings";

export default function KhaltiPaymentCallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const bookingReference = (params.get("bookingReference") || "").trim();
  const pidx = (params.get("pidx") || "").trim();
  const [state, setState] = useState({ status: "VERIFYING", message: "Verifying payment…", result: null });

  useEffect(() => {
    let active = true;
    if (!bookingReference || !pidx) {
      Promise.resolve().then(() => {
        if (active) setState({ status: "MALFORMED", message: "The Khalti callback is missing required payment information.", result: null });
      });
      return () => { active = false; };
    }
    Promise.resolve()
      .then(() => verifyKhaltiBookingPayment(bookingReference, pidx))
      .then((result) => {
        if (!active) return;
        const status = result.paymentStatus || "FAILED";
        if (status === "SUCCESS" && result.bookingStatus === "CONFIRMED") {
          setState({ status: "SUCCESS", message: "Payment verified successfully.", result });
        } else {
          setState({ status, message: statusMessage(status), result });
        }
      })
      .catch((error) => {
        if (!active || handleBookingSession(error, navigate)) return;
        setState({ status: "ERROR", message: error.message || "Unable to verify Khalti payment.", result: null });
      });
    return () => { active = false; };
  }, [bookingReference, navigate, pidx]);

  return <PassengerLayout activePage="My Bookings">
    <div className="mx-auto flex min-h-[65vh] max-w-xl items-center justify-center">
      <section className="w-full rounded-[2rem] border border-slate-200 bg-white p-7 text-center shadow-xl shadow-blue-950/10 sm:p-10">
        <StatusIcon status={state.status} />
        <h1 className="mt-5 text-2xl font-black text-slate-950 sm:text-3xl">{titleFor(state.status)}</h1>
        <p className="mt-3 text-sm font-semibold leading-6 text-slate-600">{state.message}</p>
        {bookingReference && <p className="mt-5 break-all rounded-2xl bg-slate-50 p-4 text-xs font-black text-slate-500">Booking: {bookingReference}</p>}
        {state.status !== "VERIFYING" && <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-center">
          {bookingReference && <Link to={`/passenger/bookings/${encodeURIComponent(bookingReference)}/payment`} className="rounded-2xl bg-[#08264a] px-5 py-3 font-black text-white">View booking</Link>}
          <Link to="/passenger/bookings" className="rounded-2xl border border-slate-300 px-5 py-3 font-black text-slate-700">My bookings</Link>
        </div>}
      </section>
    </div>
  </PassengerLayout>;
}

function StatusIcon({ status }) {
  if (status === "VERIFYING") return <Loader2 className="mx-auto animate-spin text-[#08264a]" size={58} />;
  if (status === "SUCCESS") return <CheckCircle2 className="mx-auto text-emerald-600" size={64} />;
  if (["PENDING", "INITIATED"].includes(status)) return <Clock3 className="mx-auto text-amber-600" size={64} />;
  if (["CANCELLED", "EXPIRED", "FAILED", "REFUNDED"].includes(status)) return <XCircle className="mx-auto text-red-600" size={64} />;
  return <ShieldAlert className="mx-auto text-red-600" size={64} />;
}
function titleFor(status) {
  if (status === "VERIFYING") return "Verifying Khalti payment";
  if (status === "SUCCESS") return "Payment confirmed";
  if (["PENDING", "INITIATED"].includes(status)) return "Payment not completed yet";
  if (status === "CANCELLED") return "Payment cancelled";
  if (status === "EXPIRED") return "Payment expired";
  if (status === "REFUNDED") return "Payment refunded";
  return "Payment could not be verified";
}
function statusMessage(status) {
  const messages = {
    PENDING: "Khalti reports that this payment is pending. Your booking has not been confirmed.",
    INITIATED: "The Khalti checkout was initiated but payment is not complete.",
    CANCELLED: "The Khalti payment was cancelled. Your booking has not been confirmed.",
    EXPIRED: "The Khalti payment attempt expired. Your booking has not been confirmed.",
    REFUNDED: "Khalti reports this payment as refunded, so the booking was not confirmed.",
    FAILED: "Khalti did not confirm this payment.",
  };
  return messages[status] || "The backend could not verify this Khalti payment.";
}
