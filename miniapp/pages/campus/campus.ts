import { request } from "../../utils/request";
import { formatTime } from "../../utils/format";
import type {
  CampusListData,
  CampusProfileView,
  CampusView,
  EmailVerificationSendResponse,
  SchoolListData,
  SchoolView,
} from "../../types/api";

Page({
  data: {
    // 基本信息（学校/校区从目录选择）
    schoolId: "",
    schoolName: "",
    campusId: "",
    campusName: "",
    schoolKeyword: "",
    schools: [] as SchoolView[],
    campuses: [] as CampusView[],
    searching: false,
    // 校园身份
    verificationStatus: "SELF_DECLARED",
    verifiedEmail: "",
    emailVerifiedAt: "",
    // 邮箱核验
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
        schoolId: p.schoolId || "",
        schoolName: p.schoolName || "",
        campusId: p.campusId || "",
        campusName: p.campusName || "",
        verificationStatus: p.verificationStatus,
        verifiedEmail: p.verifiedEmail || "",
        emailVerifiedAt: p.emailVerifiedAt ? formatTime(p.emailVerifiedAt) : "",
      });
      if (p.schoolId) {
        this.loadCampuses(p.schoolId);
      } else {
        this.searchSchools();
      }
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  // ===== 学校目录搜索 =====
  onSearchSchool(e: any) {
    this.setData({ schoolKeyword: e.detail.value });
    this.searchSchools();
  },

  async searchSchools() {
    const keyword = this.data.schoolKeyword.trim();
    this.setData({ searching: true });
    try {
      const data = await request<SchoolListData>({
        url: "/directory/schools",
        data: { page: 1, pageSize: 20, ...(keyword ? { keyword } : {}) },
      });
      this.setData({ schools: data.items });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ searching: false });
    }
  },

  async loadCampuses(schoolId: string) {
    try {
      const data = await request<CampusListData>({ url: `/directory/schools/${schoolId}/campuses` });
      this.setData({ campuses: data.items });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  onSelectSchool(e: any) {
    const { id, name } = e.currentTarget.dataset;
    this.setData({
      schoolId: id,
      schoolName: name,
      campusId: "",
      campusName: "",
      schoolKeyword: "",
      schools: [],
      campuses: [],
    });
    this.loadCampuses(id);
  },

  onChangeSchool() {
    this.setData({ schoolId: "", schoolName: "", campusId: "", campusName: "", campuses: [] });
  },

  onSelectCampus(e: any) {
    const { id, name } = e.currentTarget.dataset;
    this.setData({ campusId: id, campusName: name });
  },

  onClearCampus() {
    this.setData({ campusId: "", campusName: "" });
  },

  // ===== 保存基本信息 =====
  async onSave() {
    if (!this.data.schoolId) {
      wx.showToast({ title: "请选择学校", icon: "none" });
      return;
    }
    this.setData({ saving: true });
    try {
      await request<CampusProfileView>({
        url: "/me/campus-profile",
        method: "PUT",
        data: { schoolId: this.data.schoolId, campusId: this.data.campusId || null },
      });
      wx.showToast({ title: "已保存", icon: "success" });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  onInput(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value } as any);
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
