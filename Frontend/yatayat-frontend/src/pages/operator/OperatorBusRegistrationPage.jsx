import { useState } from "react";
import { ArrowLeft, Loader2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import OperatorLayout from "../../components/layout/OperatorLayout";
import { apiFetch } from "../../utils/api";

const currentYear = new Date().getFullYear();
const initialForm = {
  busNumber: "", busName: "", model: "", manufactureYear: "",
  seatCapacity: "", busType: "", fuelType: "", permitNumber: "",
  permitExpiryDate: "", insuranceExpiryDate: "",
};

function validate(form) {
  const errors = {};
  const busNumber = form.busNumber.trim();

  if (!busNumber) errors.busNumber = "Bus number is required.";
  else if (busNumber.length < 4) errors.busNumber = "Use at least 4 characters.";
  else if (busNumber.length > 50) errors.busNumber = "Use no more than 50 characters.";

  if (!form.busName.trim()) errors.busName = "Bus name is required.";
  else if (form.busName.trim().length > 120) errors.busName = "Use no more than 120 characters.";

  if (!form.busType.trim()) errors.busType = "Bus type is required.";
  else if (form.busType.trim().length > 50) errors.busType = "Use no more than 50 characters.";

  const seats = Number(form.seatCapacity);
  if (!form.seatCapacity || !Number.isInteger(seats) || seats < 1 || seats > 100) {
    errors.seatCapacity = "Seat capacity must be a whole number between 1 and 100.";
  }

  if (form.manufactureYear) {
    const year = Number(form.manufactureYear);
    if (!Number.isInteger(year) || year < 1900 || year > currentYear) {
      errors.manufactureYear = `Year must be between 1900 and ${currentYear}.`;
    }
  }

  if (form.model.trim().length > 100) errors.model = "Use no more than 100 characters.";
  if (form.fuelType.trim().length > 50) errors.fuelType = "Use no more than 50 characters.";
  if (form.permitNumber.trim().length > 100) errors.permitNumber = "Use no more than 100 characters.";
  return errors;
}

export default function OperatorBusRegistrationPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});

  const update = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: "" }));
  };

  const submit = async (event) => {
    event.preventDefault();
    const validationErrors = validate(form);
    if (Object.keys(validationErrors).length) {
      setFieldErrors(validationErrors);
      setError("Please correct the highlighted fields.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setFieldErrors({});
      const payload = {
        busNumber: form.busNumber.trim(),
        busName: form.busName.trim(),
        model: form.model.trim() || null,
        manufactureYear: form.manufactureYear ? Number(form.manufactureYear) : null,
        seatCapacity: Number(form.seatCapacity),
        busType: form.busType.trim(),
        fuelType: form.fuelType.trim() || null,
        permitNumber: form.permitNumber.trim() || null,
        permitExpiryDate: form.permitExpiryDate || null,
        insuranceExpiryDate: form.insuranceExpiryDate || null,
      };
      const response = await apiFetch("/api/operator/buses", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (response.status === 401) return navigate("/login", { replace: true });
      if (response.status === 403 || response.status === 404) {
        return navigate("/operator/application-status", { replace: true });
      }
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.detail || data.message || "Unable to register bus.");
      }
      toast.success("Bus registered with pending status.");
      navigate("/operator/buses", { replace: true });
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setSaving(false);
    }
  };

  return <OperatorLayout><div className="mx-auto max-w-4xl space-y-5">
    <button type="button" onClick={() => navigate("/operator/buses")} className="flex items-center gap-2 text-sm font-semibold"><ArrowLeft size={17} /> Back to buses</button>
    <div><h1 className="text-2xl font-semibold">Register Bus</h1><p className="mt-1 text-sm text-slate-500">New buses are submitted with PENDING status.</p></div>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
    <form onSubmit={submit} noValidate className="grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-2">
      <Field name="busNumber" label="Bus Number *" value={form.busNumber} error={fieldErrors.busNumber} onChange={update} />
      <Field name="busName" label="Bus Name *" value={form.busName} error={fieldErrors.busName} onChange={update} />
      <Field name="busType" label="Bus Type *" value={form.busType} error={fieldErrors.busType} onChange={update} />
      <Field name="seatCapacity" label="Seat Capacity *" type="number" min="1" max="100" value={form.seatCapacity} error={fieldErrors.seatCapacity} onChange={update} />
      <Field name="model" label="Model" value={form.model} error={fieldErrors.model} onChange={update} />
      <Field name="manufactureYear" label="Manufacture Year" type="number" min="1900" max={currentYear} value={form.manufactureYear} error={fieldErrors.manufactureYear} onChange={update} />
      <Field name="fuelType" label="Fuel Type" value={form.fuelType} error={fieldErrors.fuelType} onChange={update} />
      <Field name="permitNumber" label="Permit Number" value={form.permitNumber} error={fieldErrors.permitNumber} onChange={update} />
      <Field name="permitExpiryDate" label="Permit Expiry Date" type="date" value={form.permitExpiryDate} onChange={update} />
      <Field name="insuranceExpiryDate" label="Insurance Expiry Date" type="date" value={form.insuranceExpiryDate} onChange={update} />
      <button type="submit" disabled={saving} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] py-2.5 font-semibold text-white disabled:opacity-60 sm:col-span-2">{saving && <Loader2 className="animate-spin" size={18} />}{saving ? "Registering..." : "Register Bus"}</button>
    </form>
  </div></OperatorLayout>;
}

function Field({ label, error, ...props }) {
  return <label className="text-sm font-semibold text-slate-700">{label}
    <input {...props} aria-invalid={Boolean(error)} className={`mt-2 w-full rounded-xl border px-4 py-2.5 font-normal outline-none focus:border-[#08264a] ${error ? "border-red-400 bg-red-50" : "border-slate-300"}`} />
    {error && <span className="mt-1 block text-xs font-bold text-red-600">{error}</span>}
  </label>;
}
