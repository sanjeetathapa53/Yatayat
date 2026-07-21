import { useState } from "react";
import {
  Search,
  Bell,
  UserCircle,
  Bus,
  Ticket,
  LocateFixed,
  Plus,
  Minus,
  Filter,
  ExternalLink,
  Menu,
  X,
  MapPin,
  Navigation,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { MapContainer, TileLayer, Marker, Popup, Polyline } from "react-leaflet";
import L from "leaflet";
import { useLanguage } from "../../context/LanguageContext";

const createBusIcon = (label, background) => new L.DivIcon({
  html: `<div style="background:${background};color:white;border-radius:999px;padding:8px 12px;font-weight:800;font-size:12px;box-shadow:0 8px 20px rgba(0,0,0,.25)">🚌 ${label}</div>`,
  className: "",
});

const buses = [
  {
    id: "BA 1 PA 4521",
    route: "Ring Road Circular",
    status: "ON TIME",
    eta: "4 mins",
    nextStop: "Kalanki Chowk",
    location: "Swayambhu Stupa Gate",
    speed: "24 km/h",
    passengers: 18,
    position: [27.7172, 85.324],
  },
  {
    id: "BA 2 PA 9901",
    route: "Ratnapark - Gongabu",
    status: "DELAYED",
    eta: "11 mins",
    nextStop: "Balaju",
    location: "Ratnapark",
    speed: "16 km/h",
    passengers: 31,
    position: [27.705, 85.315],
  },
];

const routeLine = [
  [27.7172, 85.324],
  [27.713, 85.322],
  [27.71, 85.32],
  [27.705, 85.315],
  [27.699, 85.31],
];

export default function LiveTrackingPage() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  const [selectedBus, setSelectedBus] = useState(buses[0]);
  const [panelOpen, setPanelOpen] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-200 text-[#08264a]">
      <header className="fixed left-0 top-0 z-[1000] h-16 w-full border-b border-slate-200 bg-white">
        <nav className="flex h-full items-center justify-between px-4 sm:px-6">
          <div className="flex items-center gap-4">
            <button
              type="button"
              onClick={() => setMobileNavOpen(true)}
              aria-label={t("passenger.liveTracking.openMenu")}
              className="rounded-xl p-2 hover:bg-slate-100 md:hidden"
            >
              <Menu size={22} />
            </button>

            <Link to="/" className="text-xl font-black">
              Yatayat
            </Link>

            <div className="hidden gap-6 text-sm font-semibold md:flex">
              <Link to="/routes" className="text-slate-600 hover:text-[#08264a]">
                {t("passenger.liveTracking.routes")}
              </Link>
              <Link
                to="/track-bus"
                className="border-b-2 border-[#08264a] pb-5 text-[#08264a]"
              >
                {t("passenger.liveTracking.trackBus")}
              </Link>
              <Link to="/fare-pass" className="text-slate-600 hover:text-[#08264a]">
                {t("passenger.liveTracking.farePass")}
              </Link>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="hidden w-72 items-center gap-2 rounded-lg border border-slate-300 bg-slate-50 px-3 py-2 lg:flex">
              <Search size={17} />
              <input
                placeholder={t("passenger.liveTracking.searchRouteOrBus")}
                aria-label={t("passenger.liveTracking.searchRouteOrBus")}
                className="w-full bg-transparent text-sm outline-none"
              />
            </div>

            <Link to="/notifications" aria-label={t("common.openNotifications")} className="rounded-full p-2 hover:bg-slate-100">
              <Bell size={19} />
            </Link>

            <Link to="/profile" aria-label={t("common.openProfile")} className="rounded-full p-2 hover:bg-slate-100">
              <UserCircle size={21} />
            </Link>
          </div>
        </nav>
      </header>

      {mobileNavOpen && (
        <div className="fixed inset-0 z-[1100] bg-black/40 md:hidden">
          <div className="h-full w-72 bg-white p-5">
            <div className="mb-8 flex items-center justify-between">
              <h2 className="text-2xl font-black">Yatayat</h2>
              <button type="button" onClick={() => setMobileNavOpen(false)} aria-label={t("passenger.liveTracking.closeMenu")}>
                <X size={22} />
              </button>
            </div>

            <div className="space-y-3 text-sm font-bold">
              <button type="button" onClick={() => navigate("/routes")} className="block w-full rounded-xl p-3 text-left hover:bg-slate-100">
                {t("passenger.liveTracking.routes")}
              </button>
              <button type="button" onClick={() => navigate("/track-bus")} className="block w-full rounded-xl bg-[#08264a] p-3 text-left text-white">
                {t("passenger.liveTracking.trackBus")}
              </button>
              <button type="button" onClick={() => navigate("/fare-pass")} className="block w-full rounded-xl p-3 text-left hover:bg-slate-100">
                {t("passenger.liveTracking.farePass")}
              </button>
              <button type="button" onClick={() => navigate("/passenger/dashboard")} className="block w-full rounded-xl p-3 text-left hover:bg-slate-100">
                {t("common.dashboard")}
              </button>
            </div>
          </div>
        </div>
      )}

      <main className="relative pt-16">
        <button
          type="button"
          onClick={() => setPanelOpen(!panelOpen)}
          aria-label={panelOpen ? t("passenger.liveTracking.hideDetails") : t("passenger.liveTracking.showDetails")}
          className={`fixed left-0 top-24 z-[900] hidden rounded-r-xl bg-[#08264a] p-3 text-white shadow-lg transition-all lg:flex ${
            panelOpen ? "translate-x-[400px]" : "translate-x-0"
          }`}
        >
          {panelOpen ? <ChevronLeft size={20} /> : <ChevronRight size={20} />}
        </button>

        <button
          type="button"
          onClick={() => setPanelOpen(true)}
          className="fixed bottom-6 left-4 z-[900] flex items-center gap-2 rounded-full bg-[#08264a] px-5 py-3 text-sm font-black text-white shadow-lg lg:hidden"
        >
          <Filter size={18} />
          {t("passenger.liveTracking.trackingDetails")}
        </button>

        {panelOpen && (
          <div
            onClick={() => setPanelOpen(false)}
            className="fixed inset-0 z-[850] bg-black/40 lg:hidden"
          />
        )}

        <aside
          className={`fixed left-0 top-16 z-[900] h-[calc(100vh-64px)] w-full max-w-[420px] overflow-y-auto bg-transparent p-4 transition-transform duration-300 sm:p-5 lg:w-[420px] ${
            panelOpen ? "translate-x-0" : "-translate-x-full"
          }`}
        >
          <div className="space-y-4">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-lg">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-black">{t("passenger.liveTracking.title")}</h2>

                <div className="flex items-center gap-2">
                  <button type="button" className="hidden items-center gap-2 text-sm font-bold hover:text-emerald-700 sm:flex">
                    <ExternalLink size={15} />
                    {t("passenger.liveTracking.editTracking")}
                  </button>

                  <button
                    type="button"
                    onClick={() => setPanelOpen(false)}
                    aria-label={t("passenger.liveTracking.closeDetails")}
                    className="rounded-lg p-2 hover:bg-slate-100"
                  >
                    <X size={18} />
                  </button>
                </div>
              </div>

              <div className="mt-5 flex items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3">
                <Search size={18} className="text-slate-500" />
                <input
                  placeholder={t("passenger.liveTracking.searchBusPlaceholder")}
                  aria-label={t("passenger.liveTracking.searchBus")}
                  className="w-full bg-transparent text-sm outline-none"
                />
              </div>

              <button type="button" className="mt-5 flex w-full items-center justify-between rounded-xl bg-[#1d3f6e] px-5 py-4 text-sm font-bold text-white transition hover:bg-[#0d3566]">
                {t("passenger.liveTracking.showNearbyBuses")}
                <span className="flex h-6 w-11 items-center justify-end rounded-full bg-emerald-600 p-1">
                  <span className="h-5 w-5 rounded-full bg-white"></span>
                </span>
              </button>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white shadow-lg">
              <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
                <h3 className="text-sm font-black uppercase tracking-widest text-slate-600">
                  {t("passenger.liveTracking.activeVehicles", { count: buses.length })}
                </h3>
                <Filter size={18} className="text-slate-500" />
              </div>

              <div className="max-h-64 overflow-y-auto p-3">
                {buses.map((bus) => (
                  <button
                    type="button"
                    key={bus.id}
                    onClick={() => setSelectedBus(bus)}
                    className={`mb-3 w-full rounded-xl border p-4 text-left transition hover:shadow-md ${
                      selectedBus.id === bus.id
                        ? "border-[#08264a] bg-slate-100"
                        : "border-slate-200 bg-white"
                    }`}
                  >
                    <div className="flex justify-between gap-3">
                      <div>
                        <h4 className="font-black">{bus.id}</h4>
                        <p className="text-sm text-slate-500">{bus.route}</p>
                      </div>

                      <StatusBadge status={bus.status} t={t} />
                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-3 text-xs">
                      <MiniInfo title={t("passenger.liveTracking.nextStop")} value={bus.nextStop} />
                      <MiniInfo title={t("passenger.liveTracking.eta")} value={bus.eta} green />
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <SelectedBusCard selectedBus={selectedBus} navigate={navigate} t={t} />
          </div>
        </aside>

        <div className="fixed right-4 top-20 z-[800] rounded-full bg-white px-4 py-3 text-xs shadow-lg sm:right-6 sm:top-24 sm:px-6 sm:py-4 sm:text-sm">
          <span className="mr-2 inline-block h-2 w-2 rounded-full bg-emerald-600"></span>
          {t("passenger.liveTracking.onlineCount", { count: "1,240" })}
          <span className="mx-2 text-slate-300 sm:mx-4">|</span>
          NP-KTM-01
        </div>

        <div className="h-[calc(100vh-64px)] w-full">
          <MapContainer
            center={[27.7105, 85.318]}
            zoom={14}
            scrollWheelZoom={true}
            className="h-full w-full"
          >
            <TileLayer
              attribution="© OpenStreetMap"
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            <Polyline positions={routeLine} color="#047857" weight={5} />

            {buses.map((bus) => (
              <Marker
                key={bus.id}
                position={bus.position}
                icon={bus.status === "DELAYED" ? createBusIcon(t("passenger.liveTracking.status.delayed"), "#dc2626") : createBusIcon("BA 1 PA", "#047857")}
                eventHandlers={{
                  click: () => {
                    setSelectedBus(bus);
                    setPanelOpen(true);
                  },
                }}
              >
                <Popup>
                  <b>{bus.id}</b>
                  <br />
                  {bus.route}
                  <br />
                  {t("passenger.liveTracking.eta")}: {bus.eta}
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>

        <div className="fixed bottom-24 right-4 z-[800] flex flex-col gap-3 sm:bottom-8 sm:right-8">
          <button type="button" aria-label={t("passenger.liveTracking.recenterMap")} title={t("passenger.liveTracking.recenterMap")} className="rounded-xl bg-white p-3 shadow-lg hover:bg-slate-100">
            <LocateFixed size={22} />
          </button>
          <button type="button" aria-label={t("passenger.liveTracking.zoomIn")} title={t("passenger.liveTracking.zoomIn")} className="rounded-xl bg-white p-3 shadow-lg hover:bg-slate-100">
            <Plus size={22} />
          </button>
          <button type="button" aria-label={t("passenger.liveTracking.zoomOut")} title={t("passenger.liveTracking.zoomOut")} className="rounded-xl bg-white p-3 shadow-lg hover:bg-slate-100">
            <Minus size={22} />
          </button>
        </div>

        <div className="fixed bottom-0 left-0 right-0 z-[750] border-t border-slate-200 bg-white p-4 shadow-lg lg:hidden">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase tracking-widest text-emerald-700">
                {t("passenger.liveTracking.selectedBus")}
              </p>
              <h3 className="font-black">{selectedBus.id}</h3>
              <p className="text-xs text-slate-500">{selectedBus.route}</p>
            </div>

            <button
              type="button"
              onClick={() => setPanelOpen(true)}
              className="rounded-xl bg-[#08264a] px-4 py-3 text-xs font-black text-white"
            >
              {t("passenger.liveTracking.viewDetails")}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}

function SelectedBusCard({ selectedBus, navigate, t }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-lg">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-black uppercase tracking-widest text-emerald-700">
            {t("passenger.liveTracking.selectedBus")}
          </p>
          <h3 className="mt-1 text-xl font-black text-[#08264a]">
            {selectedBus.id}
          </h3>
          <p className="text-sm text-slate-500">{selectedBus.route}</p>
        </div>

        <StatusBadge status={selectedBus.status} t={t} />
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3">
        <MiniDetail label={t("passenger.liveTracking.currentLocation")} value={selectedBus.location} />
        <MiniDetail label={t("passenger.liveTracking.nextStop")} value={selectedBus.nextStop} />
        <MiniDetail label={t("passenger.liveTracking.eta")} value={selectedBus.eta} green />
        <MiniDetail label={t("passenger.liveTracking.speed")} value={selectedBus.speed} />
      </div>

      <button
        type="button"
        onClick={() => navigate("/fare-pass")}
        className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 py-3 text-sm font-black text-white transition hover:bg-emerald-800"
      >
        <Ticket size={17} />
        {t("passenger.liveTracking.payLocalFare")}
      </button>
    </div>
  );
}

function MiniInfo({ title, value, green }) {
  return (
    <div>
      <p className="font-bold uppercase text-slate-400">{title}</p>
      <p className={`mt-1 font-black ${green ? "text-emerald-700" : ""}`}>
        {value}
      </p>
    </div>
  );
}

function MiniDetail({ label, value, green }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
      <p className="text-[10px] font-black uppercase tracking-wider text-slate-400">
        {label}
      </p>
      <p
        className={`mt-1 text-sm font-black ${
          green ? "text-emerald-700" : "text-[#08264a]"
        }`}
      >
        {value}
      </p>
    </div>
  );
}

function StatusBadge({ status, t }) {
  const translatedStatus = status === "DELAYED" ? t("passenger.liveTracking.status.delayed") : t("passenger.liveTracking.status.onTime");

  return (
    <span
      className={`h-fit rounded-full px-3 py-1 text-[10px] font-black ${
        status === "DELAYED"
          ? "bg-red-100 text-red-700"
          : "bg-emerald-100 text-emerald-700"
      }`}
    >
      {translatedStatus}
    </span>
  );
}
