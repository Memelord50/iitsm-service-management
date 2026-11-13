let token = null;
export const setToken = (value) => {
  token = value;
};
export const base = import.meta.env.VITE_API_BASE_URL || "";
export async function api(path, method = "GET", body) {
  const multipart = body instanceof FormData;
  const response = await fetch(base + "/api" + path, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(!multipart && body ? { "Content-Type": "application/json" } : {}),
    },
    body: body ? (multipart ? body : JSON.stringify(body)) : undefined,
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `Request failed (${response.status})`);
  }
  if (response.status === 204) return null;
  return response.json();
}
export async function download(id, name) {
  const r = await fetch(base + "/api/attachments/" + id, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!r.ok) throw new Error("Download denied or unavailable");
  const url = URL.createObjectURL(await r.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export const date = (v) =>
  v
    ? new Date(v).toLocaleString(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "—";
export const human = (v) =>
  (v || "")
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/^./, (c) => c.toUpperCase());
export const duration = (seconds) =>
  seconds < 60
    ? `${Math.round(seconds)}s`
    : seconds < 3600
    ? `${Math.round(seconds / 60)}m`
    : `${(seconds / 3600).toFixed(1)}h`;
