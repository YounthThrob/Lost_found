// 平台会话 token 的本地存储封装（与登录逻辑解耦，避免循环依赖）。

const TOKEN_KEY = "lf_token";

export function getToken(): string {
  try {
    return wx.getStorageSync(TOKEN_KEY) || "";
  } catch {
    return "";
  }
}

export function setToken(token: string): void {
  wx.setStorageSync(TOKEN_KEY, token);
}

export function clearToken(): void {
  wx.removeStorageSync(TOKEN_KEY);
}
