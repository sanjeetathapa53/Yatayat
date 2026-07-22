import { useCallback, useEffect, useMemo, useState } from "react";
import { Bus, Loader2, Mail, Phone, RefreshCw, Search, Trash2, UserPlus, Users } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import ConfirmationModal from "../../components/common/ConfirmationModal";
import OperatorLayout from "../../components/layout/OperatorLayout";
import { apiFetch } from "../../utils/api";

export default function OperatorDriversPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("active");
  const [associations, setAssociations] = useState([]);
  const [availableDrivers, setAvailableDrivers] = useState([]);
  const [availableLoaded, setAvailableLoaded] = useState(false);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [inviting, setInviting] = useState(null);
  const [removing, setRemoving] = useState(null);
  const [driverToRemove, setDriverToRemove] = useState(null);
  const [error, setError] = useState("");

  const handleAccessError = useCallback((status) => {
    if (status === 401) {
      navigate("/login", { replace: true });
      return true;
    }
    if (status === 403 || status === 404) {
      navigate("/operator/application-status", { replace: true });
      return true;
    }
    return false;
  }, [navigate]);

  const loadAssociations = useCallback(async () => {
    const response = await apiFetch("/api/operator/drivers");
    if (handleAccessError(response.status)) return;
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || "Unable to load active drivers.");
    setAssociations(Array.isArray(data) ? data : []);
  }, [handleAccessError]);

  const loadAvailable = useCallback(async (searchQuery = "") => {
    try {
      setSearching(true);
      setError("");
      const response = await apiFetch(`/api/operator/drivers/eligible?query=${encodeURIComponent(searchQuery.trim())}`);
      if (handleAccessError(response.status)) return;
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to load available drivers.");
      setAvailableDrivers(Array.isArray(data) ? data : []);
      setAvailableLoaded(true);
    } catch (loadError) {
      setError(loadError.message || "Unable to load available drivers.");
    } finally {
      setAvailableLoaded(true);
      setSearching(false);
    }
  }, [handleAccessError]);

  useEffect(() => {
    let active = true;
    Promise.resolve()
      .then(() => loadAssociations())
      .catch((loadError) => { if (active) setError(loadError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [loadAssociations]);

  useEffect(() => {
    if (activeTab !== "available" || availableLoaded || searching) return;
    Promise.resolve().then(() => loadAvailable());
  }, [activeTab, availableLoaded, loadAvailable, searching]);

  const activeDrivers = useMemo(
    () => associations.filter((association) => association.associationStatus === "ACTIVE"),
    [associations],
  );

  const inviteDriver = async (driver) => {
    try {
      setInviting(driver.driverId);
      setError("");
      const response = await apiFetch("/api/operator/driver-invitations", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ driverId: driver.driverId }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to send invitation.");
      setAssociations((current) => [data, ...current.filter((item) => item.associationId !== data.associationId)]);
      setAvailableDrivers((current) => current.filter((item) => item.driverId !== driver.driverId));
      toast.success("Driver invitation sent.");
    } catch (inviteError) {
      setError(inviteError.message || "Unable to send invitation.");
      toast.error(inviteError.message || "Unable to send invitation.");
    } finally {
      setInviting(null);
    }
  };

  const removeDriver = async (association) => {
    try {
      setRemoving(association.associationId);
      setError("");
      const response = await apiFetch(`/api/operator/drivers/${association.associationId}`, { method: "DELETE" });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to remove driver.");
      setAssociations((current) => current.map((item) => item.associationId === data.associationId ? data : item));
      setAvailableLoaded(false);
      setDriverToRemove(null);
      toast.success("Driver association ended.");
      if (activeTab === "available") await loadAvailable(query);
    } catch (removeError) {
      setError(removeError.message || "Unable to remove driver.");
      toast.error(removeError.message || "Unable to remove driver.");
    } finally {
      setRemoving(null);
    }
  };

  const refresh = async () => {
    try {
      setLoading(true);
      setError("");
      await loadAssociations();
      if (activeTab === "available") await loadAvailable(query);
    } catch (refreshError) {
      setError(refreshError.message || "Unable to refresh driver information.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <OperatorLayout>
      <div className="space-y-6">
        <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-2xl font-black sm:text-3xl">Driver Management</h1>
            <p className="mt-1 text-sm text-slate-500">Manage active drivers and invite approved drivers who are currently available.</p>
          </div>
          <button type="button" onClick={refresh} disabled={loading} className="tap-target flex items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-black disabled:opacity-50">
            <RefreshCw size={17} className={loading ? "animate-spin" : ""} /> Refresh
          </button>
        </header>

        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}

        <div className="flex rounded-2xl border border-slate-200 bg-white p-1.5 shadow-sm">
          <TabButton active={activeTab === "active"} onClick={() => setActiveTab("active")} icon={<Users size={18} />} label="Active Drivers" count={activeDrivers.length} />
          <TabButton active={activeTab === "available"} onClick={() => setActiveTab("available")} icon={<UserPlus size={18} />} label="Available Drivers" />
        </div>

        {loading ? (
          <div className="flex min-h-72 items-center justify-center"><Loader2 size={40} className="animate-spin" /></div>
        ) : activeTab === "active" ? (
          <ActiveDrivers drivers={activeDrivers} removing={removing} onRemove={setDriverToRemove} />
        ) : (
          <AvailableDrivers
            drivers={availableDrivers}
            query={query}
            setQuery={setQuery}
            searching={searching}
            inviting={inviting}
            onSearch={(event) => { event.preventDefault(); loadAvailable(query); }}
            onInvite={inviteDriver}
          />
        )}
      </div>
      <ConfirmationModal
        open={Boolean(driverToRemove)}
        title="Remove Driver?"
        message="This will remove the driver from your operator. The driver will become available for another operator to invite. This action does not delete the driver's account."
        confirmLabel="Remove Driver"
        destructive
        busy={removing === driverToRemove?.associationId}
        onClose={() => setDriverToRemove(null)}
        onConfirm={() => removeDriver(driverToRemove)}
      />
    </OperatorLayout>
  );
}

function TabButton({ active, onClick, icon, label, count }) {
  return (
    <button type="button" onClick={onClick} className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-3 py-3 text-sm font-black transition ${active ? "bg-[#08264a] text-white" : "text-slate-500 hover:bg-slate-50"}`}>
      {icon} {label} {count !== undefined && <span className={`rounded-full px-2 py-0.5 text-xs ${active ? "bg-white/15" : "bg-slate-100"}`}>{count}</span>}
    </button>
  );
}

function ActiveDrivers({ drivers, removing, onRemove }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
      {drivers.length === 0 ? (
        <EmptyState icon={<Users size={30} />} title="No active drivers" text="Invite an available driver and wait for them to accept the invitation." />
      ) : (
        <div className="grid gap-4 xl:grid-cols-2">
          {drivers.map((driver) => (
            <article key={driver.associationId} className="rounded-2xl border border-slate-200 p-5">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0"><h2 className="safe-wrap text-lg font-black text-slate-900">{driver.driverName}</h2><p className="mt-1 flex items-center gap-2 text-sm text-slate-500"><Mail size={14} /> <span className="safe-wrap">{driver.driverEmail}</span></p><p className="mt-1 flex items-center gap-2 text-sm text-slate-500"><Phone size={14} /> {driver.driverPhone || "Phone unavailable"}</p></div>
                <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-black text-emerald-700">{driver.associationStatus}</span>
              </div>
              <div className="mt-5 rounded-xl bg-slate-50 p-4">
                <p className="flex items-center gap-2 text-xs font-black uppercase text-slate-500"><Bus size={15} /> Assigned buses</p>
                {driver.assignedBuses?.length ? <div className="mt-3 flex flex-wrap gap-2">{driver.assignedBuses.map((bus) => <span key={bus.busId} className="rounded-full bg-blue-100 px-3 py-1 text-xs font-bold text-blue-800">{bus.busNumber} · {bus.busName}</span>)}</div> : <p className="mt-2 text-sm font-semibold text-slate-500">No buses assigned.</p>}
              </div>
              <button type="button" disabled={removing === driver.associationId} onClick={() => onRemove(driver)} className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 py-3 text-sm font-black text-red-700 hover:bg-red-100 disabled:opacity-50">
                {removing === driver.associationId ? <Loader2 size={17} className="animate-spin" /> : <Trash2 size={17} />} Remove Driver
              </button>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function AvailableDrivers({ drivers, query, setQuery, searching, inviting, onSearch, onInvite }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
      <form onSubmit={onSearch} className="flex flex-col gap-3 sm:flex-row">
        <label className="flex flex-1 items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4"><Search size={18} className="text-slate-400" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by name, email or phone" className="w-full bg-transparent py-3 outline-none" /></label>
        <button type="submit" disabled={searching} className="rounded-xl bg-[#08264a] px-6 py-3 font-black text-white disabled:opacity-50">{searching ? <Loader2 size={18} className="mx-auto animate-spin" /> : "Search"}</button>
      </form>
      {searching ? <div className="flex min-h-52 items-center justify-center"><Loader2 size={34} className="animate-spin" /></div> : drivers.length === 0 ? (
        <EmptyState icon={<Search size={30} />} title="No available drivers found" text="Approved drivers with an active association or pending invitation are excluded." />
      ) : (
        <div className="mt-6 grid gap-4 xl:grid-cols-2">
          {drivers.map((driver) => (
            <article key={driver.driverId} className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-200 p-5 sm:flex-row sm:items-center">
              <div className="min-w-0"><h2 className="safe-wrap font-black text-slate-900">{driver.fullName}</h2><p className="safe-wrap mt-1 text-sm text-slate-500">{driver.email}</p><p className="mt-1 text-sm text-slate-500">{driver.phone || "Phone unavailable"}</p><span className="mt-3 inline-flex rounded-full bg-emerald-100 px-3 py-1 text-xs font-black text-emerald-700">APPROVED</span></div>
              <button type="button" disabled={inviting === driver.driverId} onClick={() => onInvite(driver)} className="flex shrink-0 items-center justify-center gap-2 rounded-xl bg-blue-50 px-4 py-3 text-sm font-black text-[#08264a] hover:bg-blue-100 disabled:opacity-50">{inviting === driver.driverId ? <Loader2 size={16} className="animate-spin" /> : <UserPlus size={16} />} Invite Driver</button>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function EmptyState({ icon, title, text }) {
  return <div className="flex min-h-56 flex-col items-center justify-center rounded-2xl bg-slate-50 p-8 text-center text-slate-500"><span className="text-slate-400">{icon}</span><h2 className="mt-4 text-lg font-black text-slate-800">{title}</h2><p className="mt-2 max-w-md text-sm font-semibold">{text}</p></div>;
}
