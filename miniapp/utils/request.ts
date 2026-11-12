import { API_BASE } from "../config";
import { getToken, clearToken } from "./token";

export interface ApiError {
  code: string;
  message: string;
}

interface RequestOptions {
  url: string;
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  data?: unknown;
}

function toError(status: number, body: any): ApiError {
  if (body && body.error && body.error.code) {
    return { code: body.error.code, message: body.error.message || body.error.code };
  }
  return { code: String(status), message: "请求失败，请稍后重试" };
}

// 统一请求封装：成功返回统一响应中的 data 字段；失败 reject 携带业务错误码与文案。
export function request<T>(options: RequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const token = getToken();
    wx.request({
      url: API_BASE + options.url,
      method: options.method || "GET",
      data: options.data,
      header: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: "Bearer " + token } : {}),
      },
      success(res) {
        const status = res.statusCode;
        const body = res.data as any;
        if (status >= 200 && status < 300 && body && !body.error) {
          resolve(body.data as T);
          return;
        }
        const err = toError(status, body);
        // 会话失效：清 token 并回到首次进入页重新登录。
        if (status === 401 || err.code === "SESSION_EXPIRED") {
          clearToken();
          wx.reLaunch({ url: "/pages/welcome/welcome" });
        }
        reject(err);
      },
      fail() {
        reject({ code: "NETWORK", message: "网络异常，请稍后重试" });
      },
    });
  });
}
