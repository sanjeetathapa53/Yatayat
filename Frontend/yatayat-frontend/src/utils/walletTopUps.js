import { apiFetch } from "./api";

async function walletTopUpRequest(path, options = {}) {
  const response = await apiFetch(path, options);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json().catch(() => null)
    : null;
  if (!response.ok) {
    const error = new Error(data?.message || fallback(response.status));
    error.status = response.status;
    throw error;
  }
  return data;
}

export const initiateWalletTopUp = (provider, amount) => walletTopUpRequest(
  `/api/wallet/topups/${provider.toLowerCase()}/initiate`,
  {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount }),
  },
);

export const verifyKhaltiWalletTopUp = (reference, pidx) => walletTopUpRequest(
  `/api/wallet/topups/${encodeURIComponent(reference)}/khalti/verify`,
  {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ pidx }),
  },
);

export const verifyEsewaWalletTopUp = (reference, transactionUuid, data) =>
  walletTopUpRequest(
    `/api/wallet/topups/${encodeURIComponent(reference)}/esewa/verify`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ transactionUuid, data }),
    },
  );

export function submitEsewaWalletTopUp(initiation) {
  const form = document.createElement("form");
  form.method = "POST";
  form.action = initiation.formAction;
  Object.entries(initiation.formFields || {}).forEach(([name, value]) => {
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = value;
    form.appendChild(input);
  });
  document.body.appendChild(form);
  form.submit();
}

function fallback(status) {
  if (status === 400) return "Check the top-up amount and try again.";
  if (status === 401) return "Your session has expired. Please sign in again.";
  if (status === 403) return "Passenger access is required.";
  if (status === 404) return "Wallet top-up not found.";
  if (status === 409) return "This wallet top-up cannot be completed.";
  if (status === 503) return "The selected payment provider is unavailable.";
  return "Unable to process the wallet top-up.";
}
