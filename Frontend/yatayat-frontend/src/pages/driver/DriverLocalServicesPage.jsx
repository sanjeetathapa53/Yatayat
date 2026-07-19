import { useCallback, useEffect, useState } from "react";
import { Bus, CalendarDays, Clock3, Loader2, MapPin, RefreshCw, Route } from "lucide-react";
import { useNavigate } from "react-router-dom";
import DriverLayout from "../../components/layout/DriverLayout";
import { formatServiceDate, handleDriverLocalAccess, localServiceRequest, serviceStatusLabel, serviceStatusTone } from "../../utils/localServices";

export default function DriverLocalServicesPage() {
  const navigate = useNavigate();
  const [runs, setRuns] = useState([]);
  const [loading, setLoading] = useState(true);
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

        <div className="rounded-3xl border border-blue-100 bg-blue-50 p-4 text-sm font-bold text-blue-900">
          Service start/finish controls and live GPS tracking will be added in the next local operations phase.
        </div>

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
            {runs.map((run) => <DriverLocalServiceCard key={run.id} run={run} />)}
          </div>
        )}
      </div>
    </DriverLayout>
  );
}

function DriverLocalServiceCard({ run }) {
  return (
    <article className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-3 py-1 text-xs font-black ${serviceStatusTone(run.status)}`}>{serviceStatusLabel(run.status)}</span>
            <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-black text-emerald-700">LOCAL</span>
          </div>
          <h2 className="safe-wrap mt-3 text-xl font-black text-slate-900">{run.routeCode} · {run.routeName}</h2>
          <p className="safe-wrap mt-1 text-sm font-semibold text-slate-500">{run.origin} → {run.destination}</p>
        </div>
        <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-black text-slate-700">
          {formatServiceDate(run.serviceDate, run.plannedStartTime, run.plannedEndTime)}
        </div>
      </div>

      <div className="mt-5 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <Meta icon={<Bus size={17} />} label="Bus" value={`${run.busNumber} · ${run.busName}`} />
        <Meta icon={<Clock3 size={17} />} label="Planned time" value={`${run.plannedStartTime} - ${run.plannedEndTime}`} />
        <Meta icon={<Route size={17} />} label="Route stops" value={`${run.orderedStops?.length || 0} stops`} />
      </div>

      {run.orderedStops?.length > 0 && (
        <div className="mt-5 rounded-3xl border border-slate-100 bg-slate-50 p-4">
          <h3 className="font-black text-slate-900">Ordered stop summary</h3>
          <div className="mt-4 grid gap-2">
            {run.orderedStops.map((stop) => (
              <div key={`${run.id}-${stop.stopOrder}`} className="flex gap-3 rounded-2xl bg-white p-3">
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#08264a] text-xs font-black text-white">{stop.stopOrder}</div>
                <div className="min-w-0">
                  <p className="safe-wrap font-black text-slate-900">{stop.stopName}</p>
                  <p className="safe-wrap text-xs font-semibold text-slate-500"><MapPin size={13} className="mr-1 inline" />{stop.landmark || "Local route stop"} · {stop.estimatedMinutesFromStart} min · NPR {stop.cumulativeFare}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </article>
  );
}

function Meta({ icon, label, value }) {
  return <div className="rounded-2xl bg-slate-50 p-4"><div className="flex items-center gap-2 text-[11px] font-black uppercase text-slate-500">{icon}{label}</div><p className="safe-wrap mt-2 font-black text-slate-800">{value}</p></div>;
}
