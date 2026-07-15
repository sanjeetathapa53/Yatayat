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

const initialNotifications = [
  {
    id: 1,
    type: "transit",
    title: "Bus delayed",
    message: "BA 2 PA 9901 is delayed by 11 minutes near Balaju.",
    time: "2 mins ago",
    unread: true,
    icon: Bus,
  },
  {
    id: 2,
    type: "wallet",
    title: "Wallet recharge successful",
    message: "NPR 500 has been added to your Yatayat Wallet.",
    time: "18 mins ago",
    unread: true,
    icon: Wallet,
  },
  {
    id: 3,
    type: "fare",
    title: "QR Fare Pass generated",
    message: "Your Ratnapark → Bhaktapur QR pass is active for 24 hours or until scanned once.",
    time: "35 mins ago",
    unread: false,
    icon: Ticket,
  },
  {
    id: 4,
    type: "security",
    title: "New device login OTP",
    message: "A new login attempt was detected. OTP verification may be required.",
    time: "1 hour ago",
    unread: false,
    icon: ShieldAlert,
  },
  {
    id: 5,
    type: "security",
    title: "Email verification pending",
    message: "Please verify your email address to secure your account.",
    time: "Yesterday",
    unread: false,
    icon: MailCheck,
  },
  {
    id: 6,
    type: "payment",
    title: "Payment confirmation saved",
    message: "Your payment record has been stored successfully.",
    time: "Yesterday",
    unread: false,
    icon: CreditCard,
  },
];

export default function NotificationsPage() {
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
      <header className="h-14 border-b border-slate-200 bg-white">
        <nav className="mx-auto flex h-full max-w-7xl items-center justify-between px-6">
          <div className="flex items-center gap-8">
            <Link to="/" className="text-xl font-black">
              Yatayat
            </Link>

            <div className="flex gap-6 text-sm font-semibold">
              <Link to="/routes" className="text-slate-600 hover:text-[#08264a]">
                Routes
              </Link>
              <Link to="/track-bus" className="text-slate-600 hover:text-[#08264a]">
                Track Bus
              </Link>
              <Link to="/fare-pass" className="text-slate-600 hover:text-[#08264a]">
                Fare Pass
              </Link>
              <Link to="/wallet" className="text-slate-600 hover:text-[#08264a]">
                Wallet
              </Link>
            </div>
          </div>

          <div className="rounded-full bg-[#08264a] px-4 py-2 text-xs font-black text-white">
            {unreadCount} unread
          </div>
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-5">
        <Link
          to="/passenger/dashboard"
          className="mb-4 inline-flex items-center gap-2 text-sm font-bold text-slate-600 hover:text-[#08264a]"
        >
          <ArrowLeft size={17} />
          Back to Dashboard
        </Link>

        <div className="mb-5 flex items-end justify-between">
          <div>
            <h1 className="text-3xl font-black text-slate-900">
              Notification Center
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              View trip alerts, QR pass updates, wallet activity, and security notifications.
            </p>
          </div>

          <button
            onClick={markAllAsRead}
            className="rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
          >
            Mark all as read
          </button>
        </div>

        <div className="grid grid-cols-12 gap-5">
          <aside className="col-span-4 space-y-5">
            <div className="rounded-2xl bg-[#08264a] p-5 text-white shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                    Unread Alerts
                  </p>
                  <h2 className="mt-2 text-4xl font-black">{unreadCount}</h2>
                  <p className="mt-1 text-sm text-slate-300">
                    Notifications needing attention
                  </p>
                </div>

                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/15">
                  <Bell size={26} />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <h2 className="mb-3 text-lg font-black text-slate-900">
                Filter Notifications
              </h2>

              <div className="space-y-2">
                <FilterButton label="All" value="all" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label="Transit" value="transit" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label="Wallet" value="wallet" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label="Fare Pass" value="fare" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label="Security" value="security" active={activeFilter} onClick={setActiveFilter} />
                <FilterButton label="Payment" value="payment" active={activeFilter} onClick={setActiveFilter} />
              </div>
            </div>

            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
              <div className="flex gap-2">
                <CheckCircle size={18} />
                <p>
                  Later this page will receive real-time alerts from Spring Boot for OTP,
                  payments, QR usage, and bus delays.
                </p>
              </div>
            </div>
          </aside>

          <section className="col-span-8">
            <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="flex items-center justify-between border-b border-slate-200 p-5">
                <div>
                  <h2 className="text-xl font-black text-slate-900">
                    Recent Notifications
                  </h2>
                  <p className="text-sm text-slate-500">
                    Showing {filteredNotifications.length} notification(s)
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
                      onClick={() => markAsRead(item.id)}
                      className={`flex w-full gap-4 p-5 text-left transition hover:bg-slate-50 ${
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

                      <div className="flex-1">
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <div className="flex items-center gap-2">
                              <h3 className="font-black text-slate-900">
                                {item.title}
                              </h3>

                              {item.unread && (
                                <span className="h-2 w-2 rounded-full bg-emerald-600"></span>
                              )}
                            </div>

                            <p className="mt-1 text-sm leading-6 text-slate-600">
                              {item.message}
                            </p>
                          </div>

                          <span className="whitespace-nowrap text-xs font-bold text-slate-400">
                            {item.time}
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
      onClick={() => onClick(value)}
      className={`flex w-full items-center justify-between rounded-xl px-4 py-3 text-sm font-black transition ${
        active === value
          ? "bg-[#08264a] text-white"
          : "bg-slate-50 text-slate-600 hover:bg-slate-100 hover:text-[#08264a]"
      }`}
    >
      {label}
    </button>
  );
}