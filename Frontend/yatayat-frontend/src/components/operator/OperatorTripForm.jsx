export default function OperatorTripForm({ form, eligibility, submitting, submitLabel, onChange, onSubmit, onCancel }) {
  return (
    <form onSubmit={onSubmit} className="space-y-5 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Select label="Route" value={form.routeId} onChange={(value) => onChange("routeId", value)} required>
          <option value="">Select an active route</option>
          {eligibility.routes.map((route) => <option key={route.id} value={route.id}>{route.code} - {route.name} - {route.tripType === "LOCAL" ? "Local" : "Outside Valley"} ({route.origin} to {route.destination})</option>)}
        </Select>
        <Select label="Bus" value={form.busId} onChange={(value) => onChange("busId", value)} required>
          <option value="">Select an eligible bus</option>
          {eligibility.buses.map((bus) => <option key={bus.id} value={bus.id}>{bus.busNumber} - {bus.busName || bus.busType} - {bus.seatCapacity} seats</option>)}
        </Select>
        <Select label="Driver" value={form.driverId} onChange={(value) => onChange("driverId", value)} required>
          <option value="">Select an eligible driver</option>
          {eligibility.drivers.map((driver) => <option key={driver.id} value={driver.id}>{driver.fullName} - {driver.licenseNumber} ({driver.licenseCategory})</option>)}
        </Select>
        <Field label="Fare (NPR)" type="number" min="0.01" step="0.01" value={form.fare} onChange={(value) => onChange("fare", value)} required />
        <Field label="Departure date and time" type="datetime-local" value={form.departureAt} onChange={(value) => onChange("departureAt", value)} required />
        <Field label="Estimated arrival date and time" type="datetime-local" value={form.estimatedArrivalAt} onChange={(value) => onChange("estimatedArrivalAt", value)} required />
      </div>
      <label className="block text-sm font-semibold text-slate-700">Boarding notes <span className="font-semibold text-slate-400">(optional)</span>
        <textarea maxLength={1000} rows={4} value={form.boardingNotes} onChange={(event) => onChange("boardingNotes", event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-2.5 font-medium outline-none focus:border-[#08264a]" placeholder="Passenger boarding instructions" />
        <span className="mt-1 block text-right text-xs text-slate-400">{form.boardingNotes.length}/1000</span>
      </label>
      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button type="button" onClick={onCancel} className="rounded-xl border border-slate-300 px-5 py-2.5 font-semibold text-slate-600">Cancel</button>
        <button disabled={submitting} className="rounded-xl bg-[#08264a] px-6 py-2.5 font-semibold text-white disabled:opacity-60">{submitting ? "Saving..." : submitLabel}</button>
      </div>
    </form>
  );
}

function Select({ label, value, onChange, children, required }) {
  return <label className="block text-sm font-semibold text-slate-700">{label}<select required={required} value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 font-medium outline-none focus:border-[#08264a]">{children}</select></label>;
}

function Field({ label, value, onChange, ...props }) {
  return <label className="block text-sm font-semibold text-slate-700">{label}<input {...props} value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-2.5 font-medium outline-none focus:border-[#08264a]" /></label>;
}
