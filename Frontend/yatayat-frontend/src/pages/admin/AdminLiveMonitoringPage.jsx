import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Building2, Clock3, Gauge, MapPinOff, Navigation, Radio, Route, Search, UserRound } from "lucide-react";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import L from "leaflet";
import AdminLayout from "../../components/layout/AdminLayout";
import { getAdminLiveFleet, getAdminLiveFleetTrip } from "../../utils/adminFleet";

const POLL_INTERVAL_MS = 7000;
const DEFAULT_CENTER = [27.7105, 85.318];
const busIcon = new L.DivIcon({
  html: '<div style="width:42px;height:42px;display:flex;align-items:center;justify-content:center;border:4px solid white;border-radius:50%;background:#08264a;color:white;font-size:21px;box-shadow:0 8px 22px rgba(0,0,0,.3)">🚌</div>',
  className: "",
  iconSize: [42, 42],
  iconAnchor: [21, 21],
});

export default function AdminLiveMonitoringPage() {
  const [fleet, setFleet] = useState([]);
  const [selectedTripId, setSelectedTripId] = useState(null);
  const [focusVersion, setFocusVersion] = useState(0);
  const [completedTrip, setCompletedTrip] = useState(null);
  const [search, setSearch] = useState("");
  const [operatorFilter, setOperatorFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const selectedTripIdRef = useRef(null);
  const requestInFlightRef = useRef(false);
  const markerRefs = useRef(new Map());

  useEffect(() => {
    selectedTripIdRef.current = selectedTripId;
  }, [selectedTripId]);

  const loadFleet = useCallback(async (signal) => {
    if (requestInFlightRef.current) return;
    requestInFlightRef.current = true;
    try {
      const data = await getAdminLiveFleet(signal);
      if (signal.aborted) return;
      setFleet(data);
      setError("");

      const selected = selectedTripIdRef.current;
      if (selected && !data.some((trip) => String(trip.tripId) === String(selected))) {
        try {
          const previous = await getAdminLiveFleetTrip(selected, signal);
          if (!signal.aborted && previous.tripStatus === "COMPLETED") setCompletedTrip(previous);
        } catch (lookupError) {
          if (lookupError.name !== "AbortError") setCompletedTrip(null);
        }
        setSelectedTripId(data[0] ? String(data[0].tripId) : null);
      } else if (!selected && data[0]) {
        setSelectedTripId(String(data[0].tripId));
      } else if (selected) {
        setCompletedTrip(null);
      }
    } catch (loadError) {
      if (loadError.name !== "AbortError") {
        setError(loadError.message || "The monitoring service is temporarily unavailable.");
      }
    } finally {
      requestInFlightRef.current = false;
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
      requestInFlightRef.current = false;
    };
  }, [loadFleet]);

  const operators = useMemo(() => {
    const unique = new Map();
    fleet.forEach((trip) => unique.set(String(trip.operatorId), trip.operatorName));
    return [...unique.entries()].sort((a, b) => a[1].localeCompare(b[1]));
  }, [fleet]);

  const filteredFleet = useMemo(() => {
    const query = search.trim().toLowerCase();
    return fleet.filter((trip) => {
      if (operatorFilter && String(trip.operatorId) !== operatorFilter) return false;
      if (!query) return true;
      return [trip.operatorName, trip.busNumber, trip.busName, trip.driverName,
        trip.routeName, trip.origin, trip.destination]
        .some((value) => String(value || "").toLowerCase().includes(query));
    });
  }, [fleet, operatorFilter, search]);

  const selectedTrip = fleet.find((trip) => String(trip.tripId) === String(selectedTripId)) || null;
  const activeOperators = new Set(fleet.map((trip) => trip.operatorId)).size;
  const busesWithGps = fleet.filter(hasCoordinates).length;

  const selectTrip = (tripId) => {
    setSelectedTripId(String(tripId));
    setFocusVersion((value) => value + 1);
  };

  return (
    <AdminLayout title="Live Monitoring" subtitle="Monitor active buses across all approved transport operators.">
      <div className="space-y-5">
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <SummaryCard icon={<Navigation />} label="Active trips" value={fleet.length} />
          <SummaryCard icon={<Building2 />} label="Operators currently active" value={activeOperators} />
          <SummaryCard icon={<Radio />} label="Buses with GPS" value={busesWithGps} tone="emerald" />
          <SummaryCard icon={<MapPinOff />} label="Buses without GPS" value={fleet.length - busesWithGps} tone="amber" />
        </section>

        {error && <Notice tone="red" title="Network Error">{error} Automatic retry remains active.</Notice>}
        {completedTrip && <Notice tone="blue" title="Trip completed">{completedTrip.busNumber} operated by {completedTrip.operatorName} has left the active list.</Notice>}
        {!loading && !error && fleet.length === 0 && <Notice title="No active buses">No approved operator currently has a trip marked IN_PROGRESS.</Notice>}

        <section className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm lg:grid-cols-[1fr_280px]">
          <label className="flex items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3">
            <Search size={18} className="text-slate-400" />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search operator, bus, driver, route, origin or destination" className="w-full bg-transparent text-sm outline-none" />
          </label>
          <select value={operatorFilter} onChange={(event) => setOperatorFilter(event.target.value)} className="rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-bold outline-none">
            <option value="">All operators</option>
            {operators.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
          </select>
        </section>

        <section className="grid min-h-[670px] overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm xl:grid-cols-[390px_1fr]">
          <aside className="max-h-[670px] overflow-y-auto border-b border-slate-200 p-4 xl:border-b-0 xl:border-r">
            <div className="flex items-center justify-between px-1">
              <h2 className="text-lg font-black">Active trips</h2>
              <span className="text-xs font-black text-slate-500">{filteredFleet.length} shown</span>
            </div>
            <div className="mt-4 space-y-3">
              {filteredFleet.map((trip) => (
                <button
                  key={trip.tripId}
                  type="button"
                  onClick={() => selectTrip(trip.tripId)}
                  className={`w-full rounded-2xl border p-4 text-left transition ${String(selectedTripId) === String(trip.tripId) ? "border-[#08264a] bg-blue-50 shadow-sm" : "border-slate-200 hover:bg-slate-50"}`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div><p className="text-xs font-black uppercase text-blue-700">{trip.operatorName}</p><p className="mt-1 font-black text-slate-900">{trip.busNumber} · {trip.busName}</p></div>
                    <span className="rounded-full bg-emerald-100 px-2.5 py-1 text-[10px] font-black text-emerald-700">{statusLabel(trip.tripStatus)}</span>
                  </div>
                  <div className="mt-3 space-y-2 text-sm text-slate-600">
                    <Line icon={<UserRound size={15} />} value={trip.driverName} />
                    <Line icon={<Route size={15} />} value={routeLabel(trip)} />
                    <Line icon={hasCoordinates(trip) ? <Clock3 size={15} /> : <MapPinOff size={15} />} value={hasCoordinates(trip) ? formatUpdatedAt(trip.updatedAt) : "GPS unavailable"} />
                    {trip.speed != null && <Line icon={<Gauge size={15} />} value={`${formatSpeed(trip.speed)} km/h`} />}
                  </div>
                </button>
              ))}
              {!loading && fleet.length > 0 && filteredFleet.length === 0 && <p className="rounded-xl bg-slate-50 p-4 text-sm font-bold text-slate-500">No active trips match these filters.</p>}
            </div>
          </aside>

          <div className="relative min-h-[520px]">
            <MapContainer center={DEFAULT_CENTER} zoom={13} scrollWheelZoom className="h-full min-h-[520px] w-full">
              <TileLayer attribution="© OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              {filteredFleet.map((trip) => {
                if (!hasCoordinates(trip)) return null;
                return (
                  <Marker
                    key={trip.tripId}
                    ref={(marker) => {
                      if (marker) markerRefs.current.set(String(trip.tripId), marker);
                      else markerRefs.current.delete(String(trip.tripId));
                    }}
                    position={[trip.latitude, trip.longitude]}
                    icon={busIcon}
                    eventHandlers={{ click: () => selectTrip(trip.tripId) }}
                  >
                    <Popup><strong>{trip.busNumber}</strong><br />{trip.operatorName}<br />{trip.driverName}<br />{routeLabel(trip)}</Popup>
                  </Marker>
                );
              })}
              {selectedTrip && hasCoordinates(selectedTrip) && (
                <FocusSelectedBus trip={selectedTrip} focusVersion={focusVersion} markerRefs={markerRefs} />
              )}
            </MapContainer>
            {fleet.length > 0 && fleet.every((trip) => !hasCoordinates(trip)) && (
              <div className="absolute left-1/2 top-5 z-[500] -translate-x-1/2 rounded-2xl bg-white px-5 py-4 text-center shadow-xl"><MapPinOff className="mx-auto text-amber-600" /><p className="mt-2 font-black">GPS unavailable</p><p className="text-xs text-slate-500">Active trips are waiting for driver coordinates.</p></div>
            )}
          </div>
        </section>
      </div>
    </AdminLayout>
  );
}

