import {
    Play,
    Pause,
    Square,
    AlertTriangle,
    Cloud,
    Navigation,
    Clock,
    Route,
    MapPin,
    CheckCircle2,
  } from "lucide-react";
  import DriverLayout from "../../components/layout/DriverLayout";
  import { useNavigate } from "react-router-dom";

  export default function TripManagementPage() {
    const navigate = useNavigate();
    return (
      <DriverLayout activePage="Trip Management">
        <div className="space-y-6">
          
          {/* Header */}
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-3xl font-black text-slate-900">
                Trip Management
              </h1>
              <p className="text-slate-500">
                Monitor and control your current route.
              </p>
            </div>
  
            <div className="rounded-full bg-emerald-100 px-4 py-2 text-sm font-black text-emerald-700">
              ● Trip Active
            </div>
          </div>
  
          <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
  
            {/* LEFT SECTION */}
            <div className="xl:col-span-8">
              <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
  
                {/* Map Placeholder */}
                <div className="relative h-[500px] bg-gradient-to-br from-orange-100 via-slate-100 to-cyan-100">
  
                  <div className="absolute left-6 top-6 rounded-2xl bg-white p-5 shadow-lg">
                    <h3 className="text-xl font-black">
                      Prithvi Highway
                    </h3>
  
                    <p className="mt-2 text-slate-500">
                      Nearing Kurintar. Heavy traffic expected in 5km.
                    </p>
                  </div>
  
                  {/* Route line */}
                  <div className="absolute inset-0 flex items-center justify-center">
                    <div className="w-[70%] border-t-4 border-dashed border-blue-400"></div>
                  </div>
  
                  {/* Emergency Button */}
                  <button className="absolute bottom-6 left-6 flex items-center gap-3 rounded-2xl bg-red-600 px-6 py-4 font-black text-white hover:bg-red-700">
                    <AlertTriangle size={20} />
                    EMERGENCY SOS
                  </button>
                </div>
  
                {/* Bottom Controls */}
                <div className="grid grid-cols-1 gap-4 p-6 md:grid-cols-5">
  
                  <button className="rounded-2xl bg-[#08264a] py-4 font-black text-white hover:bg-[#0d3566]">
                    <div className="flex items-center justify-center gap-2">
                      <Play size={18} />
                      Start Trip
                    </div>
                  </button>
  
                  <button className="rounded-2xl border border-slate-300 py-4 font-black text-slate-600 hover:bg-slate-50">
                    <div className="flex items-center justify-center gap-2">
                      <Pause size={18} />
                      Pause
                    </div>
                  </button>
  
                  <button
  onClick={() => navigate("/driver/trip-summary")}
  className="rounded-2xl border border-red-300 py-4 font-black text-red-600 hover:bg-red-50"
>
  <div className="flex items-center justify-center gap-2">
    <Square size={18} />
    End Trip
  </div>
</button>
  
                  <div className="rounded-2xl bg-slate-50 p-4 text-center">
                    <p className="text-xs font-black uppercase text-slate-500">
                      Current Speed
                    </p>
                    <h3 className="mt-2 text-3xl font-black text-[#08264a]">
                      54
                    </h3>
                    <p className="text-sm text-slate-500">km/h</p>
                  </div>
  
                  <div className="rounded-2xl bg-slate-50 p-4 text-center">
                    <p className="text-xs font-black uppercase text-slate-500">
                      ETA
                    </p>
                    <h3 className="mt-2 text-3xl font-black text-[#08264a]">
                      45
                    </h3>
                    <p className="text-sm text-slate-500">mins</p>
                  </div>
  
                </div>
              </div>
            </div>
  
            {/* RIGHT SECTION */}
            <div className="space-y-5 xl:col-span-4">
  
              {/* Trip Card */}
              <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-black">
                    Trip Details
                  </h2>
  
                  <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-black text-blue-700">
                    IN PROGRESS
                  </span>
                </div>
  
                <div className="mt-5 space-y-4">
                  <InfoRow label="Bus Number" value="BA-1-KHA-1234" />
                  <InfoRow label="Route" value="Kathmandu → Pokhara" />
                  <InfoRow label="Departure" value="07:00 AM" />
                  <InfoRow label="Distance" value="204 km" />
                </div>
  
                <div className="mt-6">
                  <div className="h-3 rounded-full bg-slate-200">
                    <div className="h-3 w-[65%] rounded-full bg-[#08264a]"></div>
                  </div>
  
                  <p className="mt-2 text-center text-sm text-slate-500">
                    65% Journey Completed
                  </p>
                </div>
              </div>
  
              {/* Timeline */}
              <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                <h2 className="text-xl font-black">
                  Stops Timeline
                </h2>
  
                <div className="mt-5 space-y-5">
  
                  <TimelineItem
                    done
                    title="Kathmandu - Gongabu"
                    desc="Departed 07:12 AM"
                  />
  
                  <TimelineItem
                    done
                    title="Naubise"
                    desc="Passed 08:35 AM"
                  />
  
                  <TimelineItem
                    current
                    title="Mugling Rest Stop"
                    desc="Expected Arrival 10:15 AM"
                  />
  
                  <TimelineItem
                    title="Dumre"
                    desc="Scheduled 11:30 AM"
                  />
  
                  <TimelineItem
                    title="Pokhara Terminal"
                    desc="ETA 01:45 PM"
                  />
  
                </div>
              </div>
  
              {/* Weather */}
              <div className="rounded-3xl bg-[#08264a] p-5 text-white shadow-sm">
                <div className="flex items-center gap-3">
                  <Cloud />
                  <div>
                    <p className="text-xs uppercase text-slate-300">
                      Destination Weather
                    </p>
                    <h3 className="font-black">
                      Pokhara: 24°C, Cloudy
                    </h3>
                  </div>
                </div>
              </div>
  
            </div>
  
          </div>
        </div>
      </DriverLayout>
    );
  }
  
  function InfoRow({ label, value }) {
    return (
      <div className="flex justify-between">
        <span className="text-slate-500">{label}</span>
        <span className="font-black">{value}</span>
      </div>
    );
  }
  
  function TimelineItem({ title, desc, done, current }) {
    return (
      <div className="flex gap-3">
        <div>
          {done ? (
            <CheckCircle2 className="text-emerald-600" size={22} />
          ) : current ? (
            <Navigation className="text-blue-600" size={22} />
          ) : (
            <MapPin className="text-slate-400" size={22} />
          )}
        </div>
  
        <div>
          <h3 className="font-black">{title}</h3>
          <p className="text-sm text-slate-500">{desc}</p>
        </div>
      </div>
    );
  }