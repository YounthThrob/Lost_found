import { MOCK_CODE } from "../config";
import { request } from "./request";
import { getToken, setToken } from "./token";

export interface LoginResult {
  token: string;
  agreementAccepted: boolean;
}

// 获取微信登录 code：开发环境返回 mock code，生产环境走 wx.login。
export function wxLoginCode(): Promise<string> {
  return new Promise<string>((resolve) => {
    if (MOCK_CODE) {
      resolve(MOCK_CODE);
      return;
    }
    wx.login({
      success: (res) => resolve(res.code || ""),
      fail: () => resolve(""),
    });
  });
}

export function isLoggedIn(): boolean {
  return !!getToken();
}

// 写操作前的登录门：已登录返回 true；未登录弹窗提示并跳转登录页，返回 false。
export function ensureLoggedIn(): Promise<boolean> {
  if (isLoggedIn()) return Promise.resolve(true);
  return new Promise<boolean>((resolve) => {
    wx.showModal({
      title: "需要登录",
      content: "登录后才能使用该功能",
      confirmText: "去登录",
      cancelText: "取消",
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: "/pages/welcome/welcome" });
        }
        resolve(false);
      },
      fail: () => resolve(false),
    });
  });
}

export async function login(): Promise<LoginResult> {
  const code = await wxLoginCode();
  const data = await request<LoginResult>({
    url: "/session/wechat",
    method: "POST",
    data: { code },
  });
  setToken(data.token);
  return data;
}

// 确认用户协议（仅内容操作前需要；后端对未确认用户拒绝发布/留言/认领）。
export async function acceptAgreement(): Promise<void> {
  await request<{ agreementAccepted: boolean }>({
    url: "/me/agreement",
    method: "POST",
  });
}
