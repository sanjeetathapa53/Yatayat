import { useState } from "react";
import {
  ArrowLeft,
  Bell,
  Bus,
  CheckCircle,
  Clock,
  CreditCard,
  MailCheck,
  ShieldAlert,
  Ticket,
  Wallet,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useLanguage } from "../../context/LanguageContext";

const initialNotifications = [
  {
    id: 1,
    type: "transit",
    titleKey: "busDelayedTitle",
    messageKey: "busDelayedMessage",
    timeKey: "twoMinutesAgo",
    unread: true,
    icon: Bus,
  },
  {
    id: 2,
    type: "wallet",
    titleKey: "walletRechargeTitle",
    messageKey: "walletRechargeMessage",
    timeKey: "eighteenMinutesAgo",
    unread: true,
    icon: Wallet,
  },
  {
    id: 3,
    type: "fare",
    titleKey: "farePassTitle",
    messageKey: "farePassMessage",
    timeKey: "thirtyFiveMinutesAgo",
    unread: false,
    icon: Ticket,
  },
  {
    id: 4,
    type: "security",
    titleKey: "newDeviceTitle",
    messageKey: "newDeviceMessage",
    timeKey: "oneHourAgo",
    unread: false,
    icon: ShieldAlert,
  },
  {
    id: 5,
    type: "security",
    titleKey: "emailPendingTitle",
    messageKey: "emailPendingMessage",
    timeKey: "yesterday",
    unread: false,
    icon: MailCheck,
  },
  {
    id: 6,
    type: "payment",
    titleKey: "paymentSavedTitle",
    messageKey: "paymentSavedMessage",
    timeKey: "yesterday",
    unread: false,
    icon: CreditCard,
  },
];

