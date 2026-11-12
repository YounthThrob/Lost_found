import { request } from "../../utils/request";
import type { MeView } from "../../types/api";

const MENU = [
  { label: "我的发布", url: "/pages/my-posts/my-posts" },
  { label: "我的申请", url: "/pages/my-claims/my-claims" },
  { label: "我的收藏", url: "/pages/bookmarks/bookmarks" },
  { label: "自报学校与校区", url: "/pages/campus/campus" },
  { label: "黑名单", url: "/pages/blocks/blocks" },
  { label: "举报记录", url: "/pages/reports/reports" },
  { label: "设置与说明", url: "/pages/settings/settings" },
];

Page({
  data: {
    menu: MENU,
    restricted: false,
  },

  onShow() {
    request<MeView>({ url: "/me" })
      .then((me) => this.setData({ restricted: me.status === "RESTRICTED" }))
      .catch(() => {});
  },

  nav(e: any) {
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },
});