function FocusSelectedBus({ trip, focusVersion, markerRefs }) {
  const map = useMap();
  useEffect(() => {
    map.flyTo([trip.latitude, trip.longitude], Math.max(map.getZoom(), 15), { duration: 0.8 });
    const timer = window.setTimeout(() => markerRefs.current.get(String(trip.tripId))?.openPopup(), 850);
    return () => window.clearTimeout(timer);
  }, [focusVersion, map, markerRefs, trip]);
  return null;
}

function SummaryCard({ icon, label, value, tone = "blue" }) {
  const style = tone === "emerald" ? "bg-emerald-100 text-emerald-700" : tone === "amber" ? "bg-amber-100 text-amber-700" : "bg-blue-100 text-blue-700";
  return <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><div className={`flex h-11 w-11 items-center justify-center rounded-xl ${style}`}>{icon}</div><p className="mt-4 text-xs font-black uppercase text-slate-500">{label}</p><p className="mt-2 text-3xl font-black text-slate-900">{value}</p></div>;
}

function Notice({ title, children, tone = "amber" }) {
  const style = tone === "red" ? "border-red-200 bg-red-50 text-red-800" : tone === "blue" ? "border-blue-200 bg-blue-50 text-blue-800" : "border-amber-200 bg-amber-50 text-amber-800";
  return <div className={`rounded-2xl border p-4 text-sm ${style}`}><p className="font-black">{title}</p><p className="mt-1 font-semibold">{children}</p></div>;
}

function Line({ icon, value }) {
  return <p className="flex items-center gap-2"><span className="text-slate-400">{icon}</span><span className="truncate">{value || "Unavailable"}</span></p>;
}

function hasCoordinates(trip) {
  return Number.isFinite(trip?.latitude) && Number.isFinite(trip?.longitude);
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
