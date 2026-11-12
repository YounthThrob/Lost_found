import { isLoggedIn } from "./utils/auth";
import { fetchMe } from "./utils/me";

App({
  globalData: {
    userId: "",
  },
  onLaunch() {
    if (isLoggedIn()) {
      // 预热当前用户 id 缓存，供详情页等前端判断“是否本人”。
      fetchMe().catch(() => {});
      wx.switchTab({ url: "/pages/index/index" });
    } else {
      wx.reLaunch({ url: "/pages/welcome/welcome" });
    }
  },
});
