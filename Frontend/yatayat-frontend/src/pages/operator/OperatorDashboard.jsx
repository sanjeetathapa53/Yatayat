import { useEffect, useState } from "react";
import { Building2, Bus, CalendarDays, CheckCircle2, Loader2, Users } from "lucide-react";
import { useNavigate } from "react-router-dom";
import OperatorLayout from "../../components/layout/OperatorLayout";
import { apiFetch } from "../../utils/api";
import { formatTripDate, getOperatorLiveTrips, statusLabel, statusTone } from "../../utils/operatorTrips";

export default function OperatorDashboard() {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState(null);
  const [tripCounts, setTripCounts] = useState({ upcoming: 0, active: 0 });
  const [liveTrips, setLiveTrips] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    Promise.all([apiFetch("/api/operator/dashboard"), apiFetch("/api/operator/trips"), getOperatorLiveTrips()]).then(async ([response, tripsResponse, live]) => {
      if (response.status === 401) {
        navigate("/login", { replace: true });
        return null;
      }
      if (response.status === 403 || response.status === 404) {
        navigate("/operator/application-status", { replace: true });
        return null;
      }
      if (!response.ok) throw new Error("Unable to load operator dashboard.");
      if (!tripsResponse.ok) throw new Error("Unable to load operator trips.");
      return Promise.all([response.json(), tripsResponse.json(), live]);
    }).then((data) => {
      if (active && data) {
        const [dashboardData, trips, live] = data;
        const now = new Date();
        setDashboard(dashboardData);
        setLiveTrips(live || []);
        setTripCounts({
          upcoming: trips.filter((trip) => ["SCHEDULED", "BOARDING"].includes(trip.status) && new Date(trip.departureAt) > now).length,
          active: trips.filter((trip) => ["BOARDING", "IN_PROGRESS"].includes(trip.status)).length,
        });
      }
    }).catch((loadError) => {
      if (active) setError(loadError.message);
    });

    return () => { active = false; };
  }, [navigate]);

  return (
    <OperatorLayout>
      {error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div>
      ) : !dashboard ? (
        <div className="flex min-h-[360px] items-center justify-center"><Loader2 className="animate-spin" size={40} /></div>
      ) : (
        <div className="space-y-6">
          <section className="rounded-3xl bg-[#08264a] p-7 text-white shadow-sm">
            <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-center">
              <div>
                <p className="text-sm font-bold text-blue-200">Namaste,</p>
                <h1 className="mt-2 text-3xl font-black">{dashboard.organizationName}</h1>
                <p className="mt-2 text-sm text-slate-300">Registration: {dashboard.registrationNumber}</p>
              </div>
              <span className="inline-flex items-center gap-2 self-start rounded-full bg-emerald-500/20 px-4 py-2 text-sm font-black text-emerald-100">
                <CheckCircle2 size={18} /> {dashboard.approvalStatus}
              </span>
            </div>
          </section>

          <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
            <Stat label="Total Buses" value={dashboard.totalBuses} icon={<Bus />} />
            <Stat label="Pending Buses" value={dashboard.pendingBuses} icon={<Building2 />} />
            <Stat label="Associated Drivers" value={dashboard.totalAssociatedDrivers} icon={<Users />} />
            <Stat label="Upcoming Trips" value={tripCounts.upcoming} icon={<CalendarDays />} />
            <Stat label="Active Trips" value={tripCounts.active} icon={<CheckCircle2 />} />
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
              <div>
                <h2 className="text-xl font-black">Live Trip Monitoring</h2>
                <p className="mt-1 text-sm text-slate-500">Boarding, on-the-way, and recently completed trips.</p>
              </div>
              <button type="button" onClick={() => navigate("/operator/trips")} className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-black text-slate-700">All trips</button>
            </div>
            <div className="mt-5 space-y-3">
              {liveTrips.length === 0 ? (
                <p className="rounded-2xl bg-slate-50 p-5 text-sm font-bold text-slate-500">No live trips to monitor right now.</p>
              ) : liveTrips.map((trip) => (
                <article key={trip.scheduledTripId} className="rounded-2xl border border-slate-100 bg-slate-50 p-4">
                  <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                    <div>
                      <p className="font-black text-slate-900">{trip.origin} → {trip.destination}</p>
                      <p className="mt-1 text-sm font-semibold text-slate-500">{trip.busNumber} • {trip.driverName}</p>
                      <p className="mt-1 text-xs font-bold text-slate-400">Departure {formatTripDate(trip.departureAt)}</p>
                    </div>
                    <span className={`self-start rounded-full px-3 py-1 text-xs font-black ${statusTone(trip.status)}`}>{statusLabel(trip.status)}</span>
                  </div>
                  <div className="mt-4 grid gap-3 sm:grid-cols-3">
                    <MiniStat label="Passengers" value={trip.passengerCount} />
                    <MiniStat label="Boarded" value={trip.boardedCount} />
                    <MiniStat label="Started" value={formatTripDate(trip.startedAt)} />
                  </div>
                </article>
              ))}
            </div>
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-xl font-black">Quick Actions</h2>
            <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
              {[
                { label: "Register Bus", path: "/operator/buses/register" },
                { label: "Manage Buses", path: "/operator/buses" },
                { label: "Manage Drivers", path: "/operator/drivers" },
                { label: "Plan Local Service", path: "/operator/local-services" },
                { label: "Create Scheduled Trip", path: "/operator/trips/create" },
                { label: "View Active Trips", path: "/operator/trips" },
                { label: "Monitor Live Fleet", path: "/operator/live-fleet" },
              ].map((action) => (
                <button key={action.label} type="button" disabled={!action.path} onClick={() => action.path && navigate(action.path)} className="rounded-2xl border border-slate-200 bg-slate-50 p-5 text-left disabled:opacity-70">
                  <p className="font-black text-slate-700">{action.label}</p>
                  <p className="mt-2 text-xs font-bold text-slate-500">{action.path ? "Open" : "Coming next"}</p>
                </button>
              ))}
            </div>
          </section>

          {dashboard.totalBuses === 0 && (
            <section className="rounded-3xl border border-blue-200 bg-blue-50 p-6">
              <h2 className="font-black">Your organization is approved.</h2>
              <p className="mt-2 text-sm text-slate-600">Register your first bus to continue.</p>
            </section>
          )}
        </div>
      )}
    </OperatorLayout>
  );
}

function Stat({ label, value, icon }) {
  return (
    <article className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="text-[#08264a]">{icon}</div>
      <p className="mt-4 text-xs font-black uppercase tracking-wider text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-black">{value}</p>
    </article>
  );
}

function MiniStat({ label, value }) {
  return (
    <div className="rounded-2xl bg-white p-3">
      <p className="text-[10px] font-black uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-1 text-sm font-black text-slate-800">{value || "—"}</p>
    </div>
  );
}
