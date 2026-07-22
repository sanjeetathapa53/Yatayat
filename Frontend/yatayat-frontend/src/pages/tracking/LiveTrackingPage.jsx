import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Clock3, Gauge, MapPin, Navigation, RefreshCw, Route } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import L from "leaflet";
import { getActiveTripLocations, getTripLocation } from "../../utils/passengerLiveTracking";

const POLL_INTERVAL_MS = 7000;
const DEFAULT_CENTER = [27.7105, 85.318];

const busIcon = new L.DivIcon({
  html: '<div style="width:44px;height:44px;display:flex;align-items:center;justify-content:center;border:4px solid white;border-radius:50%;background:#047857;color:white;font-size:22px;box-shadow:0 8px 24px rgba(0,0,0,.3)">🚌</div>',
  className: "",
  iconSize: [44, 44],
  iconAnchor: [22, 22],
});

export default function LiveTrackingPage() {
  const { id: tripId } = useParams();
  const [trips, setTrips] = useState([]);
  const [selectedTripId, setSelectedTripId] = useState(tripId || null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [finishedTrip, setFinishedTrip] = useState(null);
  const selectedTripIdRef = useRef(selectedTripId);
  const intervalRef = useRef(null);

  useEffect(() => {
    selectedTripIdRef.current = selectedTripId;
  }, [selectedTripId]);

  const loadLocations = useCallback(async (signal) => {
    try {
      const data = tripId
        ? [await getTripLocation(tripId, signal)]
        : await getActiveTripLocations(signal);
      if (signal.aborted) return;

      setError("");
      setTrips(data);

      const currentSelection = selectedTripIdRef.current;
      if (tripId) {
        setSelectedTripId(tripId);
        setFinishedTrip(data[0]?.tripStatus === "COMPLETED" ? data[0] : null);
        if (data[0]?.tripStatus === "COMPLETED" && intervalRef.current !== null) {
          window.clearInterval(intervalRef.current);
          intervalRef.current = null;
        }
      } else if (data.length === 0) {
        if (currentSelection) {
          try {
            const previous = await getTripLocation(currentSelection, signal);
            if (!signal.aborted && previous.tripStatus === "COMPLETED") setFinishedTrip(previous);
          } catch (lookupError) {
            if (lookupError.name !== "AbortError") setFinishedTrip(null);
          }
        }
        setSelectedTripId(null);
      } else {
        setFinishedTrip(null);
        if (!data.some((trip) => String(trip.tripId) === String(currentSelection))) {
          setSelectedTripId(String(data[0].tripId));
        }
      }
    } catch (loadError) {
      if (loadError.name !== "AbortError") {
        setError(loadError.message || "The live tracking service is temporarily unavailable.");
      }
    } finally {
      if (!signal.aborted) setLoading(false);
    }
  }, [tripId]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.resolve().then(() => loadLocations(controller.signal));
    intervalRef.current = window.setInterval(() => loadLocations(controller.signal), POLL_INTERVAL_MS);
    return () => {
      controller.abort();
      if (intervalRef.current !== null) window.clearInterval(intervalRef.current);
      intervalRef.current = null;
    };
  }, [loadLocations]);

  const selectedTrip = useMemo(
    () => trips.find((trip) => String(trip.tripId) === String(selectedTripId)) || finishedTrip,
    [finishedTrip, selectedTripId, trips],
  );
  const hasGps = Number.isFinite(selectedTrip?.latitude) && Number.isFinite(selectedTrip?.longitude);
  const markerPosition = hasGps ? [selectedTrip.latitude, selectedTrip.longitude] : null;

  return (
    <div className="min-h-screen bg-slate-100 text-[#08264a]">
      <header className="flex min-h-16 items-center justify-between border-b border-slate-200 bg-white px-4 py-3 sm:px-7">
        <div>
          <Link to="/passenger/dashboard" className="text-xl font-black">Yatayat</Link>
          <p className="text-xs font-semibold text-slate-500">Passenger live bus tracking</p>
        </div>
        <Link to="/passenger/dashboard" className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-black hover:bg-slate-50">
          Dashboard
        </Link>
      </header>

      <main className="grid min-h-[calc(100vh-65px)] lg:grid-cols-[360px_1fr]">
        <aside className="z-10 border-r border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h1 className="text-2xl font-black">Live buses</h1>
              <p className="mt-1 text-sm text-slate-500">Updates automatically every seven seconds.</p>
            </div>
            {loading && <RefreshCw className="animate-spin text-emerald-700" size={20} />}
          </div>

          {error && (
            <Notice tone="red" title="Network Error">{error} Automatic retry is enabled.</Notice>
          )}
          {!loading && !error && trips.length === 0 && !finishedTrip && (
            <Notice title="No active trip">There are no buses currently marked IN_PROGRESS.</Notice>
          )}
          {finishedTrip && (
            <Notice tone="blue" title="Trip finished">This bus has completed its trip. No further GPS coordinates are expected.</Notice>
          )}

          {!tripId && trips.length > 0 && (
            <div className="mt-5 space-y-3">
              {trips.map((trip) => (
                <button
                  type="button"
                  key={trip.tripId}
                  onClick={() => setSelectedTripId(String(trip.tripId))}
                  className={`w-full rounded-2xl border p-4 text-left transition ${String(selectedTripId) === String(trip.tripId) ? "border-emerald-600 bg-emerald-50" : "border-slate-200 hover:bg-slate-50"}`}
                >
                  <p className="font-black">{trip.bus?.number || "Bus details unavailable"}</p>
                  <p className="mt-1 text-sm text-slate-600">{routeLabel(trip)}</p>
                  <p className="mt-2 text-xs font-bold text-emerald-700">{statusLabel(trip.tripStatus)}</p>
                </button>
              ))}
            </div>
          )}

          {selectedTrip && (
            <section className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <h2 className="text-xl font-black">{selectedTrip.bus?.number || "Bus unavailable"}</h2>
              {selectedTrip.bus?.name && <p className="text-sm text-slate-500">{selectedTrip.bus.name}</p>}
              <div className="mt-4 space-y-3">
                <Detail icon={<Route size={17} />} label="Route" value={routeLabel(selectedTrip)} />
                <Detail icon={<Navigation size={17} />} label="Trip status" value={statusLabel(selectedTrip.tripStatus)} />
                <Detail icon={<Clock3 size={17} />} label="Last updated" value={formatUpdatedAt(selectedTrip.updatedAt)} />
                {selectedTrip.speed != null && <Detail icon={<Gauge size={17} />} label="Speed" value={`${formatSpeed(selectedTrip.speed)} km/h`} />}
              </div>
              {!hasGps && (
                <Notice title="GPS unavailable">The driver has not provided a GPS position for this trip yet.</Notice>
              )}
            </section>
          )}
        </aside>

        <section className="relative min-h-[60vh]">
          <MapContainer center={markerPosition || DEFAULT_CENTER} zoom={14} scrollWheelZoom className="h-full min-h-[60vh] w-full lg:min-h-[calc(100vh-65px)]">
            <TileLayer attribution="© OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            {markerPosition && (
              <>
                <SmoothBusMarker position={markerPosition} trip={selectedTrip} />
                <FollowBus position={markerPosition} />
              </>
            )}
          </MapContainer>
          {!markerPosition && (
            <div className="pointer-events-none absolute left-1/2 top-6 z-[500] -translate-x-1/2 rounded-2xl bg-white px-5 py-4 text-center shadow-xl">
              <MapPin className="mx-auto text-slate-400" />
              <p className="mt-2 font-black">Waiting for live GPS</p>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

function SmoothBusMarker({ position, trip }) {
  const markerRef = useRef(null);
  const previousRef = useRef(position);

  useEffect(() => {
    const marker = markerRef.current;
    if (!marker) return undefined;
    const from = previousRef.current;
    const startedAt = performance.now();
    let frame;
    const animate = (now) => {
      const progress = Math.min((now - startedAt) / 1000, 1);
      marker.setLatLng([
        from[0] + (position[0] - from[0]) * progress,
        from[1] + (position[1] - from[1]) * progress,
      ]);
      if (progress < 1) frame = requestAnimationFrame(animate);
      else previousRef.current = position;
    };
    frame = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(frame);
  }, [position]);

  return (
    <Marker ref={markerRef} position={position} icon={busIcon}>
      <Popup><strong>{trip.bus?.number || "Bus"}</strong><br />{routeLabel(trip)}</Popup>
    </Marker>
  );
}

function FollowBus({ position }) {
  const map = useMap();
  const firstPositionRef = useRef(true);
  useEffect(() => {
    if (firstPositionRef.current) {
      map.flyTo(position, Math.max(map.getZoom(), 14), { duration: 1 });
      firstPositionRef.current = false;
    }
  }, [map, position]);
  return null;
}

function Notice({ title, children, tone = "amber" }) {
  const styles = tone === "red" ? "border-red-200 bg-red-50 text-red-800" : tone === "blue" ? "border-blue-200 bg-blue-50 text-blue-800" : "border-amber-200 bg-amber-50 text-amber-800";
  return <div className={`mt-5 rounded-2xl border p-4 text-sm ${styles}`}><p className="font-black">{title}</p><p className="mt-1 font-semibold">{children}</p></div>;
}

function Detail({ icon, label, value }) {
  return <div className="flex items-start gap-3"><span className="mt-0.5 text-emerald-700">{icon}</span><div><p className="text-xs font-bold uppercase text-slate-400">{label}</p><p className="font-black">{value}</p></div></div>;
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
