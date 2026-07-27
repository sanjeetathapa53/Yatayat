import { useCallback, useEffect, useState } from "react";
import {
  AlertTriangle,
  Bus,
  CalendarDays,
  CheckCircle2,
  Loader2,
  MapPin,
  Play,
  QrCode,
  RefreshCw,
  Users,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import ConfirmationModal from "../../components/common/ConfirmationModal";
import DriverLayout from "../../components/layout/DriverLayout";
import { GPS_STATUS, useDriverLocationTracking } from "../../hooks/useDriverLocationTracking";
import {
  finishDriverLocalService,
  getCurrentDriverLocalService,
  serviceStatusLabel,
  serviceStatusTone,
  startDriverLocalService,
} from "../../utils/localServices";
import {
  beginDriverTripBoarding,
  finishDriverTrip,
  getCurrentDriverTrip,
  startDriverTrip,
  tripStatusLabel,
  tripStatusTone,
} from "../../utils/driverTrips";

export default function TripManagementPage() {
  const navigate = useNavigate();
  const [trip, setTrip] = useState(null);
  const [loading, setLoading] = useState(true);
  const [operating, setOperating] = useState(false);
  const [error, setError] = useState("");
  const [confirmFinish, setConfirmFinish] = useState(false);
  const gps = useDriverLocationTracking(
    trip?.workType === "SCHEDULED_TRIP" ? trip.scheduledTripId : null,
    trip?.workType === "SCHEDULED_TRIP" && trip.status === "IN_PROGRESS",
  );

  const loadTrip = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [scheduledTrip, localService] = await Promise.all([
        getCurrentDriverTrip(),
        getCurrentDriverLocalService(),
      ]);
      setTrip(selectCurrentWork(scheduledTrip, localService));
    } catch (loadError) {
      setError(loadError.message || "Unable to load your assigned trip.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.resolve().then(loadTrip);
  }, [loadTrip]);

  const operate = async (action) => {
    if (!trip || operating) return;
    setOperating(true);
    setError("");
    try {
      const localService = trip.workType === "LOCAL_SERVICE";
      const updated = localService
        ? action === "start"
          ? await startDriverLocalService(trip.id)
          : await finishDriverLocalService(trip.id)
        : action === "boarding"
          ? await beginDriverTripBoarding(trip.scheduledTripId)
          : action === "start"
            ? await startDriverTrip(trip.scheduledTripId)
            : await finishDriverTrip(trip.scheduledTripId);
      setTrip({ ...updated, workType: trip.workType });
      setConfirmFinish(false);
      toast.success(localService
        ? action === "start" ? "Local service started." : "Local service completed."
        : {
            boarding: "Boarding started.",
            start: "Trip started. GPS tracking is now active.",
            finish: "Trip completed.",
          }[action]);
    } catch (operationError) {
      const message = operationError.message || "Trip operation could not be completed.";
      setError(message);
      toast.error(message);
    } finally {
      setOperating(false);
    }
  };

  return (
    <DriverLayout activePage="Trip Management">
      <div className="space-y-6">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-3xl font-black text-slate-900">Trip Management</h1>
            <p className="mt-1 text-slate-500">Manage boarding, live operation, and completion for your assigned trip.</p>
          </div>
          <button
            type="button"
            onClick={loadTrip}
            disabled={loading || operating}
            className="flex items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-black text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
          >
            <RefreshCw size={17} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
        </header>

        {error && (
          <div className="flex gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-800">
            <AlertTriangle size={19} className="mt-0.5 shrink-0" />
            <p>{error}</p>
          </div>
        )}

        {loading ? (
          <div className="flex min-h-72 items-center justify-center rounded-3xl border border-slate-200 bg-white">
            <Loader2 size={30} className="animate-spin text-[#08264a]" />
          </div>
        ) : !trip ? (
          <div className="rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
            <Bus size={42} className="mx-auto text-slate-300" />
            <h2 className="mt-4 text-2xl font-black text-slate-900">No active assignment</h2>
            <p className="mt-2 text-sm font-semibold text-slate-500">
              Your current scheduled trip or local service will appear here.
            </p>
          </div>
        ) : (
          <>
            <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                <div>
                  <p className="text-xs font-black uppercase tracking-[0.2em] text-slate-500">
                    {trip.workType === "LOCAL_SERVICE" ? "Local service" : trip.routeCode || "Assigned trip"}
                  </p>
                  <h2 className="mt-2 text-2xl font-black text-slate-900">
                    {trip.origin} → {trip.destination}
                  </h2>
                  <p className="mt-1 font-semibold text-slate-500">
                    {trip.routeName}{trip.operatorName ? ` · ${trip.operatorName}` : ""}
                  </p>
                </div>
                <span className={`self-start rounded-full px-4 py-2 text-xs font-black uppercase tracking-wide ${
                  trip.workType === "LOCAL_SERVICE" ? serviceStatusTone(trip.status) : tripStatusTone(trip.status)
                }`}>
                  {trip.workType === "LOCAL_SERVICE" ? serviceStatusLabel(trip.status) : tripStatusLabel(trip.status)}
                </span>
              </div>

              {trip.workType === "SCHEDULED_TRIP" && trip.status === "IN_PROGRESS" && (
                <GpsStatus status={gps.status} message={gps.message} />
              )}

              <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <Detail icon={<Bus size={19} />} label="Assigned bus" value={`${trip.busNumber}${trip.busName ? ` · ${trip.busName}` : ""}`} />
                {trip.workType === "LOCAL_SERVICE" ? (
                  <>
                    <Detail icon={<CalendarDays size={19} />} label="Service date" value={formatServiceDate(trip.serviceDate)} />
                    <Detail icon={<MapPin size={19} />} label="Planned time" value={`${trip.plannedStartTime} - ${trip.plannedEndTime}`} />
                    <Detail icon={<Users size={19} />} label="Ordered stops" value={`${trip.orderedStops?.length || 0} stops`} />
                  </>
                ) : (
                  <>
                    <Detail icon={<CalendarDays size={19} />} label="Departure" value={formatDateTime(trip.departureAt)} />
                    <Detail icon={<MapPin size={19} />} label="Estimated arrival" value={formatDateTime(trip.estimatedArrivalAt)} />
                    <Detail icon={<Users size={19} />} label="Boarded passengers" value={`${trip.boardedPassengers || 0}/${trip.confirmedPassengers || 0}`} />
                  </>
                )}
              </div>

              {trip.workType === "LOCAL_SERVICE" && trip.orderedStops?.length > 0 && (
                <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <h3 className="font-black text-slate-900">Ordered stops</h3>
                  <div className="mt-3 grid gap-2 sm:grid-cols-2">
                    {trip.orderedStops.map((stop) => (
                      <div key={stop.id} className="flex items-center gap-3 rounded-xl bg-white p-3">
                        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#08264a] text-xs font-black text-white">
                          {stop.stopOrder}
                        </span>
                        <div className="min-w-0">
                          <p className="truncate font-black text-slate-900">{stop.stopName}</p>
                          <p className="truncate text-xs font-semibold text-slate-500">{stop.landmark || "Local route stop"}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
                {trip.workType === "LOCAL_SERVICE" && trip.status === "PLANNED" && (
                  <ActionButton busy={operating} onClick={() => operate("start")}>
                    <Play size={18} /> Start Local Service
                  </ActionButton>
                )}
                {trip.workType === "LOCAL_SERVICE" && trip.status === "IN_SERVICE" && (
                  <button
                    type="button"
                    disabled={operating}
                    onClick={() => setConfirmFinish(true)}
                    className="rounded-2xl bg-red-600 px-5 py-3 font-black text-white shadow-lg shadow-red-600/20 transition hover:bg-red-700 disabled:opacity-60"
                  >
                    End Local Service
                  </button>
                )}
                {trip.workType === "SCHEDULED_TRIP" && trip.status === "SCHEDULED" && (
                  <ActionButton busy={operating} onClick={() => operate("boarding")}>
                    <Users size={18} /> Begin Boarding
                  </ActionButton>
                )}
                {trip.workType === "SCHEDULED_TRIP" && trip.status === "BOARDING" && (
                  <>
                    <ActionButton busy={operating} onClick={() => operate("start")}>
                      <Play size={18} /> Start Trip
                    </ActionButton>
                    <SecondaryButton onClick={() => navigate("/driver/scanner")}>
                      <QrCode size={18} /> Scan Tickets
                    </SecondaryButton>
                    <SecondaryButton onClick={() => navigate(`/driver/trips/${trip.scheduledTripId}/manifest`)}>
                      <CheckCircle2 size={18} /> View Manifest
                    </SecondaryButton>
                  </>
                )}
                {trip.workType === "SCHEDULED_TRIP" && trip.status === "IN_PROGRESS" && (
                  <>
                    <SecondaryButton onClick={() => navigate("/driver/scanner")}>
                      <QrCode size={18} /> Scan Tickets
                    </SecondaryButton>
                    <SecondaryButton onClick={() => navigate(`/driver/trips/${trip.scheduledTripId}/manifest`)}>
                      <CheckCircle2 size={18} /> View Manifest
                    </SecondaryButton>
                    <button
                      type="button"
                      disabled={operating}
                      onClick={() => setConfirmFinish(true)}
                      className="rounded-2xl bg-red-600 px-5 py-3 font-black text-white shadow-lg shadow-red-600/20 transition hover:bg-red-700 disabled:opacity-60"
                    >
                      Finish Trip
                    </button>
                  </>
                )}
              </div>
            </section>
          </>
        )}
      </div>

      <ConfirmationModal
        open={confirmFinish}
        title={trip?.workType === "LOCAL_SERVICE" ? "End Local Service?" : "Finish Trip?"}
        message={trip?.workType === "LOCAL_SERVICE"
          ? "Confirm that this local service has ended. It will be marked as completed."
          : "Confirm that this trip has reached its destination. GPS tracking will stop and the trip will be marked as completed."}
        confirmLabel={trip?.workType === "LOCAL_SERVICE" ? "End Service" : "Finish Trip"}
        busyLabel="Finishing..."
        destructive
        busy={operating}
        onClose={() => setConfirmFinish(false)}
        onConfirm={() => operate("finish")}
      />
    </DriverLayout>
  );
}

function ActionButton({ busy, onClick, children }) {
  return (
    <button
      type="button"
      disabled={busy}
      onClick={onClick}
      className="flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-5 py-3 font-black text-white shadow-lg shadow-blue-950/20 transition hover:bg-[#0d3566] disabled:opacity-60"
    >
      {children}
    </button>
  );
}

function SecondaryButton({ onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center justify-center gap-2 rounded-2xl border border-slate-300 bg-white px-5 py-3 font-black text-slate-700 transition hover:bg-slate-50"
    >
      {children}
    </button>
  );
}

function Detail({ icon, label, value }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex items-center gap-2 text-slate-500">{icon}<span className="text-xs font-black uppercase">{label}</span></div>
      <p className="mt-2 font-black text-slate-900">{value || "Not available"}</p>
    </div>
  );
}

function GpsStatus({ status, message }) {
  const tone = {
    [GPS_STATUS.ACTIVE]: "border-emerald-200 bg-emerald-50 text-emerald-800",
    [GPS_STATUS.NETWORK_ERROR]: "border-red-200 bg-red-50 text-red-800",
    [GPS_STATUS.PERMISSION_DENIED]: "border-red-200 bg-red-50 text-red-800",
    [GPS_STATUS.UNAVAILABLE]: "border-amber-200 bg-amber-50 text-amber-800",
    [GPS_STATUS.WAITING]: "border-blue-200 bg-blue-50 text-blue-800",
  }[status] || "border-slate-200 bg-slate-50 text-slate-700";
  return (
    <div className={`mt-5 rounded-2xl border p-4 ${tone}`}>
      <p className="font-black">{status}</p>
      {message && <p className="mt-1 text-sm font-semibold">{message}</p>}
    </div>
  );
}

function formatDateTime(value) {
  if (!value) return "Not available";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-NP", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function formatServiceDate(value) {
  if (!value) return "Not available";
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-NP", { dateStyle: "medium" }).format(date);
}

function selectCurrentWork(scheduledTrip, localService) {
  if (scheduledTrip?.status === "IN_PROGRESS") {
    return { ...scheduledTrip, workType: "SCHEDULED_TRIP" };
  }
  if (localService?.status === "IN_SERVICE") {
    return { ...localService, workType: "LOCAL_SERVICE" };
  }
  if (scheduledTrip?.status === "BOARDING") {
    return { ...scheduledTrip, workType: "SCHEDULED_TRIP" };
  }
  if (!scheduledTrip) return localService ? { ...localService, workType: "LOCAL_SERVICE" } : null;
  if (!localService) return { ...scheduledTrip, workType: "SCHEDULED_TRIP" };

  const scheduledTime = new Date(scheduledTrip.departureAt).getTime();
  const localTime = new Date(`${localService.serviceDate}T${localService.plannedStartTime}`).getTime();
  return localTime < scheduledTime
    ? { ...localService, workType: "LOCAL_SERVICE" }
    : { ...scheduledTrip, workType: "SCHEDULED_TRIP" };
}
