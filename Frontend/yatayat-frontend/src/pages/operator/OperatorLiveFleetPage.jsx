import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Bus, Clock3, Gauge, Loader2, MapPinOff, Navigation, Route, UserRound } from "lucide-react";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import L from "leaflet";
import OperatorLayout from "../../components/layout/OperatorLayout";
import {
  getOperatorFleetLocations,
  getOperatorFleetTrip,
  getOperatorLocalFleetLocations,
  getOperatorLocalFleetRun,
} from "../../utils/operatorFleet";

const POLL_INTERVAL_MS = 7000;
const DEFAULT_CENTER = [27.7105, 85.318];
const fleetIcon = new L.DivIcon({
  html: '<div style="width:42px;height:42px;display:flex;align-items:center;justify-content:center;border:4px solid white;border-radius:50%;background:#08264a;color:white;font-size:21px;box-shadow:0 8px 22px rgba(0,0,0,.3)">🚌</div>',
  className: "",
  iconSize: [42, 42],
  iconAnchor: [21, 21],
});

export default function OperatorLiveFleetPage() {
  const [fleet, setFleet] = useState([]);
  const [selectedOperationKey, setSelectedOperationKey] = useState(null);
  const [completedTrip, setCompletedTrip] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const selectedOperationKeyRef = useRef(null);

  useEffect(() => {
    selectedOperationKeyRef.current = selectedOperationKey;
  }, [selectedOperationKey]);

  const loadFleet = useCallback(async (signal) => {
    try {
      const [scheduledData, localData] = await Promise.all([
        getOperatorFleetLocations(signal),
        getOperatorLocalFleetLocations(signal),
      ]);
      if (signal.aborted) return;
      const data = [
        ...scheduledData.map(normalizeScheduledOperation),
        ...localData.map(normalizeLocalOperation),
      ];
      setFleet(data);
      setError("");

      const selected = selectedOperationKeyRef.current;
      if (selected && !data.some((trip) => trip.operationKey === selected)) {
        try {
          const [type, id] = selected.split(":");
          const previous = type === "LOCAL"
            ? normalizeLocalOperation(await getOperatorLocalFleetRun(id, signal))
            : normalizeScheduledOperation(await getOperatorFleetTrip(id, signal));
          if (!signal.aborted && previous.status === "COMPLETED") setCompletedTrip(previous);
        } catch (lookupError) {
          if (lookupError.name !== "AbortError") setCompletedTrip(null);
        }
        setSelectedOperationKey(data[0]?.operationKey || null);
      } else if (!selected && data[0]) {
        setSelectedOperationKey(data[0].operationKey);
      }
      if (selected && data.some((trip) => trip.operationKey === selected)) {
        setCompletedTrip(null);
      }
    } catch (loadError) {
      if (loadError.name !== "AbortError") {
        setError(loadError.message || "The live fleet service is temporarily unavailable.");
      }
    } finally {
      if (!signal.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    Promise.resolve().then(() => loadFleet(controller.signal));
    const interval = window.setInterval(() => loadFleet(controller.signal), POLL_INTERVAL_MS);
    return () => {
      controller.abort();
      window.clearInterval(interval);
    };
  }, [loadFleet]);

  const selectedTrip = useMemo(
    () => fleet.find((trip) => trip.operationKey === selectedOperationKey) || null,
    [fleet, selectedOperationKey],
  );
  const selectedPosition = coordinates(selectedTrip);

  return (
    <OperatorLayout>
      <div className="space-y-5">
        <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">Live Fleet Monitoring</h1>
            <p className="mt-1 text-sm font-semibold text-slate-500">Your active buses refresh automatically every seven seconds.</p>
          </div>
          <div className="rounded-full bg-emerald-100 px-4 py-2 text-sm font-semibold text-emerald-700">
            {loading ? <span className="flex items-center gap-2"><Loader2 size={16} className="animate-spin" /> Loading</span> : `${fleet.length} active`}
          </div>
        </header>

        {error && <Notice tone="red" title="Network Error">{error} The page will retry automatically.</Notice>}
        {completedTrip && <Notice tone="blue" title="Trip completed">{completedTrip.busNumber} has completed its trip and was removed from the active map.</Notice>}
        {!loading && !error && fleet.length === 0 && <Notice title="No active buses">No scheduled trips or local services in your fleet are currently active.</Notice>}

        <div className="grid min-h-[650px] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm xl:grid-cols-[360px_1fr]">
          <aside className="max-h-[650px] overflow-y-auto border-b border-slate-200 p-4 xl:border-b-0 xl:border-r">
            <h2 className="px-1 text-lg font-semibold">Active buses</h2>
            <div className="mt-4 space-y-3">
              {fleet.map((trip) => {
                const hasGps = Boolean(coordinates(trip));
                return (
                  <button
                    key={trip.operationKey}
                    type="button"
                    onClick={() => setSelectedOperationKey(trip.operationKey)}
                    className={`w-full rounded-2xl border p-4 text-left transition ${selectedOperationKey === trip.operationKey ? "border-[#08264a] bg-blue-50 shadow-sm" : "border-slate-200 hover:bg-slate-50"}`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <OperationBadge type={trip.operationType} />
                        <p className="font-semibold text-slate-900">{trip.busNumber}</p>
                        <p className="text-xs font-semibold text-slate-500">{trip.busName}</p>
                      </div>
                      <span className="rounded-full bg-emerald-100 px-2.5 py-1 text-[10px] font-semibold text-emerald-700">{statusLabel(trip.status)}</span>
                    </div>
                    <div className="mt-3 space-y-2 text-sm text-slate-600">
                      <Line icon={<UserRound size={15} />} value={trip.driverName} />
                      <Line icon={<Route size={15} />} value={routeLabel(trip)} />
                      <Line icon={hasGps ? <Clock3 size={15} /> : <MapPinOff size={15} />} value={hasGps ? formatUpdatedAt(trip.updatedAt) : "GPS unavailable"} />
                      {trip.speed != null && <Line icon={<Gauge size={15} />} value={`${formatSpeed(trip.speed)} km/h`} />}
                    </div>
                  </button>
                );
              })}
            </div>
          </aside>

          <section className="relative min-h-[500px]">
            <MapContainer center={DEFAULT_CENTER} zoom={13} scrollWheelZoom className="h-full min-h-[500px] w-full">
              <TileLayer attribution="© OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              {fleet.map((trip) => {
                const position = coordinates(trip);
                if (!position) return null;
                return (
                  <Marker
                    key={trip.operationKey}
                    position={position}
                    icon={fleetIcon}
                    eventHandlers={{ click: () => setSelectedOperationKey(trip.operationKey) }}
                  >
                    <Popup><strong>{trip.busNumber}</strong><br />{trip.driverName}<br />{routeLabel(trip)}</Popup>
                  </Marker>
                );
              })}
              {selectedPosition && <CenterSelectedBus position={selectedPosition} operationKey={selectedOperationKey} />}
            </MapContainer>
            {fleet.length > 0 && fleet.every((trip) => !coordinates(trip)) && (
              <div className="absolute left-1/2 top-5 z-[500] -translate-x-1/2 rounded-2xl bg-white px-5 py-4 text-center shadow-xl">
                <MapPinOff className="mx-auto text-amber-600" />
                <p className="mt-2 font-semibold">GPS unavailable</p>
                <p className="text-xs text-slate-500">Active trips are waiting for driver coordinates.</p>
              </div>
            )}
          </section>
        </div>

        {selectedTrip && (
          <section className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-2 lg:grid-cols-4">
            <Detail icon={<Bus />} label={`Bus · ${operationLabel(selectedTrip.operationType)}`} value={`${selectedTrip.busNumber} · ${selectedTrip.busName}`} />
            <Detail icon={<UserRound />} label="Driver" value={selectedTrip.driverName} />
            <Detail icon={<Navigation />} label="Route" value={routeLabel(selectedTrip)} />
            <Detail icon={<Clock3 />} label="Last updated" value={formatUpdatedAt(selectedTrip.updatedAt)} />
          </section>
        )}
      </div>
    </OperatorLayout>
  );
}

