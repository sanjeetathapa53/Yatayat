import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ArrowLeft,
  Bell,
  CheckCircle,
  Clock,
  CreditCard,
  Ticket,
  Wallet,
  XCircle,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useLanguage } from "../../hooks/useLanguage";
import { useNotifications } from "../../context/NotificationContext";
import PassengerLayout from "../../components/layout/PassengerLayout";
import {
  getPassengerNotifications,
  markAllPassengerNotificationsRead,
  markPassengerNotificationRead,
} from "../../utils/notifications";

const typePresentation = {
  BOOKING_CONFIRMED: { category: "booking", icon: CheckCircle },
  BOOKING_CANCELLED: { category: "booking", icon: XCircle },
  PAYMENT_SUCCESSFUL: { category: "payment", icon: CreditCard },
  WALLET_TOP_UP_SUCCESSFUL: { category: "wallet", icon: Wallet },
  TICKET_GENERATED: { category: "ticket", icon: Ticket },
};

export default function NotificationsPage() {
  const { t, language } = useLanguage();
  const { unreadCount, refreshUnreadCount, setUnreadCount } = useNotifications();
  const [activeFilter, setActiveFilter] = useState("all");
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setNotifications(await getPassengerNotifications());
    } catch (requestError) {
      setError(requestError.message || t("passenger.notifications.loadError"));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    Promise.resolve().then(loadNotifications);
  }, [loadNotifications]);

  const filteredNotifications = useMemo(
    () => activeFilter === "all"
      ? notifications
      : notifications.filter((item) => typePresentation[item.type]?.category === activeFilter),
    [activeFilter, notifications]
  );

  const markAllAsRead = async () => {
    try {
      await markAllPassengerNotificationsRead();
      setNotifications((previous) => previous.map((item) => ({ ...item, read: true })));
      setUnreadCount(0);
    } catch (requestError) {
      setError(requestError.message || t("passenger.notifications.updateError"));
    }
  };

  const markAsRead = async (item) => {
    if (item.read) return;
    try {
      await markPassengerNotificationRead(item.id);
      setNotifications((previous) =>
        previous.map((notification) =>
          notification.id === item.id ? { ...notification, read: true } : notification
        )
      );
      await refreshUnreadCount();
    } catch (requestError) {
      setError(requestError.message || t("passenger.notifications.updateError"));
    }
  };

  return (
    <PassengerLayout activePage="Notifications" title={t("passenger.notifications.title")} subtitle={t("passenger.notifications.subtitle")}>
      <div className="mx-auto max-w-6xl">
        <Link to="/passenger/dashboard" className="mb-4 inline-flex items-center gap-2 text-sm font-bold text-slate-600 hover:text-[#08264a]">
          <ArrowLeft size={17} />
          {t("passenger.notifications.backToDashboard")}
        </Link>

        <div className="mb-5 flex justify-end">
          <button type="button" onClick={markAllAsRead} disabled={!unreadCount}
            className="tap-target rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-50">
            {t("passenger.notifications.markAllAsRead")}
          </button>
        </div>

        {error && <div role="alert" className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">{error}</div>}

        <div className="grid grid-cols-12 gap-5">
          <aside className="col-span-12 space-y-5 lg:col-span-4">
            <div className="rounded-2xl bg-[#08264a] p-5 text-white shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-xs font-black uppercase tracking-widest text-slate-300">{t("passenger.notifications.unreadAlerts")}</p>
                  <h2 className="mt-2 text-4xl font-black">{unreadCount}</h2>
                  <p className="mt-1 text-sm text-slate-300">{t("passenger.notifications.needsAttention")}</p>
                </div>
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/15"><Bell size={26} /></div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <h2 className="mb-3 text-lg font-black text-slate-900">{t("passenger.notifications.filterNotifications")}</h2>
              <div className="space-y-2">
                {["all", "booking", "payment", "wallet", "ticket"].map((filter) => (
                  <FilterButton key={filter} label={t(`passenger.notifications.filters.${filter}`)}
                    value={filter} active={activeFilter} onClick={setActiveFilter} />
                ))}
              </div>
            </div>

            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
              <div className="flex gap-2"><CheckCircle size={18} /><p>{t("passenger.notifications.refreshInfo")}</p></div>
            </div>
          </aside>

          <section className="col-span-12 lg:col-span-8">
            <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="flex items-center justify-between gap-3 border-b border-slate-200 p-4 sm:p-5">
                <div className="min-w-0">
                  <h2 className="text-xl font-black text-slate-900">{t("passenger.notifications.recentNotifications")}</h2>
                  <p className="text-sm text-slate-500">{t("passenger.notifications.showingCount", { count: filteredNotifications.length })}</p>
                </div>
                <Clock size={20} className="text-slate-400" />
              </div>

              <div className="divide-y divide-slate-100">
                {loading && <p className="p-8 text-center text-sm font-bold text-slate-500">{t("common.loading")}...</p>}
                {!loading && filteredNotifications.length === 0 && (
                  <p className="p-8 text-center text-sm font-bold text-slate-500">{t("passenger.notifications.empty")}</p>
                )}
                {!loading && filteredNotifications.map((item) => {
                  const Icon = typePresentation[item.type]?.icon || Bell;
                  return (
                    <button key={item.id} type="button" onClick={() => markAsRead(item)}
                      className={`flex w-full gap-3 p-4 text-left transition hover:bg-slate-50 sm:gap-4 sm:p-5 ${!item.read ? "bg-emerald-50/60" : "bg-white"}`}>
                      <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${!item.read ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
                        <Icon size={20} />
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                          <div className="min-w-0">
                            <div className="flex min-w-0 items-center gap-2">
                              <h3 className="safe-wrap font-black text-slate-900">{t(`passenger.notifications.types.${item.type}.title`, item.metadata || {})}</h3>
                              {!item.read && <span className="h-2 w-2 rounded-full bg-emerald-600" />}
                            </div>
                            <p className="safe-wrap mt-1 text-sm leading-6 text-slate-600">{t(`passenger.notifications.types.${item.type}.message`, item.metadata || {})}</p>
                          </div>
                          <span className="shrink-0 text-xs font-bold text-slate-400 sm:whitespace-nowrap">{formatRelativeTime(item.createdAt, language)}</span>
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          </section>
        </div>
      </div>
    </PassengerLayout>
  );
}

function formatRelativeTime(createdAt, language) {
  const timestamp = new Date(createdAt).getTime();
  if (!Number.isFinite(timestamp)) return "";
  const seconds = Math.round((timestamp - Date.now()) / 1000);
  const ranges = [["day", 86400], ["hour", 3600], ["minute", 60]];
  const [unit, divisor] = ranges.find(([, size]) => Math.abs(seconds) >= size) || ["second", 1];
  return new Intl.RelativeTimeFormat(language === "ne" ? "ne-NP" : "en", { numeric: "auto" })
    .format(Math.round(seconds / divisor), unit);
}

function FilterButton({ label, value, active, onClick }) {
  return (
    <button type="button" onClick={() => onClick(value)}
      className={`tap-target flex w-full items-center justify-between rounded-xl px-4 py-3 text-sm font-black transition ${active === value ? "bg-[#08264a] text-white" : "bg-slate-50 text-slate-600 hover:bg-slate-100 hover:text-[#08264a]"}`}>
      {label}
    </button>
  );
}
