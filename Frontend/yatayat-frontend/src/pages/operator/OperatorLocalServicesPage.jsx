import { useCallback, useEffect, useMemo, useState } from "react";
import { Bus, CalendarDays, Edit3, Loader2, Plus, RefreshCw, Route, UserRound, X } from "lucide-react";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import OperatorLayout from "../../components/layout/OperatorLayout";
import ConfirmationModal from "../../components/common/ConfirmationModal";
import {
  formatServiceDate,
  handleOperatorLocalAccess,
  localServicePayload,
  localServiceRequest,
  LOCAL_SERVICE_STATUSES,
  serviceStatusLabel,
  serviceStatusTone,
} from "../../utils/localServices";

const emptyForm = {
  routeId: "",
  busId: "",
  driverId: "",
  serviceDate: "",
  plannedStartTime: "",
  plannedEndTime: "",
  notes: "",
};

export default function OperatorLocalServicesPage() {
  const navigate = useNavigate();
  const [runs, setRuns] = useState([]);
  const [options, setOptions] = useState({ routes: [], buses: [], drivers: [] });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterDate, setFilterDate] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [runData, optionData] = await Promise.all([
        localServiceRequest("/api/operator/local-services"),
        localServiceRequest("/api/operator/local-services/options"),
      ]);
      setRuns(runData);
      setOptions(optionData);
    } catch (loadError) {
      if (!handleOperatorLocalAccess(loadError, navigate)) setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    Promise.resolve().then(load);
  }, [load]);

  const visibleRuns = useMemo(
    () => runs
      .filter((run) => filterStatus === "ALL" || run.status === filterStatus)
      .filter((run) => !filterDate || run.serviceDate === filterDate),
    [filterDate, filterStatus, runs]
  );

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setFormOpen(true);
    setError("");
  };

  const openEdit = (run) => {
    setEditing(run);
    setForm({
      routeId: String(run.routeId),
      busId: String(run.busId),
      driverId: String(run.driverId),
      serviceDate: run.serviceDate || "",
      plannedStartTime: run.plannedStartTime || "",
      plannedEndTime: run.plannedEndTime || "",
      notes: run.notes || "",
    });
    setFormOpen(true);
    setError("");
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const path = editing ? `/api/operator/local-services/${editing.id}` : "/api/operator/local-services";
      const method = editing ? "PUT" : "POST";
      await localServiceRequest(path, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(localServicePayload(form)),
      });
      toast.success(editing ? "Local service updated." : "Local service assigned.");
      setFormOpen(false);
      await load();
    } catch (saveError) {
      if (!handleOperatorLocalAccess(saveError, navigate)) setError(saveError.message);
    } finally {
      setSaving(false);
    }
  };

  const cancelRun = async () => {
    if (!cancelTarget) return;
    setError("");
    setCancelling(true);
    try {
      await localServiceRequest(`/api/operator/local-services/${cancelTarget.id}/cancel`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ reason: "Cancelled from operator planning page" }),
      });
      toast.success("Local service cancelled.");
      setCancelTarget(null);
      await load();
    } catch (cancelError) {
      if (!handleOperatorLocalAccess(cancelError, navigate)) setError(cancelError.message);
    } finally {
      setCancelling(false);
    }
  };

  return (
    <OperatorLayout>
      <div className="space-y-5">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-2xl font-semibold">Local Services</h1>
            <p className="mt-1 text-sm text-slate-500">Assign approved buses and active associated drivers to local routes.</p>
          </div>
          <button type="button" onClick={openCreate} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-2.5 font-semibold text-white">
            <Plus size={18} /> Create Assignment
          </button>
        </div>

        <div className="rounded-2xl border border-blue-100 bg-blue-50 p-4 text-sm font-bold text-blue-900">
          Create dated local service runs, assign an approved driver and bus, and monitor active services from Live Fleet.
        </div>

        <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_auto] md:items-end">
          <label className="text-xs font-semibold uppercase text-slate-500">
            Service date
            <input type="date" value={filterDate} onChange={(event) => setFilterDate(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 font-bold normal-case outline-none focus:border-[#08264a]" />
          </label>
          <button type="button" onClick={() => { setFilterDate(""); setFilterStatus("ALL"); }} className="rounded-xl border border-slate-300 px-5 py-2.5 text-sm font-semibold text-slate-700">
            Clear Filters
          </button>
        </div>

        <div className="flex flex-wrap gap-2">
          {["ALL", ...LOCAL_SERVICE_STATUSES].map((status) => (
            <button key={status} type="button" onClick={() => setFilterStatus(status)} className={`rounded-full px-4 py-2 text-xs font-semibold ${filterStatus === status ? "bg-[#08264a] text-white" : "border border-slate-200 bg-white text-slate-600"}`}>
              {status === "ALL" ? "All" : serviceStatusLabel(status)}
            </button>
          ))}
        </div>

        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}

        {loading ? (
          <div className="flex min-h-48 items-center justify-center"><Loader2 className="animate-spin" size={32} /></div>
        ) : visibleRuns.length === 0 ? (
          <EmptyLocalServices onCreate={openCreate} />
        ) : (
          <div className="grid items-start gap-3 xl:grid-cols-2">
            {visibleRuns.map((run) => <LocalServiceCard key={run.id} run={run} onEdit={openEdit} onCancel={setCancelTarget} />)}
          </div>
        )}

        <button type="button" onClick={load} disabled={loading} className="flex items-center gap-2 text-sm font-semibold disabled:opacity-50">
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {formOpen && (
        <LocalServiceForm
          form={form}
          options={options}
          editing={editing}
          saving={saving}
          onChange={(key, value) => setForm((current) => ({ ...current, [key]: value }))}
          onClose={() => setFormOpen(false)}
          onSubmit={submit}
        />
      )}
      <ConfirmationModal
        open={Boolean(cancelTarget)}
        title="Cancel local service?"
        message="This will cancel the planned local service and notify the assigned driver."
        confirmLabel="Cancel Service"
        destructive
        busy={cancelling}
        busyLabel="Cancelling..."
        onConfirm={cancelRun}
        onClose={() => setCancelTarget(null)}
      />
    </OperatorLayout>
  );
}

