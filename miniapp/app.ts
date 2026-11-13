import { isLoggedIn } from "./utils/auth";
import { fetchMe } from "./utils/me";

App({
  globalData: {
    userId: "",
  },
  onLaunch() {
    // 公开浏览：默认进入首页，不强制登录；已登录时预热用户信息缓存。
    if (isLoggedIn()) {
      fetchMe().catch(() => {});
    }
  },
});
