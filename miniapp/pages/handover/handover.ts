import { request } from "../../utils/request";
import { handoverStatus } from "../../utils/status";
import { formatTime } from "../../utils/format";
import type { HandoverView } from "../../types/api";

interface HandoverItem extends HandoverView {
  statusLabel: string;
  timeLabel: string;
}

Page({
  data: {
    claimId: "",
    timeWindow: "",
    publicPlace: "",
    handover: null as HandoverItem | null,
    submitting: false,
  },

  onLoad(options: any) {
    this.setData({ claimId: options.claimId || "" });
  },

  onInput(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value } as any);
  },

  decorate(h: HandoverView): HandoverItem {
    return { ...h, statusLabel: handoverStatus(h.status), timeLabel: formatTime(h.createdAt) };
  },

  async create() {
    const timeWindow = this.data.timeWindow.trim();
    const publicPlace = this.data.publicPlace.trim();
    if (!timeWindow || !publicPlace) {
      wx.showToast({ title: "请填写时间窗口与公共区域", icon: "none" });
      return;
    }
    this.setData({ submitting: true });
    try {
      const h = await request<HandoverView>({
        url: "/handovers",
        method: "POST",
        data: { claimId: this.data.claimId, timeWindow, publicPlace },
      });
      this.setData({ handover: this.decorate(h) });
      wx.showToast({ title: "交接安排已创建", icon: "success" });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },

  async act(e: any) {
    const action = e.currentTarget.dataset.action;
    const id = this.data.handover?.id;
    if (!id) return;
    const label = action === "confirm" ? "确认完成" : "终止交接";
    const res = await new Promise<boolean>((r) =>
      wx.showModal({ title: "确认操作", content: `确定${label}？`, success: (m) => r(m.confirm), fail: () => r(false) })
    );
    if (!res) return;
    try {
      const h = await request<HandoverView>({ url: `/handovers/${id}/${action}`, method: "POST" });
      this.setData({ handover: this.decorate(h) });
      wx.showToast({ title: label, icon: "success" });
    } catch (err: any) {
      wx.showToast({ title: err.message, icon: "none" });
    }
  },
});
