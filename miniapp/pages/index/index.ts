import { request } from "../../utils/request";
import { postStatus } from "../../utils/status";
import { formatTime } from "../../utils/format";
import type { ItemPostListData, ItemPostView } from "../../types/api";

interface IndexItem extends ItemPostView {
  statusLabel: string;
  timeLabel: string;
}

const CATEGORIES = ["全部", "校园卡", "证件", "电子产品", "生活用品", "衣物", "其他"];

Page({
  data: {
    keyword: "",
    activeType: "FOUND" as string,
    activeCategory: "全部",
    categories: CATEGORIES,
    items: [] as IndexItem[],
    page: 1,
    pageSize: 20,
    total: 0,
    hasMore: false,
    loading: false,
  },

  onShow() {
    this.reload();
  },

  onPullDownRefresh() {
    this.reload().finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.load(this.data.page + 1);
    }
  },

  async reload() {
    this.setData({ page: 1, items: [] });
    await this.load(1);
  },

  async load(page: number) {
    this.setData({ loading: true });
    try {
      const data = await request<ItemPostListData>({
        url: "/posts",
        data: {
          keyword: this.data.keyword || undefined,
          type: this.data.activeType || undefined,
          category: this.data.activeCategory === "全部" ? undefined : this.data.activeCategory,
          page,
          pageSize: this.data.pageSize,
        },
      });
      const items = data.items.map((it) => ({
        ...it,
        statusLabel: postStatus(it.status),
        timeLabel: formatTime(it.createdAt),
      }));
      const merged = page === 1 ? items : this.data.items.concat(items);
      this.setData({
        items: merged,
        page,
        total: data.pagination.total,
        hasMore: data.pagination.hasMore,
      });
    } catch (e: any) {
      if (e.code !== "NETWORK") {
        wx.showToast({ title: e.message, icon: "none" });
      }
    } finally {
      this.setData({ loading: false });
    }
  },

  onKeyword(e: any) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.reload();
  },

  switchType(e: any) {
    const t = e.currentTarget.dataset.type;
    if (t === this.data.activeType) return;
    this.setData({ activeType: t });
    this.reload();
  },

  switchCategory(e: any) {
    const c = e.currentTarget.dataset.cat;
    this.setData({ activeCategory: c });
    this.reload();
  },

  goDetail(e: any) {
    wx.navigateTo({ url: "/pages/detail/detail?id=" + e.currentTarget.dataset.id });
  },

  goPublish() {
    wx.navigateTo({ url: "/pages/publish/publish" });
  },
});
