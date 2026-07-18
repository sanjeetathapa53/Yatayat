import { useState } from "react";
import {
  Users,
  UserCheck,
  ClipboardList,
  Search,
  MoreVertical,
  ChevronLeft,
  ChevronRight,
  CheckCircle,
  Clock,
  Phone,
  Eye,
} from "lucide-react";
import DriverLayout from "../../components/layout/DriverLayout";

const passengers = [
  {
    seat: "A1",
    name: "Anish Shrestha",
    initials: "AS",
    ticketId: "#YAT-8829-X",
    phone: "+977 9841000001",
    status: "Boarded",
    time: "08:15 AM",
  },
  {
    seat: "A2",
    name: "Bipina Pradhan",
    initials: "BP",
    ticketId: "#YAT-4410-Y",
    phone: "+977 9841000002",
    status: "Pending",
    time: "-",
  },
  {
    seat: "A3",
    name: "Ramesh Khatri",
    initials: "RK",
    ticketId: "#YAT-9021-Z",
    phone: "+977 9841000003",
    status: "Pending",
    time: "-",
  },
  {
    seat: "B1",
    name: "Sunita Jha",
    initials: "SJ",
    ticketId: "#YAT-1122-A",
    phone: "+977 9841000004",
    status: "Boarded",
    time: "08:10 AM",
  },
  {
    seat: "B2",
    name: "Milan Gurung",
    initials: "MG",
    ticketId: "#YAT-5566-B",
    phone: "+977 9841000005",
    status: "Pending",
    time: "-",
  },
];

