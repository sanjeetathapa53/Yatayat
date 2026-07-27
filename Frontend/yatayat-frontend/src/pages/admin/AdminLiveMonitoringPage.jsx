import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Bus, Clock3, LocateFixed, MapPinOff, Navigation, RefreshCw, Route, Search, UserRound } from "lucide-react";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import { useNavigate } from "react-router-dom";
import L from "leaflet";
import AdminLayout from "../../components/layout/AdminLayout";
import { getAdminLiveMonitoring } from "../../utils/adminFleet";

const POLL_MS = 10000;
const CENTER = [27.7105, 85.318];
const FILTERS = ["ALL", "LIVE", "STALE", "OFFLINE", "LOCAL", "OUT_OF_VALLEY"];

export default function AdminLiveMonitoringPage() {
  const navigate = useNavigate();
  const [vehicles, setVehicles] = useState([]);
  const [selectedKey, setSelectedKey] = useState(null);
  const [filter, setFilter] = useState("ALL");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [lastRefresh, setLastRefresh] = useState(null);
  const [focusVersion, setFocusVersion] = useState(0);
  const inFlight = useRef(false);
  const mounted = useRef(true);
  const controllerRef = useRef(null);
  const markerRefs = useRef(new Map());

  const load = useCallback(async (manual = false) => {
    if (inFlight.current) return;
    inFlight.current = true;
    if (manual) setRefreshing(true);
    const controller = new AbortController();
    controllerRef.current = controller;
    try {
      const response = await getAdminLiveMonitoring(controller.signal);
      if (!mounted.current) return;
      const next = Array.isArray(response?.vehicles) ? response.vehicles : [];
      setVehicles(next);
      setLastRefresh(response?.generatedAt ? new Date(response.generatedAt) : new Date());
      setError("");
      setSelectedKey((current) => current && next.some((item) => keyOf(item) === current)
        ? current
        : next[0] ? keyOf(next[0]) : null);
    } catch (requestError) {
      if (requestError.name !== "AbortError" && mounted.current) {
        setError(requestError.message || "Live monitoring is temporarily unavailable.");
      }
    } finally {
      inFlight.current = false;
      if (mounted.current) {
        setLoading(false);
        setRefreshing(false);
      }
    }
  }, []);

  useEffect(() => {
    mounted.current = true;
    Promise.resolve().then(() => load());
    const interval = window.setInterval(load, POLL_MS);
    return () => {
      mounted.current = false;
      controllerRef.current?.abort();
      window.clearInterval(interval);
      inFlight.current = false;
    };
  }, [load]);

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return vehicles.filter((vehicle) => {
      if (filter !== "ALL" && vehicle.locationFreshness !== filter && vehicle.tripType !== filter) return false;
      return !query || [vehicle.busNumber, vehicle.busName, vehicle.driverName, vehicle.operatorName, vehicle.routeName]
        .some((value) => String(value || "").toLowerCase().includes(query));
    });
  }, [filter, search, vehicles]);

  const selected = vehicles.find((item) => keyOf(item) === selectedKey) || null;
  const counts = useMemo(() => ({
    monitored: vehicles.length,
    live: count(vehicles, "locationFreshness", "LIVE"),
    stale: count(vehicles, "locationFreshness", "STALE"),
    offline: count(vehicles, "locationFreshness", "OFFLINE"),
    local: count(vehicles, "tripType", "LOCAL"),
    outOfValley: count(vehicles, "tripType", "OUT_OF_VALLEY"),
  }), [vehicles]);

  const select = (vehicle) => {
    setSelectedKey(keyOf(vehicle));
    setFocusVersion((value) => value + 1);
  };

  return (
    <AdminLayout title="Live Monitoring" subtitle="Monitor operating buses, drivers, routes, and GPS freshness across all operators.">
      <div className="space-y-5">
        <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
          <Summary label="Monitored vehicles" value={counts.monitored} icon={<Bus />} />
          <Summary label="Live" value={counts.live} icon={<LocateFixed />} tone="emerald" />
          <Summary label="Stale" value={counts.stale} icon={<Clock3 />} tone="amber" />
          <Summary label="Offline" value={counts.offline} icon={<MapPinOff />} tone="slate" />
          <Summary label="Local services" value={counts.local} icon={<Navigation />} />
          <Summary label="Out-of-valley" value={counts.outOfValley} icon={<Route />} />
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex flex-col gap-3 lg:flex-row">
            <label className="flex flex-1 items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3">
              <Search size={18} className="text-slate-400" />
              <input value={search} onChange={(event) => setSearch(event.target.value)}
                placeholder="Search bus, driver, operator, or route" className="w-full bg-transparent text-sm outline-none" />
            </label>
            <button type="button" onClick={() => load(true)} disabled={refreshing}
              className="flex items-center justify-center gap-2 rounded-xl border border-[#08264a] px-4 py-3 text-sm font-black hover:bg-blue-50 disabled:opacity-60">
              <RefreshCw size={17} className={refreshing ? "animate-spin" : ""} />Refresh
            </button>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {FILTERS.map((item) => (
              <button key={item} type="button" onClick={() => setFilter(item)} aria-pressed={filter === item}
                className={`rounded-full border px-3 py-1.5 text-xs font-black ${filter === item ? "border-[#08264a] bg-[#08264a] text-white" : "border-slate-200 text-slate-600 hover:border-slate-400"}`}>
                {labelOf(item)}
              </button>
            ))}
            {lastRefresh && <span className="ml-auto text-xs font-semibold text-slate-500">Last refreshed {lastRefresh.toLocaleTimeString()}</span>}
          </div>
        </section>

        {error && <Notice title="Monitoring unavailable" tone="red">{error} Automatic retry remains active.</Notice>}
        {loading && <div className="h-[640px] animate-pulse rounded-3xl bg-slate-100" aria-label="Loading live monitoring" />}
        {!loading && vehicles.length === 0 && <Notice title="No monitored vehicles">No relevant service or trip is currently operating or approaching departure.</Notice>}

        {!loading && vehicles.length > 0 && (
          <section className="grid min-h-[640px] overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm xl:grid-cols-[370px_1fr]">
            <aside className="max-h-[640px] overflow-y-auto border-b border-slate-200 p-4 xl:border-b-0 xl:border-r">
              <div className="flex justify-between"><h2 className="font-black">Vehicles</h2><span className="text-xs font-bold text-slate-500">{filtered.length} shown</span></div>
              <div className="mt-4 space-y-3">
                {filtered.map((vehicle) => (
                  <button key={keyOf(vehicle)} type="button" onClick={() => select(vehicle)}
                    className={`w-full rounded-2xl border p-4 text-left focus-visible:outline-2 focus-visible:outline-[#08264a] ${keyOf(vehicle) === selectedKey ? "border-[#08264a] bg-blue-50" : "border-slate-200 hover:bg-slate-50"}`}>
                    <div className="flex items-start justify-between gap-2">
                      <div><p className="font-black">{vehicle.busNumber}</p><p className="text-xs font-bold text-slate-500">{vehicle.routeName}</p></div>
                      <Badge value={vehicle.locationFreshness} />
                    </div>
                    <p className="mt-3 flex items-center gap-2 text-xs font-semibold text-slate-600"><UserRound size={14} />{vehicle.driverName}</p>
                    <p className="mt-1 flex items-center gap-2 text-xs font-semibold text-slate-600"><Navigation size={14} />{typeOf(vehicle.tripType)} · {statusOf(vehicle.operationStatus)}</p>
                    <p className="mt-1 flex items-center gap-2 text-xs font-semibold text-slate-600"><Clock3 size={14} />{relativeGps(vehicle)}</p>
                  </button>
                ))}
                {filtered.length === 0 && <p className="rounded-xl bg-slate-50 p-4 text-sm font-bold text-slate-500">No vehicles match the current filters.</p>}
              </div>
            </aside>

            <div className="grid min-h-[640px] lg:grid-rows-[1fr_auto]">
              <div className="relative min-h-[450px]">
                <MapContainer center={CENTER} zoom={13} scrollWheelZoom className="h-full min-h-[450px] w-full">
                  <TileLayer attribution="© OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                  {filtered.filter(hasCoordinates).map((vehicle) => (
                    <Marker key={keyOf(vehicle)} position={[vehicle.latitude, vehicle.longitude]}
                      icon={markerIcon(vehicle.locationFreshness, keyOf(vehicle) === selectedKey)}
                      ref={(marker) => setMarker(markerRefs, keyOf(vehicle), marker)}
                      eventHandlers={{ click: () => select(vehicle) }}>
                      <Popup><strong>{vehicle.busNumber}</strong><br />{vehicle.routeName}<br />{labelOf(vehicle.locationFreshness)}</Popup>
                    </Marker>
                  ))}
                  {selected && hasCoordinates(selected) && <Focus vehicle={selected} version={focusVersion} refs={markerRefs} />}
                </MapContainer>
                {filtered.length > 0 && filtered.every((item) => !hasCoordinates(item)) && (
                  <div className="absolute left-1/2 top-5 z-[500] -translate-x-1/2 rounded-xl bg-white p-4 text-center shadow-xl"><MapPinOff className="mx-auto" /><p className="mt-2 font-black">No usable GPS coordinates</p></div>
                )}
              </div>
              {selected && <Details vehicle={selected} navigate={navigate} />}
            </div>
          </section>
        )}
      </div>
    </AdminLayout>
  );
}

