import { MEDIA_BASE } from "../../config";
import { request, uploadFile } from "../../utils/request";
import type { MeView, ProfileView } from "../../types/api";

Page({
  data: {
    nickname: "",
    avatarUrl: "",
  },

  onLoad() {
    request<MeView>({ url: "/me" })
      .then((me) => {
        this.setData({
          nickname: me.nickname || "",
          avatarUrl: me.avatarUrl ? MEDIA_BASE + me.avatarUrl : "",
        });
      })
      .catch(() => {});
  },

  onNicknameInput(e: any) {
    this.setData({ nickname: e.detail.value });
  },

  onChooseAvatar(e: any) {
    const tmp = (e.detail || {}).avatarUrl as string | undefined;
    if (!tmp) return;
    wx.showLoading({ title: "上传中" });
    uploadFile<{ avatarUrl: string }>("/me/avatar", tmp)
      .then((res) => {
        this.setData({ avatarUrl: MEDIA_BASE + res.avatarUrl });
        wx.hideLoading();
        wx.showToast({ title: "头像已更新", icon: "success" });
      })
      .catch((err) => {
        wx.hideLoading();
        wx.showToast({ title: err.message || "上传失败", icon: "none" });
      });
  },

  onSave() {
    const nickname = (this.data.nickname || "").trim();
    if (!nickname) {
      wx.showToast({ title: "昵称不能为空", icon: "none" });
      return;
    }
    request<ProfileView>({ url: "/me/profile", method: "PUT", data: { nickname } })
      .then(() => wx.showToast({ title: "已保存", icon: "success" }))
      .catch((err) => wx.showToast({ title: err.message || "保存失败", icon: "none" }));
  },
});