export default function DriverPassengerListPage() {
  const [activeFilter, setActiveFilter] = useState("All");
  const [searchTerm, setSearchTerm] = useState("");
  const [list, setList] = useState(passengers);

  const filteredPassengers = list.filter((passenger) => {
    const matchesFilter =
      activeFilter === "All" || passenger.status === activeFilter;

    const matchesSearch =
      passenger.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      passenger.ticketId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      passenger.seat.toLowerCase().includes(searchTerm.toLowerCase());

    return matchesFilter && matchesSearch;
  });

  const total = list.length;
  const boarded = list.filter((item) => item.status === "Boarded").length;
  const remaining = list.filter((item) => item.status === "Pending").length;

  const markBoarded = (ticketId) => {
    setList((prev) =>
      prev.map((item) =>
        item.ticketId === ticketId
          ? { ...item, status: "Boarded", time: "Now" }
          : item
      )
    );
  };

  return (
    <DriverLayout activePage="Passenger List">
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Passenger List
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            View booked passengers and manage boarding status for the current trip.
          </p>
        </div>

        <div className="rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white shadow-sm">
          Arriving at Terminal
        </div>
      </header>

      <section className="mb-6 grid grid-cols-1 gap-5 md:grid-cols-3">
        <StatCard
          title="Total Capacity"
          value="32"
          icon={<Users size={26} />}
          border="border-l-[#08264a]"
        />
        <StatCard
          title="Boarded Passengers"
          value={boarded}
          icon={<UserCheck size={26} />}
          border="border-l-emerald-500"
          green
        />
        <StatCard
          title="Remaining to Board"
          value={remaining}
          icon={<ClipboardList size={26} />}
          border="border-l-[#1d3f6e]"
        />
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-4 border-b border-slate-200 p-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="responsive-scroll flex rounded-xl bg-slate-100 p-1">
            {["All", "Boarded", "Pending"].map((item) => (
              <button
                key={item}
                onClick={() => setActiveFilter(item)}
                className={`rounded-lg px-5 py-3 text-sm font-black transition ${
                  activeFilter === item
                    ? "bg-white text-[#08264a] shadow-sm"
                    : "text-slate-500 hover:text-[#08264a]"
                }`}
              >
                {item}
              </button>
            ))}
          </div>

          <div className="flex w-full items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 lg:w-96">
            <Search size={18} className="text-slate-500" />
            <input
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by name, seat or Ticket ID..."
              className="w-full min-w-0 bg-transparent text-sm outline-none"
            />
          </div>
        </div>

        <div className="hidden lg:block">
          <table className="w-full text-left">
            <thead className="bg-slate-100 text-xs font-black uppercase tracking-widest text-slate-600">
              <tr>
                <th className="px-6 py-4">Seat</th>
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Ticket ID</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Boarding Time</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>

            <tbody>
              {filteredPassengers.map((passenger) => (
                <tr
                  key={passenger.ticketId}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >
                  <td className="px-6 py-5 text-lg font-black">
                    {passenger.seat}
                  </td>

                  <td className="px-6 py-5">
                    <div className="flex items-center gap-3">
                      <Avatar initials={passenger.initials} />
                      <div>
                        <p className="font-black text-slate-900">
                          {passenger.name}
                        </p>
                        <p className="text-xs text-slate-500">
                          {passenger.phone}
                        </p>
                      </div>
                    </div>
                  </td>

                  <td className="px-6 py-5 font-semibold text-slate-500">
                    {passenger.ticketId}
                  </td>

                  <td className="px-6 py-5">
                    <StatusBadge status={passenger.status} />
                  </td>

                  <td className="px-6 py-5 text-sm font-bold text-slate-500">
                    {passenger.time}
                  </td>

                  <td className="px-6 py-5">
                    <div className="flex justify-end gap-2">
                      <IconButton icon={<Eye size={18} />} title="View Ticket" />
                      <IconButton icon={<Phone size={18} />} title="Call Passenger" />

                      {passenger.status === "Pending" ? (
                        <button
                          onClick={() => markBoarded(passenger.ticketId)}
                          className="rounded-lg bg-blue-100 px-4 py-2 text-xs font-black text-[#1d3f6e] hover:bg-blue-200"
                        >
                          Mark Boarded
                        </button>
                      ) : (
                        <IconButton icon={<MoreVertical size={18} />} title="More" />
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="space-y-4 p-4 lg:hidden">
          {filteredPassengers.map((passenger) => (
            <MobilePassengerCard
              key={passenger.ticketId}
              passenger={passenger}
              onBoard={() => markBoarded(passenger.ticketId)}
            />
          ))}
        </div>

        <div className="flex flex-col gap-4 bg-slate-50 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-slate-500">
            Showing 1-{filteredPassengers.length} of {total} passengers
          </p>

          <div className="responsive-scroll flex gap-2">
            <button className="rounded-lg border border-slate-300 bg-white p-3 text-slate-500 hover:bg-slate-100">
              <ChevronLeft size={18} />
            </button>
            <button className="rounded-lg bg-[#08264a] px-4 py-2 text-sm font-black text-white">
              1
            </button>
            <button className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-black text-slate-600 hover:bg-slate-100">
              2
            </button>
            <button className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-black text-slate-600 hover:bg-slate-100">
              3
            </button>
            <button className="rounded-lg border border-slate-300 bg-white p-3 text-slate-500 hover:bg-slate-100">
              <ChevronRight size={18} />
            </button>
          </div>
        </div>
      </section>
    </DriverLayout>
  );
}

function StatCard({ title, value, icon, border, green }) {
  return (
    <div
      className={`rounded-2xl border border-slate-200 border-l-4 ${border} bg-white p-5 shadow-sm`}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-semibold text-slate-500">{title}</p>
          <h2
            className={`mt-2 text-2xl font-black ${
              green ? "text-emerald-600" : "text-[#08264a]"
            }`}
          >
            {value}
          </h2>
        </div>

        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-[#08264a]">
          {icon}
        </div>
      </div>
    </div>
  );
}

function Avatar({ initials }) {
  return (
    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-100 text-xs font-black text-[#1d3f6e]">
      {initials}
    </div>
  );
}

function StatusBadge({ status }) {
  const boarded = status === "Boarded";

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-black ${
        boarded
          ? "bg-emerald-100 text-emerald-700"
          : "bg-orange-100 text-orange-700"
      }`}
    >
      {boarded ? <CheckCircle size={13} /> : <Clock size={13} />}
      {status}
    </span>
  );
}

function IconButton({ icon, title }) {
  return (
    <button
      title={title}
      className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-[#08264a]"
    >
      {icon}
    </button>
  );
}

function MobilePassengerCard({ passenger, onBoard }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <Avatar initials={passenger.initials} />
          <div>
            <h3 className="font-black text-slate-900">{passenger.name}</h3>
            <p className="text-xs text-slate-500">{passenger.ticketId}</p>
            <p className="mt-1 text-xs text-slate-500">{passenger.phone}</p>
          </div>
        </div>

        <div className="text-right">
          <p className="text-xl font-black text-[#08264a]">{passenger.seat}</p>
          <StatusBadge status={passenger.status} />
        </div>
      </div>

      <div className="mt-4 grid gap-2 min-[360px]:grid-cols-3">
        <button className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-slate-300 py-3 text-sm font-black text-slate-600">
          <Eye size={17} />
          View
        </button>

        <button className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-slate-300 py-3 text-sm font-black text-slate-600">
          <Phone size={17} />
          Call
        </button>

        {passenger.status === "Pending" && (
          <button
            onClick={onBoard}
            className="flex flex-1 items-center justify-center rounded-xl bg-[#08264a] py-3 text-sm font-black text-white"
          >
            Board
          </button>
        )}
      </div>
    </div>
  );
}
