import { request } from "../../utils/request";
import { isLoggedIn } from "../../utils/auth";
import type { BookmarkListData, CampusProfileView, ClaimListData, ItemPostListData, MeView } from "../../types/api";

const ARCHIVE_MENU = [
  { label: "自报学校与校区", desc: "设置公开的校园范围", url: "/pages/campus/campus", icon: "/assets/ui/my-school.png", tone: "green", code: "01" },
  { label: "黑名单", desc: "管理已屏蔽的用户", url: "/pages/blocks/blocks", icon: "/assets/ui/my-block.png", tone: "muted", code: "02" },
  { label: "举报记录", desc: "查看提交状态与处理结果", url: "/pages/reports/reports", icon: "/assets/ui/my-report.png", tone: "orange", code: "03" },
  { label: "设置与说明", desc: "隐私、协议与平台边界", url: "/pages/settings/settings", icon: "/assets/ui/my-settings.png", tone: "gray", code: "04" },
];

function countLabel(total: number): string {
  return Math.max(0, total).toString().padStart(2, "0");
}

Page({
  data: {
    statusBarHeight: 47,
    archiveMenu: ARCHIVE_MENU,
    restricted: false,
    school: "",
    verified: false,
    postCount: "00",
    claimCount: "00",
    bookmarkCount: "00",
  },

  onShow() {
    wx.hideTabBar({ animation: false });
    if (!isLoggedIn()) {
      wx.showModal({
        title: "需要登录",
        content: "登录后才能查看「我的」",
        confirmText: "去登录",
        cancelText: "取消",
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: "/pages/welcome/welcome" });
          } else {
            wx.showTabBar({ animation: false });
            wx.switchTab({ url: "/pages/index/index" });
          }
        },
      });
      return;
    }
    request<MeView>({ url: "/me" })
      .then((me) => this.setData({ restricted: me.status === "RESTRICTED" }))
      .catch(() => {});
    request<CampusProfileView>({ url: "/me/campus-profile" })
      .then((p) => this.setData({ school: p.school || "", verified: p.verificationStatus === "EMAIL_VERIFIED" }))
      .catch(() => {});
    this.loadActivityCounts();
  },

  async loadActivityCounts() {
    const [posts, claims, bookmarks] = await Promise.allSettled([
      request<ItemPostListData>({ url: "/me/posts", data: { page: 1, pageSize: 1 } }),
      request<ClaimListData>({ url: "/me/claims", data: { page: 1, pageSize: 1 } }),
      request<BookmarkListData>({ url: "/me/bookmarks", data: { page: 1, pageSize: 1 } }),
    ]);
    const next: Record<string, string> = {};
    if (posts.status === "fulfilled") next.postCount = countLabel(posts.value.pagination.total);
    if (claims.status === "fulfilled") next.claimCount = countLabel(claims.value.pagination.total);
    if (bookmarks.status === "fulfilled") next.bookmarkCount = countLabel(bookmarks.value.pagination.total);
    this.setData(next);
  },

  nav(e: any) { wx.navigateTo({ url: e.currentTarget.dataset.url }); },
  goCampus() { wx.navigateTo({ url: "/pages/campus/campus" }); },
  goHome() {
    wx.showTabBar({ animation: false });
    wx.switchTab({ url: "/pages/index/index" });
  },
});
