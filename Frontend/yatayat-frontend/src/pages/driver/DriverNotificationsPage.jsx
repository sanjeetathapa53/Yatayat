import { useState } from "react";
import {
  Search,
  SlidersHorizontal,
  ShieldAlert,
  Route,
  UserRound,
  Mail,
  Wrench,
  MoreVertical,
  ChevronDown,
  CheckCircle,
} from "lucide-react";
import DriverLayout from "../../components/layout/DriverLayout";

const notifications = [
  {
    id: 1,
    group: "New Alerts",
    type: "security",
    title: "New device login OTP",
    message:
      "A new login attempt was detected from a Windows device in Kathmandu. Use OTP 449-102 to verify your identity.",
    time: "Just now",
    unread: true,
    urgent: true,
    actions: ["Verify", "Dismiss"],
  },
  {
    id: 2,
    group: "New Alerts",
    type: "route",
    title: "Bus delay alert",
    message:
      "Route #422 (Ratna Park - Koteshwor) is experiencing a 15-minute delay due to road construction at New Baneshwor. Adjust speed accordingly.",
    time: "12m ago",
    unread: true,
    actions: ["View Route Map"],
  },
  {
    id: 3,
    group: "Earlier Today",
    type: "passenger",
    title: "Priority Seating Request",
    message:
      "Passenger at Terminal B requires ramp assistance for wheelchair boarding. Arrival estimated in 10 minutes.",
    time: "3h ago",
    unread: false,
  },
  {
    id: 4,
    group: "Earlier Today",
    type: "admin",
    title: "Shift Schedule Finalized",
    message:
      "The duty roster for the upcoming Dashain festival has been updated. Please review your assigned shifts in the portal.",
    time: "5h ago",
    unread: false,
  },
  {
    id: 5,
    group: "Earlier Today",
    type: "maintenance",
    title: "Vehicle Health Report",
    message:
      "Bus BA 1 PA 4502: Tire pressure in rear-left is 5 PSI below optimal. Maintenance suggested at next depot stop.",
    time: "Yesterday",
    unread: false,
  },
];

export default function DriverNotificationsPage() {
  const [activeFilter, setActiveFilter] = useState("All");
  const [searchTerm, setSearchTerm] = useState("");
  const [items, setItems] = useState(notifications);

  const filtered = items.filter((item) => {
    const matchesFilter =
      activeFilter === "All" ||
      (activeFilter === "Unread" && item.unread) ||
      (activeFilter === "Archived" && item.archived);

    const matchesSearch =
      item.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.message.toLowerCase().includes(searchTerm.toLowerCase());

    return matchesFilter && matchesSearch;
  });

  const markAllRead = () => {
    setItems((prev) => prev.map((item) => ({ ...item, unread: false })));
  };

  const grouped = filtered.reduce((acc, item) => {
    if (!acc[item.group]) acc[item.group] = [];
    acc[item.group].push(item);
    return acc;
  }, {});

  return (
    <DriverLayout activePage="Notifications">
      <header className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Notifications
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Manage route updates, passenger alerts, admin messages, and security notices.
          </p>
        </div>

        <div className="rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white shadow-sm">
          Arriving at Terminal
        </div>
      </header>

      <section className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap gap-3">
          {["All", "Unread", "Archived"].map((item) => (
            <button
              key={item}
              onClick={() => setActiveFilter(item)}
              className={`rounded-full px-5 py-3 text-sm font-black transition ${
                activeFilter === item
                  ? "bg-[#08264a] text-white"
                  : "bg-white text-slate-500 hover:text-[#08264a]"
              }`}
            >
              {item}
            </button>
          ))}
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 sm:w-80">
            <Search size={18} className="text-slate-500" />
            <input
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search notifications..."
              className="w-full bg-transparent text-sm outline-none"
            />
          </div>

          <button className="flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-3 text-sm font-bold text-slate-600 hover:bg-slate-50">
            <SlidersHorizontal size={17} />
            Filter
          </button>

          <button
            onClick={markAllRead}
            className="rounded-xl px-4 py-3 text-sm font-black text-[#1d3f6e] hover:bg-white"
          >
            Mark all as read
          </button>
        </div>
      </section>

      <section className="space-y-10">
        {Object.entries(grouped).map(([group, groupItems]) => (
          <div key={group}>
            <div className="mb-4 flex items-center gap-4">
              <p className="text-xs font-black uppercase tracking-[0.25em] text-slate-500">
                {group}
              </p>
              <div className="h-px flex-1 bg-slate-200"></div>
            </div>

            <div className="space-y-4">
              {groupItems.map((item) => (
                <NotificationCard key={item.id} item={item} />
              ))}
            </div>
          </div>
        ))}
      </section>

      <div className="mt-10 flex justify-center">
        <button className="flex items-center gap-2 rounded-xl px-5 py-3 text-sm font-black text-slate-500 hover:bg-white hover:text-[#08264a]">
          Load more notifications
          <ChevronDown size={18} />
        </button>
      </div>
    </DriverLayout>
  );
}

function NotificationCard({ item }) {
  const config = getNotificationConfig(item.type);

  return (
    <div
      className={`relative rounded-2xl border border-slate-100 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-md ${
        item.urgent ? "border-l-4 border-l-red-600" : item.unread ? "border-l-4 border-l-[#1d3f6e]" : ""
      }`}
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex gap-4">
          <div
            className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${config.bg} ${config.text}`}
          >
            {config.icon}
          </div>

          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="font-black text-slate-900">{item.title}</h2>
              {item.unread && (
                <span className="h-2 w-2 rounded-full bg-[#1d3f6e]"></span>
              )}
            </div>

            <p className="mt-2 max-w-4xl text-sm leading-6 text-slate-500">
              {item.message}
            </p>

            {item.actions && (
              <div className="mt-4 flex flex-wrap gap-3">
                {item.actions.map((action) => (
                  <button
                    key={action}
                    className={`rounded-lg px-4 py-2 text-xs font-black ${
                      action === "Dismiss"
                        ? "text-slate-500 hover:bg-slate-100"
                        : "bg-blue-50 text-[#1d3f6e] hover:bg-blue-100"
                    }`}
                  >
                    {action}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-3 sm:justify-end">
          <p className="text-xs font-black uppercase text-slate-400">
            {item.time}
          </p>

          {!item.unread ? (
            <button className="rounded-full p-2 text-slate-400 hover:bg-slate-100">
              <MoreVertical size={18} />
            </button>
          ) : (
            <CheckCircle size={16} className="text-[#1d3f6e]" />
          )}
        </div>
      </div>
    </div>
  );
}

function getNotificationConfig(type) {
  const configs = {
    security: {
      icon: <ShieldAlert size={22} />,
      bg: "bg-red-50",
      text: "text-red-600",
    },
    route: {
      icon: <Route size={22} />,
      bg: "bg-blue-50",
      text: "text-[#1d3f6e]",
    },
    passenger: {
      icon: <UserRound size={22} />,
      bg: "bg-slate-100",
      text: "text-slate-500",
    },
    admin: {
      icon: <Mail size={22} />,
      bg: "bg-slate-100",
      text: "text-slate-500",
    },
    maintenance: {
      icon: <Wrench size={22} />,
      bg: "bg-slate-100",
      text: "text-slate-500",
    },
  };

  return configs[type];
}