function CenterSelectedBus({ position, operationKey }) {
  const map = useMap();
  useEffect(() => {
    map.flyTo(position, Math.max(map.getZoom(), 15), { duration: 0.8 });
  }, [map, position, operationKey]);
  return null;
}

function Notice({ title, children, tone = "amber" }) {
  const style = tone === "red" ? "border-red-200 bg-red-50 text-red-800" : tone === "blue" ? "border-blue-200 bg-blue-50 text-blue-800" : "border-amber-200 bg-amber-50 text-amber-800";
  return <div className={`rounded-2xl border p-4 text-sm ${style}`}><p className="font-semibold">{title}</p><p className="mt-1 font-semibold">{children}</p></div>;
}

function Line({ icon, value }) {
  return <p className="flex items-center gap-2"><span className="text-slate-400">{icon}</span><span className="truncate">{value || "Unavailable"}</span></p>;
}

function Detail({ icon, label, value }) {
  return <div className="flex items-start gap-3 rounded-xl bg-slate-50 p-3"><span className="text-[#08264a]">{icon}</span><div><p className="text-[10px] font-semibold uppercase text-slate-400">{label}</p><p className="mt-1 font-semibold">{value}</p></div></div>;
}

function OperationBadge({ type }) {
  const local = type === "LOCAL";
  return (
    <span className={`mb-2 inline-flex rounded-full px-2.5 py-1 text-[10px] font-semibold tracking-wide ${local ? "bg-blue-100 text-blue-700" : "bg-violet-100 text-violet-700"}`}>
      {operationLabel(type)}
    </span>
  );
}

function normalizeScheduledOperation(trip) {
  return {
    ...trip,
    operationType: "OUT_OF_VALLEY",
    operationId: trip.tripId,
    operationKey: `OUT_OF_VALLEY:${trip.tripId}`,
    status: trip.tripStatus,
  };
}

function normalizeLocalOperation(run) {
  return {
    ...run,
    operationType: "LOCAL",
    operationId: run.runId,
    operationKey: `LOCAL:${run.runId}`,
    status: run.serviceStatus,
  };
}

function operationLabel(type) {
  return type === "LOCAL" ? "LOCAL" : "OUT OF VALLEY";
}

function coordinates(trip) {
  return Number.isFinite(trip?.latitude) && Number.isFinite(trip?.longitude)
    ? [trip.latitude, trip.longitude]
    : null;
}

function routeLabel(trip) {
  return trip.routeName || `${trip.origin || "Unknown origin"} → ${trip.destination || "Unknown destination"}`;
}

function statusLabel(status) {
  return String(status || "UNKNOWN").replaceAll("_", " ");
}

function formatUpdatedAt(value) {
  if (!value) return "Waiting for GPS";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat("en-NP", { dateStyle: "medium", timeStyle: "medium" }).format(date);
}

function formatSpeed(metersPerSecond) {
  return (metersPerSecond * 3.6).toFixed(1);
}
