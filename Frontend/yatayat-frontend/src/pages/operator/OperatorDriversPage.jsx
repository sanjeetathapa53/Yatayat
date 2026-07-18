import { useEffect, useMemo, useState } from "react";
import { Loader2, Mail, RefreshCw, Search, UserPlus, Users, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import OperatorLayout from "../../components/layout/OperatorLayout";
import { apiFetch } from "../../utils/api";

export default function OperatorDriversPage() {
  const navigate = useNavigate();
  const [associations, setAssociations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showSearch, setShowSearch] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [inviting, setInviting] = useState(null);

  const loadAssociations = async () => {
    try {
      setLoading(true);
      setError("");
      const response = await apiFetch("/api/operator/drivers");
      if (response.status === 401) return navigate("/login", { replace: true });
      if (response.status === 403 || response.status === 404) {
        return navigate("/operator/application-status", { replace: true });
      }
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to load drivers.");
      setAssociations(Array.isArray(data) ? data : []);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    apiFetch("/api/operator/drivers").then(async (response) => {
      if (response.status === 401) return navigate("/login", { replace: true });
      if (response.status === 403 || response.status === 404) {
        return navigate("/operator/application-status", { replace: true });
      }
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to load drivers.");
      return data;
    }).then((data) => { if (active && data) setAssociations(data); })
      .catch((loadError) => { if (active) setError(loadError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [navigate]);

  const activeDrivers = useMemo(
    () => associations.filter((item) => item.associationStatus === "ACTIVE"),
    [associations]
  );
  const invitations = useMemo(
    () => associations.filter((item) => item.associationStatus !== "ACTIVE"),
    [associations]
  );

  const searchDrivers = async (event) => {
    event?.preventDefault();
    try {
      setSearching(true);
      setError("");
      const response = await apiFetch(`/api/operator/drivers/eligible?query=${encodeURIComponent(query.trim())}`);
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to search drivers.");
      setResults(Array.isArray(data) ? data : []);
    } catch (searchError) {
      setError(searchError.message);
    } finally {
      setSearching(false);
    }
  };

  const invite = async (driver) => {
    try {
      setInviting(driver.driverId);
      const response = await apiFetch("/api/operator/driver-invitations", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ driverId: driver.driverId }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to send invitation.");
      setAssociations((current) => [data, ...current]);
      setResults((current) => current.filter((item) => item.driverId !== driver.driverId));
      toast.success("Driver invitation sent.");
    } catch (inviteError) {
      setError(inviteError.message);
    } finally {
      setInviting(null);
    }
  };

  return (
    <OperatorLayout>
      <div className="space-y-7">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div className="min-w-0">
            <h1 className="text-2xl font-black sm:text-3xl">Manage Drivers</h1>
            <p className="mt-1 text-sm text-slate-500">
              Invite approved drivers and view current associations.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setShowSearch(true)}
            className="tap-target flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white"
          >
            <UserPlus size={18} /> Find Driver
          </button>
        </div>

        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}

        {loading ? (
          <div className="flex min-h-72 items-center justify-center"><Loader2 size={40} className="animate-spin" /></div>
        ) : (
          <>
            <Section title="Associated Drivers" icon={<Users />} empty="No active drivers are associated with your organization.">
              {activeDrivers.map((item) => <DriverCard key={item.associationId} item={item} />)}
            </Section>
            <Section title="Invitation History" icon={<Mail />} empty="No driver invitations have been sent.">
              {invitations.map((item) => <DriverCard key={item.associationId} item={item} />)}
            </Section>
            <button type="button" onClick={loadAssociations} className="tap-target flex items-center gap-2 text-sm font-black">
              <RefreshCw size={16} /> Refresh
            </button>
          </>
        )}
      </div>

      {showSearch && (
        <DriverSearchModal
          query={query}
          setQuery={setQuery}
          results={results}
          searching={searching}
          inviting={inviting}
          onSearch={searchDrivers}
          onInvite={invite}
          onClose={() => setShowSearch(false)}
        />
      )}
    </OperatorLayout>
  );
}

function DriverSearchModal({ query, setQuery, results, searching, inviting, onSearch, onInvite, onClose }) {
  return (
    <div className="responsive-modal-backdrop fixed inset-0 z-50 flex justify-center bg-black/50 sm:items-center">
      <div className="responsive-modal-panel w-full max-w-2xl rounded-3xl bg-white p-5 shadow-2xl sm:p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h2 className="text-xl font-black sm:text-2xl">Find approved driver</h2>
            <p className="mt-1 text-sm text-slate-500">Search by name, email, phone or licence number.</p>
          </div>
          <button type="button" aria-label="Close driver search" onClick={onClose} className="rounded-lg p-2 hover:bg-slate-100">
            <X />
          </button>
        </div>
        <form onSubmit={onSearch} className="mt-5 flex flex-col gap-3 sm:flex-row">
          <div className="flex flex-1 items-center gap-2 rounded-xl border border-slate-300 px-4">
            <Search size={18} />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="w-full min-w-0 py-3 outline-none"
              placeholder="Search drivers"
            />
          </div>
          <button type="submit" disabled={searching} className="tap-target rounded-xl bg-[#08264a] px-5 font-black text-white">
            {searching ? <Loader2 className="animate-spin" /> : "Search"}
          </button>
        </form>
        <div className="mt-5 space-y-3">
          {results.length === 0 ? (
            <p className="rounded-xl bg-slate-50 p-6 text-center text-sm font-bold text-slate-500">
              Search to find approved, available drivers.
            </p>
          ) : results.map((driver) => (
            <div key={driver.driverId} className="flex flex-col justify-between gap-3 rounded-2xl border border-slate-200 p-4 sm:flex-row sm:items-center">
              <div className="min-w-0">
                <p className="safe-wrap font-black">{driver.fullName}</p>
                <p className="safe-wrap mt-1 text-sm text-slate-500">{driver.email} · {driver.licenseNumber}</p>
              </div>
              <button
                type="button"
                disabled={inviting === driver.driverId}
                onClick={() => onInvite(driver)}
                className="tap-target flex items-center justify-center gap-2 rounded-xl bg-blue-50 px-4 py-2 font-black text-[#08264a] disabled:opacity-50"
              >
                {inviting === driver.driverId && <Loader2 size={16} className="animate-spin" />}
                Invite
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function Section({ title, icon, empty, children }) {
  const items = Array.isArray(children) ? children : [children].filter(Boolean);
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
      <div className="flex items-center gap-3">
        <span className="text-[#08264a]">{icon}</span>
        <h2 className="text-xl font-black">{title}</h2>
      </div>
      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        {items.length ? items : <p className="rounded-2xl bg-slate-50 p-8 text-center font-bold text-slate-500 lg:col-span-2">{empty}</p>}
      </div>
    </section>
  );
}

function DriverCard({ item }) {
  const tones = { ACTIVE: "bg-emerald-100 text-emerald-700", PENDING: "bg-amber-100 text-amber-700", REJECTED: "bg-red-100 text-red-700" };
  return (
    <article className="rounded-2xl border border-slate-200 p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="safe-wrap font-black text-slate-900">{item.driverName}</h3>
          <p className="safe-wrap mt-1 text-sm text-slate-500">{item.driverEmail}</p>
        </div>
        <span className={`shrink-0 rounded-full px-3 py-1 text-xs font-black ${tones[item.associationStatus]}`}>
          {item.associationStatus}
        </span>
      </div>
      <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
        <p className="safe-wrap">Licence: <b>{item.licenseNumber}</b></p>
        <p>Invited: <b>{new Date(item.invitedAt).toLocaleDateString()}</b></p>
      </div>
    </article>
  );
}
