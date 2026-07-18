import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, Loader2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { createPassengerBooking, formatBookingDate, formatNpr, handleBookingSession } from "../../utils/passengerBookings";
import { passengerTripRequest } from "../../utils/passengerTrips";
import { getTripSeats } from "../../utils/passengerSeats";

export default function CreatePassengerBookingPage() {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const stored = localStorage.getItem("yatayatUser");
  const user = stored ? JSON.parse(stored) : {};
  const [trip, setTrip] = useState(null);
  const [heldSeats, setHeldSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [pageError, setPageError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [form, setForm] = useState({ passengerName: user.fullName || "", passengerPhone: user.phone || "" });

  useEffect(() => {
    let active = true;
    Promise.all([passengerTripRequest(`/api/passenger/trips/${tripId}`), getTripSeats(tripId)])
      .then(([tripData, seatData]) => {
        if (active) {
          setTrip(tripData);
          setHeldSeats(seatData.ownHeldSeats || []);
        }
      })
      .catch((loadError) => {
        if (active && !handleBookingSession(loadError, navigate)) setPageError(loadError.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [navigate, tripId]);

  const estimatedTotal = useMemo(() => trip ? Number(trip.fare) * heldSeats.length : 0, [heldSeats.length, trip]);

  const updateForm = (key, value) => {
    setForm((current) => ({ ...current, [key]: value }));
    setFieldErrors((current) => ({ ...current, [key]: "" }));
  };

  const notifyValidation = (nextErrors, message) => {
    setFieldErrors(nextErrors);
    toast.error(message, { toastId: `booking-form-${Object.keys(nextErrors)[0]}` });
  };

  const submit = async (event) => {
    event.preventDefault();
    const passengerName = form.passengerName.trim();
    const passengerPhone = form.passengerPhone.trim();

    if (passengerName.length < 2) {
      notifyValidation({ passengerName: "Please enter the passenger's full name." }, "Passenger name looks incomplete.");
      return;
    }
    if (!/^[0-9+()\-\s]{7,20}$/.test(passengerPhone)) {
      notifyValidation({ passengerPhone: "Use a valid phone number, e.g. 98XXXXXXXX or +977 98XXXXXXXX." }, "Enter a valid passenger phone number.");
      return;
    }
    if (!heldSeats.length) {
      toast.error("Your seat hold is missing or expired. Select seats again.");
      return;
    }

    setSubmitting(true);
    setFieldErrors({});
    try {
      const booking = await createPassengerBooking({
        tripId: Number(tripId),
        passengerName,
        passengerPhone,
        numberOfSeats: heldSeats.length,
        seatNumbers: heldSeats,
      });
      navigate(`/passenger/bookings/${booking.bookingReference}`, { replace: true, state: { created: true } });
    } catch (submitError) {
      if (!handleBookingSession(submitError, navigate)) {
        toast.error(submitError.message || "Unable to create booking. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return <PassengerLayout activePage="Find Trips"><div className="mx-auto max-w-5xl space-y-6">
    <button onClick={() => navigate(`/passenger/trips/${tripId}`)} className="flex items-center gap-2 text-sm font-black"><ArrowLeft size={17} /> Back to trip details</button>
    <div>
      <h1 className="text-3xl font-black text-slate-900">Create Booking</h1>
      <p className="mt-1 text-sm text-slate-500">Confirm passenger information for your held seats.</p>
    </div>
    {pageError && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{pageError}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : trip && (trip.tripType === "LOCAL"
      ? <div className="rounded-3xl border border-amber-200 bg-amber-50 p-8"><h2 className="text-2xl font-black text-amber-900">Seat booking is not available</h2><p className="mt-2 font-semibold text-amber-800">This is a local trip. Local fare ticket support will be added in a later phase.</p><button onClick={() => navigate(`/passenger/trips/${tripId}`)} className="mt-5 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">Return to Trip Details</button></div>
      : !heldSeats.length
        ? <div className="rounded-3xl border border-amber-200 bg-amber-50 p-8"><h2 className="text-2xl font-black text-amber-900">Seat hold required</h2><p className="mt-2 font-semibold text-amber-800">Your seat hold is missing or expired. Select available seats before creating the booking.</p><button onClick={() => navigate(`/passenger/trips/${tripId}/seats`)} className="mt-5 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">Select Seats</button></div>
        : <div className="grid gap-6 lg:grid-cols-[1fr_0.85fr]">
          <TripSummary trip={trip} />
          <form onSubmit={submit} noValidate className="space-y-5 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="rounded-2xl bg-amber-50 p-4"><p className="text-xs font-black uppercase text-amber-700">Held seats</p><p className="mt-1 text-2xl font-black text-amber-900">{heldSeats.join(", ")}</p></div>
            <Input label="Passenger name" maxLength={120} value={form.passengerName} error={fieldErrors.passengerName} onChange={(value) => updateForm("passengerName", value)} required />
            <Input label="Passenger phone" maxLength={20} inputMode="tel" value={form.passengerPhone} error={fieldErrors.passengerPhone} onChange={(value) => updateForm("passengerPhone", value)} required />
            <div className="rounded-2xl bg-blue-50 p-4"><p className="text-xs font-black uppercase text-blue-700">Estimated total</p><p className="mt-1 text-3xl font-black text-[#08264a]">{formatNpr(estimatedTotal)}</p><p className="mt-1 text-xs text-slate-500">Calculated from the held seats and current trip fare.</p></div>
            <button disabled={submitting} className="w-full rounded-xl bg-[#08264a] py-3 font-black text-white disabled:opacity-60">{submitting ? "Creating booking..." : "Continue to Payment"}</button>
            <p className="text-center text-xs text-slate-500">A pending booking will be created, then you can pay with your Yatayat Wallet.</p>
          </form>
        </div>)}
  </div></PassengerLayout>;
}

function Input({ label, value, onChange, error, type = "text", ...props }) {
  return <label className="block text-sm font-black text-slate-700">
    {label}
    <input {...props} type={type} value={value} aria-invalid={Boolean(error)} onChange={(event) => onChange(event.target.value)} className={`mt-2 w-full rounded-xl border px-4 py-3 font-semibold outline-none transition ${error ? "border-red-300 bg-red-50 text-red-900 focus:border-red-500" : "border-slate-300 focus:border-[#08264a]"}`} />
    {error && <p className="mt-2 rounded-xl bg-red-50 px-3 py-2 text-xs font-bold text-red-700">{error}</p>}
  </label>;
}

function TripSummary({ trip }) {
  return <section className="rounded-3xl bg-[#08264a] p-6 text-white"><p className="text-sm font-bold text-blue-200">{trip.routeCode} - Outside Valley</p><h2 className="mt-1 text-2xl font-black">{trip.routeName}</h2><p className="mt-2 text-slate-300">{trip.origin} to {trip.destination}</p><div className="mt-6 space-y-3 text-sm">{[["Operator", trip.operatorName], ["Bus", `${trip.busName || "Bus"} (${trip.busNumber})`], ["Departure", formatBookingDate(trip.departureAt)], ["Arrival", formatBookingDate(trip.estimatedArrivalAt)], ["Fare per seat", formatNpr(trip.fare)], ["Bus capacity", `${trip.seatCapacity} seats`]].map(([label, value]) => <div key={label} className="flex justify-between gap-4 border-b border-white/10 pb-3"><span className="text-slate-300">{label}</span><b className="text-right">{value}</b></div>)}</div></section>;
}
