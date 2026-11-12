import { request } from "../../utils/request";
import { formatTime } from "../../utils/format";
import type { BlockListData, BlockView } from "../../types/api";

interface BlockItem extends BlockView {
  timeLabel: string;
}

Page({
  data: {
    items: [] as BlockItem[],
  },

  onShow() {
    this.load();
  },

  async load() {
    try {
      const data = await request<BlockListData>({ url: "/me/blocks" });
      this.setData({
        items: data.items.map((b) => ({ ...b, timeLabel: formatTime(b.createdAt) })),
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  async onUnblock(e: any) {
    const id = e.currentTarget.dataset.id;
    try {
      await request<Record<string, never>>({ url: `/blocks/${id}`, method: "DELETE" });
      wx.showToast({ title: "已解除拉黑", icon: "success" });
      this.load();
    } catch (err: any) {
      wx.showToast({ title: err.message, icon: "none" });
    }
  },
});
