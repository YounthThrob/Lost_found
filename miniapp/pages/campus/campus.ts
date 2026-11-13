import { request } from "../../utils/request";
import { formatTime } from "../../utils/format";
import type { CampusProfileView, EmailVerificationSendResponse } from "../../types/api";

Page({
  data: {
    school: "",
    campus: "",
    verificationStatus: "SELF_DECLARED",
    verifiedEmail: "",
    emailVerifiedAt: "",
    email: "",
    code: "",
    codeSent: false,
    saving: false,
    verifying: false,
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
        verifiedEmail: p.verifiedEmail || "",
        emailVerifiedAt: p.emailVerifiedAt ? formatTime(p.emailVerifiedAt) : "",
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

  async onSendCode() {
    const email = this.data.email.trim();
    if (!email) {
      wx.showToast({ title: "请填写校园邮箱", icon: "none" });
      return;
    }
    this.setData({ verifying: true });
    try {
      await request<EmailVerificationSendResponse>({
        url: "/me/campus-verification/email",
        method: "POST",
        data: { email },
      });
      this.setData({ codeSent: true, code: "" });
      wx.showToast({ title: "验证码已发送", icon: "none" });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ verifying: false });
    }
  },

  async onConfirmCode() {
    const email = this.data.email.trim();
    const code = this.data.code.trim();
    if (!email || !code) {
      wx.showToast({ title: "请填写邮箱与验证码", icon: "none" });
      return;
    }
    this.setData({ verifying: true });
    try {
      const p = await request<CampusProfileView>({
        url: "/me/campus-verification/email/confirm",
        method: "POST",
        data: { email, code },
      });
      this.setData({
        verificationStatus: p.verificationStatus,
        verifiedEmail: p.verifiedEmail || "",
        emailVerifiedAt: p.emailVerifiedAt ? formatTime(p.emailVerifiedAt) : "",
        codeSent: false,
        code: "",
        email: "",
      });
      wx.showToast({ title: "核验完成", icon: "success" });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ verifying: false });
    }
  },
});
