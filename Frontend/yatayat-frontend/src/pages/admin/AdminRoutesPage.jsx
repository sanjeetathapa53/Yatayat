import { useCallback, useEffect, useState } from "react";
import { Edit3, Loader2, MapPin, Plus, Route as RouteIcon, X } from "lucide-react";
import AdminLayout from "../../components/layout/AdminLayout";
import { apiFetch } from "../../utils/api";
import { expireAdminSession } from "../../utils/adminSession";
import { useNavigate } from "react-router-dom";

const emptyForm = {
  code: "",
  name: "",
  origin: "",
  destination: "",
  distanceKm: "",
  estimatedDurationMinutes: "",
  tripType: "OUT_OF_VALLEY",
  status: "ACTIVE",
};

export default function AdminRoutesPage() {
  const navigate = useNavigate();
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const loadRoutes = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const response = await apiFetch("/api/admin/routes");
      if (response.status === 401) {
        expireAdminSession(navigate);
        return;
      }
      if (!response.ok) throw new Error("Unable to load routes.");
      const data = await response.json();
      setRoutes(Array.isArray(data) ? data : []);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    loadRoutes();
  }, [loadRoutes]);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setError("");
    setShowForm(true);
  };

  const openEdit = (route) => {
    setEditingId(route.id);
    setForm({
      code: route.code,
      name: route.name,
      origin: route.origin,
      destination: route.destination,
      distanceKm: route.distanceKm,
      estimatedDurationMinutes: route.estimatedDurationMinutes,
      tripType: route.tripType,
      status: route.status,
    });
    setError("");
    setShowForm(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    try {
      setSaving(true);
      setError("");
      const response = await apiFetch(
        editingId ? `/api/admin/routes/${editingId}` : "/api/admin/routes",
        {
          method: editingId ? "PUT" : "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            ...form,
            distanceKm: Number(form.distanceKm),
            estimatedDurationMinutes: Number(form.estimatedDurationMinutes),
          }),
        }
      );
      if (response.status === 401) {
        expireAdminSession(navigate);
        return;
      }
      const data = await response.json().catch(() => null);
      if (!response.ok) {
        throw new Error(data?.message || "Unable to save route.");
      }
      setShowForm(false);
      await loadRoutes();
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdminLayout title="Route Management" subtitle="Create and maintain platform routes available to approved operators.">
      <div className="space-y-6">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div className="min-w-0">
            <h1 className="text-2xl font-black text-slate-900">Platform Routes</h1>
            <p className="mt-1 text-sm text-slate-500">Inactive routes remain preserved but are hidden from operators.</p>
          </div>
          <button type="button" onClick={openCreate} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white hover:bg-[#0d3566]">
            <Plus size={18} /> Add Route
          </button>
        </div>

        {error && <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">{error}</div>}

        {loading ? (
          <div className="flex min-h-72 items-center justify-center rounded-3xl border border-slate-200 bg-white"><Loader2 className="animate-spin" size={40} /></div>
        ) : routes.length === 0 ? (
          <div className="rounded-3xl border border-slate-200 bg-white p-12 text-center">
            <RouteIcon className="mx-auto text-slate-400" size={44} />
            <h2 className="mt-4 text-xl font-black">No routes created</h2>
            <p className="mt-2 text-sm text-slate-500">Add the first platform route to begin building the network.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            {routes.map((route) => (
              <article key={route.id} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-lg bg-blue-50 px-3 py-1 text-xs font-black text-[#08264a]">{route.code}</span>
                      <span className={`rounded-full px-3 py-1 text-xs font-black ${route.status === "ACTIVE" ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-600"}`}>{route.status}</span>
                      <TripTypeBadge tripType={route.tripType} />
                    </div>
                    <h2 className="safe-wrap mt-3 text-xl font-black text-slate-900">{route.name}</h2>
                  </div>
                  <button type="button" onClick={() => openEdit(route)} className="rounded-xl border border-slate-200 p-3 text-slate-600 hover:bg-slate-50" title="Edit route"><Edit3 size={18} /></button>
                </div>
                <div className="safe-wrap mt-5 flex items-start gap-3 rounded-2xl bg-slate-50 p-4 text-sm font-bold text-slate-700">
                  <MapPin size={18} /> {route.origin} to {route.destination}
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <Info label="Distance" value={`${route.distanceKm} km`} />
                  <Info label="Estimated duration" value={`${route.estimatedDurationMinutes} min`} />
                </div>
              </article>
            ))}
          </div>
        )}
      </div>

      {showForm && (
        <div className="responsive-modal-backdrop fixed inset-0 z-50 flex justify-center bg-black/50 sm:items-center">
          <form onSubmit={submit} className="responsive-modal-panel w-full max-w-2xl rounded-3xl bg-white p-5 shadow-2xl sm:p-6">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0"><h2 className="text-xl font-black sm:text-2xl">{editingId ? "Edit Route" : "Create Route"}</h2><p className="mt-1 text-sm text-slate-500">Routes are managed by administrators.</p></div>
              <button type="button" aria-label="Close route form" onClick={() => setShowForm(false)} className="rounded-xl p-2 hover:bg-slate-100"><X /></button>
            </div>
            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Route Code" value={form.code} onChange={(value) => setForm({ ...form, code: value })} />
              <Field label="Route Name" value={form.name} onChange={(value) => setForm({ ...form, name: value })} />
              <Field label="Origin" value={form.origin} onChange={(value) => setForm({ ...form, origin: value })} />
              <Field label="Destination" value={form.destination} onChange={(value) => setForm({ ...form, destination: value })} />
              <Field label="Distance (km)" type="number" step="0.01" value={form.distanceKm} onChange={(value) => setForm({ ...form, distanceKm: value })} />
              <Field label="Estimated Duration (minutes)" type="number" value={form.estimatedDurationMinutes} onChange={(value) => setForm({ ...form, estimatedDurationMinutes: value })} />
              <label><span className="text-xs font-black uppercase tracking-wider text-slate-500">Trip Type</span><select required value={form.tripType} onChange={(event) => setForm({ ...form, tripType: event.target.value })} className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 font-bold outline-none"><option value="OUT_OF_VALLEY">Outside Valley</option><option value="LOCAL">Local</option></select></label>
              <label className="sm:col-span-2"><span className="text-xs font-black uppercase tracking-wider text-slate-500">Status</span><select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })} className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 font-bold outline-none"><option value="ACTIVE">ACTIVE</option><option value="INACTIVE">INACTIVE</option></select></label>
            </div>
            <button type="submit" disabled={saving} className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 font-black text-white disabled:opacity-60">{saving && <Loader2 size={18} className="animate-spin" />}{saving ? "Saving..." : "Save Route"}</button>
          </form>
        </div>
      )}
    </AdminLayout>
  );
}

function Field({ label, value, onChange, type = "text", step }) {
  return <label><span className="text-xs font-black uppercase tracking-wider text-slate-500">{label}</span><input required type={type} min={type === "number" ? "0.01" : undefined} step={step} value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 font-bold outline-none focus:border-[#08264a]" /></label>;
}

function Info({ label, value }) {
  return <div className="rounded-xl border border-slate-100 p-3"><p className="text-xs font-black uppercase text-slate-400">{label}</p><p className="mt-1 font-black text-slate-800">{value}</p></div>;
}

function TripTypeBadge({ tripType }) {
  const local = tripType === "LOCAL";
  return <span className={`rounded-full px-3 py-1 text-xs font-black ${local ? "bg-amber-100 text-amber-700" : "bg-violet-100 text-violet-700"}`}>{local ? "Local" : "Outside Valley"}</span>;
}
