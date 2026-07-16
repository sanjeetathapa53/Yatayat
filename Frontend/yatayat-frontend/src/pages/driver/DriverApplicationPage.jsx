import { useMemo, useState } from "react";
import {
  User,
  MapPin,
  Phone,
  Calendar,
  BadgeCheck,
  FileText,
  Upload,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  ShieldCheck,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import AuthLayout from "../../components/auth/AuthLayout";

const steps = [
  "Personal Details",
  "Licence Details",
  "Documents",
  "Review",
];

export default function DriverApplicationPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const registeredUser = JSON.parse(
    localStorage.getItem("yatayatUser") || "null"
  );

  const email =
    location.state?.email ||
    registeredUser?.email ||
    localStorage.getItem("pendingDriverEmail") ||
    "";

  const fullName =
    location.state?.fullName ||
    registeredUser?.fullName ||
    "Driver Applicant";

  const [currentStep, setCurrentStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const [form, setForm] = useState({
    dateOfBirth: "",
    permanentAddress: "",
    currentAddress: "",
    emergencyContactName: "",
    emergencyContactPhone: "",

    citizenshipNumber: "",
    licenseNumber: "",
    licenseCategory: "",
    licenseIssueDate: "",
    licenseExpiryDate: "",
    yearsOfExperience: "",
    preferredOperatingArea: "",
    applicationNote: "",

    profilePhoto: null,
    citizenshipFront: null,
    citizenshipBack: null,
    licenseFront: null,
    licenseBack: null,

    declarationAccepted: false,
  });

  const progress = useMemo(
    () => ((currentStep + 1) / steps.length) * 100,
    [currentStep]
  );

  const updateField = (field, value) => {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));

    setError("");
  };

  const validateCurrentStep = () => {
    if (currentStep === 0) {
      if (
        !form.dateOfBirth ||
        !form.permanentAddress.trim() ||
        !form.currentAddress.trim() ||
        !form.emergencyContactName.trim() ||
        !form.emergencyContactPhone.trim()
      ) {
        return "Please complete all personal details.";
      }
    }

    if (currentStep === 1) {
      if (
        !form.citizenshipNumber.trim() ||
        !form.licenseNumber.trim() ||
        !form.licenseCategory ||
        !form.licenseIssueDate ||
        !form.licenseExpiryDate ||
        form.yearsOfExperience === ""
      ) {
        return "Please complete all licence and identity details.";
      }

      if (Number(form.yearsOfExperience) < 0) {
        return "Years of experience cannot be negative.";
      }

      if (
        new Date(form.licenseExpiryDate) <=
        new Date(form.licenseIssueDate)
      ) {
        return "Licence expiry date must be after the issue date.";
      }
    }

    if (currentStep === 2) {
      const missingDocument =
        !form.profilePhoto ||
        !form.citizenshipFront ||
        !form.citizenshipBack ||
        !form.licenseFront ||
        !form.licenseBack;

      if (missingDocument) {
        return "Please upload all required documents.";
      }
    }

    if (currentStep === 3 && !form.declarationAccepted) {
      return "You must accept the declaration before submitting.";
    }

    return "";
  };

  const goNext = () => {
    const validationError = validateCurrentStep();

    if (validationError) {
      setError(validationError);
      return;
    }

    setCurrentStep((previous) =>
      Math.min(previous + 1, steps.length - 1)
    );
  };

  const goBack = () => {
    setError("");

    setCurrentStep((previous) => Math.max(previous - 1, 0));
  };

  const handleSubmit = async () => {
    const validationError = validateCurrentStep();

    if (validationError) {
      setError(validationError);
      return;
    }

    if (!email) {
      setError(
        "Driver account email was not found. Please register or log in again."
      );
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      const payload = new FormData();

      payload.append("email", email);
      payload.append("dateOfBirth", form.dateOfBirth);
      payload.append("permanentAddress", form.permanentAddress.trim());
      payload.append("currentAddress", form.currentAddress.trim());
      payload.append(
        "emergencyContactName",
        form.emergencyContactName.trim()
      );
      payload.append(
        "emergencyContactPhone",
        form.emergencyContactPhone.trim()
      );

      payload.append(
        "citizenshipNumber",
        form.citizenshipNumber.trim()
      );
      payload.append("licenseNumber", form.licenseNumber.trim());
      payload.append("licenseCategory", form.licenseCategory);
      payload.append("licenseIssueDate", form.licenseIssueDate);
      payload.append("licenseExpiryDate", form.licenseExpiryDate);
      payload.append(
        "yearsOfExperience",
        String(form.yearsOfExperience)
      );
      payload.append(
        "preferredOperatingArea",
        form.preferredOperatingArea.trim()
      );
      payload.append("applicationNote", form.applicationNote.trim());

      payload.append("profilePhoto", form.profilePhoto);
      payload.append("citizenshipFront", form.citizenshipFront);
      payload.append("citizenshipBack", form.citizenshipBack);
      payload.append("licenseFront", form.licenseFront);
      payload.append("licenseBack", form.licenseBack);

      const response = await fetch(
        "http://localhost:8080/api/drivers/application",
        {
          method: "POST",
          credentials: "include",
          body: payload,
        }
      );

      const result = await response.json();

      if (!response.ok || !result.success) {
        throw new Error(
          result.message || "Unable to submit driver application."
        );
      }

      navigate("/driver/application-status", {
        state: {
          status: "PENDING",
          message:
            result.message ||
            "Your driver application has been submitted.",
          application: result.application,
        },
      });
    } catch (submissionError) {
      console.error(submissionError);

      setError(
        submissionError.message ||
          "Something went wrong while submitting the application."
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout>
      <div className="w-full max-w-5xl rounded-3xl border border-slate-200 bg-white shadow-xl">
        <div className="border-b border-slate-200 px-5 py-5 sm:px-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.2em] text-emerald-600">
                Driver Verification
              </p>

              <h1 className="mt-2 text-2xl font-black text-[#08264a] sm:text-3xl">
                Complete Your Driver Application
              </h1>

              <p className="mt-2 text-sm text-slate-500">
                Applicant: {fullName}
                {email ? ` · ${email}` : ""}
              </p>
            </div>

            <div className="rounded-2xl bg-amber-50 px-4 py-3 text-sm font-bold text-amber-700">
              Application status: Draft
            </div>
          </div>

          <div className="mt-6 h-2 overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full rounded-full bg-[#08264a] transition-all duration-300"
              style={{ width: `${progress}%` }}
            />
          </div>

          <div className="mt-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
            {steps.map((step, index) => (
              <StepIndicator
                key={step}
                number={index + 1}
                label={step}
                active={currentStep === index}
                completed={currentStep > index}
              />
            ))}
          </div>
        </div>

        <div className="px-5 py-6 sm:px-8 sm:py-8">
          {currentStep === 0 && (
            <PersonalDetailsStep
              form={form}
              updateField={updateField}
            />
          )}

          {currentStep === 1 && (
            <LicenceDetailsStep
              form={form}
              updateField={updateField}
            />
          )}

          {currentStep === 2 && (
            <DocumentsStep
              form={form}
              updateField={updateField}
            />
          )}

          {currentStep === 3 && (
            <ReviewStep
              form={form}
              fullName={fullName}
              email={email}
              updateField={updateField}
            />
          )}

          {error && (
            <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">
              {error}
            </div>
          )}

          <div className="mt-8 flex flex-col-reverse gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:items-center sm:justify-between">
            <button
              type="button"
              onClick={
                currentStep === 0
                  ? () => navigate("/register")
                  : goBack
              }
              disabled={submitting}
              className="flex items-center justify-center gap-2 rounded-2xl border border-slate-300 px-6 py-3 text-sm font-black text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
            >
              <ArrowLeft size={18} />
              {currentStep === 0 ? "Back to Register" : "Previous"}
            </button>

            {currentStep < steps.length - 1 ? (
              <button
                type="button"
                onClick={goNext}
                className="flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-7 py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
              >
                Continue
                <ArrowRight size={18} />
              </button>
            ) : (
              <button
                type="button"
                onClick={handleSubmit}
                disabled={submitting}
                className="flex items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-7 py-3 text-sm font-black text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-70"
              >
                {submitting ? (
                  "Submitting Application..."
                ) : (
                  <>
                    <ShieldCheck size={18} />
                    Submit Application
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      </div>
    </AuthLayout>
  );
}

function PersonalDetailsStep({ form, updateField }) {
  return (
    <StepSection
      icon={<User size={23} />}
      title="Personal Information"
      description="Provide your address and emergency contact information."
    >
      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        <Field
          label="Date of Birth"
          icon={<Calendar size={18} />}
        >
          <input
            type="date"
            value={form.dateOfBirth}
            onChange={(event) =>
              updateField("dateOfBirth", event.target.value)
            }
            className="field-input"
          />
        </Field>

        <Field
          label="Emergency Contact Name"
          icon={<User size={18} />}
        >
          <input
            type="text"
            value={form.emergencyContactName}
            onChange={(event) =>
              updateField(
                "emergencyContactName",
                event.target.value
              )
            }
            placeholder="Full name"
            className="field-input"
          />
        </Field>

        <Field
          label="Emergency Contact Phone"
          icon={<Phone size={18} />}
        >
          <input
            type="tel"
            value={form.emergencyContactPhone}
            onChange={(event) =>
              updateField(
                "emergencyContactPhone",
                event.target.value
              )
            }
            placeholder="+977 98XXXXXXXX"
            className="field-input"
          />
        </Field>

        <div className="hidden md:block" />

        <Field
          label="Permanent Address"
          icon={<MapPin size={18} />}
        >
          <textarea
            value={form.permanentAddress}
            onChange={(event) =>
              updateField(
                "permanentAddress",
                event.target.value
              )
            }
            placeholder="Permanent address"
            rows={3}
            className="field-input resize-none"
          />
        </Field>

        <Field
          label="Current Address"
          icon={<MapPin size={18} />}
        >
          <textarea
            value={form.currentAddress}
            onChange={(event) =>
              updateField("currentAddress", event.target.value)
            }
            placeholder="Current address"
            rows={3}
            className="field-input resize-none"
          />
        </Field>
      </div>
    </StepSection>
  );
}

function LicenceDetailsStep({ form, updateField }) {
  return (
    <StepSection
      icon={<BadgeCheck size={23} />}
      title="Licence and Identity Information"
      description="Enter information exactly as shown on your official documents."
    >
      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        <Field
          label="Citizenship Number"
          icon={<FileText size={18} />}
        >
          <input
            type="text"
            value={form.citizenshipNumber}
            onChange={(event) =>
              updateField(
                "citizenshipNumber",
                event.target.value
              )
            }
            placeholder="Citizenship number"
            className="field-input"
          />
        </Field>

        <Field
          label="Driving Licence Number"
          icon={<BadgeCheck size={18} />}
        >
          <input
            type="text"
            value={form.licenseNumber}
            onChange={(event) =>
              updateField("licenseNumber", event.target.value)
            }
            placeholder="Licence number"
            className="field-input"
          />
        </Field>

        <Field
          label="Licence Category"
          icon={<BadgeCheck size={18} />}
        >
          <select
            value={form.licenseCategory}
            onChange={(event) =>
              updateField("licenseCategory", event.target.value)
            }
            className="field-input"
          >
            <option value="">Select category</option>
            <option value="D">Category D — Heavy vehicle</option>
            <option value="E">Category E — Heavy equipment</option>
            <option value="F">Category F — Minibus / minitruck</option>
            <option value="OTHER">Other applicable category</option>
          </select>
        </Field>

        <Field
          label="Years of Driving Experience"
          icon={<User size={18} />}
        >
          <input
            type="number"
            min="0"
            max="60"
            value={form.yearsOfExperience}
            onChange={(event) =>
              updateField(
                "yearsOfExperience",
                event.target.value
              )
            }
            placeholder="Example: 5"
            className="field-input"
          />
        </Field>

        <Field
          label="Licence Issue Date"
          icon={<Calendar size={18} />}
        >
          <input
            type="date"
            value={form.licenseIssueDate}
            onChange={(event) =>
              updateField(
                "licenseIssueDate",
                event.target.value
              )
            }
            className="field-input"
          />
        </Field>

        <Field
          label="Licence Expiry Date"
          icon={<Calendar size={18} />}
        >
          <input
            type="date"
            value={form.licenseExpiryDate}
            onChange={(event) =>
              updateField(
                "licenseExpiryDate",
                event.target.value
              )
            }
            className="field-input"
          />
        </Field>

        <Field
          label="Preferred Operating Area"
          icon={<MapPin size={18} />}
        >
          <input
            type="text"
            value={form.preferredOperatingArea}
            onChange={(event) =>
              updateField(
                "preferredOperatingArea",
                event.target.value
              )
            }
            placeholder="Example: Kathmandu Valley"
            className="field-input"
          />
        </Field>

        <Field
          label="Application Note"
          icon={<FileText size={18} />}
        >
          <textarea
            rows={3}
            value={form.applicationNote}
            onChange={(event) =>
              updateField("applicationNote", event.target.value)
            }
            placeholder="Optional information for the reviewing admin"
            className="field-input resize-none"
          />
        </Field>
      </div>
    </StepSection>
  );
}

function DocumentsStep({ form, updateField }) {
  return (
    <StepSection
      icon={<Upload size={23} />}
      title="Required Documents"
      description="Upload clear JPG, PNG, or PDF copies. Each file should be readable."
    >
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <DocumentUpload
          label="Profile Photo"
          file={form.profilePhoto}
          accept="image/png,image/jpeg"
          onChange={(file) => updateField("profilePhoto", file)}
        />

        <DocumentUpload
          label="Citizenship Front"
          file={form.citizenshipFront}
          onChange={(file) =>
            updateField("citizenshipFront", file)
          }
        />

        <DocumentUpload
          label="Citizenship Back"
          file={form.citizenshipBack}
          onChange={(file) =>
            updateField("citizenshipBack", file)
          }
        />

        <DocumentUpload
          label="Licence Front"
          file={form.licenseFront}
          onChange={(file) => updateField("licenseFront", file)}
        />

        <DocumentUpload
          label="Licence Back"
          file={form.licenseBack}
          onChange={(file) => updateField("licenseBack", file)}
        />
      </div>

      <div className="mt-5 rounded-2xl border border-blue-200 bg-blue-50 p-4 text-sm leading-6 text-blue-700">
        Your documents will only be used for driver verification.
        Approval is completed by the Yatayat administrator.
      </div>
    </StepSection>
  );
}

function ReviewStep({
  form,
  fullName,
  email,
  updateField,
}) {
  return (
    <StepSection
      icon={<CheckCircle2 size={23} />}
      title="Review and Submit"
      description="Check your details carefully before submitting for admin review."
    >
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <ReviewCard label="Applicant" value={fullName} />
        <ReviewCard label="Email" value={email} />
        <ReviewCard
          label="Date of Birth"
          value={form.dateOfBirth}
        />
        <ReviewCard
          label="Emergency Contact"
          value={`${form.emergencyContactName} · ${form.emergencyContactPhone}`}
        />
        <ReviewCard
          label="Citizenship Number"
          value={form.citizenshipNumber}
        />
        <ReviewCard
          label="Licence Number"
          value={form.licenseNumber}
        />
        <ReviewCard
          label="Licence Category"
          value={form.licenseCategory}
        />
        <ReviewCard
          label="Licence Validity"
          value={`${form.licenseIssueDate} to ${form.licenseExpiryDate}`}
        />
        <ReviewCard
          label="Driving Experience"
          value={`${form.yearsOfExperience} year(s)`}
        />
        <ReviewCard
          label="Operating Area"
          value={form.preferredOperatingArea || "Not specified"}
        />
      </div>

      <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-5">
        <p className="text-sm font-black text-slate-900">
          Uploaded documents
        </p>

        <div className="mt-3 grid grid-cols-1 gap-2 text-sm text-slate-600 sm:grid-cols-2">
          <DocumentName label="Profile photo" file={form.profilePhoto} />
          <DocumentName
            label="Citizenship front"
            file={form.citizenshipFront}
          />
          <DocumentName
            label="Citizenship back"
            file={form.citizenshipBack}
          />
          <DocumentName
            label="Licence front"
            file={form.licenseFront}
          />
          <DocumentName
            label="Licence back"
            file={form.licenseBack}
          />
        </div>
      </div>

      <label className="mt-6 flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm leading-6 text-emerald-800">
        <input
          type="checkbox"
          checked={form.declarationAccepted}
          onChange={(event) =>
            updateField(
              "declarationAccepted",
              event.target.checked
            )
          }
          className="mt-1 h-4 w-4"
        />

        <span>
          I declare that the information and documents provided are
          genuine. I understand that false information may result in
          rejection or suspension.
        </span>
      </label>
    </StepSection>
  );
}

function StepSection({
  icon,
  title,
  description,
  children,
}) {
  return (
    <section>
      <div className="mb-6 flex items-start gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#08264a] text-white">
          {icon}
        </div>

        <div>
          <h2 className="text-xl font-black text-slate-900 sm:text-2xl">
            {title}
          </h2>

          <p className="mt-1 text-sm leading-6 text-slate-500">
            {description}
          </p>
        </div>
      </div>

      {children}
    </section>
  );
}

function StepIndicator({
  number,
  label,
  active,
  completed,
}) {
  return (
    <div
      className={`rounded-2xl border p-3 transition ${
        active
          ? "border-[#08264a] bg-blue-50"
          : completed
          ? "border-emerald-200 bg-emerald-50"
          : "border-slate-200 bg-slate-50"
      }`}
    >
      <div className="flex items-center gap-3">
        <span
          className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-black ${
            active
              ? "bg-[#08264a] text-white"
              : completed
              ? "bg-emerald-600 text-white"
              : "bg-slate-200 text-slate-600"
          }`}
        >
          {completed ? <CheckCircle2 size={16} /> : number}
        </span>

        <p className="text-xs font-black text-slate-700">
          {label}
        </p>
      </div>
    </div>
  );
}

function Field({ label, icon, children }) {
  return (
    <div>
      <label className="mb-2 block text-xs font-black uppercase tracking-wider text-slate-500">
        {label}
      </label>

      <div className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-400 focus-within:border-[#08264a] focus-within:bg-white">
        <span className="mt-0.5 shrink-0">{icon}</span>
        {children}
      </div>
    </div>
  );
}

function DocumentUpload({
  label,
  file,
  onChange,
  accept = "image/png,image/jpeg,application/pdf",
}) {
  return (
    <label className="cursor-pointer rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-5 transition hover:border-[#08264a] hover:bg-blue-50">
      <input
        type="file"
        accept={accept}
        onChange={(event) =>
          onChange(event.target.files?.[0] || null)
        }
        className="hidden"
      />

      <Upload size={25} className="text-[#08264a]" />

      <p className="mt-3 text-sm font-black text-slate-900">
        {label}
      </p>

      <p className="mt-1 truncate text-xs text-slate-500">
        {file ? file.name : "Click to select a file"}
      </p>
    </label>
  );
}

function ReviewCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <p className="text-xs font-black uppercase tracking-wider text-slate-500">
        {label}
      </p>

      <p className="mt-2 wrap-break-word text-sm font-black text-slate-900">
        {value || "Not provided"}
      </p>
    </div>
  );
}

function DocumentName({ label, file }) {
  return (
    <p className="truncate">
      <span className="font-bold text-slate-900">{label}:</span>{" "}
      {file?.name || "Missing"}
    </p>
  );
}
