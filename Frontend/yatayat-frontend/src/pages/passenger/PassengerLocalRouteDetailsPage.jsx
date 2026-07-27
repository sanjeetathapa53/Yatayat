import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, Bus, Clock3, Loader2, MapPin, WalletCards } from "lucide-react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { localRouteRequest } from "../../utils/localRoutes";
import { getActiveLocalServices } from "../../utils/passengerLocalLiveTracking";

export default function PassengerLocalRouteDetailsPage() {
  const { routeId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [route, setRoute] = useState(null);
  const [error, setError] = useState("");
  const [activeBuses, setActiveBuses] = useState([]);
  const [trackingError, setTrackingError] = useState("");
  const fromStopId = location.state?.fromStopId;
  const toStopId = location.state?.toStopId;

  useEffect(() => {
    localRouteRequest(`/api/passenger/local-routes/${routeId}`)
      .then(setRoute)
      .catch((requestError) => {
        if (requestError.status === 401) navigate("/login", { replace: true });
        else setError(requestError.message);
      });
  }, [navigate, routeId]);

  useEffect(() => {
    const controller = new AbortController();
    const loadActiveBuses = async () => {
      try {
        setActiveBuses(await getActiveLocalServices(controller.signal, routeId));
        setTrackingError("");
      } catch (requestError) {
        if (requestError.name !== "AbortError") {
          setTrackingError(requestError.message || "Active buses could not be loaded.");
        }
      }
    };
    Promise.resolve().then(loadActiveBuses);
    const interval = window.setInterval(loadActiveBuses, 7000);
    return () => {
      controller.abort();
      window.clearInterval(interval);
    };
  }, [routeId]);

  const selectedSegment = useMemo(() => {
    if (!route?.orderedStops?.length || !fromStopId || !toStopId) return null;
    const from = route.orderedStops.find((stop) => stop.busStopId === fromStopId);
    const to = route.orderedStops.find((stop) => stop.busStopId === toStopId);
    if (!from || !to || from.stopOrder >= to.stopOrder) return null;
    return {
      fare: Number(to.cumulativeFare) - Number(from.cumulativeFare),
      duration: to.estimatedMinutesFromStart - from.estimatedMinutesFromStart,
      fromOrder: from.stopOrder,
      toOrder: to.stopOrder,
    };
  }, [fromStopId, route, toStopId]);

  return (
    <PassengerLayout activePage="Find Local Route" title={route?.routeName || "Route details"} subtitle={route?.routeCode || "View local route information and stops."}>
      <div className="mx-auto max-w-4xl space-y-6">
        <button type="button" onClick={() => navigate("/passenger/local-routes")} className="tap-target flex items-center gap-2 text-sm font-black"><ArrowLeft size={17} /> Back to local route search</button>
        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div>}
        {!route && !error ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : route && <>
          <header className="rounded-3xl bg-[#08264a] p-5 text-white sm:p-7">
            <span className="rounded-full bg-emerald-400/20 px-3 py-1 text-xs font-black text-emerald-100">LOCAL</span>
          </header>

          <section className="grid gap-4 sm:grid-cols-3">
            <Info icon={<WalletCards size={18} />} label="Estimated fare" value={selectedSegment ? `NPR ${selectedSegment.fare}` : route.fareInformation} />
            <Info icon={<Clock3 size={18} />} label="Estimated duration" value={`${selectedSegment?.duration ?? route.segmentDurationMinutes ?? route.estimatedDurationMinutes} minutes`} />
            <Info icon={<Bus size={18} />} label="Operating hours" value={`${route.operatingStartTime || "--"} to ${route.operatingEndTime || "--"}`} />
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <h2 className="text-xl font-black">Stop timeline</h2>
            <p className="mt-1 text-sm font-semibold text-slate-500">Choose an ordered segment, then purchase a secure 24-hour local fare pass with your wallet.</p>
            <div className="mt-6 space-y-3">
              {(route.orderedStops?.length ? route.orderedStops : route.stopSummary.map((name, index) => ({ stopOrder: index + 1, stopName: name }))).map((stop) => {
                const highlighted = selectedSegment && stop.stopOrder >= selectedSegment.fromOrder && stop.stopOrder <= selectedSegment.toOrder;
                return <div key={`${stop.stopOrder}-${stop.stopName}`} className={`flex gap-3 rounded-2xl border p-4 ${highlighted ? "border-emerald-200 bg-emerald-50" : "border-slate-100 bg-slate-50"}`}><div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#08264a] text-sm font-black text-white">{stop.stopOrder}</div><div className="min-w-0"><p className="safe-wrap font-black text-slate-900">{stop.stopName}</p><p className="safe-wrap text-sm font-semibold text-slate-500">{stop.landmark || "Local route stop"} · {stop.estimatedMinutesFromStart ?? "--"} min · NPR {stop.cumulativeFare ?? "--"}</p></div></div>;
              })}
            </div>
            {selectedSegment && (
              <button
                type="button"
                onClick={() => navigate("/fare-pass", { state: { routeId: route.routeId, boardingStopId: fromStopId, destinationStopId: toStopId } })}
                className="mt-5 w-full rounded-xl bg-[#08264a] px-5 py-3 font-black text-white"
              >
                Buy Fare Pass · NPR {selectedSegment.fare}
              </button>
            )}
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <h2 className="text-xl font-black">Active Buses</h2>
            <p className="mt-1 text-sm font-semibold text-slate-500">Buses currently operating on this local route.</p>
            {trackingError && <p className="mt-4 rounded-xl bg-red-50 p-4 text-sm font-bold text-red-700">{trackingError}</p>}
            {!trackingError && activeBuses.length === 0 && (
              <p className="mt-4 rounded-xl bg-slate-50 p-4 text-sm font-bold text-slate-500">No buses are currently in service on this route.</p>
            )}
            <div className="mt-4 space-y-3">
              {activeBuses.map((service) => (
                <div key={service.runId} className="flex flex-col justify-between gap-3 rounded-2xl border border-slate-200 p-4 sm:flex-row sm:items-center">
                  <div>
                    <p className="font-black text-slate-900">{service.busNumber}</p>
                    {service.busName && <p className="text-sm font-semibold text-slate-500">{service.busName}</p>}
                    <p className="mt-2 flex items-center gap-1 text-xs font-bold text-slate-500">
                      <MapPin size={14} />
                      {formatLastUpdate(service.updatedAt)}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => navigate(`/track-bus?operationType=LOCAL&operationId=${service.runId}`)}
                    className="tap-target rounded-xl bg-emerald-600 px-5 py-3 text-sm font-black text-white"
                  >
                    Track
                  </button>
                </div>
              ))}
            </div>
          </section>
        </>}
      </div>
    </PassengerLayout>
  );
}

function formatLastUpdate(value) {
  if (!value) return "Waiting for GPS";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : `Last update ${new Intl.DateTimeFormat("en-NP", { timeStyle: "medium" }).format(date)}`;
}

function Info({ icon, label, value }) {
  return <div className="rounded-2xl bg-white p-4 shadow-sm"><div className="flex items-center gap-2 text-xs font-black uppercase text-slate-500">{icon}{label}</div><p className="safe-wrap mt-2 font-black text-slate-900">{value}</p></div>;
}
