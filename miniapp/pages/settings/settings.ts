import { clearToken } from "../../utils/token";

Page({
  data: {},

  onLogout() {
    wx.showModal({
      title: "退出登录",
      content: "退出后需要重新授权进入平台。",
      success: (m) => {
        if (m.confirm) {
          clearToken();
          wx.reLaunch({ url: "/pages/welcome/welcome" });
        }
      },
    });
  },
});
