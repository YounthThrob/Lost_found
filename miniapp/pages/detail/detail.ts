import { request } from "../../utils/request";
import { ensureLoggedIn } from "../../utils/auth";
import { fetchMe, getUserId } from "../../utils/me";
import { clueStatus, postStatus } from "../../utils/status";
import { formatTime, mediaUrl } from "../../utils/format";
import type { ClueListData, ClueView, ItemPostView } from "../../types/api";

const REPORT_REASONS = ["虚假信息", "泄露隐私", "骚扰广告", "违规内容", "其他"];

interface ClueItem extends ClueView {
  statusLabel: string;
}

Page({
  data: {
    id: "",
    post: null as ItemPostView | null,
    gallery: [] as string[],
    statusLabel: "",
    timeLabel: "",
    isOwner: false,
    bookmarked: false,
    clues: [] as ClueItem[],
    clueInput: "",
    showReport: false,
    reportReasons: REPORT_REASONS,
    reportReason: REPORT_REASONS[0],
    reportDescription: "",
    submitting: false,
  },

  onLoad(options: any) {
    this.setData({ id: options.id || "" });
  },

  onShow() {
    this.refresh();
  },

  async refresh() {
    if (!this.data.id) return;
    try {
      const post = await request<ItemPostView>({ url: "/posts/" + this.data.id });
      let uid = getUserId();
      if (!uid) {
        try {
          await fetchMe();
          uid = getUserId();
        } catch {
          /* 忽略 */
        }
      }
      this.setData({
        post,
        gallery: (post.images || []).map((img) => mediaUrl(img.url)).filter(Boolean),
        statusLabel: postStatus(post.status),
        timeLabel: formatTime(post.createdAt),
        isOwner: uid === post.authorId,
      });
      if (uid === post.authorId && post.type === "LOST") {
        await this.loadClues();
      }
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  async loadClues() {
    const data = await request<ClueListData>({ url: `/posts/${this.data.id}/clues` });
    this.setData({
      clues: data.items.map((c) => ({ ...c, statusLabel: clueStatus(c.status) })),
    });
  },

  goEdit() {
    wx.navigateTo({ url: "/pages/publish/publish?id=" + this.data.id });
  },

  async onClose() {
    const res = await new Promise<boolean>((r) =>
      wx.showModal({ title: "关闭信息", content: "关闭后将不再公开展示，确定？", success: (m) => r(m.confirm), fail: () => r(false) })
    );
    if (!res) return;
    try {
      await request<ItemPostView>({ url: `/posts/${this.data.id}/close`, method: "POST" });
      wx.showToast({ title: "已关闭", icon: "success" });
      this.refresh();
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  goClaims() {
    wx.navigateTo({ url: "/pages/claims/claims?postId=" + this.data.id });
  },

  async goClaimForm() {
    if (!(await ensureLoggedIn())) return;
    wx.navigateTo({ url: "/pages/claim-form/claim-form?postId=" + this.data.id });
  },

  onClueInput(e: any) {
    this.setData({ clueInput: e.detail.value });
  },

  async submitClue() {
    if (!(await ensureLoggedIn())) return;
    const content = this.data.clueInput.trim();
    if (!content) {
      wx.showToast({ title: "请填写线索内容", icon: "none" });
      return;
    }
    try {
      await request<ClueView>({ url: `/posts/${this.data.id}/clues`, method: "POST", data: { content } });
      wx.showToast({ title: "线索已提交", icon: "success" });
      this.setData({ clueInput: "" });
      this.loadClues();
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  async decideClue(e: any) {
    const { id, decision } = e.currentTarget.dataset;
    try {
      await request<ClueView>({ url: `/clues/${id}/decide`, method: "POST", data: { decision } });
      wx.showToast({ title: decision === "VALID" ? "已标记有效" : "已标记无效", icon: "success" });
      this.loadClues();
    } catch (err: any) {
      wx.showToast({ title: err.message, icon: "none" });
    }
  },

  async onBookmark() {
    if (!(await ensureLoggedIn())) return;
    try {
      if (this.data.bookmarked) {
        await request<Record<string, never>>({ url: `/posts/${this.data.id}/bookmark`, method: "DELETE" });
        wx.showToast({ title: "已取消收藏", icon: "none" });
      } else {
        await request<unknown>({ url: `/posts/${this.data.id}/bookmark`, method: "POST" });
        wx.showToast({ title: "已收藏", icon: "success" });
      }
      this.setData({ bookmarked: !this.data.bookmarked });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  async onBlock() {
    if (!this.data.post) return;
    if (!(await ensureLoggedIn())) return;
    const res = await new Promise<boolean>((r) =>
      wx.showModal({ title: "拉黑发布者", content: "拉黑后对方无法再向你发送消息或申请。", success: (m) => r(m.confirm), fail: () => r(false) })
    );
    if (!res) return;
    try {
      await request<unknown>({ url: "/blocks", method: "POST", data: { blockedUserId: this.data.post.authorId } });
      wx.showToast({ title: "已拉黑", icon: "success" });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  openReport() {
    this.setData({ showReport: true, reportDescription: "", reportReason: REPORT_REASONS[0] });
  },

  closeReport() {
    this.setData({ showReport: false });
  },

  onReportReason(e: any) {
    this.setData({ reportReason: REPORT_REASONS[Number(e.detail.value)] });
  },

  onReportDesc(e: any) {
    this.setData({ reportDescription: e.detail.value });
  },

  async submitReport() {
    if (!(await ensureLoggedIn())) return;
    this.setData({ submitting: true });
    try {
      await request<unknown>({
        url: "/reports",
        method: "POST",
        data: {
          targetType: "POST",
          targetId: this.data.id,
          reason: this.data.reportReason,
          description: this.data.reportDescription.trim() || null,
        },
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
