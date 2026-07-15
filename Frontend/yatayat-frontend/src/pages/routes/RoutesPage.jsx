import { useState } from "react";
import {
  Search,
  Bus,
  Map,
  Wifi,
  Snowflake,
  Zap,
  MapPin,
  SlidersHorizontal,
  X,
  Filter,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";

const routes = [
  {
    id: 1,
    company: "Sajha Yatayat",
    route: "Route #04 • Express",
    fare: 45,
    status: "On Time",
    travelTime: "55 mins",
    nextBus: "8 mins",
    occupancy: "Medium",
    location: "Tundikhel Station",
    type: "Express",
    featured: false,
  },
  {
    id: 2,
    company: "Local Yatayat Co.",
    route: "Route #12 • Local",
    fare: 30,
    status: "Delayed 5m",
    travelTime: "1h 15m",
    nextBus: "15 mins",
    occupancy: "High",
    location: "Bagbazar",
    type: "Local",
    featured: false,
  },
  {
    id: 3,
    company: "Electric Nepal Express",
    route: "Route #E1 • Super Express",
    fare: 50,
    status: "Eco Choice",
    travelTime: "45 mins",
    nextBus: "2 mins",
    occupancy: "Low",
    location: "Ratnapark Terminal",
    type: "Electric",
    featured: true,
  },
];

export default function RoutesPage() {
  const [showFilters, setShowFilters] = useState(false);
  const navigate = useNavigate();

  return (
    <PassengerLayout activePage="Routes">
      <header className="mb-6">
        <p className="text-xs font-semibold text-slate-500">
          Home › Search ›{" "}
          <span className="text-[#08264a]">Ratnapark to Bhaktapur</span>
        </p>

        <div className="mt-3 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
              Available Routes
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              12 local buses found for Ratnapark → Bhaktapur
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-3 py-3 shadow-sm sm:w-72">
              <Search size={17} />
              <input
                placeholder="Search routes..."
                className="w-full bg-transparent text-sm outline-none"
              />
            </div>

            <button
              onClick={() => setShowFilters(true)}
              className="flex items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-black text-[#08264a] shadow-sm hover:bg-slate-50 lg:hidden"
            >
              <Filter size={17} />
              Filters
            </button>

            <button className="flex items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-black hover:bg-slate-50">
              <SlidersHorizontal size={16} />
              Sort: Earliest
            </button>
          </div>
        </div>
      </header>

      {showFilters && (
        <div
          onClick={() => setShowFilters(false)}
          className="fixed inset-0 z-40 bg-black/40 lg:hidden"
        />
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <aside
          className={`fixed left-0 top-0 z-50 h-screen w-72 overflow-y-auto border-r border-slate-200 bg-white p-5 transition-transform duration-300 lg:static lg:z-auto lg:h-fit lg:w-auto lg:translate-x-0 lg:rounded-2xl lg:border lg:shadow-sm lg:col-span-3 ${
            showFilters ? "translate-x-0" : "-translate-x-full"
          }`}
        >
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-black">Filters</h2>

            <button
              onClick={() => setShowFilters(false)}
              className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 lg:hidden"
            >
              <X size={20} />
            </button>
          </div>

          <button className="mt-2 text-xs font-semibold text-slate-500 hover:text-[#08264a]">
            Clear all
          </button>

          <FilterGroup title="Bus Type">
            <Check label="Express" checked />
            <Check label="Local" />
            <Check label="Electric / Sajha" />
          </FilterGroup>

          <FilterGroup title="Fare Range">
            <div className="mt-3 h-2 rounded-full bg-slate-200">
              <div className="h-2 w-1/2 rounded-full bg-[#08264a]"></div>
            </div>

            <div className="mt-2 flex justify-between text-xs text-slate-500">
              <span>Rs. 20</span>
              <span>Rs. 500</span>
            </div>
          </FilterGroup>

          <FilterGroup title="Departure Time">
            <div className="grid grid-cols-2 gap-2">
              {["Morning", "Afternoon", "Evening", "Night"].map((item) => (
                <button
                  key={item}
                  className="rounded-lg border border-slate-300 py-2 text-sm hover:border-[#08264a] hover:bg-slate-50"
                >
                  {item}
                </button>
              ))}
            </div>
          </FilterGroup>

          <div className="mt-7 rounded-xl bg-[#1d3f6e] p-4 text-white">
            <p className="text-xs text-slate-300">Transit Pass</p>
            <h3 className="mt-2 font-black leading-6">
              Generate a QR fare pass after choosing a local route.
            </h3>
            <button
              onClick={() => navigate("/fare-pass")}
              className="mt-3 text-sm font-bold text-emerald-300 hover:underline"
            >
              Generate Pass →
            </button>
          </div>
        </aside>

        <main className="lg:col-span-9">
          <div className="space-y-5">
            {routes.map((item) => (
              <RouteCard
                key={item.id}
                item={item}
                onMap={() => navigate(`/track-bus/${item.id}`)}
                onFare={() => navigate("/fare-pass")}
              />
            ))}
          </div>

          <div className="mt-8 text-center">
            <button className="rounded-full border border-slate-400 bg-white px-8 py-3 text-sm font-black hover:bg-slate-50">
              Load More Results
            </button>
            <p className="mt-3 text-xs text-slate-500">
              Showing 3 of 12 routes
            </p>
          </div>
        </main>
      </div>

      <footer className="mt-10 rounded-2xl bg-[#04294f] px-6 py-8 text-white">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-4">
          <div>
            <h2 className="text-xl font-black">Yatayat</h2>
            <p className="mt-4 text-sm leading-6 text-slate-300">
              Connecting Nepal through reliable and transparent public transit.
            </p>
          </div>

          <FooterColumn
            title="Product"
            items={["Route Directory", "Live Tracking", "Local Fare Pass"]}
          />
          <FooterColumn title="Company" items={["About Us", "Contact Us", "Partners"]} />
          <FooterColumn title="Support" items={["Help Desk", "Terms", "Privacy"]} />
        </div>
      </footer>
    </PassengerLayout>
  );
}

function FilterGroup({ title, children }) {
  return (
    <div className="mt-7">
      <h3 className="text-xs font-black uppercase tracking-widest text-slate-500">
        {title}
      </h3>
      <div className="mt-3 space-y-3">{children}</div>
    </div>
  );
}

function Check({ label, checked }) {
  return (
    <label className="flex cursor-pointer items-center gap-3 text-sm font-medium text-slate-700">
      <input
        type="checkbox"
        defaultChecked={checked}
        className="h-4 w-4 accent-[#08264a]"
      />
      {label}
    </label>
  );
}

function RouteCard({ item, onMap, onFare }) {
  return (
    <div
      className={`overflow-hidden rounded-2xl border bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-lg ${
        item.featured ? "border-emerald-600" : "border-slate-200"
      }`}
    >
      {item.featured && (
        <div className="bg-emerald-700 px-5 py-2 text-xs font-black uppercase tracking-widest text-white">
          Recommended: Eco-Friendly Choice
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-12">
        <div className="p-5 sm:p-6 xl:col-span-9">
          <div className="flex flex-col gap-4 sm:flex-row sm:justify-between">
            <div className="flex gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-[#1d3f6e] text-white">
                {item.type === "Electric" ? <Zap /> : <Bus />}
              </div>

              <div>
                <h2 className="text-xl font-black text-slate-900">
                  {item.company}
                </h2>
                <p className="text-xs font-black uppercase tracking-widest text-emerald-700">
                  {item.route}
                </p>
              </div>
            </div>

            <div className="text-left sm:text-right">
              <StatusChip status={item.status} />
              <h3 className="mt-2 text-3xl font-black">Rs. {item.fare}</h3>
            </div>
          </div>

          <div className="my-5 border-t border-slate-200"></div>

          <div className="grid grid-cols-2 gap-4 text-sm md:grid-cols-4">
            <Metric label="Est. Travel Time" value={item.travelTime} />
            <Metric label="Next Bus In" value={item.nextBus} green />
            <Metric label="Occupancy" value={item.occupancy} />
            <div>
              <p className="text-xs font-bold text-slate-500">Amenities</p>
              <div className="mt-1 flex gap-1 text-slate-600">
                <Wifi size={15} />
                <Snowflake size={15} />
              </div>
            </div>
          </div>

          <div className="my-5 border-t border-slate-200"></div>

          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <p className="flex items-center gap-2 text-sm text-slate-600">
              <MapPin size={17} />
              Currently at: <span className="font-bold">{item.location}</span>
            </p>

            <div className="flex flex-col gap-3 sm:flex-row">
              <button
                onClick={onMap}
                className="flex items-center justify-center gap-2 rounded-lg border border-[#08264a] px-5 py-2 text-sm font-black text-[#08264a] transition hover:bg-[#08264a] hover:text-white"
              >
                <Map size={17} />
                View on Map
              </button>

              <button
                onClick={onFare}
                className="rounded-lg bg-emerald-700 px-5 py-2 text-sm font-black text-white transition hover:bg-emerald-800"
              >
                Pay Local Fare
              </button>
            </div>
          </div>
        </div>

        <div className="hidden bg-gradient-to-br from-slate-900 to-slate-600 xl:col-span-3 xl:block">
          <div className="flex h-full min-h-72 items-center justify-center text-sm text-slate-300">
            Bus Image
          </div>
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value, green }) {
  return (
    <div>
      <p className="text-xs font-bold text-slate-500">{label}</p>
      <p
        className={`mt-1 font-black ${
          green ? "text-emerald-700" : "text-slate-900"
        }`}
      >
        {value}
      </p>
    </div>
  );
}

function StatusChip({ status }) {
  const style =
    status.includes("Delayed")
      ? "bg-red-100 text-red-700"
      : status.includes("Eco")
      ? "bg-emerald-100 text-emerald-700"
      : "bg-emerald-100 text-emerald-700";

  return (
    <span className={`rounded-full px-3 py-1 text-xs font-black ${style}`}>
      {status}
    </span>
  );
}

function FooterColumn({ title, items }) {
  return (
    <div>
      <h3 className="text-xs font-black uppercase tracking-widest">{title}</h3>
      <div className="mt-4 space-y-3 text-sm text-slate-300">
        {items.map((item) => (
          <p key={item}>{item}</p>
        ))}
      </div>
    </div>
  );
}