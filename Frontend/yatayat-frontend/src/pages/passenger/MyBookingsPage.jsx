import { useEffect, useState } from "react";
import {
  Bus,
  Eye,
  Download,
  XCircle,
  Plus,
  Calendar,
  Armchair,
  AlertTriangle,
  CheckCircle2,
  Wallet,
  Mail,
  Loader2,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";

export default function MyBookingsPage() {
  const navigate = useNavigate();

  const user = JSON.parse(localStorage.getItem("yatayatUser"));

  const [activeTab, setActiveTab] = useState("upcoming");
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  // Cancellation modal states
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelSuccess, setCancelSuccess] = useState(false);
  const [cancelError, setCancelError] = useState("");

  // ==============================
  // FETCH BOOKINGS
  // ==============================

  const fetchBookings = async () => {
    if (!user?.id) {
      setLoading(false);
      return;
    }

    try {
      setLoading(true);

      const res = await fetch(
        `http://localhost:8080/api/bookings/user/${user.id}`,
        { credentials: "include" }
      );

      if (!res.ok) {
        throw new Error("Failed to load bookings");
      }

      const data = await res.json();

      setBookings(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Booking fetch error:", error);
      setBookings([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  // ==============================
  // VIEW TICKET
  // ==============================

  const handleViewTicket = (booking) => {
    navigate("/ticket", {
      state: {
        booking,

        passenger: {
          name: user?.fullName || "Passenger",
          email: user?.email || "",
          phone: user?.phone || "",
        },
      },
    });
  };

  // ==============================
  // DOWNLOAD PDF
  // ==============================

  const handleDownload = (booking) => {
    window.open(
      `http://localhost:8080/api/bookings/${booking.id}/ticket-pdf`,
      "_blank"
    );
  };

  // ==============================
  // OPEN CANCEL MODAL
  // ==============================

  const openCancelModal = (booking) => {
    setSelectedBooking(booking);
    setCancelError("");
    setCancelSuccess(false);
    setShowCancelModal(true);
  };

  // ==============================
  // CLOSE CANCEL MODAL
  // ==============================

  const closeCancelModal = () => {
    if (cancelling) return;

    setShowCancelModal(false);
    setSelectedBooking(null);
    setCancelSuccess(false);
    setCancelError("");
  };

  // ==============================
  // CANCEL BOOKING
  // ==============================

  const confirmCancellation = async () => {
    if (!selectedBooking) return;

    try {
      setCancelling(true);
      setCancelError("");

      const response = await fetch(
        `http://localhost:8080/api/bookings/${selectedBooking.id}/cancel`,
        {
          method: "PUT",
          credentials: "include",
        }
      );

      const data = await response.json();

      if (!data.success) {
        setCancelError(
          data.message || "Unable to cancel this booking."
        );

        setCancelling(false);
        return;
      }

      // Update the booking immediately in frontend
      setBookings((previousBookings) =>
        previousBookings.map((booking) =>
          booking.id === selectedBooking.id
            ? {
                ...booking,
                bookingStatus: "CANCELLED",
                paymentStatus: "REFUNDED",
              }
            : booking
        )
      );

      setCancelSuccess(true);
      setCancelling(false);

      // Show success state before closing
      setTimeout(() => {
        setShowCancelModal(false);
        setSelectedBooking(null);
        setCancelSuccess(false);

        // Automatically move user to cancelled tab
        setActiveTab("cancelled");
      }, 1800);
    } catch (error) {
      console.error("Cancellation error:", error);

      setCancelError(
        "Something went wrong while cancelling the booking."
      );

      setCancelling(false);
    }
  };

  // ==============================
  // FILTER BOOKINGS
  // ==============================

  const filteredBookings = bookings.filter((item) => {
    if (activeTab === "upcoming") {
      return item.bookingStatus === "CONFIRMED";
    }

    if (activeTab === "completed") {
      return item.bookingStatus === "COMPLETED";
    }

    if (activeTab === "cancelled") {
      return item.bookingStatus === "CANCELLED";
    }

    return true;
  });

  return (
    <PassengerLayout activePage="My Bookings" title="My Bookings" subtitle="Manage your out-of-valley tickets and travel history.">

      {/* HEADER */}

      <header className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <button
          onClick={() => navigate("/book-ticket")}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-[#0d3566] sm:w-auto"
        >
          <Plus size={18} />
          New Booking
        </button>
      </header>

      {/* TABS */}

      <div className="mb-6 overflow-x-auto border-b border-slate-300">
        <div className="flex min-w-max gap-4 sm:gap-8">

          <TabButton
            label="Upcoming"
            value="upcoming"
            active={activeTab}
            setActive={setActiveTab}
          />

          <TabButton
            label="Completed"
            value="completed"
            active={activeTab}
            setActive={setActiveTab}
          />

          <TabButton
            label="Cancelled"
            value="cancelled"
            active={activeTab}
            setActive={setActiveTab}
          />

        </div>
      </div>

      {/* LOADING */}

      {loading && (
        <div className="flex min-h-75 items-center justify-center">
          <div className="text-center">

            <Loader2
              size={34}
              className="mx-auto animate-spin text-[#08264a]"
            />

            <p className="mt-3 text-sm font-bold text-slate-500">
              Loading your bookings...
            </p>

          </div>
        </div>
      )}

      {/* BOOKING LIST */}

      {!loading && (
        <section className="space-y-4">

          {filteredBookings.map((booking) => (
            <BookingCard
              key={booking.id}
              booking={booking}
              onView={() => handleViewTicket(booking)}
              onDownload={() => handleDownload(booking)}
              onCancel={() => openCancelModal(booking)}
            />
          ))}

        </section>
      )}

      {/* EMPTY STATE */}

      {!loading && filteredBookings.length === 0 && (
        <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">

          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100">
            <Bus size={25} className="text-slate-500" />
          </div>

          <h2 className="mt-4 text-xl font-black text-slate-900">
            No bookings found
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            You do not have any {activeTab} bookings right now.
          </p>

        </div>
      )}

      {/* CANCEL MODAL */}

      {showCancelModal && selectedBooking && (
       <div className="fixed inset-0 z-[999] flex items-start justify-center overflow-y-auto bg-black/50 px-4 py-6 backdrop-blur-sm sm:items-center">

       <div className="my-auto max-h-[90vh] w-full max-w-md overflow-y-auto rounded-3xl bg-white shadow-2xl">

            {!cancelSuccess ? (
              <>
                {/* MODAL TOP */}

                <div className="px-6 pb-2 pt-7 text-center">

                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-red-100 text-red-600">
                    <AlertTriangle size={31} />
                  </div>

                  <h2 className="mt-5 text-2xl font-black text-slate-900">
                    Cancel this ticket?
                  </h2>

                  <p className="mt-2 text-sm leading-6 text-slate-500">
                    Your booking will be cancelled and the ticket QR code
                    will no longer be valid for boarding.
                  </p>

                </div>

                {/* BOOKING DETAILS */}

                <div className="mx-6 mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">

                  <div className="flex justify-between border-b border-slate-200 pb-3">
                    <span className="text-sm text-slate-500">
                      Booking ID
                    </span>

                    <span className="text-sm font-black text-slate-900">
                      YT-{selectedBooking.id}
                    </span>
                  </div>

                  <div className="flex justify-between border-b border-slate-200 py-3">
                    <span className="text-sm text-slate-500">
                      Route
                    </span>

                    <span className="max-w-[220px] text-right text-sm font-black text-slate-900">
                      {selectedBooking.routeName}
                    </span>
                  </div>

                  <div className="flex justify-between border-b border-slate-200 py-3">
                    <span className="text-sm text-slate-500">
                      Seat
                    </span>

                    <span className="text-sm font-black text-slate-900">
                      {selectedBooking.seatNumber}
                    </span>
                  </div>

                  <div className="flex justify-between pt-3">
                    <span className="text-sm text-slate-500">
                      Refund Amount
                    </span>

                    <span className="text-sm font-black text-emerald-700">
                      NPR {selectedBooking.fare}
                    </span>
                  </div>

                </div>

                {/* INFORMATION */}

                <div className="mx-6 mt-4 space-y-2">

                  <InfoRow
                    icon={<Wallet size={17} />}
                    text="Fare will be refunded to your wallet"
                  />

                  <InfoRow
                    icon={<Armchair size={17} />}
                    text="Your seat will become available again"
                  />

                  <InfoRow
                    icon={<Mail size={17} />}
                    text="A cancellation email will be sent"
                  />

                  <InfoRow
                    icon={<XCircle size={17} />}
                    text="The existing QR ticket will become invalid"
                    danger
                  />

                </div>

                {/* ERROR */}

                {cancelError && (
                  <div className="mx-6 mt-4 rounded-xl border border-red-200 bg-red-50 p-3 text-center text-sm font-bold text-red-700">
                    {cancelError}
                  </div>
                )}

                {/* BUTTONS */}

                <div className="mt-6 flex gap-3 border-t border-slate-200 bg-slate-50 p-5">

                  <button
                    onClick={closeCancelModal}
                    disabled={cancelling}
                    className="flex-1 rounded-xl border border-slate-300 bg-white py-3 text-sm font-black text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Keep Ticket
                  </button>

                  <button
                    onClick={confirmCancellation}
                    disabled={cancelling}
                    className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-red-600 py-3 text-sm font-black text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-70"
                  >
                    {cancelling ? (
                      <>
                        <Loader2
                          size={17}
                          className="animate-spin"
                        />
                        Cancelling...
                      </>
                    ) : (
                      <>
                        <XCircle size={17} />
                        Cancel Ticket
                      </>
                    )}
                  </button>

                </div>
              </>
            ) : (
              /* SUCCESS STATE */

              <div className="px-7 py-9 text-center">

                <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
                  <CheckCircle2 size={42} />
                </div>

                <h2 className="mt-5 text-2xl font-black text-slate-900">
                  Ticket Cancelled
                </h2>

                <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-500">
                  Your booking has been cancelled successfully and the fare
                  has been returned to your Yatayat Wallet.
                </p>

                <div className="mt-6 rounded-2xl border border-emerald-200 bg-emerald-50 p-5">

                  <p className="text-xs font-black uppercase tracking-widest text-emerald-600">
                    Amount Refunded
                  </p>

                  <p className="mt-2 text-3xl font-black text-emerald-700">
                    NPR {selectedBooking.fare}
                  </p>

                </div>

                <p className="mt-5 text-xs font-semibold text-slate-400">
                  Moving this booking to your Cancelled tab...
                </p>

              </div>
            )}

          </div>
        </div>
      )}

    </PassengerLayout>
  );
}


// ====================================
// BOOKING CARD
// ====================================

function BookingCard({
  booking,
  onView,
  onDownload,
  onCancel,
}) {

  const isCancelled =
    booking.bookingStatus === "CANCELLED";

  const isCompleted =
    booking.bookingStatus === "COMPLETED";

  const [from, to] = booking.routeName?.includes(" to ")
    ? booking.routeName.split(" to ")
    : booking.routeName?.includes("→")
    ? booking.routeName.split("→")
    : ["Kathmandu", "Pokhara"];

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-md">

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-12 xl:items-center">

        {/* ROUTE */}

        <div className="xl:col-span-3">

          <h2 className="text-xl font-black text-slate-900">
            {from.trim()}
          </h2>

          <div className="my-2 flex items-center gap-2 text-slate-400">

            <span className="h-2 w-2 rounded-full bg-[#08264a]" />
            <span className="h-px w-10 bg-slate-300" />

            <Bus size={15} />

            <span className="h-px w-10 bg-slate-300" />
            <span className="h-2 w-2 rounded-full bg-[#08264a]" />

          </div>

          <h2 className="text-xl font-black text-slate-900">
            {to.trim()}
          </h2>

        </div>

        {/* DATE */}

        <div className="border-t border-slate-200 pt-4 xl:col-span-2 xl:border-l xl:border-t-0 xl:pl-5 xl:pt-0">

          <p className="text-xs font-black uppercase tracking-widest text-slate-500">
            Date & Time
          </p>

          <p className="mt-2 flex items-center gap-2 font-black text-slate-900">
            <Calendar size={17} />
            {booking.travelDate}
          </p>

          <p className="mt-1 font-black">
            {booking.departureTime}
          </p>

        </div>

        {/* BUS */}

        <div className="xl:col-span-2">

          <p className="text-xs font-black uppercase tracking-widest text-slate-500">
            Bus Number
          </p>

          <p className="mt-2 font-black text-slate-900">
            {booking.busNumber}
          </p>

        </div>

        {/* SEAT */}

        <div className="xl:col-span-2">

          <p className="text-xs font-black uppercase tracking-widest text-slate-500">
            Seat Number
          </p>

          <p className="mt-2 flex items-center gap-2 font-black text-slate-900">
            <Armchair size={17} />
            {booking.seatNumber}
          </p>

        </div>

        {/* STATUS */}

        <div className="xl:col-span-1 xl:text-center">
          <StatusBadge status={booking.bookingStatus} />
        </div>

        {/* ACTIONS */}

        <div className="flex justify-start gap-3 border-t border-slate-100 pt-4 xl:col-span-2 xl:justify-end xl:border-t-0 xl:pt-0">

          <IconButton
            icon={<Eye size={20} />}
            onClick={onView}
            title="View Ticket"
          />

          <IconButton
            icon={<Download size={20} />}
            onClick={onDownload}
            title="Download Ticket"
          />

          {!isCompleted && !isCancelled && (
            <IconButton
              icon={<XCircle size={20} />}
              onClick={onCancel}
              title="Cancel Booking"
              danger
            />
          )}

        </div>

      </div>
    </div>
  );
}


// ====================================
// INFO ROW
// ====================================

function InfoRow({ icon, text, danger }) {
  return (
    <div
      className={`flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-semibold ${
        danger
          ? "bg-red-50 text-red-700"
          : "text-slate-600"
      }`}
    >
      {icon}
      <span>{text}</span>
    </div>
  );
}


// ====================================
// TAB BUTTON
// ====================================

function TabButton({
  label,
  value,
  active,
  setActive,
}) {
  return (
    <button
      onClick={() => setActive(value)}
      className={`border-b-2 px-4 pb-4 text-sm font-black transition ${
        active === value
          ? "border-[#1d3f6e] text-[#1d3f6e]"
          : "border-transparent text-slate-500 hover:text-[#08264a]"
      }`}
    >
      {label}
    </button>
  );
}


// ====================================
// STATUS BADGE
// ====================================

function StatusBadge({ status }) {

  const label =
    status === "CONFIRMED"
      ? "Confirmed"
      : status === "COMPLETED"
      ? "Completed"
      : status === "CANCELLED"
      ? "Cancelled"
      : status;

  const style =
    status === "CONFIRMED"
      ? "bg-emerald-100 text-emerald-700"
      : status === "COMPLETED"
      ? "bg-blue-100 text-blue-700"
      : "bg-red-100 text-red-700";

  return (
    <span
      className={`inline-block rounded-full px-4 py-2 text-xs font-black ${style}`}
    >
      {label}
    </span>
  );
}


// ====================================
// ICON BUTTON
// ====================================

function IconButton({
  icon,
  onClick,
  title,
  danger,
}) {
  return (
    <button
      onClick={onClick}
      title={title}
      className={`rounded-full p-2 transition ${
        danger
          ? "text-red-600 hover:bg-red-50"
          : "text-slate-600 hover:bg-slate-100 hover:text-[#08264a]"
      }`}
    >
      {icon}
    </button>
  );
}