function Details({ vehicle, navigate }) {
  return (
    <div className="border-t border-slate-200 p-5">
      <div className="flex flex-wrap justify-between gap-3">
        <div><h2 className="text-xl font-black">{vehicle.busNumber} · {vehicle.busName}</h2><p className="text-sm font-semibold text-slate-500">{vehicle.operatorName}</p></div>
        <div className="flex gap-2"><Link label="Buses" onClick={() => navigate("/admin/buses")} /><Link label="Drivers" onClick={() => navigate("/admin/driver-applications")} /><Link label="Routes" onClick={() => navigate("/admin/routes")} /></div>
      </div>
      <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2 xl:grid-cols-4">
        <Detail label="Driver" value={`${vehicle.driverName} · ${statusOf(vehicle.driverOperationalStatus)}`} />
        <Detail label="Route" value={`${vehicle.routeName} (${vehicle.origin} → ${vehicle.destination})`} />
        <Detail label="Transport type" value={typeOf(vehicle.tripType)} />
        <Detail label="Trip/service status" value={statusOf(vehicle.operationStatus)} />
        <Detail label="Freshness" value={labelOf(vehicle.locationFreshness)} />
        <Detail label="Last GPS update" value={relativeGps(vehicle)} />
      </dl>
    </div>
  );
}

