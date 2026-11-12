import { request } from "../../utils/request";
import type { ItemPostView } from "../../types/api";

const CATEGORIES = ["校园卡", "证件", "电子产品", "生活用品", "衣物", "其他"];
const TYPE_LABEL: Record<string, string> = { LOST: "寻物（我丢了）", FOUND: "招领（我捡到）" };

Page({
  data: {
    id: "",
    editing: false,
    type: "FOUND",
    typeLabel: TYPE_LABEL.FOUND,
    types: ["FOUND", "LOST"],
    typeLabels: [TYPE_LABEL.FOUND, TYPE_LABEL.LOST],
    categories: CATEGORIES,
    categoryIndex: 0,
    category: CATEGORIES[0],
    title: "",
    publicDescription: "",
    school: "",
    campus: "",
    area: "",
    saving: false,
  },

  onLoad(options: any) {
    if (options.id) {
      this.setData({ id: options.id, editing: true });
      this.loadPost(options.id);
    }
  },

  async loadPost(id: string) {
    try {
      const post = await request<ItemPostView>({ url: "/posts/" + id });
      const catIndex = Math.max(0, CATEGORIES.indexOf(post.category));
      this.setData({
        type: post.type,
        typeLabel: TYPE_LABEL[post.type],
        category: post.category,
        categoryIndex: catIndex,
        title: post.title,
        publicDescription: post.publicDescription,
        school: post.school,
        campus: post.campus || "",
        area: post.area || "",
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  onTypeChange(e: any) {
    const idx = Number(e.detail.value);
    this.setData({ type: this.data.types[idx], typeLabel: this.data.typeLabels[idx] });
  },

  onCategoryChange(e: any) {
    const idx = Number(e.detail.value);
    this.setData({ categoryIndex: idx, category: CATEGORIES[idx] });
  },

  onInput(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value } as any);
  },

  validate(): string {
    if (!this.data.title.trim()) return "请填写标题";
    if (!this.data.publicDescription.trim()) return "请填写公开描述";
    if (!this.data.school.trim()) return "请填写学校";
    return "";
  },

  buildBody() {
    return {
      type: this.data.type,
      category: this.data.category,
      title: this.data.title.trim(),
      publicDescription: this.data.publicDescription.trim(),
      school: this.data.school.trim(),
      campus: this.data.campus.trim() || null,
      area: this.data.area.trim() || null,
    };
  },

  async saveDraft(): Promise<string> {
    if (this.data.editing) {
      await request<ItemPostView>({
        url: "/posts/" + this.data.id,
        method: "PATCH",
        data: this.buildBody(),
      });
      return this.data.id;
    }
    const post = await request<ItemPostView>({ url: "/posts", method: "POST", data: this.buildBody() });
    return post.id;
  },

  async onSaveDraft() {
    const msg = this.validate();
    if (msg) {
      wx.showToast({ title: msg, icon: "none" });
      return;
    }
    this.setData({ saving: true });
    try {
      await this.saveDraft();
      wx.showToast({ title: "已保存草稿", icon: "success" });
      setTimeout(() => wx.navigateBack(), 800);
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async onPublish() {
    const msg = this.validate();
    if (msg) {
      wx.showToast({ title: msg, icon: "none" });
      return;
    }
    this.setData({ saving: true });
    try {
      const id = await this.saveDraft();
      await request<ItemPostView>({ url: `/posts/${id}/publish`, method: "POST" });
      wx.showToast({ title: "发布成功", icon: "success" });
      setTimeout(() => wx.navigateBack(), 800);
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },
});
