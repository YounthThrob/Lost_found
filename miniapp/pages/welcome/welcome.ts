import { acceptAgreement, login } from "../../utils/auth";
import { fetchMe } from "../../utils/me";

Page({
  data: {
    loading: false,
  },

  async onAgree() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const res = await login();
      if (!res.agreementAccepted) {
        await acceptAgreement();
      }
      await fetchMe();
      wx.switchTab({ url: "/pages/index/index" });
    } catch (e: any) {
      wx.showToast({ title: e.message || "登录失败，请稍后重试", icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },
});