function Focus({ vehicle, version, refs }) {
  const map = useMap();
  useEffect(() => {
    map.flyTo([vehicle.latitude, vehicle.longitude], Math.max(map.getZoom(), 15), { duration: 0.7 });
    const timer = window.setTimeout(() => refs.current.get(keyOf(vehicle))?.openPopup(), 750);
    return () => window.clearTimeout(timer);
  }, [map, refs, vehicle, version]);
  return null;
}

function Summary({ label, value, icon, tone = "blue" }) {
  const colors = { blue: "bg-blue-100 text-blue-700", emerald: "bg-emerald-100 text-emerald-700", amber: "bg-amber-100 text-amber-700", slate: "bg-slate-200 text-slate-700" };
  return <div className="rounded-2xl border bg-white p-4 shadow-sm"><div className={`flex h-9 w-9 items-center justify-center rounded-xl ${colors[tone]}`}>{icon}</div><p className="mt-3 text-[11px] font-black uppercase text-slate-500">{label}</p><p className="text-2xl font-black">{value}</p></div>;
}
function Badge({ value }) {
  const styles = { LIVE: "bg-emerald-100 text-emerald-700", STALE: "bg-amber-100 text-amber-800", OFFLINE: "bg-slate-100 text-slate-700" };
  const symbol = value === "LIVE" ? "●" : value === "STALE" ? "◐" : "○";
  return <span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${styles[value] || styles.OFFLINE}`}>{symbol} {labelOf(value)}</span>;
}
function Detail({ label, value }) { return <div><dt className="text-xs font-black uppercase text-slate-400">{label}</dt><dd className="mt-1 font-bold text-slate-700">{value || "Unavailable"}</dd></div>; }
function Link({ label, onClick }) { return <button type="button" onClick={onClick} className="rounded-lg border px-3 py-2 text-xs font-black hover:bg-blue-50">{label}</button>; }
function Notice({ title, children, tone = "amber" }) { return <div className={`rounded-2xl border p-4 text-sm ${tone === "red" ? "border-red-200 bg-red-50 text-red-800" : "border-amber-200 bg-amber-50 text-amber-800"}`}><p className="font-black">{title}</p><p className="mt-1 font-semibold">{children}</p></div>; }
function count(items, field, value) { return items.filter((item) => item[field] === value).length; }
function keyOf(vehicle) { return `${vehicle.operationType}-${vehicle.operationId}`; }
function hasCoordinates(vehicle) { return Number.isFinite(vehicle?.latitude) && Number.isFinite(vehicle?.longitude); }
function typeOf(value) { return value === "LOCAL" ? "Local" : "Out-of-Valley"; }
function statusOf(value) { return String(value || "Unknown").replaceAll("_", " "); }
function labelOf(value) { return value === "OUT_OF_VALLEY" ? "Out-of-Valley" : String(value || "Offline").charAt(0) + String(value || "Offline").slice(1).toLowerCase(); }
function relativeGps(vehicle) {
  const seconds = vehicle.lastGpsUpdateAgeSeconds;
  if (seconds == null) return "No GPS received";
  if (seconds < 60) return `${seconds}s ago`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  return `${Math.floor(seconds / 3600)}h ago`;
}
function markerIcon(freshness, selected) {
  const color = freshness === "LIVE" ? "#059669" : freshness === "STALE" ? "#d97706" : "#64748b";
  const size = selected ? 48 : 42;
  return new L.DivIcon({ html: `<div style="width:${size}px;height:${size}px;display:flex;align-items:center;justify-content:center;border:${selected ? 5 : 3}px solid ${selected ? "#08264a" : "white"};border-radius:50%;background:${color};color:white;font-size:11px;font-weight:900;box-shadow:0 8px 22px rgba(0,0,0,.3)">BUS</div>`, className: "", iconSize: [size, size], iconAnchor: [size / 2, size / 2] });
}
function setMarker(refs, key, marker) { if (marker) refs.current.set(key, marker); else refs.current.delete(key); }
