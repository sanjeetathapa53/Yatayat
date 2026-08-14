import { useCallback, useEffect, useRef, useState } from "react";
import { AlertCircle, Bell, Building2, Bus, CheckCircle2, RefreshCw } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";

export default function AdminApprovalNotifications() {
  const navigate = useNavigate();
  const panelRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    setError("");
    const [busResult, operatorResult] = await Promise.allSettled([
      apiFetch("/api/admin/buses?status=PENDING"),
      apiFetch("/api/admin/operators"),
    ]);
    const nextItems = [];
    let failedSources = 0;

    if (busResult.status === "fulfilled" && busResult.value.ok) {
      const buses = await busResult.value.json();
      if (Array.isArray(buses)) {
        buses.forEach((bus) => nextItems.push({
          id: `bus-${bus.id}`,
          type: "BUS",
          title: "Bus approval",
          name: bus.busName || bus.busNumber || "A bus",
          description: `${bus.operatorName || "An operator"} submitted this bus for review.`,
          path: "/admin/buses",
          createdAt: bus.createdAt,
        }));
      }
    } else failedSources++;

    if (operatorResult.status === "fulfilled" && operatorResult.value.ok) {
      const operators = await operatorResult.value.json();
      if (Array.isArray(operators)) {
        operators.filter((operator) => operator.verificationStatus === "PENDING")
          .forEach((operator) => nextItems.push({
            id: `operator-${operator.id}`,
            type: "OPERATOR",
            title: "Operator application",
            name: operator.name || "An operator",
            description: "This transport operator is waiting for review.",
            path: "/admin/operators",
            createdAt: operator.createdAt,
          }));
      }
    } else failedSources++;

    nextItems.sort((first, second) =>
      new Date(second.createdAt || 0) - new Date(first.createdAt || 0));
    setItems(nextItems);
    setError(failedSources === 2
      ? "Approval notifications are temporarily unavailable."
      : failedSources === 1 ? "Some approval notifications could not be loaded." : "");
    setLoading(false);
  }, []);

  useEffect(() => {
    Promise.resolve().then(loadNotifications);
  }, [loadNotifications]);

  useEffect(() => {
    if (!open) return undefined;
    const closeOutside = (event) => {
      if (!panelRef.current?.contains(event.target)) setOpen(false);
    };
    const closeOnEscape = (event) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", closeOutside);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeOutside);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  const review = (path) => {
    setOpen(false);
    navigate(path);
  };

  return (
    <div className="relative" ref={panelRef}>
      <button
        type="button"
        aria-label={items.length ? `${items.length} pending approvals` : "Approval notifications"}
        aria-expanded={open}
        aria-controls="admin-approval-notifications"
        onClick={() => setOpen((current) => !current)}
        className="relative flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition hover:bg-slate-50 hover:text-[#08264a] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#08264a]"
      >
        <Bell size={19} aria-hidden="true" />
        {!loading && items.length > 0 && (
          <span className="absolute -right-1.5 -top-1.5 flex min-h-5 min-w-5 items-center justify-center rounded-full border-2 border-white bg-red-600 px-1 text-[10px] font-bold leading-none text-white">
            {items.length > 99 ? "99+" : items.length}
          </span>
        )}
      </button>

      {open && (
        <section
          id="admin-approval-notifications"
          aria-label="Pending approval notifications"
          className="fixed inset-x-4 top-[4.5rem] z-50 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl sm:absolute sm:inset-x-auto sm:right-0 sm:top-12 sm:w-[23rem]"
        >
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
            <div>
              <h3 className="text-sm font-semibold text-slate-900">Pending approvals</h3>
              <p className="mt-0.5 text-xs text-slate-500">
                {loading ? "Checking for new submissions…" : `${items.length} item${items.length === 1 ? "" : "s"} requiring review`}
              </p>
            </div>
            <button type="button" onClick={loadNotifications} disabled={loading}
              aria-label="Refresh approval notifications"
              className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-[#08264a] disabled:opacity-50">
              <RefreshCw size={16} className={loading ? "animate-spin" : ""} aria-hidden="true" />
            </button>
          </div>

          {error && (
            <div className="flex gap-2 border-b border-amber-100 bg-amber-50 px-4 py-2.5 text-xs leading-5 text-amber-800">
              <AlertCircle size={15} className="mt-0.5 shrink-0" aria-hidden="true" />
              <span>{error}</span>
            </div>
          )}

          <div className="max-h-[min(24rem,calc(100vh-9rem))] overflow-y-auto">
            {loading && items.length === 0 ? <LoadingState />
              : items.length === 0 ? <EmptyState />
                : <div className="divide-y divide-slate-100">
                  {items.map((item) => <NotificationItem key={item.id} item={item} onReview={() => review(item.path)} />)}
                </div>}
          </div>
        </section>
      )}
    </div>
  );
}

function NotificationItem({ item, onReview }) {
  const Icon = item.type === "BUS" ? Bus : Building2;
  const tone = item.type === "BUS" ? "bg-blue-50 text-blue-700" : "bg-emerald-50 text-emerald-700";
  return (
    <button type="button" onClick={onReview}
      className="flex w-full gap-3 px-4 py-3 text-left transition hover:bg-slate-50 focus-visible:bg-slate-50 focus-visible:outline-none">
      <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${tone}`}>
        <Icon size={17} aria-hidden="true" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center justify-between gap-2">
          <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">{item.title}</span>
          <span className="shrink-0 text-[11px] font-semibold text-[#08264a]">Review</span>
        </span>
        <span className="mt-1 block truncate text-sm font-semibold text-slate-900">{item.name}</span>
        <span className="mt-0.5 block text-xs leading-5 text-slate-500">{item.description}</span>
      </span>
    </button>
  );
}

function LoadingState() {
  return <div className="space-y-3 p-4" aria-label="Loading approval notifications">
    {[1, 2].map((item) => <div key={item} className="flex animate-pulse gap-3">
      <div className="h-9 w-9 shrink-0 rounded-xl bg-slate-100" />
      <div className="flex-1 space-y-2 py-1"><div className="h-3 w-2/5 rounded bg-slate-100" /><div className="h-3 w-4/5 rounded bg-slate-100" /></div>
    </div>)}
  </div>;
}

function EmptyState() {
  return <div className="px-5 py-8 text-center">
    <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
      <CheckCircle2 size={20} aria-hidden="true" />
    </div>
    <p className="mt-3 text-sm font-semibold text-slate-900">You&apos;re all caught up</p>
    <p className="mt-1 text-xs text-slate-500">There are no pending bus or operator approvals.</p>
  </div>;
}
