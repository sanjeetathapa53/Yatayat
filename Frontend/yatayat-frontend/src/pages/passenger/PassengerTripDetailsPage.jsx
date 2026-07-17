import { useEffect, useState } from "react";
import { ArrowLeft, Bus, Loader2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { formatPassengerTripDate, handlePassengerSession, passengerTripRequest } from "../../utils/passengerTrips";

export default function PassengerTripDetailsPage() {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const [trip, setTrip] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    passengerTripRequest(`/api/passenger/trips/${tripId}`)
      .then((data) => { if (active) setTrip(data); })
      .catch((loadError) => { if (active && !handlePassengerSession(loadError, navigate)) setError(loadError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [navigate, tripId]);

  const rows = trip ? [
    ["Trip type", trip.tripType === "LOCAL" ? "Local" : "Outside Valley"],
    ["Operator", trip.operatorName], ["Bus", `${trip.busName || "Bus"} (${trip.busNumber})`],
    ["Departure", formatPassengerTripDate(trip.departureAt)],
    ["Estimated arrival", formatPassengerTripDate(trip.estimatedArrivalAt)],
    ["Fare", `NPR ${Number(trip.fare).toLocaleString()}`], ["Bus capacity", `${trip.seatCapacity} seats`],
    ["Status", trip.status], ["Estimated duration", trip.estimatedDurationMinutes ? `${trip.estimatedDurationMinutes} minutes` : "Not specified"],
    ["Boarding notes", trip.boardingNotes || "No boarding notes provided"],
  ] : [];

  return <PassengerLayout activePage="Find Trips"><div className="mx-auto max-w-5xl space-y-6">
    <button onClick={() => navigate("/passenger/trips")} className="flex items-center gap-2 text-sm font-black"><ArrowLeft size={17} /> Back to trip search</button>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : trip && <>
      <header className="rounded-3xl bg-[#08264a] p-7 text-white"><div className="flex items-center gap-4"><Bus size={34} /><div><p className="text-sm font-bold text-blue-200">{trip.routeCode}</p><h1 className="text-3xl font-black">{trip.routeName}</h1><p className="mt-2 text-slate-300">{trip.origin} to {trip.destination}</p></div></div></header>
      <section className="grid grid-cols-1 gap-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2">{rows.map(([label, value]) => <div key={label} className="rounded-2xl bg-slate-50 p-4"><p className="text-xs font-black uppercase tracking-wide text-slate-500">{label}</p><p className="mt-2 font-bold text-slate-800">{value}</p></div>)}</section>
      {trip.tripType === "LOCAL" ? <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5"><p className="font-bold text-amber-900">Local fare ticket support is coming in a later phase. Seat reservations are not available for local trips.</p><button disabled className="mt-4 cursor-not-allowed rounded-xl bg-amber-200 px-6 py-3 font-black text-amber-800">Local Fare Ticket - Coming Next</button></div> : <div className="rounded-2xl border border-blue-200 bg-blue-50 p-5"><p className="font-bold text-blue-900">Choose seats before confirming your passenger details. Your selection will be held temporarily.</p><button onClick={() => navigate(`/passenger/trips/${tripId}/seats`)} className="mt-4 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">Select Seats</button></div>}
    </>}
  </div></PassengerLayout>;
}
