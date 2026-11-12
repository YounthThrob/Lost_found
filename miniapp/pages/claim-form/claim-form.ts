import { request } from "../../utils/request";
import type { ClaimView } from "../../types/api";

Page({
  data: {
    postId: "",
    statement: "",
    submitting: false,
  },

  onLoad(options: any) {
    this.setData({ postId: options.postId || "" });
  },

  onInput(e: any) {
    this.setData({ statement: e.detail.value });
  },

  async onSubmit() {
    const statement = this.data.statement.trim();
    if (!statement) {
      wx.showToast({ title: "请填写归属证明", icon: "none" });
      return;
    }
    this.setData({ submitting: true });
    try {
      await request<ClaimView>({
        url: `/posts/${this.data.postId}/claims`,
        method: "POST",
        data: { statement },
      });
      wx.showToast({ title: "申请已提交", icon: "success" });
      setTimeout(() => wx.navigateBack(), 800);
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
