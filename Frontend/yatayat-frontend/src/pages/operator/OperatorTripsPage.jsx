import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertTriangle, CalendarDays, CheckCircle2, Loader2, Plus, RefreshCw } from "lucide-react";
import { useNavigate } from "react-router-dom";
import OperatorLayout from "../../components/layout/OperatorLayout";
import {
  formatTripDate,
  handleOperatorAccess,
  statusLabel,
  statusTone,
  TRIP_STATUSES,
  tripRequest,
} from "../../utils/operatorTrips";

export default function OperatorTripsPage() {
  const navigate = useNavigate();
  const [trips, setTrips] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadTrips = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setTrips(await tripRequest("/api/operator/trips"));
    } catch (loadError) {
      if (!handleOperatorAccess(loadError, navigate)) setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    Promise.resolve().then(loadTrips);
  }, [loadTrips]);

  const visibleTrips = useMemo(
    () => (filter === "ALL" ? trips : trips.filter((trip) => trip.status === filter)),
    [filter, trips]
  );

  return (
    <OperatorLayout>
      <div className="space-y-6">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-3xl font-black">Scheduled Trips</h1>
            <p className="mt-1 text-sm text-slate-500">
              Plan and manage your organization&apos;s scheduled services.
            </p>
          </div>
          <button
            onClick={() => navigate("/operator/trips/create")}
            className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white"
          >
            <Plus size={18} /> Create Trip
          </button>
        </div>

        <div className="flex flex-wrap gap-2">
          {["ALL", ...TRIP_STATUSES].map((status) => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={`rounded-full px-4 py-2 text-xs font-black ${
                filter === status
                  ? "bg-[#08264a] text-white"
                  : "border border-slate-200 bg-white text-slate-600"
              }`}
            >
              {status === "ALL" ? "All" : statusLabel(status)}
            </button>
          ))}
        </div>

        {error && (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">
            {error}
          </div>
        )}

        {loading ? (
          <div className="flex min-h-72 items-center justify-center">
            <Loader2 className="animate-spin" size={40} />
          </div>
        ) : visibleTrips.length === 0 ? (
          <div className="rounded-3xl border border-slate-200 bg-white px-6 py-16 text-center">
            <CalendarDays className="mx-auto text-slate-300" size={48} />
            <h2 className="mt-5 text-xl font-black">
              No {filter === "ALL" ? "scheduled trips" : `${statusLabel(filter).toLowerCase()} trips`} found
            </h2>
            <p className="mt-2 text-sm text-slate-500">
              Create a trip when an eligible route, bus, and driver are available.
            </p>
          </div>
        ) : (
          <div className="grid gap-4">
            {visibleTrips.map((trip) => (
              <TripCard
                key={trip.id}
                trip={trip}
                onOpen={() => navigate(`/operator/trips/${trip.id}`)}
              />
            ))}
          </div>
        )}

        <button
          onClick={loadTrips}
          disabled={loading}
          className="flex items-center gap-2 text-sm font-black disabled:opacity-50"
        >
          <RefreshCw size={16} /> Refresh
        </button>
      </div>
    </OperatorLayout>
  );
}

function TripCard({ trip, onOpen }) {
  const confirmed = trip.confirmedPassengers || 0;
  const boarded = trip.boardedPassengers || 0;
  const remaining = Math.max(0, confirmed - boarded);

  return (
    <button
      type="button"
      onClick={onOpen}
      className="rounded-3xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-md"
    >
      <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-3 py-1 text-xs font-black ${statusTone(trip.status)}`}>
              {statusLabel(trip.status)}
            </span>
            {trip.assignmentComplete ? (
              <span className="flex items-center gap-1 rounded-full bg-emerald-50 px-3 py-1 text-xs font-black text-emerald-700">
                <CheckCircle2 size={14} /> Assigned
              </span>
            ) : (
              <span className="flex items-center gap-1 rounded-full bg-amber-50 px-3 py-1 text-xs font-black text-amber-700">
                <AlertTriangle size={14} /> Assignment needed
              </span>
            )}
          </div>
          <h2 className="mt-3 text-xl font-black text-slate-900">
            {trip.routeCode} · {trip.routeName}
          </h2>
          <p className="mt-1 text-sm font-semibold text-slate-500">
            {trip.origin} → {trip.destination}
          </p>
        </div>
        <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-black text-slate-700">
          NPR {Number(trip.fare).toLocaleString()}
        </div>
      </div>

      <div className="mt-5 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
        <TripMeta label="Departure" value={formatTripDate(trip.departureAt)} />
        <TripMeta label="Estimated arrival" value={formatTripDate(trip.estimatedArrivalAt)} />
        <TripMeta label="Bus" value={trip.busNumber || "Not assigned"} warn={!trip.busNumber} />
        <TripMeta label="Driver" value={trip.driverName || "Not assigned"} warn={!trip.driverName} />
        <TripMeta label="Capacity" value={`${trip.seatCapacity || 0} seats`} />
        <TripMeta label="Confirmed" value={`${confirmed}`} />
        <TripMeta label="Boarded" value={`${boarded}`} />
        <TripMeta label="Remaining" value={`${remaining}`} />
      </div>
    </button>
  );
}

function TripMeta({ label, value, warn }) {
  return (
    <div className={`rounded-2xl p-4 ${warn ? "bg-amber-50 text-amber-800" : "bg-slate-50 text-slate-700"}`}>
      <p className="text-[11px] font-black uppercase tracking-wide opacity-70">{label}</p>
      <p className="mt-1 font-black">{value}</p>
    </div>
  );
}
