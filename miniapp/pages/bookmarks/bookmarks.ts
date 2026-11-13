import { request } from "../../utils/request";
import { postStatus } from "../../utils/status";
import { formatTime, mediaUrl } from "../../utils/format";
import type { BookmarkListData, BookmarkView } from "../../types/api";

interface BookmarkItem extends BookmarkView {
  statusLabel: string;
  timeLabel: string;
  inactive: boolean;
  cover: string;
  glyph: string;
}

Page({
  data: {
    items: [] as BookmarkItem[],
  },

  onShow() {
    this.load();
  },

  async load() {
    try {
      const data = await request<BookmarkListData>({ url: "/me/bookmarks" });
      this.setData({
        items: data.items.map((b) => ({
          ...b,
          statusLabel: postStatus(b.post.status),
          timeLabel: formatTime(b.createdAt),
          inactive: b.post.status === "REMOVED" || b.post.status === "CLOSED",
          cover: mediaUrl((b.post.images || [])[0]?.url),
          glyph: (b.post.category || "物").slice(0, 1),
        })),
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  async onUnbookmark(e: any) {
    const id = e.currentTarget.dataset.id;
    try {
      await request<Record<string, never>>({ url: `/posts/${id}/bookmark`, method: "DELETE" });
      wx.showToast({ title: "已取消收藏", icon: "none" });
      this.load();
    } catch (err: any) {
      wx.showToast({ title: err.message, icon: "none" });
    }
  },

  goDetail(e: any) {
    const { id, inactive } = e.currentTarget.dataset;
    if (inactive) {
      wx.showToast({ title: "该信息已下架或关闭", icon: "none" });
      return;
    }
    wx.navigateTo({ url: "/pages/detail/detail?id=" + id });
  },
});
