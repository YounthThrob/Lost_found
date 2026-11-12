import { request } from "../../utils/request";
import type { CampusProfileView } from "../../types/api";

Page({
  data: {
    school: "",
    campus: "",
    verificationStatus: "",
    saving: false,
  },

  onShow() {
    this.load();
  },

  async load() {
    try {
      const p = await request<CampusProfileView>({ url: "/me/campus-profile" });
      this.setData({
        school: p.school || "",
        campus: p.campus || "",
        verificationStatus: p.verificationStatus,
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  onInput(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value } as any);
  },

  async onSave() {
    const school = this.data.school.trim();
    if (!school) {
      wx.showToast({ title: "请填写学校", icon: "none" });
      return;
    }
    this.setData({ saving: true });
    try {
      await request<CampusProfileView>({
        url: "/me/campus-profile",
        method: "PUT",
        data: { school, campus: this.data.campus.trim() || null },
      });
      wx.showToast({ title: "已保存", icon: "success" });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },
});
