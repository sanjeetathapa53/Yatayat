import {
  Download,
  Mail,
  Eye,
  ShieldCheck,
  MapPin,
} from "lucide-react";
import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import TicketPreview from "../../components/ticket/TicketPreview";
import { toast } from "react-toastify";

export default function TicketPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [showCancelModal, setShowCancelModal] = useState(false);
const [cancelling, setCancelling] = useState(false);
const [cancelSuccess, setCancelSuccess] = useState(false);

  const booking = location.state?.booking;
  const passenger = location.state?.passenger || {
    name: "Passenger",
    email: "passenger@example.com",
    phone: "",
  };

  if (!booking) {
    return (
      <PassengerLayout activePage="My Bookings">
        <div className="rounded-2xl bg-white p-8 text-center shadow-sm">
          <h1 className="text-2xl font-black text-red-600">
            No ticket data found
          </h1>
          <button
            onClick={() => navigate("/my-bookings")}
            className="mt-4 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-bold text-white"
          >
            Go to My Bookings
          </button>
        </div>
      </PassengerLayout>
    );
  }

  const resendEmail = () => {
    alert(`Ticket email feature will send to ${passenger.email}`);
  };

  const downloadTicket = () => {
    window.open(
      `http://localhost:8080/api/bookings/${booking.id}/ticket-pdf`,
      "_blank"
    );
  };
  
  const cancelTicket = async () => {
    setCancelling(true);
  
    try {
      const response = await fetch(
        `http://localhost:8080/api/bookings/${booking.id}/cancel`,
        { method: "PUT" }
      );
  
      const data = await response.json();
  
      if (data.success) {
        setCancelSuccess(true);
  
        setTimeout(() => {
          navigate("/my-bookings");
        }, 1600);
      } else {
        alert(data.message || "Unable to cancel ticket.");
        setCancelling(false);
      }
    } catch (error) {
      console.error(error);
      alert("Something went wrong while cancelling ticket.");
      setCancelling(false);
    }
  };

  return (
    <PassengerLayout activePage="My Bookings">
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Booking Confirmed
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Your ticket has been generated successfully.
          </p>
        </div>

        <div className="flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-5 py-3 text-sm font-black text-emerald-700">
          <ShieldCheck size={18} />
          Paid & Confirmed
        </div>
      </header>

      <TicketPreview booking={booking} passenger={passenger} />

      <section className="mt-6 grid grid-cols-1 gap-5 md:grid-cols-4">
        <button
          onClick={downloadTicket}
          className="flex items-center justify-center gap-2 rounded-xl bg-emerald-600 py-4 text-sm font-black text-white transition hover:bg-emerald-700"
        >
          <Download size={19} />
          Download PDF
        </button>

        <button
          onClick={() => navigate("/my-bookings")}
          className="flex items-center justify-center gap-2 rounded-xl border border-[#1d3f6e] bg-white py-4 text-sm font-black text-[#08264a] transition hover:bg-slate-50"
        >
          <Eye size={19} />
          My Bookings
        </button>

        <button
          onClick={resendEmail}
          className="flex items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white py-4 text-sm font-black text-slate-700 transition hover:bg-slate-50"
        >
          <Mail size={19} />
          Resend Email
        </button>

        <button
          onClick={() => setShowCancelModal(true)}
          className="flex items-center justify-center rounded-xl border border-red-300 bg-white py-4 text-sm font-black text-red-600 transition hover:bg-red-50"
        >
          Cancel Ticket
        </button>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100 text-[#1d3f6e]">
            <MapPin size={19} />
          </div>

          <div>
            <h3 className="font-black text-slate-900">
              Local Tracking Reminder
            </h3>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              This ticket feature is for out-of-valley booking. The main Yatayat
              system still focuses on local vehicle route tracking and public
              bus information.
            </p>
          </div>
        </div>
      </section>
      {showCancelModal && (
  <div className="fixed inset-0 z-[999] flex items-center justify-center bg-black/50 px-4 backdrop-blur-sm">
    <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
      {!cancelSuccess ? (
        <>
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-red-100 text-3xl">
            ⚠️
          </div>

          <div className="mt-5 text-center">
            <h2 className="text-2xl font-black text-slate-900">
              Cancel this ticket?
            </h2>

            <p className="mt-2 text-sm leading-6 text-slate-500">
              This ticket will be cancelled, the QR will become invalid, and
              NPR {booking.fare} will be refunded to your Yatayat Wallet.
            </p>
          </div>

          <div className="mt-5 rounded-2xl bg-slate-50 p-4 text-sm">
            <div className="flex justify-between border-b border-slate-200 pb-2">
              <span className="text-slate-500">Booking ID</span>
              <span className="font-black">YT-{booking.id}</span>
            </div>

            <div className="flex justify-between border-b border-slate-200 py-2">
              <span className="text-slate-500">Seat</span>
              <span className="font-black">{booking.seatNumber}</span>
            </div>

            <div className="flex justify-between pt-2">
              <span className="text-slate-500">Refund</span>
              <span className="font-black text-emerald-700">
                NPR {booking.fare}
              </span>
            </div>
          </div>

          <div className="mt-5 rounded-xl bg-red-50 p-4 text-xs font-bold text-red-700">
            This action will free your seat for other passengers.
          </div>

          <div className="mt-6 flex gap-3">
            <button
              onClick={() => setShowCancelModal(false)}
              disabled={cancelling}
              className="flex-1 rounded-xl border border-slate-300 py-3 text-sm font-black text-slate-700 hover:bg-slate-50 disabled:opacity-60"
            >
              Keep Ticket
            </button>

            <button
              onClick={cancelTicket}
              disabled={cancelling}
              className="flex-1 rounded-xl bg-red-600 py-3 text-sm font-black text-white hover:bg-red-700 disabled:opacity-60"
            >
              {cancelling ? "Cancelling..." : "Cancel Ticket"}
            </button>
          </div>
        </>
      ) : (
        <div className="text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100 text-3xl">
            ✅
          </div>

          <h2 className="mt-5 text-2xl font-black text-slate-900">
            Ticket Cancelled
          </h2>

          <p className="mt-2 text-sm leading-6 text-slate-500">
            Your fare has been refunded to your wallet and a cancellation email
            has been sent.
          </p>

          <div className="mt-5 rounded-2xl bg-emerald-50 p-4">
            <p className="text-sm font-black text-emerald-700">
              Refunded: NPR {booking.fare}
            </p>
          </div>
        </div>
      )}
    </div>
  </div>
)}
    </PassengerLayout>
  );
}