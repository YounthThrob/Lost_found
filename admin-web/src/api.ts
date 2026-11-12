import { API_BASE, TOKEN_KEY } from "./config";

export interface ApiError {
  code: string;
  message: string;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
}

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

// 统一请求封装：成功返回 data 字段；失败抛 ApiError（含业务错误码）。
export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const token = getToken();
  const res = await fetch(API_BASE + url, {
    method: options.method || "GET",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });
  const data = await res.json().catch(() => null);
  if (res.ok && data && !data.error) {
    return data.data as T;
  }
  const err: ApiError =
    data && data.error
      ? { code: data.error.code, message: data.error.message }
      : { code: String(res.status), message: "请求失败" };
  if (res.status === 401 || err.code === "SESSION_EXPIRED") {
    clearToken();
  }
  throw err;
}
