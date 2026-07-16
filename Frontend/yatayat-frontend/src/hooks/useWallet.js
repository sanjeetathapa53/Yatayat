import { useEffect, useState } from "react";

export default function useWallet() {
  const user = JSON.parse(localStorage.getItem("yatayatUser"));

  const [balance, setBalance] = useState(0);
  const [transactions, setTransactions] = useState([]);

  const loadWallet = async () => {
    if (!user?.id) return;

    const balanceRes = await fetch(
      `http://localhost:8080/api/wallet/balance/${user.id}`,
      { credentials: "include" }
    );
    const balanceText = await balanceRes.text();
    setBalance(Number(balanceText));

    const historyRes = await fetch(
      `http://localhost:8080/api/wallet/history/${user.id}`,
      { credentials: "include" }
    );
    const historyData = await historyRes.json();
    setTransactions(historyData);
  };

  const topUpWallet = async (amount, paymentMethod) => {
    if (!user?.id || !amount || amount <= 0) return false;

    const res = await fetch("http://localhost:8080/api/wallet/topup", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        userId: user.id,
        amount,
        paymentMethod,
      }),
    });

    const text = await res.text();

    if (text === "Wallet topped up successfully") {
      await loadWallet();
      return true;
    }

    return false;
  };

  useEffect(() => {
    loadWallet();
  }, []);

  return {
    balance,
    transactions,
    loadWallet,
    topUpWallet,
  };
}
