import { request } from "../../utils/request";
import { claimStatus } from "../../utils/status";
import { formatTime } from "../../utils/format";
import type { ClaimListData, ClaimView } from "../../types/api";

interface ClaimItem extends ClaimView {
  statusLabel: string;
  timeLabel: string;
}

Page({
  data: {
    items: [] as ClaimItem[],
  },

  onShow() {
    this.load();
  },

  async load() {
    try {
      const data = await request<ClaimListData>({ url: "/me/claims" });
      this.setData({
        items: data.items.map((c) => ({ ...c, statusLabel: claimStatus(c.status), timeLabel: formatTime(c.createdAt) })),
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  goDetail(e: any) {
    wx.navigateTo({ url: "/pages/detail/detail?id=" + e.currentTarget.dataset.post });
  },

  goHandover(e: any) {
    wx.navigateTo({ url: "/pages/handover/handover?claimId=" + e.currentTarget.dataset.id });
  },

  goMessages(e: any) {
    wx.navigateTo({ url: "/pages/messages/messages?conversationId=" + e.currentTarget.dataset.id });
  },
});
