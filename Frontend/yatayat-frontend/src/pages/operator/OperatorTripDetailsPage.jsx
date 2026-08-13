import { useCallback, useEffect, useState } from "react";
import { ArrowLeft, Loader2, Pencil, XCircle } from "lucide-react";
import { toast } from "react-toastify";
import { useNavigate, useParams } from "react-router-dom";
import OperatorLayout from "../../components/layout/OperatorLayout";
import OperatorTripForm from "../../components/operator/OperatorTripForm";
import {
  assignOperatorTrip,
  formatTripDate,
  handleOperatorAccess,
  statusLabel,
  statusTone,
  tripFormPayload,
  tripRequest,
} from "../../utils/operatorTrips";

export default function OperatorTripDetailsPage() {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const [trip, setTrip] = useState(null);
  const [eligibility, setEligibility] = useState(null);
  const [form, setForm] = useState(null);
  const [editing, setEditing] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [showCancel, setShowCancel] = useState(false);
  const [reason, setReason] = useState("");

  const loadTrip = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setTrip(await tripRequest(`/api/operator/trips/${tripId}`));
    } catch (loadError) {
      if (!handleOperatorAccess(loadError, navigate)) {
        setError(loadError.status === 404 ? "Trip not found or unavailable." : loadError.message);
      }
    } finally {
      setLoading(false);
    }
  }, [navigate, tripId]);

  useEffect(() => {
    Promise.resolve().then(loadTrip);
  }, [loadTrip]);

  const beginEdit = async () => {
    setError("");
    try {
      const data = await tripRequest("/api/operator/trips/eligibility");
      setEligibility(withCurrentOptions(data, trip));
      setForm({
        routeId: String(trip.routeId),
        busId: String(trip.busId),
        driverId: String(trip.driverId),
        departureAt: toInputDate(trip.departureAt),
        estimatedArrivalAt: toInputDate(trip.estimatedArrivalAt),
        fare: String(trip.fare),
        boardingNotes: trip.boardingNotes || "",
      });
      setEditing(true);
    } catch (loadError) {
      if (!handleOperatorAccess(loadError, navigate)) setError(loadError.message);
    }
  };

  const save = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      setTrip(await tripRequest(`/api/operator/trips/${tripId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(tripFormPayload(form)),
      }));
      setEditing(false);
      toast.success("Trip updated successfully.");
    } catch (saveError) {
      if (!handleOperatorAccess(saveError, navigate)) setError(saveError.message);
    } finally {
      setSubmitting(false);
    }
  };

  const beginAssignment = async () => {
    setError("");
    try {
      const data = await tripRequest("/api/operator/trips/eligibility");
      setEligibility(withCurrentOptions(data, trip));
      setForm({ busId: String(trip.busId), driverId: String(trip.driverId) });
      setAssigning(true);
    } catch (loadError) {
      if (!handleOperatorAccess(loadError, navigate)) setError(loadError.message);
    }
  };

  const saveAssignment = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      setTrip(await assignOperatorTrip(tripId, form));
      setAssigning(false);
      toast.success("Trip assignment updated.");
    } catch (assignError) {
      if (!handleOperatorAccess(assignError, navigate)) setError(assignError.message);
    } finally {
      setSubmitting(false);
    }
  };

  const cancelTrip = async () => {
    setSubmitting(true);
    setError("");
    try {
      setTrip(await tripRequest(`/api/operator/trips/${tripId}/cancel`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ reason: reason.trim() || null }),
      }));
      setShowCancel(false);
      toast.success("Trip cancelled.");
    } catch (cancelError) {
      if (!handleOperatorAccess(cancelError, navigate)) {
        setError(cancelError.message);
        setShowCancel(false);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <OperatorLayout>
      <div className="mx-auto max-w-5xl space-y-5">
        <button onClick={() => navigate("/operator/trips")} className="flex items-center gap-2 text-sm font-semibold">
          <ArrowLeft size={17} /> Back to trips
        </button>
        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
        {loading ? (
          <div className="flex min-h-48 items-center justify-center"><Loader2 className="animate-spin" size={32} /></div>
        ) : trip && editing ? (
          <>
            <div>
              <h1 className="text-2xl font-semibold">Edit Scheduled Trip</h1>
              <p className="mt-1 text-sm text-slate-500">
                Changes are allowed only before confirmed passenger bookings exist.
              </p>
            </div>
            <OperatorTripForm
              form={form}
              eligibility={eligibility}
              submitting={submitting}
              submitLabel="Save Changes"
              onChange={(key, value) => setForm((current) => ({ ...current, [key]: value }))}
              onSubmit={save}
              onCancel={() => setEditing(false)}
            />
          </>
        ) : trip && assigning ? (
          <AssignmentForm
            trip={trip}
            eligibility={eligibility}
            form={form}
            submitting={submitting}
            onChange={(key, value) => setForm((current) => ({ ...current, [key]: value }))}
            onSubmit={saveAssignment}
            onCancel={() => setAssigning(false)}
          />
        ) : trip ? (
          <TripDetails trip={trip} onEdit={beginEdit} onAssign={beginAssignment} onCancel={() => setShowCancel(true)} />
        ) : null}
        {showCancel && (
          <CancelModal
            reason={reason}
            setReason={setReason}
            submitting={submitting}
            onClose={() => setShowCancel(false)}
            onConfirm={cancelTrip}
          />
        )}
      </div>
    </OperatorLayout>
  );
}

function TripDetails({ trip, onEdit, onAssign, onCancel }) {
  const cancellable = ["SCHEDULED", "BOARDING"].includes(trip.status);
  const rows = [
    ["Route", `${trip.routeCode} · ${trip.routeName}`],
    ["Journey", `${trip.origin} → ${trip.destination}`],
    ["Bus", `${trip.busNumber} · ${trip.busName || "Unnamed bus"}`],
    ["Driver", trip.driverName],
    ["Departure", formatTripDate(trip.departureAt)],
    ["Estimated arrival", formatTripDate(trip.estimatedArrivalAt)],
    ["Fare", `NPR ${Number(trip.fare).toLocaleString()}`],
    ["Seat capacity", trip.seatCapacity],
    ["Confirmed passengers", trip.confirmedPassengers || 0],
    ["Boarded passengers", trip.boardedPassengers || 0],
    ["Assignment", trip.assignmentComplete ? "Complete" : "Needs bus and driver"],
    ["Boarding notes", trip.boardingNotes || "None"],
    ["Cancellation reason", trip.cancellationReason || "—"],
  ];

  return (
    <>
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold">Trip #{trip.id}</h1>
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusTone(trip.status)}`}>
              {statusLabel(trip.status)}
            </span>
          </div>
          <p className="mt-2 text-slate-500">{trip.routeName}</p>
        </div>
        <div className="flex flex-wrap gap-3">
          {trip.status === "SCHEDULED" && (
            <button onClick={onEdit} className="flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-2.5 font-semibold">
              <Pencil size={17} /> Edit Plan
            </button>
          )}
          {trip.status === "SCHEDULED" && (
            <button onClick={onAssign} className="rounded-xl bg-[#08264a] px-4 py-2.5 font-semibold text-white">
              Assign Bus / Driver
            </button>
          )}
          {cancellable && (
            <button onClick={onCancel} className="flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2.5 font-semibold text-white">
              <XCircle size={17} /> Cancel Trip
            </button>
          )}
        </div>
      </div>
      <div className="grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-2">
        {rows.map(([label, value]) => (
          <div key={label} className="rounded-2xl bg-slate-50 p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-2 font-bold text-slate-800">{value}</p>
          </div>
        ))}
      </div>
    </>
  );
}