function EmptyLocalServices({ onCreate }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white px-5 py-10 text-center shadow-sm">
      <CalendarDays className="mx-auto text-slate-300" size={36} />
      <h2 className="mt-3 text-lg font-semibold">No local service assignments found</h2>
      <p className="mt-2 text-sm text-slate-500">Create an assignment when an approved bus and active associated driver are available.</p>
      <button type="button" onClick={onCreate} className="mt-4 rounded-xl bg-[#08264a] px-4 py-2.5 text-sm font-semibold text-white">Create Assignment</button>
    </div>
  );
}

function LocalServiceCard({ run, onEdit, onCancel }) {
  const editable = run.status === "PLANNED" || run.status === "READY";
  return (
    <article className="min-w-0 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`inline-flex h-6 items-center rounded-full px-2.5 text-[11px] font-semibold leading-none ${serviceStatusTone(run.status)}`}>{serviceStatusLabel(run.status)}</span>
            <span className="inline-flex h-6 items-center rounded-full bg-emerald-100 px-2.5 text-[11px] font-semibold leading-none text-emerald-700">LOCAL</span>
          </div>
          <h2 className="safe-wrap mt-2 text-base font-semibold leading-6 text-slate-900">{run.routeCode} · {run.routeName}</h2>
          <p className="safe-wrap mt-0.5 text-sm font-medium text-slate-500">{run.origin} → {run.destination}</p>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2">
          {editable && <button type="button" onClick={() => onEdit(run)} className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-300 px-3 text-xs font-semibold text-slate-700"><Edit3 size={14} /> Edit</button>}
          {editable && <button type="button" onClick={() => onCancel(run)} className="inline-flex h-9 items-center rounded-lg border border-red-200 bg-red-50 px-3 text-xs font-semibold text-red-700">Cancel</button>}
        </div>
      </div>
      <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
        <Meta icon={<CalendarDays size={17} />} label="Schedule" value={formatServiceDate(run.serviceDate, run.plannedStartTime, run.plannedEndTime)} />
        <Meta icon={<Bus size={17} />} label="Bus" value={`${run.busNumber} · ${run.busName}`} />
        <Meta icon={<UserRound size={17} />} label="Driver" value={run.driverName} />
        <Meta icon={<Route size={17} />} label="Stops" value={`${run.orderedStops?.length || 0} ordered stops`} />
      </div>
      {run.notes && <p className="safe-wrap mt-3 rounded-xl bg-slate-50 px-3 py-2.5 text-sm text-slate-600">{run.notes}</p>}
    </article>
  );
}

function Meta({ icon, label, value }) {
  return <div className="rounded-xl border border-slate-100 bg-slate-50 px-3 py-2.5"><div className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wide text-slate-500">{icon}{label}</div><p className="safe-wrap mt-1 text-sm font-semibold leading-5 text-slate-800">{value}</p></div>;
}

function LocalServiceForm({ form, options, editing, saving, onChange, onClose, onSubmit }) {
  return (
    <div className="responsive-modal-backdrop fixed inset-0 z-50 flex justify-center bg-black/50 sm:items-center">
      <form onSubmit={onSubmit} className="responsive-modal-panel w-full max-w-4xl rounded-2xl bg-white p-5 shadow-2xl sm:p-6">
        <div className="mb-6 flex items-start justify-between gap-4">
          <div>
            <h2 className="text-2xl font-semibold">{editing ? "Edit Local Service" : "Create Local Service"}</h2>
            <p className="mt-1 text-sm text-slate-500">Choose one active local route, one owned approved bus, and one active associated driver.</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-xl p-2 hover:bg-slate-100"><X /></button>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <Select label="Local route" value={form.routeId} onChange={(value) => onChange("routeId", value)} required>
            <option value="">Select route</option>
            {options.routes.map((route) => <option key={route.id} value={route.id}>{route.code} · {route.name} ({route.origin} → {route.destination})</option>)}
          </Select>
          <Select label="Approved bus" value={form.busId} onChange={(value) => onChange("busId", value)} required>
            <option value="">Select bus</option>
            {options.buses.map((bus) => <option key={bus.id} value={bus.id}>{bus.busNumber} · {bus.busName} · {bus.seatCapacity} seats</option>)}
          </Select>
          <Select label="Associated driver" value={form.driverId} onChange={(value) => onChange("driverId", value)} required>
            <option value="">Select driver</option>
            {options.drivers.map((driver) => <option key={driver.id} value={driver.id}>{driver.fullName} · {driver.licenseCategory}</option>)}
          </Select>
          <Field label="Service date" type="date" value={form.serviceDate} onChange={(value) => onChange("serviceDate", value)} required />
          <Field label="Planned start" type="time" value={form.plannedStartTime} onChange={(value) => onChange("plannedStartTime", value)} required />
          <Field label="Planned end" type="time" value={form.plannedEndTime} onChange={(value) => onChange("plannedEndTime", value)} required />
          <label className="md:col-span-2 text-xs font-semibold uppercase text-slate-500">
            Notes
            <textarea value={form.notes} onChange={(event) => onChange("notes", event.target.value)} rows={3} className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 font-semibold normal-case outline-none focus:border-[#08264a]" placeholder="Optional dispatch notes" />
          </label>
        </div>
        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button type="button" onClick={onClose} className="rounded-xl border border-slate-300 px-5 py-2.5 font-semibold text-slate-700">Close</button>
          <button type="submit" disabled={saving} className="rounded-xl bg-[#08264a] px-5 py-2.5 font-semibold text-white disabled:opacity-60">{saving ? "Saving..." : editing ? "Save Changes" : "Create Assignment"}</button>
        </div>
      </form>
    </div>
  );
}

function Field({ label, type, value, onChange, required }) {
  return <label className="text-xs font-semibold uppercase text-slate-500">{label}<input type={type} value={value} onChange={(event) => onChange(event.target.value)} required={required} className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 font-bold normal-case outline-none focus:border-[#08264a]" /></label>;
}

function Select({ label, value, onChange, required, children }) {
  return <label className="text-xs font-semibold uppercase text-slate-500">{label}<select value={value} onChange={(event) => onChange(event.target.value)} required={required} className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 font-bold normal-case outline-none focus:border-[#08264a]">{children}</select></label>;
}
