import { useState } from "react";
import {
  Ticket,
  QrCode,
  ArrowDownLeft,
  ArrowUpRight,
  Calendar,
  Download,
} from "lucide-react";
import PassengerLayout from "../../components/layout/PassengerLayout";

const historyItems = [
  {
    id: 1,
    type: "local",
    title: "Local QR Fare Pass",
    detail: "Ratnapark → Bhaktapur",
    date: "Today • 08:15 AM",
    amount: "NPR 45",
    status: "Used",
  },
  {
    id: 2,
    type: "booking",
    title: "Out-of-Valley Ticket",
    detail: "Kathmandu → Pokhara • Seat B2",
    date: "Jul 25, 2026 • 07:30 AM",
    amount: "NPR 2,141",
    status: "Confirmed",
  },
  {
    id: 3,
    type: "wallet",
    title: "Wallet Recharge",
    detail: "Added using eSewa mock payment",
    date: "Yesterday • 09:30 AM",
    amount: "+ NPR 500",
    status: "Completed",
  },
  {
    id: 4,
    type: "local",
    title: "Local QR Fare Pass",
    detail: "Baneshwor → Chabahil",
    date: "Yesterday • 04:40 PM",
    amount: "NPR 25",
    status: "Used",
  },
  {
    id: 5,
    type: "cancelled",
    title: "Cancelled Booking",
    detail: "Pokhara → Kathmandu",
    date: "Apr 10, 2026",
    amount: "Refund Pending",
    status: "Cancelled",
  },
];

export default function HistoryPage() {
  const [activeFilter, setActiveFilter] = useState("all");

  const filtered =
    activeFilter === "all"
      ? historyItems
      : historyItems.filter((item) => item.type === activeFilter);

  return (
    <PassengerLayout activePage="History">
      <header className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Travel History
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            View your local fare passes, bookings, wallet activity, and cancellations.
          </p>
        </div>

        <button className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white hover:bg-[#0d3566] sm:w-auto">
          <Download size={18} />
          Export History
        </button>
      </header>

      <section className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard title="Total Trips" value="42" />
        <StatCard title="Local Fare Passes" value="26" />
        <StatCard title="Bookings" value="09" />
        <StatCard title="Wallet Activity" value="07" />
      </section>

      <section className="mb-6 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap gap-3">
          <FilterButton label="All" value="all" active={activeFilter} setActive={setActiveFilter} />
          <FilterButton label="Local Fare" value="local" active={activeFilter} setActive={setActiveFilter} />
          <FilterButton label="Bookings" value="booking" active={activeFilter} setActive={setActiveFilter} />
          <FilterButton label="Wallet" value="wallet" active={activeFilter} setActive={setActiveFilter} />
          <FilterButton label="Cancelled" value="cancelled" active={activeFilter} setActive={setActiveFilter} />
        </div>
      </section>

      <section className="space-y-4">
        {filtered.map((item) => (
          <HistoryCard key={item.id} item={item} />
        ))}
      </section>
    </PassengerLayout>
  );
}

function StatCard({ title, value }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-xs font-black uppercase tracking-widest text-slate-500">
        {title}
      </p>
      <h2 className="mt-2 text-3xl font-black text-[#08264a]">{value}</h2>
    </div>
  );
}

function FilterButton({ label, value, active, setActive }) {
  return (
    <button
      onClick={() => setActive(value)}
      className={`rounded-xl px-5 py-3 text-sm font-black transition ${
        active === value
          ? "bg-[#08264a] text-white"
          : "bg-slate-100 text-slate-600 hover:bg-slate-200"
      }`}
    >
      {label}
    </button>
  );
}

function HistoryCard({ item }) {
  const config = {
    local: {
      icon: <QrCode size={22} />,
      bg: "bg-emerald-100",
      text: "text-emerald-700",
    },
    booking: {
      icon: <Ticket size={22} />,
      bg: "bg-blue-100",
      text: "text-blue-700",
    },
    wallet: {
      icon: <ArrowDownLeft size={22} />,
      bg: "bg-purple-100",
      text: "text-purple-700",
    },
    cancelled: {
      icon: <ArrowUpRight size={22} />,
      bg: "bg-red-100",
      text: "text-red-700",
    },
  };

  const current = config[item.type];

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-md">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <div
            className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${current.bg} ${current.text}`}
          >
            {current.icon}
          </div>

          <div>
            <h3 className="font-black text-slate-900">{item.title}</h3>
            <p className="mt-1 text-sm text-slate-500">{item.detail}</p>
            <p className="mt-1 flex items-center gap-2 text-xs text-slate-400">
              <Calendar size={13} />
              {item.date}
            </p>
          </div>
        </div>

        <div className="text-left sm:text-right">
          <p
            className={`text-lg font-black ${
              item.amount.includes("+") ? "text-emerald-700" : "text-[#08264a]"
            }`}
          >
            {item.amount}
          </p>

          <span
            className={`mt-2 inline-block rounded-full px-3 py-1 text-xs font-black ${
              item.status === "Cancelled"
                ? "bg-red-100 text-red-700"
                : item.status === "Used"
                ? "bg-emerald-100 text-emerald-700"
                : "bg-blue-100 text-blue-700"
            }`}
          >
            {item.status}
          </span>
        </div>
      </div>
    </div>
  );
}