function AssignmentForm({ trip, eligibility, form, submitting, onChange, onSubmit, onCancel }) {
  return (
    <form onSubmit={onSubmit} className="space-y-5 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div>
        <h1 className="text-2xl font-semibold">Assign Bus and Driver</h1>
        <p className="mt-1 text-sm text-slate-500">
          {trip.routeName} · {formatTripDate(trip.departureAt)} to {formatTripDate(trip.estimatedArrivalAt)}
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <label className="block text-sm font-semibold text-slate-700">
          Bus
          <select
            required
            value={form.busId}
            onChange={(event) => onChange("busId", event.target.value)}
            className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 font-medium outline-none focus:border-[#08264a]"
          >
            <option value="">Select an eligible bus</option>
            {eligibility.buses.map((bus) => (
              <option key={bus.id} value={bus.id}>
                {bus.busNumber} - {bus.busName || bus.busType} - {bus.seatCapacity} seats
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm font-semibold text-slate-700">
          Driver
          <select
            required
            value={form.driverId}
            onChange={(event) => onChange("driverId", event.target.value)}
            className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 font-medium outline-none focus:border-[#08264a]"
          >
            <option value="">Select an eligible driver</option>
            {eligibility.drivers.map((driver) => (
              <option key={driver.id} value={driver.id}>
                {driver.fullName} - {driver.licenseNumber} ({driver.licenseCategory})
              </option>
            ))}
          </select>
        </label>
      </div>
      <p className="rounded-2xl bg-blue-50 p-4 text-sm font-bold text-blue-800">
        The server will re-check ownership, active association, licence validity, vehicle documents and overlapping schedules before saving.
      </p>
      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button type="button" onClick={onCancel} disabled={submitting} className="rounded-xl border border-slate-300 px-5 py-2.5 font-semibold text-slate-600">
          Cancel
        </button>
        <button disabled={submitting} className="rounded-xl bg-[#08264a] px-6 py-2.5 font-semibold text-white disabled:opacity-60">
          {submitting ? "Saving..." : "Save Assignment"}
        </button>
      </div>
    </form>
  );
}

function CancelModal({ reason, setReason, submitting, onClose, onConfirm }) {
  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white p-5 shadow-xl">
        <h2 className="text-2xl font-semibold">Cancel this trip?</h2>
        <p className="mt-2 text-sm text-slate-500">The trip will remain visible with a cancelled status.</p>
        <label className="mt-5 block text-sm font-semibold">
          Reason <span className="font-medium text-slate-400">(optional)</span>
          <textarea
            rows={4}
            maxLength={1000}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            className="mt-2 w-full rounded-xl border border-slate-300 p-3 outline-none focus:border-red-500"
          />
        </label>
        <div className="mt-6 flex justify-end gap-3">
          <button onClick={onClose} disabled={submitting} className="rounded-xl border border-slate-300 px-4 py-2.5 font-semibold">
            Keep Trip
          </button>
          <button disabled={submitting} onClick={onConfirm} className="rounded-xl bg-red-600 px-4 py-2.5 font-semibold text-white disabled:opacity-60">
            {submitting ? "Cancelling..." : "Confirm Cancellation"}
          </button>
        </div>
      </div>
    </div>
  );
}

function toInputDate(value) {
  return value ? value.slice(0, 16) : "";
}

function withCurrentOptions(data, trip) {
  return {
    routes: data.routes.some((item) => item.id === trip.routeId)
      ? data.routes
      : [{ id: trip.routeId, code: trip.routeCode, name: trip.routeName, origin: trip.origin, destination: trip.destination }, ...data.routes],
    buses: data.buses.some((item) => item.id === trip.busId)
      ? data.buses
      : [{ id: trip.busId, busNumber: trip.busNumber, busName: trip.busName, seatCapacity: trip.seatCapacity }, ...data.buses],
    drivers: data.drivers.some((item) => item.id === trip.driverId)
      ? data.drivers
      : [{ id: trip.driverId, fullName: trip.driverName, licenseNumber: "Current assignment", licenseCategory: "" }, ...data.drivers],
  };
}
