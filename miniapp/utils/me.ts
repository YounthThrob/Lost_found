import type { MeView } from "../types/api";
import { request } from "./request";

const KEY = "lf_uid";

// 当前用户 id 的本地缓存（用于详情页判断“是否为发布者”等前端展示逻辑）。
export function getUserId(): string {
  try {
    return wx.getStorageSync(KEY) || "";
  } catch {
    return "";
  }
}

export function setUserId(id: string): void {
  wx.setStorageSync(KEY, id);
}

export async function fetchMe(): Promise<MeView> {
  const me = await request<MeView>({ url: "/me" });
  setUserId(me.id);
  return me;
}
