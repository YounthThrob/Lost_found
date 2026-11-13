import { request } from "../../utils/request";
import { postStatus } from "../../utils/status";
import { formatTime, mediaUrl } from "../../utils/format";
import type { ItemPostListData, ItemPostView } from "../../types/api";

interface PostItem extends ItemPostView {
  statusLabel: string;
  timeLabel: string;
  cover: string;
  glyph: string;
}

Page({
  data: {
    items: [] as PostItem[],
  },

  onShow() {
    this.load();
  },

  async load() {
    try {
      const data = await request<ItemPostListData>({ url: "/me/posts" });
      this.setData({
        items: data.items.map((p) => ({
          ...p,
          statusLabel: postStatus(p.status),
          timeLabel: formatTime(p.createdAt),
          cover: mediaUrl((p.images || [])[0]?.url),
          glyph: (p.category || "物").slice(0, 1),
        })),
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  goDetail(e: any) {
    wx.navigateTo({ url: "/pages/detail/detail?id=" + e.currentTarget.dataset.id });
  },

  goEdit(e: any) {
    wx.navigateTo({ url: "/pages/publish/publish?id=" + e.currentTarget.dataset.id });
  },

  goClaims(e: any) {
    wx.navigateTo({ url: "/pages/claims/claims?postId=" + e.currentTarget.dataset.id });
  },

  async onClose(e: any) {
    const id = e.currentTarget.dataset.id;
    const res = await new Promise<boolean>((r) =>
      wx.showModal({ title: "关闭信息", content: "关闭后将不再公开展示，确定？", success: (m) => r(m.confirm), fail: () => r(false) })
    );
    if (!res) return;
    try {
      await request<ItemPostView>({ url: `/posts/${id}/close`, method: "POST" });
      wx.showToast({ title: "已关闭", icon: "success" });
      this.load();
    } catch (err: any) {
      wx.showToast({ title: err.message, icon: "none" });
    }
  },
});
