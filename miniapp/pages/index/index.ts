import { request } from "../../utils/request";
import { postStatus } from "../../utils/status";
import { formatTime, mediaUrl } from "../../utils/format";
import type { ItemPostListData, ItemPostView, StatsData } from "../../types/api";

interface IndexItem extends ItemPostView {
  statusLabel: string;
  timeLabel: string;
  typeLabel: string;
  cover: string;
  imageCount: number;
  glyph: string;
  placeholder: string;
}

const CATEGORIES = ["全部", "校园卡", "证件", "电子产品", "生活用品", "衣物", "其他"];
const TYPE_TABS = [
  { key: "FOUND", label: "招领" },
  { key: "LOST", label: "寻物" },
];

Page({
  data: {
    statusBarHeight: 47,
    keyword: "",
    activeType: "FOUND",
    activeCategory: "全部",
    typeTabs: TYPE_TABS,
    categories: CATEGORIES,
    items: [] as IndexItem[],
    page: 1,
    pageSize: 20,
    total: 0,
    hasMore: false,
    loading: false,
    foundCount: 0,
    lostCount: 0,
    completedToday: 0,
  },

  onShow() {
    wx.hideTabBar({ animation: false });
    this.reload();
    this.loadStats();
  },

  onPullDownRefresh() {
    Promise.all([this.reload(), this.loadStats()]).finally(() => wx.stopPullDownRefresh());
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

  async loadStats() {
    try {
      const stats = await request<StatsData>({ url: "/stats" });
      this.setData({
        foundCount: stats.foundCount,
        lostCount: stats.lostCount,
        completedToday: stats.completedToday,
      });
    } catch (e: any) {
      if (e.code !== "NETWORK") {
        wx.showToast({ title: e.message, icon: "none" });
      }
    }
  },

  decorate(it: ItemPostView): IndexItem {
    const images = it.images || [];
    return {
      ...it,
      statusLabel: postStatus(it.status),
      timeLabel: formatTime(it.createdAt),
      typeLabel: it.type === "FOUND" ? "招领" : "寻物",
      cover: mediaUrl(images[0]?.url),
      imageCount: images.length,
      glyph: (it.category || "物").slice(0, 1),
      placeholder: it.category === "校园卡" ? "/assets/ui/campus-card.png" : "/assets/ui/umbrella.png",
    };
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
      const items = data.items.map((it) => this.decorate(it));
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
    if (c === this.data.activeCategory) return;
    this.setData({ activeCategory: c });
    this.reload();
  },

  goDetail(e: any) {
    wx.navigateTo({ url: "/pages/detail/detail?id=" + e.currentTarget.dataset.id });
  },

  goPublish() {
    wx.navigateTo({ url: "/pages/publish/publish" });
  },

  goMy() {
    wx.showTabBar({ animation: false });
    wx.switchTab({ url: "/pages/my/my" });
  },
});
