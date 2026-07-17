import { useCallback, useEffect, useMemo, useState } from "react";
import { CalendarDays, Loader2, Plus, RefreshCw } from "lucide-react";
import { useNavigate } from "react-router-dom";
import OperatorLayout from "../../components/layout/OperatorLayout";
import { formatTripDate, handleOperatorAccess, statusLabel, statusTone, TRIP_STATUSES, tripRequest } from "../../utils/operatorTrips";

export default function OperatorTripsPage() {
  const navigate = useNavigate();
  const [trips, setTrips] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadTrips = useCallback(async () => {
    setLoading(true); setError("");
    try {
      setTrips(await tripRequest("/api/operator/trips"));
    } catch (loadError) {
      if (!handleOperatorAccess(loadError, navigate)) setError(loadError.message);
    } finally { setLoading(false); }
  }, [navigate]);

  useEffect(() => { Promise.resolve().then(loadTrips); }, [loadTrips]);
  const visibleTrips = useMemo(() => filter === "ALL" ? trips : trips.filter((trip) => trip.status === filter), [filter, trips]);

  return <OperatorLayout><div className="space-y-6">
    <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"><div><h1 className="text-3xl font-black">Scheduled Trips</h1><p className="mt-1 text-sm text-slate-500">Plan and manage your organization’s scheduled services.</p></div><button onClick={() => navigate("/operator/trips/create")} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white"><Plus size={18} /> Create Trip</button></div>
    <div className="flex flex-wrap gap-2">{["ALL", ...TRIP_STATUSES].map((status) => <button key={status} onClick={() => setFilter(status)} className={`rounded-full px-4 py-2 text-xs font-black ${filter === status ? "bg-[#08264a] text-white" : "border border-slate-200 bg-white text-slate-600"}`}>{status === "ALL" ? "All" : statusLabel(status)}</button>)}</div>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : visibleTrips.length === 0 ? <div className="rounded-3xl border border-slate-200 bg-white px-6 py-16 text-center"><CalendarDays className="mx-auto text-slate-300" size={48} /><h2 className="mt-5 text-xl font-black">No {filter === "ALL" ? "scheduled trips" : statusLabel(filter).toLowerCase() + " trips"} found</h2><p className="mt-2 text-sm text-slate-500">Create a trip when an eligible route, bus, and driver are available.</p></div> : <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm"><div className="overflow-x-auto"><table className="min-w-[1050px] w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{["Route", "Bus", "Driver", "Departure", "Arrival", "Fare", "Capacity", "Status"].map((heading) => <th key={heading} className="px-5 py-4 font-black">{heading}</th>)}</tr></thead><tbody className="divide-y divide-slate-100">{visibleTrips.map((trip) => <tr key={trip.id} onClick={() => navigate(`/operator/trips/${trip.id}`)} className="cursor-pointer hover:bg-slate-50"><td className="px-5 py-4"><b>{trip.routeCode}</b><p className="text-slate-500">{trip.origin} → {trip.destination}</p></td><td className="px-5 py-4 font-bold">{trip.busNumber}</td><td className="px-5 py-4">{trip.driverName}</td><td className="px-5 py-4">{formatTripDate(trip.departureAt)}</td><td className="px-5 py-4">{formatTripDate(trip.estimatedArrivalAt)}</td><td className="px-5 py-4 font-bold">NPR {Number(trip.fare).toLocaleString()}</td><td className="px-5 py-4">{trip.seatCapacity}</td><td className="px-5 py-4"><span className={`rounded-full px-3 py-1 text-xs font-black ${statusTone(trip.status)}`}>{statusLabel(trip.status)}</span></td></tr>)}</tbody></table></div></div>}
    <button onClick={loadTrips} disabled={loading} className="flex items-center gap-2 text-sm font-black disabled:opacity-50"><RefreshCw size={16} /> Refresh</button>
  </div></OperatorLayout>;
}
