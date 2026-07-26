import { useEffect, useState } from "react";
import { CheckCircle2, Clock3, Loader2, ShieldAlert, XCircle } from "lucide-react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useNotifications } from "../../context/NotificationContext";
import {
  handleBookingSession,
  isAlreadyPaidBooking,
  verifyEsewaBookingPayment,
} from "../../utils/passengerBookings";

export default function EsewaPaymentCallbackPage() {
  const [params] = useSearchParams();
  const pathParams = useParams();
  const navigate = useNavigate();
  const { refreshUnreadCount } = useNotifications();
  const bookingReference = (
    pathParams.bookingReference || params.get("bookingReference") || ""
  ).trim();
  const rawTransactionUuid = (
    pathParams.transactionUuid || params.get("transactionUuid") || ""
  ).trim();
  const appendedDataMarker = "?data=";
  const appendedDataIndex = rawTransactionUuid.indexOf(appendedDataMarker);
  const transactionUuid = (
    appendedDataIndex >= 0
      ? rawTransactionUuid.slice(0, appendedDataIndex)
      : rawTransactionUuid
  ).trim();
  const data = (
    params.get("data")
    || (appendedDataIndex >= 0
      ? rawTransactionUuid.slice(appendedDataIndex + appendedDataMarker.length)
      : "")
  ).trim();
  const outcome = (
    pathParams.outcome || params.get("outcome") || ""
  ).trim().toLowerCase();
  const [state, setState] = useState({
    status: "VERIFYING",
    message: "Verifying eSewa payment…",
    result: null,
  });

  useEffect(() => {
    let active = true;
    if (!bookingReference || (!transactionUuid && !data)) {
      Promise.resolve().then(() => {
        if (active) {
          setState({
            status: "MALFORMED",
            message: "The eSewa callback is missing required payment information.",
            result: null,
          });
        }
      });
      return () => { active = false; };
    }
    Promise.resolve()
      .then(() => verifyEsewaBookingPayment(bookingReference, transactionUuid, data))
      .then((result) => {
        if (!active) return;
        const status = result.paymentStatus || "FAILED";
        if (status === "SUCCESS" && result.bookingStatus === "CONFIRMED") {
          setState({ status: "SUCCESS", message: "Payment verified successfully.", result });
          refreshUnreadCount();
        } else {
          setState({ status, message: statusMessage(status, outcome), result });
        }
      })
      .catch((error) => {
        if (!active) return;
        if (isAlreadyPaidBooking(error)) {
          setState({
            status: "SUCCESS",
            message: "Your payment has already been processed successfully.",
            result: null,
          });
          refreshUnreadCount();
          return;
        }
        if (handleBookingSession(error, navigate)) return;
        setState({
          status: "ERROR",
          message: error.message || "Unable to verify eSewa payment.",
          result: null,
        });
      });
    return () => { active = false; };
  }, [bookingReference, data, navigate, outcome, refreshUnreadCount, transactionUuid]);

  return <PassengerLayout activePage="My Bookings" title={titleFor(state.status)} subtitle={state.message}>
    <div className="mx-auto flex min-h-[65vh] max-w-xl items-center justify-center">
      <section className="w-full rounded-[2rem] border border-slate-200 bg-white p-7 text-center shadow-xl shadow-blue-950/10 sm:p-10">
        <StatusIcon status={state.status} />
        {bookingReference && <p className="mt-5 break-all rounded-2xl bg-slate-50 p-4 text-xs font-black text-slate-500">
          Booking: {bookingReference}
        </p>}
        {state.status !== "VERIFYING" && <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-center">
          {bookingReference && <Link
            to={`/passenger/bookings/${encodeURIComponent(bookingReference)}/payment`}
            className="rounded-2xl bg-[#08264a] px-5 py-3 font-black text-white"
          >
            View booking
          </Link>}
          <Link
            to="/passenger/bookings"
            className="rounded-2xl border border-slate-300 px-5 py-3 font-black text-slate-700"
          >
            My bookings
          </Link>
        </div>}
      </section>
    </div>
  </PassengerLayout>;
}

function StatusIcon({ status }) {
  if (status === "VERIFYING") {
    return <Loader2 className="mx-auto animate-spin text-[#08264a]" size={58} />;
  }
  if (status === "SUCCESS") {
    return <CheckCircle2 className="mx-auto text-emerald-600" size={64} />;
  }
  if (["PENDING", "INITIATED"].includes(status)) {
    return <Clock3 className="mx-auto text-amber-600" size={64} />;
  }
  if (["CANCELLED", "EXPIRED", "FAILED", "REFUNDED"].includes(status)) {
    return <XCircle className="mx-auto text-red-600" size={64} />;
  }
  return <ShieldAlert className="mx-auto text-red-600" size={64} />;
}

function titleFor(status) {
  if (status === "VERIFYING") return "Verifying eSewa payment";
  if (status === "SUCCESS") return "Payment Successful";
  if (["PENDING", "INITIATED"].includes(status)) return "Payment not completed yet";
  if (status === "CANCELLED") return "Payment cancelled";
  if (status === "EXPIRED") return "Payment expired";
  if (status === "REFUNDED") return "Payment refunded";
  return "Payment could not be verified";
}

function statusMessage(status, outcome) {
  const messages = {
    PENDING: "eSewa reports that this payment is pending. Your booking has not been confirmed.",
    INITIATED: "The eSewa checkout was initiated but payment is not complete.",
    CANCELLED: "The eSewa payment was cancelled. Your booking has not been confirmed.",
    EXPIRED: "The eSewa payment attempt expired. Your booking has not been confirmed.",
    REFUNDED: "eSewa reports this payment as refunded, so the booking was not confirmed.",
    FAILED: "eSewa did not confirm this payment.",
  };
  if (messages[status]) return messages[status];
  if (outcome === "failure") {
    return "eSewa returned from an unsuccessful checkout and the backend did not verify payment.";
  }
  return "The backend could not verify this eSewa payment.";
}
