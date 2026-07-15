import {
    CheckCircle,
    Route,
    Clock,
    MapPin,
    Users,
    Ticket,
    Wallet,
    Gauge,
    Star,
    Download,
    Home,
    RotateCcw,
    Fuel,
    AlertTriangle,
  } from "lucide-react";
  import { useNavigate } from "react-router-dom";
  import DriverLayout from "../../components/layout/DriverLayout";
  
  export default function TripSummaryPage() {
    const navigate = useNavigate();
  
    return (
      <DriverLayout activePage="Trip Management">
        <section className="mb-6 rounded-3xl bg-[#08264a] p-6 text-white shadow-sm sm:p-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-center gap-5">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-emerald-500">
                <CheckCircle size={38} />
              </div>
  
              <div>
                <p className="text-xs font-black uppercase tracking-widest text-emerald-200">
                  Trip Completed Successfully
                </p>
                <h1 className="mt-2 text-3xl font-black">
                  Kathmandu → Pokhara Express
                </h1>
                <p className="mt-1 text-sm text-slate-300">
                  Bus BA-1-KHA-1234 • Completed at 01:45 PM
                </p>
              </div>
            </div>
  
            <div className="rounded-2xl bg-white/10 p-4 text-left lg:text-right">
              <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                Trip ID
              </p>
              <h2 className="mt-1 text-2xl font-black">TRP-2026-0148</h2>
            </div>
          </div>
        </section>
  
        <section className="mb-6 grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
          <SummaryCard icon={<Users size={25} />} label="Passengers Served" value="30 / 32" />
          <SummaryCard icon={<Wallet size={25} />} label="Revenue Generated" value="NPR 55,500" green />
          <SummaryCard icon={<MapPin size={25} />} label="Distance Covered" value="204 km" />
          <SummaryCard icon={<Clock size={25} />} label="Trip Duration" value="6h 42m" />
        </section>
  
        <section className="grid grid-cols-1 gap-6 xl:grid-cols-12">
          <div className="space-y-6 xl:col-span-8">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="mb-5 flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#08264a] text-white">
                  <Route size={22} />
                </div>
                <div>
                  <h2 className="text-xl font-black text-slate-900">
                    Route Completion
                  </h2>
                  <p className="text-sm text-slate-500">
                    Stop-by-stop completion summary
                  </p>
                </div>
              </div>
  
              <div className="space-y-5">
                <StopItem done title="Kathmandu - Gongabu" time="Departed 07:12 AM" />
                <StopItem done title="Naubise" time="Passed 08:35 AM" />
                <StopItem done title="Mugling Rest Stop" time="Arrived 10:18 AM" />
                <StopItem done title="Dumre" time="Passed 11:34 AM" />
                <StopItem done title="Pokhara Terminal" time="Arrived 01:45 PM" />
              </div>
            </div>
  
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-xl font-black text-slate-900">
                Performance Metrics
              </h2>
  
              <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <MetricCard icon={<Gauge size={22} />} label="Average Speed" value="48 km/h" />
                <MetricCard icon={<Fuel size={22} />} label="Fuel Efficiency" value="4.2 km/L" />
                <MetricCard icon={<Star size={22} />} label="Driver Rating" value="4.9 / 5" green />
                <MetricCard icon={<Ticket size={22} />} label="Tickets Verified" value="30" />
                <MetricCard icon={<AlertTriangle size={22} />} label="Issues Reported" value="1 Minor" warning />
                <MetricCard icon={<Clock size={22} />} label="Delay Time" value="12 mins" warning />
              </div>
            </div>
          </div>
  
          <aside className="space-y-6 xl:col-span-4">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-xl font-black text-slate-900">
                Fare Collection
              </h2>
  
              <div className="mt-5 space-y-3 text-sm">
                <MoneyRow label="Out-of-Valley Tickets" value="NPR 52,000" />
                <MoneyRow label="Local QR Fare Pass" value="NPR 2,250" />
                <MoneyRow label="Service Fee" value="NPR 1,250" />
                <MoneyRow label="Refunds" value="NPR 0" />
              </div>
  
              <div className="mt-5 rounded-xl bg-emerald-50 p-4">
                <div className="flex justify-between">
                  <span className="font-black text-slate-900">Total Revenue</span>
                  <span className="text-xl font-black text-emerald-700">
                    NPR 55,500
                  </span>
                </div>
              </div>
            </div>
  
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-xl font-black text-slate-900">
                Boarding Summary
              </h2>
  
              <div className="mt-5 space-y-4">
                <ProgressRow label="Boarded" value="30" percent="94%" />
                <ProgressRow label="No Show" value="2" percent="6%" red />
                <ProgressRow label="Cancelled" value="0" percent="0%" />
              </div>
            </div>
  
            <div className="rounded-2xl bg-[#08264a] p-5 text-white shadow-sm">
              <h2 className="text-xl font-black">Next Action</h2>
              <p className="mt-2 text-sm leading-6 text-slate-300">
                Trip report is ready. You may download the report or return to the dashboard.
              </p>
  
              <div className="mt-5 space-y-3">
                <button className="flex w-full items-center justify-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-black text-[#08264a] hover:bg-slate-100">
                  <Download size={18} />
                  Download Report
                </button>
  
                <button
                  onClick={() => navigate("/driver/dashboard")}
                  className="flex w-full items-center justify-center gap-2 rounded-xl bg-white/10 px-5 py-3 text-sm font-black text-white hover:bg-white/20"
                >
                  <Home size={18} />
                  Return Dashboard
                </button>
  
                <button
                  onClick={() => navigate("/driver/trip")}
                  className="flex w-full items-center justify-center gap-2 rounded-xl border border-white/20 px-5 py-3 text-sm font-black text-white hover:bg-white/10"
                >
                  <RotateCcw size={18} />
                  Start New Trip
                </button>
              </div>
            </div>
          </aside>
        </section>
      </DriverLayout>
    );
  }
  
  function SummaryCard({ icon, label, value, green }) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-widest text-slate-500">
              {label}
            </p>
            <h2
              className={`mt-2 text-2xl font-black ${
                green ? "text-emerald-700" : "text-[#08264a]"
              }`}
            >
              {value}
            </h2>
          </div>
  
          <div
            className={`flex h-12 w-12 items-center justify-center rounded-xl ${
              green ? "bg-emerald-100 text-emerald-700" : "bg-blue-50 text-[#08264a]"
            }`}
          >
            {icon}
          </div>
        </div>
      </div>
    );
  }
  
  function StopItem({ title, time }) {
    return (
      <div className="flex gap-4">
        <div className="flex flex-col items-center">
          <CheckCircle className="text-emerald-600" size={23} />
          <span className="mt-1 h-10 w-px bg-slate-200"></span>
        </div>
  
        <div>
          <h3 className="font-black text-slate-900">{title}</h3>
          <p className="text-sm text-slate-500">{time}</p>
        </div>
      </div>
    );
  }
  
  function MetricCard({ icon, label, value, green, warning }) {
    return (
      <div className="rounded-xl bg-slate-50 p-4">
        <div
          className={`mb-3 ${
            green ? "text-emerald-700" : warning ? "text-orange-600" : "text-[#08264a]"
          }`}
        >
          {icon}
        </div>
        <p className="text-xs font-black uppercase tracking-widest text-slate-500">
          {label}
        </p>
        <h3 className="mt-1 text-xl font-black text-slate-900">{value}</h3>
      </div>
    );
  }
  
  function MoneyRow({ label, value }) {
    return (
      <div className="flex justify-between border-b border-slate-100 pb-2">
        <span className="text-slate-500">{label}</span>
        <span className="font-black text-slate-900">{value}</span>
      </div>
    );
  }
  
  function ProgressRow({ label, value, percent, red }) {
    return (
      <div>
        <div className="mb-2 flex justify-between text-sm">
          <span className="font-bold text-slate-600">{label}</span>
          <span className="font-black text-slate-900">
            {value} • {percent}
          </span>
        </div>
  
        <div className="h-2 overflow-hidden rounded-full bg-slate-200">
          <div
            className={`h-full rounded-full ${red ? "bg-red-500" : "bg-emerald-600"}`}
            style={{ width: percent }}
          ></div>
        </div>
      </div>
    );
  }