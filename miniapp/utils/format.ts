// 时间与展示格式化工具。后端时间为 UTC RFC3339，这里按本地时区展示。

export function formatTime(iso?: string | null): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  const pad = (n: number) => (n < 10 ? "0" + n : String(n));
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function formatDate(iso?: string | null): string {
  return formatTime(iso).slice(0, 10);
}

export function shortId(id?: string): string {
  if (!id) return "";
  return id.length > 8 ? id.slice(0, 8) : id;
}
