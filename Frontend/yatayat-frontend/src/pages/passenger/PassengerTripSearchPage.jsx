import { useState } from "react";
import { Bus, CalendarDays, Loader2, MapPin, RotateCcw, Search } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { formatPassengerTripDate, handlePassengerSession, passengerTripRequest } from "../../utils/passengerTrips";

export default function PassengerTripSearchPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ origin: location.state?.origin || "", destination: location.state?.destination || "", date: location.state?.date || "" });
  const [trips, setTrips] = useState([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const searchTrips = async (event) => {
    event.preventDefault();
    const origin = form.origin.trim(); const destination = form.destination.trim();
    if (!origin || !destination) { setError("Enter both an origin and destination."); return; }
    setLoading(true); setError(""); setSearched(true);
    const query = new URLSearchParams({ origin, destination });
    if (form.date) query.set("date", form.date);
    try { setTrips(await passengerTripRequest(`/api/passenger/trips/search?${query}`)); }
    catch (loadError) { if (!handlePassengerSession(loadError, navigate)) { setError(loadError.message); setTrips([]); } }
    finally { setLoading(false); }
  };
  const clear = () => { setForm({ origin: "", destination: "", date: "" }); setTrips([]); setSearched(false); setError(""); };

  return <PassengerLayout activePage="Book Out-of-Valley"><div className="space-y-6">
    <header><p className="text-xs font-black uppercase tracking-widest text-violet-700">Out-of-Valley transport</p><h1 className="mt-1 text-3xl font-black text-slate-900">Book Out-of-Valley Bus</h1><p className="mt-1 text-sm text-slate-500">Search scheduled intercity services by route and departure date.</p></header>
    <form onSubmit={searchTrips} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_1fr_0.8fr_auto] lg:items-end"><Field icon={<MapPin size={18} />} label="From / origin" value={form.origin} onChange={(value) => setForm((current) => ({ ...current, origin: value }))} placeholder="Kathmandu" required /><Field icon={<MapPin size={18} />} label="To / destination" value={form.destination} onChange={(value) => setForm((current) => ({ ...current, destination: value }))} placeholder="Pokhara" required /><Field icon={<CalendarDays size={18} />} label="Departure date (optional)" type="date" value={form.date} onChange={(value) => setForm((current) => ({ ...current, date: value }))} /><div className="flex gap-2"><button disabled={loading} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white disabled:opacity-60"><Search size={18} /> Search</button><button type="button" onClick={clear} title="Clear search" className="rounded-xl border border-slate-300 p-3 text-slate-600"><RotateCcw size={18} /></button></div></div></form>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-64 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : searched && trips.length === 0 && !error ? <div className="rounded-3xl border border-slate-200 bg-white py-16 text-center"><Bus className="mx-auto text-slate-300" size={48} /><h2 className="mt-4 text-xl font-black">No Out-of-Valley trips found for the selected route and date.</h2><p className="mt-2 text-sm text-slate-500">Try another date or route direction.</p></div> : <div className="space-y-4">{trips.map((trip) => <TripCard key={trip.tripId} trip={trip} onView={() => navigate(`/passenger/trips/${trip.tripId}`)} />)}</div>}
  </div></PassengerLayout>;
}

function Field({ icon, label, value, onChange, type = "text", ...props }) { return <label className="block text-xs font-black uppercase tracking-wide text-slate-500">{label}<div className="mt-2 flex items-center gap-2 rounded-xl border border-slate-300 px-3 focus-within:border-[#08264a]"><span>{icon}</span><input {...props} type={type} value={value} onChange={(event) => onChange(event.target.value)} className="w-full py-3 font-semibold normal-case text-slate-800 outline-none" /></div></label>; }
function TripCard({ trip, onView }) { const local = trip.tripType === "LOCAL"; return <article className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center"><div><div className="flex flex-wrap items-center gap-2"><h2 className="text-xl font-black text-slate-900">{trip.routeName}</h2><span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-black text-blue-700">{trip.status}</span><span className={`rounded-full px-3 py-1 text-xs font-black ${local ? "bg-amber-100 text-amber-700" : "bg-violet-100 text-violet-700"}`}>{local ? "Local" : "Outside Valley"}</span></div><p className="mt-1 font-bold text-slate-500">{trip.routeCode} - {trip.origin} to {trip.destination}</p><div className="mt-4 grid grid-cols-1 gap-2 text-sm sm:grid-cols-2 lg:grid-cols-3"><p><b>Operator:</b> {trip.operatorName}</p><p><b>Bus:</b> {trip.busName || trip.busNumber} ({trip.busNumber})</p><p><b>Bus capacity:</b> {trip.seatCapacity} seats</p><p><b>Departure:</b> {formatPassengerTripDate(trip.departureAt)}</p><p><b>Arrival:</b> {formatPassengerTripDate(trip.estimatedArrivalAt)}</p><p><b>Fare:</b> NPR {Number(trip.fare).toLocaleString()}</p></div></div><div className="flex shrink-0 flex-col gap-2"><button onClick={onView} className="rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">View Details</button><button onClick={onView} className="rounded-xl border border-[#08264a] px-6 py-3 font-black text-[#08264a]">{local ? "View Local Service" : "Select Trip"}</button></div></div></article>; }
