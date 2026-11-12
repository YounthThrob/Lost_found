import { request } from "../../utils/request";
import { formatTime } from "../../utils/format";
import type { ReportListData, ReportView } from "../../types/api";

interface ReportItem extends ReportView {
  timeLabel: string;
  targetLabel: string;
}

Page({
  data: {
    items: [] as ReportItem[],
  },

  onShow() {
    this.load();
  },

  async load() {
    try {
      const data = await request<ReportListData>({ url: "/reports/me" });
      this.setData({
        items: data.items.map((r) => ({
          ...r,
          timeLabel: formatTime(r.createdAt),
          targetLabel: r.targetType === "POST" ? "信息" : "用户",
        })),
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },
});
