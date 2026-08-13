import { useId, useState } from "react";
import { ChevronDown, MapPin } from "lucide-react";

export default function DriverRouteStops({ stops, title = "Route stops", className = "mt-4" }) {
  const [expanded, setExpanded] = useState(false);
  const panelId = useId();

  if (!stops?.length) return null;

  return (
    <section className={`${className} overflow-hidden rounded-2xl border border-slate-200 bg-slate-50`}>
      <button
        type="button"
        aria-controls={panelId}
        aria-expanded={expanded}
        onClick={() => setExpanded((current) => !current)}
        className="flex min-h-11 w-full items-center gap-3 px-3 py-2.5 text-left transition hover:bg-slate-100 sm:px-4"
      >
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-emerald-100 text-emerald-700">
          <MapPin aria-hidden="true" size={17} />
        </span>
        <span className="min-w-0 flex-1">
          <span className="font-semibold text-slate-900">{title}</span>
          <span className="ml-2 rounded-full bg-white px-2 py-0.5 text-xs font-semibold text-slate-600">
            {stops.length} stops
          </span>
        </span>
        <span className="hidden text-xs font-medium text-slate-500 sm:inline">
          {expanded ? "Hide route" : "View route"}
        </span>
        <ChevronDown
          aria-hidden="true"
          size={18}
          className={`shrink-0 text-slate-500 transition-transform ${expanded ? "rotate-180" : ""}`}
        />
      </button>

      {expanded && (
        <div id={panelId} className="max-h-72 space-y-1.5 overflow-y-auto border-t border-slate-200 p-2 sm:p-3">
          {stops.map((stop) => (
            <div
              key={stop.id ?? `${stop.stopOrder}-${stop.stopName}`}
              className="flex min-w-0 items-center gap-2.5 rounded-xl bg-white px-2.5 py-2 sm:px-3"
            >
              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#08264a] text-[11px] font-semibold text-white">
                {stop.stopOrder}
              </span>
              <div className="min-w-0 flex-1">
                <p className="safe-wrap text-sm font-semibold leading-5 text-slate-900">{stop.stopName}</p>
                <p className="safe-wrap text-xs leading-4 text-slate-500">{stop.landmark || "Local route stop"}</p>
              </div>
              <div className="shrink-0 text-right text-[11px] font-medium leading-4 text-slate-500 sm:text-xs">
                <p>{stop.estimatedMinutesFromStart ?? "--"} min</p>
                <p>NPR {stop.cumulativeFare ?? "--"}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}