export default function NotificationsPage() {
  const { t } = useLanguage();
  const [activeFilter, setActiveFilter] = useState("all");
  const [notifications, setNotifications] = useState(initialNotifications);

  const unreadCount = notifications.filter((item) => item.unread).length;

  const filteredNotifications =
    activeFilter === "all"
      ? notifications
      : notifications.filter((item) => item.type === activeFilter);

  const markAllAsRead = () => {
    setNotifications((prev) =>
      prev.map((item) => ({
        ...item,
        unread: false,
      }))
    );
  };

  const markAsRead = (id) => {
    setNotifications((prev) =>
      prev.map((item) =>
        item.id === id
          ? {
              ...item,
              unread: false,
            }
          : item
      )
    );
  };

  return (
    <div className="min-h-screen bg-[#eef3f8] text-[#08264a]">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex min-h-14 max-w-7xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6">
          <div className="flex min-w-0 flex-wrap items-center gap-3 sm:gap-8">
            <Link to="/" className="text-xl font-black">
              Yatayat
            </Link>

            <div className="flex flex-wrap gap-x-4 gap-y-2 text-sm font-semibold sm:gap-6">
              <Link to="/routes" className="text-slate-600 hover:text-[#08264a]">
                {t("passenger.notifications.routes")}
              </Link>
              <Link to="/track-bus" className="text-slate-600 hover:text-[#08264a]">
                {t("passenger.notifications.trackBus")}
              </Link>
              <Link to="/fare-pass" className="text-slate-600 hover:text-[#08264a]">
                {t("passenger.notifications.farePass")}
              </Link>
              <Link to="/wallet" className="text-slate-600 hover:text-[#08264a]">
                {t("passenger.notifications.wallet")}
              </Link>
            </div>
          </div>

          <div className="shrink-0 rounded-full bg-[#08264a] px-4 py-2 text-xs font-black text-white">
            {t("passenger.notifications.unreadCount", { count: unreadCount })}
          </div>
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-5 sm:px-6">
        <Link
          to="/passenger/dashboard"
          className="mb-4 inline-flex items-center gap-2 text-sm font-bold text-slate-600 hover:text-[#08264a]"
        >
          <ArrowLeft size={17} />
          {t("passenger.notifications.backToDashboard")}
        </Link>

        <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="min-w-0">
            <h1 className="safe-wrap text-2xl font-black text-slate-900 sm:text-3xl">
              {t("passenger.notifications.title")}
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              {t("passenger.notifications.subtitle")}
            </p>
          </div>

          <button
            type="button"
            onClick={markAllAsRead}
            className="tap-target rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
          >
            {t("passenger.notifications.markAllAsRead")}
          </button>
        </div>

        <div className="grid grid-cols-12 gap-5">
          <aside className="col-span-12 space-y-5 lg:col-span-4">
            <div className="rounded-2xl bg-[#08264a] p-5 text-white shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                    {t("passenger.notifications.unreadAlerts")}
                  </p>
                  <h2 className="mt-2 text-4xl font-black">{unreadCount}</h2>
                  <p className="mt-1 text-sm text-slate-300">
                    {t("passenger.notifications.needsAttention")}
                  </p>
                </div>

                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/15">
                  <Bell size={26} />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <h2 className="mb-3 text-lg font-black text-slate-900">
                {t("passenger.notifications.filterNotifications")}
              </h2>

              <div className="space-y-2">
                <FilterButton label={t("passenger.notifications.all")} value="all" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label={t("passenger.notifications.transit")} value="transit" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label={t("passenger.notifications.wallet")} value="wallet" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label={t("passenger.notifications.farePass")} value="fare" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label={t("passenger.notifications.security")} value="security" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label={t("passenger.notifications.payment")} value="payment" active={activeFilter} onClick={setActiveFilter} />
              </div>
            </div>

            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
              <div className="flex gap-2">
                <CheckCircle size={18} />
                <p>{t("passenger.notifications.futureInfo")}</p>
              </div>
            </div>
          </aside>

          <section className="col-span-12 lg:col-span-8">
            <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="flex items-center justify-between gap-3 border-b border-slate-200 p-4 sm:p-5">
                <div className="min-w-0">
                  <h2 className="text-xl font-black text-slate-900">
                    {t("passenger.notifications.recentNotifications")}
                  </h2>
                  <p className="text-sm text-slate-500">
                    {t("passenger.notifications.showingCount", { count: filteredNotifications.length })}
                  </p>
                </div>

                <Clock size={20} className="text-slate-400" />
              </div>

              <div className="divide-y divide-slate-100">
                {filteredNotifications.map((item) => {
                  const Icon = item.icon;

                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => markAsRead(item.id)}
                      className={`flex w-full gap-3 p-4 text-left transition hover:bg-slate-50 sm:gap-4 sm:p-5 ${
                        item.unread ? "bg-emerald-50/60" : "bg-white"
                      }`}
                    >
                      <div
                        className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${
                          item.unread
                            ? "bg-emerald-100 text-emerald-700"
                            : "bg-slate-100 text-slate-500"
                        }`}
                      >
                        <Icon size={20} />
                      </div>

                      <div className="min-w-0 flex-1">
                        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                          <div className="min-w-0">
                            <div className="flex min-w-0 items-center gap-2">
                              <h3 className="safe-wrap font-black text-slate-900">
                                {t(`passenger.notifications.${item.titleKey}`)}
                              </h3>

                              {item.unread && (
                                <span className="h-2 w-2 rounded-full bg-emerald-600"></span>
                              )}
                            </div>

                            <p className="safe-wrap mt-1 text-sm leading-6 text-slate-600">
                              {t(`passenger.notifications.${item.messageKey}`)}
                            </p>
                          </div>

                          <span className="shrink-0 text-xs font-bold text-slate-400 sm:whitespace-nowrap">
                            {t(`passenger.notifications.${item.timeKey}`)}
                          </span>
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}

function FilterButton({ label, value, active, onClick }) {
  return (
    <button
      type="button"
      onClick={() => onClick(value)}
      className={`tap-target flex w-full items-center justify-between rounded-xl px-4 py-3 text-sm font-black transition ${
        active === value
          ? "bg-[#08264a] text-white"
          : "bg-slate-50 text-slate-600 hover:bg-slate-100 hover:text-[#08264a]"
      }`}
    >
      {label}
    </button>
  );
}
