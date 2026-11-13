// 全局配置：后端地址与开发环境 mock 登录。
// 生产环境应通过域名指向已部署后端，并在微信公众平台配置 request 合法域名。

// Java 后端默认 8080，Python 后端默认 8000。二选一部署，接口契约等价。
export const API_BASE = "http://127.0.0.1:8000/api/v1";

// 图片静态托管根（/media/{storage_key}）。与 API_BASE 同主机，去掉 /api/v1 前缀。
export const MEDIA_BASE = "http://127.0.0.1:8000";

// 空字符串 = 使用 wx.login 获取真实 code，由后端 code2session 换取 openid（真实微信授权登录）。
// 如需本地 mock 联调（后端 WECHAT_MOCK=true），可临时改为 "valid-code"。
export const MOCK_CODE = "";

export const APP_NAME = "校园失物招领（非官方）";
