import { useEffect, useState } from "react";
import {
  Wallet,
  Bus,
  Ticket,
  ArrowLeft,
  Lock,
  ShieldCheck,
  CreditCard,
  CheckCircle,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import PassengerLayout from "../../components/layout/PassengerLayout";

export default function PaymentPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const user = JSON.parse(localStorage.getItem("yatayatUser"));
  const booking = location.state;

  const [walletBalance, setWalletBalance] = useState(0);
  const [walletPin, setWalletPin] = useState("");
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    const fetchBalance = async () => {
      try {
        const res = await fetch(
          `http://localhost:8080/api/wallet/balance/${user.id}`,
          { credentials: "include" }
        );
        const data = await res.text();
        setWalletBalance(Number(data));
      } catch (error) {
        console.error(error);
        toast.error("Failed to load wallet balance");
      }
    };

    if (user?.id) fetchBalance();
  }, []);

  if (!booking?.bookingData) {
    return (
      <PassengerLayout activePage="Out-of-Valley">
        <div className="rounded-2xl bg-white p-6 text-center shadow-sm">
          <h1 className="text-2xl font-black text-red-600">
            No booking data found
          </h1>
          <button
            onClick={() => navigate("/book-ticket")}
            className="mt-4 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-bold text-white"
          >
            Go Back to Booking
          </button>
        </div>
      </PassengerLayout>
    );
  }

  const { bookingData, passenger, bus } = booking;
  const totalAmount = bookingData.fare;

  const handlePinChange = (index, value) => {
    if (!/^\d?$/.test(value)) return;

    const pinArray = walletPin.padEnd(4, "").split("");
    pinArray[index] = value;

    const newPin = pinArray.join("").trim();
    setWalletPin(newPin);

    if (value && index < 3) {
      document.getElementById(`wallet-pin-${index + 1}`)?.focus();
    }
  };

  const handlePayment = async () => {
    if (walletPin.length !== 4) {
      toast.error("Please enter your 4-digit wallet PIN");
      return;
    }
  
    if (walletBalance < totalAmount) {
      toast.error("Insufficient wallet balance");
      return;
    }
  
    setProcessing(true);
  
    try {
      const res = await fetch("http://localhost:8080/api/bookings/create", {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          userId: user.id,
          routeName: bookingData.routeName,
          busNumber: bookingData.busNumber,
          seatNumber: bookingData.seatNumber,
          travelDate: bookingData.travelDate,
          departureTime: bookingData.departureTime,
          fare: bookingData.fare,
          walletPin: walletPin,
        }),
      });
  
      const result = await res.json();
  
      if (!result.success) {
        toast.error(result.message);
        setProcessing(false);
        return;
      }
  
      const createdBooking = result.booking;
  
      toast.success(result.message);
  
      setTimeout(() => {
        navigate("/ticket", {
          state: {
            booking: createdBooking,
            passenger,
            bus,
          },
        });
      }, 1000);
    } catch (error) {
      console.error(error);
      toast.error("Payment failed");
      setProcessing(false);
    }
  };

  return (
    <PassengerLayout activePage="Out-of-Valley">
      <button
        onClick={() => navigate("/booking-summary", { state: booking })}
        className="mb-5 flex items-center gap-2 text-sm font-black text-slate-600 hover:text-[#08264a]"
      >
        <ArrowLeft size={17} />
        Back to summary
      </button>

      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Secure Checkout
          </h1>
          <p className="mt-1 flex items-center gap-2 text-sm text-slate-600">
            <Lock size={16} className="text-emerald-700" />
            Pay using your Yatayat Wallet.
          </p>
        </div>

        <div className="rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white">
          Pay NPR {totalAmount.toLocaleString()}
        </div>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-7">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              Wallet Payment
            </h2>

            <div className="mt-5 rounded-2xl bg-[#08264a] p-5 text-white">
              <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                Available Wallet Balance
              </p>
              <h3 className="mt-2 text-4xl font-black">
                NPR{" "}
                {walletBalance.toLocaleString(undefined, {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </h3>
            </div>

            <div className="mt-5 rounded-xl bg-slate-50 p-4 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-500">Trip Amount</span>
                <span className="font-black">
                  NPR {totalAmount.toLocaleString()}
                </span>
              </div>

              <div className="mt-3 border-t border-slate-200 pt-3 flex justify-between">
                <span className="font-black">Remaining Balance</span>
                <span
                  className={`font-black ${
                    walletBalance - totalAmount < 0
                      ? "text-red-600"
                      : "text-emerald-700"
                  }`}
                >
                  NPR {(walletBalance - totalAmount).toLocaleString()}
                </span>
              </div>
            </div>

            <div className="mt-6">
              <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                Enter 4-Digit Wallet PIN
              </label>

              <div className="mt-4 flex gap-3">
                {[0, 1, 2, 3].map((index) => (
                  <input
                    key={index}
                    id={`wallet-pin-${index}`}
                    type="password"
                    inputMode="numeric"
                    maxLength={1}
                    value={walletPin[index] || ""}
                    onChange={(e) => handlePinChange(index, e.target.value)}
                    className="h-14 w-14 rounded-xl border border-slate-300 bg-slate-50 text-center text-2xl font-black outline-none focus:border-[#08264a]"
                  />
                ))}
              </div>
            </div>

            <button
              onClick={handlePayment}
              disabled={processing}
              className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-4 text-sm font-black text-white transition hover:bg-[#0d3566] disabled:opacity-70"
            >
              {processing ? (
                <>
                  <CheckCircle size={18} />
                  Processing Payment...
                </>
              ) : (
                <>
                  <CreditCard size={18} />
                  Confirm Payment
                </>
              )}
            </button>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <ShieldCheck size={21} />
              </div>

              <div>
                <h3 className="font-black text-slate-900">
                  Security Guarantee
                </h3>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  This payment is deducted from your Yatayat Wallet. After
                  successful payment, your booking is saved and a QR ticket is
                  generated.
                </p>
              </div>
            </div>
          </div>
        </section>

        <aside className="xl:col-span-5">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              Booking Summary
            </h2>

            <div className="mt-5 space-y-4">
              <SummaryBlock label="Route" value={bookingData.routeName} />
              <SummaryBlock
                label="Departure"
                value={`${bookingData.travelDate} • ${bookingData.departureTime}`}
              />
              <SummaryBlock label="Bus" value={bus?.name || "Yatayat Bus"} />
              <SummaryBlock label="Seat" value={bookingData.seatNumber} />
              <SummaryBlock label="Email Ticket To" value={passenger?.email} />
            </div>

            <div className="my-5 border-t border-dashed border-slate-300"></div>

            <div className="flex justify-between">
              <span className="text-lg font-black">Total</span>
              <span className="text-xl font-black text-emerald-700">
                NPR {totalAmount.toLocaleString()}
              </span>
            </div>

            <p className="mt-4 text-center text-xs leading-5 text-slate-500">
              After successful payment, your ticket and QR code will be created.
            </p>
          </div>
        </aside>
      </div>
    </PassengerLayout>
  );
}

function SummaryBlock({ label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 p-4">
      <p className="text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </p>
      <p className="mt-1 font-black text-slate-900">{value}</p>
    </div>
  );
}
