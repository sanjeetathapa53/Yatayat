import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowDown, ArrowUp, Edit3, Loader2, MapPin, Plus, Route as RouteIcon, Search, Trash2, X } from "lucide-react";
import AdminLayout from "../../components/layout/AdminLayout";
import { apiFetch } from "../../utils/api";
import { expireAdminSession } from "../../utils/adminSession";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import ConfirmationModal from "../../components/common/ConfirmationModal";

const emptyStopForm = { name: "", landmark: "", latitude: "", longitude: "", active: true };
const emptyRouteForm = {
  code: "", name: "", origin: "", destination: "", distanceKm: "",
  estimatedDurationMinutes: "", operatingStartTime: "", operatingEndTime: "",
  tripType: "OUT_OF_VALLEY", status: "ACTIVE", stops: [],
};

export default function AdminRoutesPage() {
  const navigate = useNavigate();
  const [routes, setRoutes] = useState([]);
  const [stops, setStops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [routeForm, setRouteForm] = useState(emptyRouteForm);
  const [stopForm, setStopForm] = useState(emptyStopForm);
  const [editingRouteId, setEditingRouteId] = useState(null);
  const [editingStopId, setEditingStopId] = useState(null);
  const [showRouteForm, setShowRouteForm] = useState(false);
  const [showStopForm, setShowStopForm] = useState(false);
  const [stopToDeactivate, setStopToDeactivate] = useState(null);
  const [routeSearch, setRouteSearch] = useState("");
  const [routeTypeFilter, setRouteTypeFilter] = useState("ALL");
  const [routeStatusFilter, setRouteStatusFilter] = useState("ALL");
  const [routeToDelete, setRouteToDelete] = useState(null);
  const [deletingRouteId, setDeletingRouteId] = useState(null);
  const [routeStatusBusyId, setRouteStatusBusyId] = useState(null);

  const filteredRoutes = useMemo(() => {
    const search = routeSearch.trim().toLowerCase();
    return routes.filter((route) => {
      const matchesType = routeTypeFilter === "ALL" || route.tripType === routeTypeFilter;
      const matchesStatus = routeStatusFilter === "ALL" || route.status === routeStatusFilter;
      const matchesSearch = !search || [route.code, route.name, route.origin, route.destination]
        .some((value) => String(value || "").toLowerCase().includes(search));
      return matchesType && matchesStatus && matchesSearch;
    });
  }, [routeSearch, routeStatusFilter, routeTypeFilter, routes]);
  const localRoutes = useMemo(
    () => filteredRoutes.filter((route) => route.tripType === "LOCAL"),
    [filteredRoutes]
  );
  const outsideRoutes = useMemo(
    () => filteredRoutes.filter((route) => route.tripType === "OUT_OF_VALLEY"),
    [filteredRoutes]
  );

  const request = useCallback(async (path, options) => {
    const response = await apiFetch(path, options);
    if (response.status === 401) {
      expireAdminSession(navigate);
      return null;
    }
    const data = await response.json().catch(() => null);
    if (!response.ok) {
      const requestError = new Error(data?.message || "Request could not be completed.");
      requestError.status = response.status;
      throw requestError;
    }
    return data;
  }, [navigate]);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const [routeData, stopData] = await Promise.all([
        request("/api/admin/routes"),
        request("/api/admin/stops"),
      ]);
      setRoutes(Array.isArray(routeData) ? routeData : []);
      setStops(Array.isArray(stopData) ? stopData : []);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }, [request]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadData();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadData]);

  const openStopCreate = () => {
    setEditingStopId(null);
    setStopForm(emptyStopForm);
    setShowStopForm(true);
    setError("");
  };

  const openStopEdit = (stop) => {
    setEditingStopId(stop.id);
    setStopForm({
      name: stop.name || "",
      landmark: stop.landmark || "",
      latitude: stop.latitude ?? "",
      longitude: stop.longitude ?? "",
      active: stop.active,
    });
    setShowStopForm(true);
    setError("");
  };

  const submitStop = async (event) => {
    event.preventDefault();
    try {
      setSaving(true);
      setError("");
      await request(editingStopId ? `/api/admin/stops/${editingStopId}` : "/api/admin/stops", {
        method: editingStopId ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ...stopForm,
          latitude: stopForm.latitude === "" ? null : Number(stopForm.latitude),
          longitude: stopForm.longitude === "" ? null : Number(stopForm.longitude),
        }),
      });
      toast.success(editingStopId ? "Bus stop updated." : "Bus stop created.");
      setShowStopForm(false);
      await loadData();
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  };

  const toggleStop = async (stop) => {
    if (stop.active) {
      setStopToDeactivate(stop);
      return;
    }
    await updateStopStatus(stop);
  };

  const updateStopStatus = async (stop) => {
    try {
      setSaving(true);
      setError("");
      await request(`/api/admin/stops/${stop.id}/status`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ active: !stop.active }),
      });
      toast.success(stop.active ? "Bus stop deactivated." : "Bus stop activated.");
      setStopToDeactivate(null);
      await loadData();
    } catch (toggleError) {
      setError(toggleError.message);
    } finally {
      setSaving(false);
    }
  };

  const openRouteCreate = (tripType = "OUT_OF_VALLEY") => {
    setEditingRouteId(null);
    setRouteForm({ ...emptyRouteForm, tripType, stops: tripType === "LOCAL" ? defaultStopRows() : [] });
    setShowRouteForm(true);
    setError("");
  };

  const openRouteEdit = (route) => {
    setEditingRouteId(route.id);
    setRouteForm({
      code: route.code || "",
      name: route.name || "",
      origin: route.origin || "",
      destination: route.destination || "",
      distanceKm: route.distanceKm ?? "",
      estimatedDurationMinutes: route.estimatedDurationMinutes ?? "",
      operatingStartTime: route.operatingStartTime || "",
      operatingEndTime: route.operatingEndTime || "",
      tripType: route.tripType || "OUT_OF_VALLEY",
      status: route.status || "ACTIVE",
      stops: route.tripType === "LOCAL" && route.stops?.length
        ? route.stops.map((stop, index) => ({
            busStopId: String(stop.busStopId),
            stopOrder: index + 1,
            estimatedMinutesFromStart: stop.estimatedMinutesFromStart ?? "",
            cumulativeFare: stop.cumulativeFare ?? "",
          }))
        : route.tripType === "LOCAL" ? defaultStopRows() : [],
    });
    setShowRouteForm(true);
    setError("");
  };

  const submitRoute = async (event) => {
    event.preventDefault();
    const payload = buildRoutePayload(routeForm);
    if (payload.tripType === "LOCAL") {
      const validationError = validateLocalStops(payload.stops);
      if (validationError) {
        setError(validationError);
        return;
      }
    }
    try {
      setSaving(true);
      setError("");
      await request(
        editingRouteId ? `/api/admin/routes/${editingRouteId}` : payload.tripType === "LOCAL" ? "/api/admin/routes/local" : "/api/admin/routes",
        {
          method: editingRouteId ? "PUT" : "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        }
      );
      toast.success(editingRouteId ? "Route updated." : "Route created.");
      setShowRouteForm(false);
      await loadData();
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  };

  const updateRouteStatus = async (route) => {
    if (!route || routeStatusBusyId) return;
    const nextStatus = route.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      setRouteStatusBusyId(route.id);
      setError("");
      const updated = await request(`/api/admin/routes/${route.id}/status`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: nextStatus }),
      });
      if (updated) {
        setRoutes((current) => current.map((item) => item.id === route.id ? updated : item));
        toast.success(nextStatus === "ACTIVE" ? "Route activated." : "Route deactivated.");
      }
    } catch (statusError) {
      setError(statusError.message);
      toast.error(statusError.message);
    } finally {
      setRouteStatusBusyId(null);
    }
  };

  const deleteRoute = async () => {
    if (!routeToDelete || deletingRouteId) return;
    const routeId = routeToDelete.id;
    try {
      setDeletingRouteId(routeId);
      setError("");
      await request(`/api/admin/routes/${routeId}`, { method: "DELETE" });
      setRoutes((current) => current.filter((route) => route.id !== routeId));
      setRouteToDelete(null);
      toast.success("Route permanently deleted.");
    } catch (deleteError) {
      setError(deleteError.message);
      toast.error(deleteError.status === 409
        ? deleteError.message
        : `Unable to delete route: ${deleteError.message}`);
    } finally {
      setDeletingRouteId(null);
    }
  };

  const updateRouteStop = (index, field, value) => {
    setRouteForm((current) => ({
      ...current,
      stops: current.stops.map((stop, stopIndex) => stopIndex === index ? { ...stop, [field]: value } : stop),
    }));
  };

  const addRouteStop = () => {
    setRouteForm((current) => ({
      ...current,
      stops: [...current.stops, { busStopId: "", stopOrder: current.stops.length + 1, estimatedMinutesFromStart: "", cumulativeFare: "" }],
    }));
  };

  const removeRouteStop = (index) => {
    setRouteForm((current) => ({
      ...current,
      stops: current.stops.filter((_, stopIndex) => stopIndex !== index).map((stop, stopIndex) => ({ ...stop, stopOrder: stopIndex + 1 })),
    }));
  };

  const moveRouteStop = (index, direction) => {
    setRouteForm((current) => {
      const nextIndex = index + direction;
      if (nextIndex < 0 || nextIndex >= current.stops.length) return current;
      const nextStops = [...current.stops];
      [nextStops[index], nextStops[nextIndex]] = [nextStops[nextIndex], nextStops[index]];
      return { ...current, stops: nextStops.map((stop, stopIndex) => ({ ...stop, stopOrder: stopIndex + 1 })) };
    });
  };

  return (
    <AdminLayout title="Route Management" subtitle="Manage outside-valley routes, local routes, and bus stops.">
      <div className="space-y-8">
        <div className="flex flex-col justify-between gap-4 xl:flex-row xl:items-center">
          <div className="min-w-0">
            <h1 className="safe-wrap text-2xl font-black text-slate-900">Routes & Local Stops</h1>
            <p className="mt-1 text-sm text-slate-500">Local travel uses ordered stops and cumulative fare. Outside-valley scheduled trips remain separate.</p>
          </div>
          <div className="grid gap-2 sm:grid-cols-3">
            <button type="button" onClick={openStopCreate} className="tap-target rounded-xl border border-slate-300 px-4 py-3 text-sm font-black text-slate-700 hover:bg-white"><Plus size={17} className="inline" /> Add Stop</button>
            <button type="button" onClick={() => openRouteCreate("LOCAL")} className="tap-target rounded-xl bg-emerald-600 px-4 py-3 text-sm font-black text-white hover:bg-emerald-700"><Plus size={17} className="inline" /> Local Route</button>
            <button type="button" onClick={() => openRouteCreate("OUT_OF_VALLEY")} className="tap-target rounded-xl bg-[#08264a] px-4 py-3 text-sm font-black text-white hover:bg-[#0d3566]"><Plus size={17} className="inline" /> Outside Route</button>
          </div>
        </div>

        {error && <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">{error}</div>}
        {loading ? <LoadingCard /> : (
          <>
            <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
              <div className="flex flex-col justify-between gap-2 lg:flex-row lg:items-end">
                <div>
                  <h2 className="text-xl font-black text-slate-900">Route Directory</h2>
                  <p className="mt-1 text-sm font-semibold text-slate-500">
                    Showing {filteredRoutes.length} of {routes.length} routes
                  </p>
                </div>
                <label className="relative block w-full lg:max-w-sm">
                  <span className="sr-only">Search routes</span>
                  <Search className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                  <input
                    type="search"
                    value={routeSearch}
                    onChange={(event) => setRouteSearch(event.target.value)}
                    placeholder="Search code, route, origin or destination"
                    className="w-full rounded-xl border border-slate-300 bg-slate-50 py-3 pl-11 pr-4 text-sm font-bold outline-none focus:border-[#08264a]"
                  />
                </label>
              </div>
              <div className="mt-5 grid gap-4 xl:grid-cols-2">
                <FilterGroup
                  label="Route type"
                  value={routeTypeFilter}
                  onChange={setRouteTypeFilter}
                  options={[
                    ["ALL", "All"],
                    ["LOCAL", "Local"],
                    ["OUT_OF_VALLEY", "Out of Valley"],
                  ]}
                />
                <FilterGroup
                  label="Status"
                  value={routeStatusFilter}
                  onChange={setRouteStatusFilter}
                  options={[["ALL", "All"], ["ACTIVE", "Active"], ["INACTIVE", "Inactive"]]}
                />
              </div>
            </section>
            {filteredRoutes.length === 0 ? (
              <Empty text="No routes match the selected search and filters." />
            ) : (
              <>
                {routeTypeFilter !== "OUT_OF_VALLEY" && localRoutes.length > 0 && (
                  <RouteSection
                    title="Local Routes"
                    routes={localRoutes}
                    onEdit={openRouteEdit}
                    onStatusChange={updateRouteStatus}
                    onDelete={setRouteToDelete}
                    routeStatusBusyId={routeStatusBusyId}
                    deletingRouteId={deletingRouteId}
                  />
                )}
                {routeTypeFilter !== "LOCAL" && outsideRoutes.length > 0 && (
                  <RouteSection
                    title="Outside Valley Routes"
                    routes={outsideRoutes}
                    onEdit={openRouteEdit}
                    onStatusChange={updateRouteStatus}
                    onDelete={setRouteToDelete}
                    routeStatusBusyId={routeStatusBusyId}
                    deletingRouteId={deletingRouteId}
                  />
                )}
              </>
            )}
            <StopSection stops={stops} onEdit={openStopEdit} onToggle={toggleStop} />
          </>
        )}
      </div>

      {showStopForm && (
        <Modal title={editingStopId ? "Edit Bus Stop" : "Create Bus Stop"} onClose={() => setShowStopForm(false)}>
          <form onSubmit={submitStop} className="space-y-4">
            <Field label="Stop Name" value={stopForm.name} onChange={(name) => setStopForm({ ...stopForm, name })} />
            <Field label="Landmark / Description" required={false} value={stopForm.landmark} onChange={(landmark) => setStopForm({ ...stopForm, landmark })} />
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Latitude" type="number" step="0.0000001" required={false} value={stopForm.latitude} onChange={(latitude) => setStopForm({ ...stopForm, latitude })} />
              <Field label="Longitude" type="number" step="0.0000001" required={false} value={stopForm.longitude} onChange={(longitude) => setStopForm({ ...stopForm, longitude })} />
            </div>
            <label className="flex items-center gap-3 rounded-2xl bg-slate-50 p-4 text-sm font-black text-slate-700">
              <input type="checkbox" checked={stopForm.active} onChange={(event) => setStopForm({ ...stopForm, active: event.target.checked })} />
              Active stop
            </label>
            <SaveButton saving={saving} label="Save Stop" />
          </form>
        </Modal>
      )}

      {showRouteForm && (
        <Modal title={editingRouteId ? "Edit Route" : routeForm.tripType === "LOCAL" ? "Create Local Route" : "Create Outside Valley Route"} onClose={() => setShowRouteForm(false)} wide>
          <form onSubmit={submitRoute} className="space-y-5">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Route Code" value={routeForm.code} onChange={(code) => setRouteForm({ ...routeForm, code })} />
              <Field label="Route Name" value={routeForm.name} onChange={(name) => setRouteForm({ ...routeForm, name })} />
              <Field label="Origin" value={routeForm.origin} onChange={(origin) => setRouteForm({ ...routeForm, origin })} />
              <Field label="Destination" value={routeForm.destination} onChange={(destination) => setRouteForm({ ...routeForm, destination })} />
              <Field label="Distance (km)" type="number" step="0.01" value={routeForm.distanceKm} onChange={(distanceKm) => setRouteForm({ ...routeForm, distanceKm })} />
              <Field label="Estimated Duration (minutes)" type="number" value={routeForm.estimatedDurationMinutes} onChange={(estimatedDurationMinutes) => setRouteForm({ ...routeForm, estimatedDurationMinutes })} />
              <label><span className="text-xs font-black uppercase tracking-wider text-slate-500">Trip Type</span><select required value={routeForm.tripType} disabled={Boolean(editingRouteId)} onChange={(event) => setRouteForm({ ...routeForm, tripType: event.target.value, stops: event.target.value === "LOCAL" ? defaultStopRows() : [] })} className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 font-bold outline-none"><option value="OUT_OF_VALLEY">Outside Valley</option><option value="LOCAL">Local</option></select></label>
              <label><span className="text-xs font-black uppercase tracking-wider text-slate-500">Status</span><select value={routeForm.status} onChange={(event) => setRouteForm({ ...routeForm, status: event.target.value })} className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 font-bold outline-none"><option value="ACTIVE">ACTIVE</option><option value="INACTIVE">INACTIVE</option></select></label>
              {routeForm.tripType === "LOCAL" && <>
                <Field label="Operating Start Time" type="time" required={false} value={routeForm.operatingStartTime} onChange={(operatingStartTime) => setRouteForm({ ...routeForm, operatingStartTime })} />
                <Field label="Operating End Time" type="time" required={false} value={routeForm.operatingEndTime} onChange={(operatingEndTime) => setRouteForm({ ...routeForm, operatingEndTime })} />
              </>}
            </div>

            {routeForm.tripType === "LOCAL" && (
              <section className="rounded-3xl border border-emerald-100 bg-emerald-50/60 p-4">
                <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
                  <div><h3 className="font-black text-slate-900">Ordered Local Stops</h3><p className="text-sm font-semibold text-slate-600">First stop is boarding origin; last stop is destination. Fare is cumulative from route start.</p></div>
                  <button type="button" onClick={addRouteStop} className="tap-target rounded-xl bg-emerald-600 px-4 py-2 text-sm font-black text-white">Add Stop</button>
                </div>
                <div className="mt-4 space-y-3">
                  {routeForm.stops.map((stop, index) => (
                    <div key={index} className="grid gap-3 rounded-2xl bg-white p-3 shadow-sm lg:grid-cols-[auto_1fr_1fr_1fr_auto] lg:items-end">
                      <div className="font-black text-slate-500">#{index + 1}</div>
                      <label><span className="text-xs font-black uppercase text-slate-500">Stop</span><select required value={stop.busStopId} onChange={(event) => updateRouteStop(index, "busStopId", event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-3 font-bold"><option value="">Select stop</option>{stops.filter((item) => item.active).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
                      <Field label="Minutes from start" type="number" value={stop.estimatedMinutesFromStart} onChange={(value) => updateRouteStop(index, "estimatedMinutesFromStart", value)} />
                      <Field label="Cumulative fare" type="number" step="0.01" value={stop.cumulativeFare} onChange={(value) => updateRouteStop(index, "cumulativeFare", value)} />
                      <div className="flex gap-2">
                        <IconButton label="Move up" onClick={() => moveRouteStop(index, -1)} disabled={index === 0}><ArrowUp size={16} /></IconButton>
                        <IconButton label="Move down" onClick={() => moveRouteStop(index, 1)} disabled={index === routeForm.stops.length - 1}><ArrowDown size={16} /></IconButton>
                        <IconButton label="Remove stop" danger onClick={() => removeRouteStop(index)} disabled={routeForm.stops.length <= 2}><Trash2 size={16} /></IconButton>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            )}
            <SaveButton saving={saving} label="Save Route" />
          </form>
        </Modal>
      )}
      <ConfirmationModal
        open={Boolean(stopToDeactivate)}
        title="Deactivate bus stop?"
        message={`${stopToDeactivate?.name || "This stop"} will be hidden from passenger stop search. Existing route data will remain unchanged.`}
        confirmLabel="Deactivate Stop"
        destructive
        busy={saving}
        busyLabel="Deactivating..."
        onConfirm={() => updateStopStatus(stopToDeactivate)}
        onClose={() => setStopToDeactivate(null)}
      />
      <ConfirmationModal
        open={Boolean(routeToDelete)}
        title="Permanently delete route?"
        message={`${routeToDelete?.name || "This route"} (${routeToDelete?.origin || "Origin"} → ${routeToDelete?.destination || "Destination"}) will be permanently deleted. This cannot be undone. Routes with operational history cannot be deleted and should be deactivated instead.`}
        confirmLabel="Delete Route"
        destructive
        busy={Boolean(deletingRouteId)}
        busyLabel="Deleting..."
        onConfirm={deleteRoute}
        onClose={() => setRouteToDelete(null)}
      />
    </AdminLayout>
  );
}

function StopSection({ stops, onEdit, onToggle }) {
  return <section className="space-y-4"><h2 className="text-xl font-black text-slate-900">Bus Stops</h2>{stops.length === 0 ? <Empty text="No bus stops created yet." /> : <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{stops.map((stop) => <article key={stop.id} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><h3 className="safe-wrap font-black text-slate-900">{stop.name}</h3><p className="safe-wrap mt-1 text-sm font-semibold text-slate-500">{stop.landmark || "No landmark"}</p></div><Status active={stop.active} /></div><p className="mt-4 text-sm font-bold text-slate-600">{coordinate(stop.latitude, stop.longitude)}</p><div className="mt-4 grid gap-2 sm:grid-cols-2"><button type="button" onClick={() => onEdit(stop)} className="tap-target rounded-xl border border-slate-300 px-4 py-2 font-black text-slate-700">Edit</button><button type="button" onClick={() => onToggle(stop)} className="tap-target rounded-xl bg-slate-100 px-4 py-2 font-black text-slate-700">{stop.active ? "Deactivate" : "Activate"}</button></div></article>)}</div>}</section>;
}

function RouteSection({ title, routes, onEdit, onStatusChange, onDelete, routeStatusBusyId, deletingRouteId }) {
  return (
    <section className="space-y-4">
      <h2 className="text-xl font-black text-slate-900">{title}</h2>
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        {routes.map((route) => (
          <article key={route.id} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-start justify-between gap-4"><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><span className="rounded-lg bg-blue-50 px-3 py-1 text-xs font-black text-[#08264a]">{route.code}</span><Status active={route.status === "ACTIVE"} label={route.status} /><TripTypeBadge tripType={route.tripType} /></div><h3 className="safe-wrap mt-3 text-xl font-black text-slate-900">{route.name}</h3></div><div className="flex gap-2"><button type="button" onClick={() => onEdit(route)} disabled={routeStatusBusyId === route.id || deletingRouteId === route.id} className="tap-target rounded-xl border border-slate-200 p-3 text-slate-600 disabled:opacity-50" title="Edit route"><Edit3 size={18} /></button><button type="button" onClick={() => onStatusChange(route)} disabled={routeStatusBusyId === route.id || deletingRouteId === route.id} className="tap-target rounded-xl bg-slate-100 px-3 text-xs font-black text-slate-700 disabled:opacity-50">{routeStatusBusyId === route.id ? <Loader2 size={16} className="animate-spin" /> : route.status === "ACTIVE" ? "Deactivate" : "Activate"}</button><button type="button" onClick={() => onDelete(route)} disabled={routeStatusBusyId === route.id || deletingRouteId === route.id} className="tap-target rounded-xl border border-red-200 bg-red-50 p-3 text-red-700 disabled:opacity-50" title="Delete route">{deletingRouteId === route.id ? <Loader2 size={18} className="animate-spin" /> : <Trash2 size={18} />}</button></div></div>
            <div className="safe-wrap mt-5 flex items-start gap-3 rounded-2xl bg-slate-50 p-4 text-sm font-bold text-slate-700"><MapPin size={18} /> {route.origin} to {route.destination}</div><div className="mt-4 grid grid-cols-2 gap-3 text-sm"><Info label="Distance" value={`${route.distanceKm} km`} /><Info label="Duration" value={`${route.estimatedDurationMinutes} min`} /></div>{route.tripType === "LOCAL" && <div className="safe-wrap mt-4 rounded-2xl border border-emerald-100 bg-emerald-50 p-3 text-sm font-bold text-emerald-800">{route.stops?.length || 0} ordered stop(s) · {route.operatingStartTime || "--"} to {route.operatingEndTime || "--"}</div>}
          </article>
        ))}
      </div>
    </section>
  );
}

function FilterGroup({ label, value, onChange, options }) {
  return <fieldset><legend className="text-xs font-black uppercase tracking-wider text-slate-500">{label}</legend><div className="mt-2 flex flex-wrap gap-2">{options.map(([optionValue, optionLabel]) => <button key={optionValue} type="button" aria-pressed={value === optionValue} onClick={() => onChange(optionValue)} className={`rounded-xl px-4 py-2 text-sm font-black transition ${value === optionValue ? "bg-[#08264a] text-white shadow-sm" : "border border-slate-200 bg-slate-50 text-slate-600 hover:bg-slate-100"}`}>{optionLabel}</button>)}</div></fieldset>;
}
function Modal({ title, onClose, children, wide = false }) {
  return <div className="responsive-modal-backdrop fixed inset-0 z-50 flex justify-center bg-black/50 sm:items-center"><div className={`responsive-modal-panel w-full ${wide ? "max-w-5xl" : "max-w-2xl"} rounded-3xl bg-white p-5 shadow-2xl sm:p-6`}><div className="mb-6 flex items-start justify-between gap-4"><div className="min-w-0"><h2 className="safe-wrap text-xl font-black sm:text-2xl">{title}</h2><p className="mt-1 text-sm text-slate-500">Admin-only local travel foundation controls.</p></div><button type="button" aria-label="Close form" onClick={onClose} className="tap-target rounded-xl p-2 hover:bg-slate-100"><X /></button></div>{children}</div></div>;
}

function Field({ label, value, onChange, type = "text", step, required = true }) {
  return <label><span className="text-xs font-black uppercase tracking-wider text-slate-500">{label}</span><input required={required} type={type} min={type === "number" ? "0" : undefined} step={step} value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 font-bold outline-none focus:border-[#08264a]" /></label>;
}

function SaveButton({ saving, label }) {
  return <button type="submit" disabled={saving} className="tap-target flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 font-black text-white disabled:opacity-60">{saving && <Loader2 size={18} className="animate-spin" />}{saving ? "Saving..." : label}</button>;
}

function IconButton({ label, onClick, disabled, danger, children }) {
  return <button type="button" aria-label={label} disabled={disabled} onClick={onClick} className={`tap-target rounded-xl border px-3 py-2 disabled:opacity-40 ${danger ? "border-red-200 text-red-600" : "border-slate-200 text-slate-600"}`}>{children}</button>;
}

function Info({ label, value }) {
  return <div className="rounded-xl border border-slate-100 p-3"><p className="text-xs font-black uppercase text-slate-400">{label}</p><p className="safe-wrap mt-1 font-black text-slate-800">{value}</p></div>;
}

function Status({ active, label }) {
  return <span className={`rounded-full px-3 py-1 text-xs font-black ${active ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-600"}`}>{label || (active ? "ACTIVE" : "INACTIVE")}</span>;
}

function TripTypeBadge({ tripType }) {
  const local = tripType === "LOCAL";
  return <span className={`rounded-full px-3 py-1 text-xs font-black ${local ? "bg-amber-100 text-amber-700" : "bg-violet-100 text-violet-700"}`}>{local ? "Local" : "Outside Valley"}</span>;
}

function Empty({ text }) {
  return <div className="rounded-3xl border border-slate-200 bg-white p-10 text-center"><RouteIcon className="mx-auto text-slate-400" size={44} /><p className="mt-4 font-black text-slate-700">{text}</p></div>;
}

function LoadingCard() {
  return <div className="flex min-h-72 items-center justify-center rounded-3xl border border-slate-200 bg-white"><Loader2 className="animate-spin" size={40} /></div>;
}

function defaultStopRows() {
  return [
    { busStopId: "", stopOrder: 1, estimatedMinutesFromStart: 0, cumulativeFare: 0 },
    { busStopId: "", stopOrder: 2, estimatedMinutesFromStart: "", cumulativeFare: "" },
  ];
}

function buildRoutePayload(form) {
  return {
    ...form,
    distanceKm: Number(form.distanceKm),
    estimatedDurationMinutes: Number(form.estimatedDurationMinutes),
    operatingStartTime: form.operatingStartTime || null,
    operatingEndTime: form.operatingEndTime || null,
    stops: form.tripType === "LOCAL" ? form.stops.map((stop, index) => ({
      busStopId: Number(stop.busStopId),
      stopOrder: index + 1,
      estimatedMinutesFromStart: Number(stop.estimatedMinutesFromStart),
      cumulativeFare: Number(stop.cumulativeFare),
    })) : null,
  };
}

function validateLocalStops(stops) {
  if (!stops || stops.length < 2) return "Local route must have at least two stops.";
  const selected = new Set();
  for (let index = 0; index < stops.length; index += 1) {
    const stop = stops[index];
    if (!stop.busStopId) return "Select every local route stop.";
    if (selected.has(stop.busStopId)) return "Duplicate stops are not allowed.";
    selected.add(stop.busStopId);
    if (stop.estimatedMinutesFromStart < 0 || stop.cumulativeFare < 0) return "Minutes and fare cannot be negative.";
    if (index > 0) {
      const previous = stops[index - 1];
      if (Number(stop.estimatedMinutesFromStart) < Number(previous.estimatedMinutesFromStart)) return "Estimated minutes must increase or stay equal along the route.";
      if (Number(stop.cumulativeFare) < Number(previous.cumulativeFare)) return "Cumulative fare must increase or stay equal along the route.";
    }
  }
  return "";
}

function coordinate(latitude, longitude) {
  if (latitude == null || longitude == null) return "Coordinates not set";
  return `${latitude}, ${longitude}`;
}
