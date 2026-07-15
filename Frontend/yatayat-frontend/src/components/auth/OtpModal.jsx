export default function OtpModal({ otp, setOtp, onVerify, onClose }) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
        <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-xl">
          <h2 className="text-2xl font-black text-[#08264a]">Verify OTP</h2>
  
          <p className="mt-2 text-sm text-slate-500">
            Enter the OTP sent to your email.
          </p>
  
          <input
            type="text"
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
            placeholder="Enter OTP"
            className="mt-5 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none"
          />
  
          <button
            type="button"
            onClick={onVerify}
            className="mt-5 w-full rounded-2xl bg-[#08264a] py-3 text-sm font-bold text-white"
          >
            Verify OTP
          </button>
  
          <button
            type="button"
            onClick={onClose}
            className="mt-3 w-full rounded-2xl border border-slate-200 py-3 text-sm font-bold text-slate-600"
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }