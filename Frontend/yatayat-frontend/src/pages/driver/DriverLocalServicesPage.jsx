import { useCallback, useEffect, useState } from "react";
import { Bus, CalendarDays, Clock3, Loader2, RefreshCw, Route } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import DriverRouteStops from "../../components/driver/DriverRouteStops";
import DriverLayout from "../../components/layout/DriverLayout";
import {
  formatServiceDate,
  handleDriverLocalAccess,
  localServiceRequest,
  serviceStatusLabel,
  serviceStatusTone,
  startDriverLocalService,
} from "../../utils/localServices";

export default function DriverLocalServicesPage() {
  const navigate = useNavigate();
  const [runs, setRuns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [startingId, setStartingId] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setRuns(await localServiceRequest("/api/driver/local-services"));
    } catch (loadError) {
      if (!handleDriverLocalAccess(loadError, navigate)) setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    Promise.resolve().then(load);
  }, [load]);

  const startService = async (run) => {
    if (startingId !== null) return;
    setStartingId(run.id);
    setError("");
    try {
      await startDriverLocalService(run.id);
      toast.success("Local service started.");
      navigate("/driver/trip");
    } catch (startError) {
      if (!handleDriverLocalAccess(startError, navigate)) {
        setError(startError.message || "Local service could not be started.");
      }
    } finally {
      setStartingId(null);
    }
  };

  return (
    <DriverLayout activePage="Local Services">
      <div className="space-y-6">
        <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-3xl font-black text-slate-900">Local Assignments</h1>
            <p className="mt-1 text-sm text-slate-500">View upcoming local service assignments from your operator.</p>
          </div>
          <button type="button" onClick={load} disabled={loading} className="flex items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-3 font-black text-slate-700 disabled:opacity-60">
            <RefreshCw size={17} className={loading ? "animate-spin" : ""} /> Refresh
          </button>
        </header>

        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}

        {loading ? (
          <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div>
        ) : runs.length === 0 ? (
          <div className="rounded-3xl border border-slate-200 bg-white px-6 py-16 text-center shadow-sm">
            <CalendarDays className="mx-auto text-slate-300" size={48} />
            <h2 className="mt-5 text-xl font-black">No local assignments yet</h2>
            <p className="mt-2 text-sm text-slate-500">Assigned local services will appear here after your operator schedules them.</p>
          </div>
        ) : (
          <div className="grid gap-4">
            {runs.map((run) => (
              <DriverLocalServiceCard
                key={run.id}
                run={run}
                starting={startingId === run.id}
                onStart={() => startService(run)}
                onOpen={() => navigate("/driver/trip")}
              />
            ))}
          </div>
        )}
      </div>
    </DriverLayout>
  );
}

function DriverLocalServiceCard({ run, starting, onStart, onOpen }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${serviceStatusTone(run.status)}`}>{serviceStatusLabel(run.status)}</span>
            <span className="rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-700">LOCAL</span>
          </div>
          <h2 className="safe-wrap mt-2 text-lg font-semibold leading-6 text-slate-900">{run.routeCode} · {run.routeName}</h2>
          <p className="safe-wrap mt-0.5 text-sm font-medium text-slate-500">{run.origin} → {run.destination}</p>
        </div>
        <div className="w-fit rounded-xl bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-700">
          {formatServiceDate(run.serviceDate, run.plannedStartTime, run.plannedEndTime)}
        </div>
      </div>

      <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <Meta icon={<Bus size={17} />} label="Bus" value={`${run.busNumber} · ${run.busName}`} />
        <Meta icon={<Clock3 size={17} />} label="Planned time" value={`${run.plannedStartTime} - ${run.plannedEndTime}`} />
        <Meta icon={<Route size={17} />} label="Route stops" value={`${run.orderedStops?.length || 0} stops`} />
      </div>

      <DriverRouteStops stops={run.orderedStops} title="Ordered stop summary" className="mt-4" />

      <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-3">
        {run.status === "PLANNED" && (
          <button
            type="button"
            disabled={starting}
            onClick={onStart}
            className="inline-flex h-10 items-center rounded-xl bg-[#08264a] px-4 text-sm font-semibold text-white disabled:opacity-60"
          >
            {starting ? "Starting..." : "Start Service"}
          </button>
        )}
        {run.status === "IN_SERVICE" && (
          <button
            type="button"
            onClick={onOpen}
            className="inline-flex h-10 items-center rounded-xl bg-emerald-600 px-4 text-sm font-semibold text-white"
          >
            Open Trip Management
          </button>
        )}
        {run.status === "COMPLETED" && (
          <span className="inline-flex h-10 items-center rounded-xl bg-slate-100 px-4 text-sm font-semibold text-slate-600">
            Completed
          </span>
        )}
      </div>
    </article>
  );
}

function Meta({ icon, label, value }) {
  return <div className="rounded-xl border border-slate-100 bg-slate-50 px-3 py-2.5"><div className="flex items-center gap-2 text-[11px] font-semibold uppercase text-slate-500">{icon}{label}</div><p className="safe-wrap mt-1 text-sm font-semibold text-slate-800">{value}</p></div>;
}
