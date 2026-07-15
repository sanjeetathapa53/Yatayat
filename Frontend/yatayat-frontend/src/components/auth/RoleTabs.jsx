import {
  BusFront,
  Building2,
  UserRound,
} from "lucide-react";

export default function RoleTabs({ role, setRole }) {
  const tabs = [
    {
      key: "passenger",
      label: "Passenger",
      icon: <UserRound size={17} />,
    },
    {
      key: "driver",
      label: "Driver",
      icon: <BusFront size={17} />,
    },
    {
      key: "operator",
      label: "Operator",
      icon: <Building2 size={17} />,
    },
  ];

  return (
    <div className="grid grid-cols-3 gap-1 rounded-2xl bg-slate-100 p-1">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          type="button"
          onClick={() => setRole(tab.key)}
          className={`flex min-w-0 items-center justify-center gap-1.5 rounded-xl px-2 py-3 text-xs font-bold transition sm:text-sm ${
            role === tab.key
              ? "bg-white text-[#08264a] shadow-sm"
              : "text-slate-500 hover:text-[#08264a]"
          }`}
        >
          <span className="hidden sm:inline-flex">
            {tab.icon}
          </span>

          <span className="truncate">{tab.label}</span>
        </button>
      ))}
    </div>
  );
}