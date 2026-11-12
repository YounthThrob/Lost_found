import { request } from "../../utils/request";
import { claimStatus } from "../../utils/status";
import { formatTime } from "../../utils/format";
import type { ClaimListData, ClaimView } from "../../types/api";

const REPORT_REASONS = ["虚假申请", "骚扰", "违规内容", "其他"];

interface ClaimItem extends ClaimView {
  statusLabel: string;
  timeLabel: string;
}

Page({
  data: {
    postId: "",
    items: [] as ClaimItem[],
    showReport: false,
    reportReasons: REPORT_REASONS,
    reportReason: REPORT_REASONS[0],
    reportTargetId: "",
    submitting: false,
  },

  onLoad(options: any) {
    this.setData({ postId: options.postId || "" });
  },

  onShow() {
    this.load();
  },

  async load() {
    try {
      const data = await request<ClaimListData>({ url: `/posts/${this.data.postId}/claims` });
      this.setData({
        items: data.items.map((c) => ({ ...c, statusLabel: claimStatus(c.status), timeLabel: formatTime(c.createdAt) })),
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  async act(e: any) {
    const { id, action } = e.currentTarget.dataset;
    const label = action === "accept" ? "通过" : action === "reject" ? "拒绝" : "请求补充";
    const res = await new Promise<boolean>((r) =>
      wx.showModal({ title: "确认操作", content: `确定${label}该申请？`, success: (m) => r(m.confirm), fail: () => r(false) })
    );
    if (!res) return;
    try {
      await request<ClaimView>({ url: `/claims/${id}/${action}`, method: "POST" });
      wx.showToast({ title: `已${label}`, icon: "success" });
      this.load();
    } catch (err: any) {
      wx.showToast({ title: err.message, icon: "none" });
    }
  },

  goMessages(e: any) {
    const { id, peer } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/messages/messages?conversationId=${id}&peerId=${peer}` });
  },

  goHandover(e: any) {
    wx.navigateTo({ url: `/pages/handover/handover?claimId=${e.currentTarget.dataset.id}` });
  },

  openReport(e: any) {
    this.setData({ showReport: true, reportTargetId: e.currentTarget.dataset.applicant, reportReason: REPORT_REASONS[0] });
  },

  closeReport() {
    this.setData({ showReport: false });
  },

  onReportReason(e: any) {
    this.setData({ reportReason: REPORT_REASONS[Number(e.detail.value)] });
  },

  async submitReport() {
    this.setData({ submitting: true });
    try {
      await request<unknown>({
        url: "/reports",
        method: "POST",
        data: { targetType: "USER", targetId: this.data.reportTargetId, reason: this.data.reportReason },
      });
      this.setData({ showReport: false });
      wx.showToast({ title: "举报已提交", icon: "success" });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
