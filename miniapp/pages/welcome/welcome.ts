import { acceptAgreement, login } from "../../utils/auth";
import { fetchMe } from "../../utils/me";

Page({
  data: {
    loading: false,
    agreed: false,
  },

  toggleAgree() {
    this.setData({ agreed: !this.data.agreed });
  },

  openAgreement(e: any) {
    const type = e.currentTarget.dataset.type === "privacy" ? "privacy" : "user";
    wx.navigateTo({ url: "/pages/agreement/agreement?type=" + type });
  },

  async onAgree() {
    if (this.data.loading) return;
    if (!this.data.agreed) {
      wx.showToast({ title: "请先阅读并勾选协议", icon: "none" });
      return;
    }
    this.setData({ loading: true });
    try {
      const res = await login();
      if (!res.agreementAccepted) {
        await acceptAgreement();
      }
      await fetchMe();
      // 从「去登录」进入则返回原页面；作为首次入口进入则回首页。
      const pages = getCurrentPages();
      if (pages.length > 1) {
        wx.navigateBack();
      } else {
        wx.switchTab({ url: "/pages/index/index" });
      }
    } catch (e: any) {
      wx.showToast({ title: e.message || "登录失败，请稍后重试", icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },
});
