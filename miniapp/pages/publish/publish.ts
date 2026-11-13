import { request, uploadFile } from "../../utils/request";
import { mediaUrl } from "../../utils/format";
import type { ItemImage, ItemPostView } from "../../types/api";

const CATEGORIES = ["校园卡", "证件", "电子产品", "生活用品", "衣物", "其他"];
const TYPE_LABEL: Record<string, string> = { LOST: "寻物（我丢了）", FOUND: "招领（我捡到）" };
const MAX_IMAGES = 9;

interface ImageEntry {
  local: boolean; // true = 尚未上传的本地临时文件
  src: string; // 展示用地址（本地临时路径或绝对 media 地址）
  id?: string; // 服务端图片 id（已上传时存在）
}

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
    images: [] as ImageEntry[],
    maxImages: MAX_IMAGES,
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
        images: (post.images || []).map((img) => ({ local: false, src: mediaUrl(img.url), id: img.id })),
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

  // ===== 图片 =====
  chooseImages() {
    const remaining = MAX_IMAGES - this.data.images.length;
    if (remaining <= 0) {
      wx.showToast({ title: `最多上传 ${MAX_IMAGES} 张图片`, icon: "none" });
      return;
    }
    wx.chooseMedia({
      count: remaining,
      mediaType: ["image"],
      sizeType: ["compressed"],
      sourceType: ["album", "camera"],
      success: (res) => {
        const added = res.tempFiles.map((f) => ({ local: true, src: f.tempFilePath }) as ImageEntry);
        this.setData({ images: this.data.images.concat(added) });
      },
    });
  },

  async removeImage(e: any) {
    const idx = Number(e.currentTarget.dataset.idx);
    const img = this.data.images[idx];
    if (!img) return;
    const images = this.data.images.slice();
    if (img.local) {
      images.splice(idx, 1);
      this.setData({ images });
      return;
    }
    // 删除已上传到草稿的服务端图片
    try {
      await request<Record<string, never>>({ url: `/posts/${this.data.id}/images/${img.id}`, method: "DELETE" });
      images.splice(idx, 1);
      this.setData({ images });
    } catch (err: any) {
      wx.showToast({ title: err.message, icon: "none" });
    }
  },

  previewImage(e: any) {
    const idx = Number(e.currentTarget.dataset.idx);
    const urls = this.data.images.map((i) => i.src);
    wx.previewImage({ current: urls[idx], urls });
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

  // 创建或更新信息，返回信息 id。
  async persist(): Promise<string> {
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

  // 把本地临时图片逐一上传到草稿；已成功的就地标记为 remote，避免重试时重复上传。
  async uploadLocalImages(postId: string): Promise<void> {
    const images = this.data.images.slice();
    for (let i = 0; i < images.length; i++) {
      if (!images[i].local) continue;
      const uploaded = await uploadFile<ItemImage>(`/posts/${postId}/images`, images[i].src);
      images[i] = { local: false, src: mediaUrl(uploaded.url), id: uploaded.id };
      this.setData({ images });
    }
  },

  async onSaveDraft() {
    const msg = this.validate();
    if (msg) {
      wx.showToast({ title: msg, icon: "none" });
      return;
    }
    this.setData({ saving: true });
    try {
      const id = await this.persist();
      await this.uploadLocalImages(id);
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
      const id = await this.persist();
      await this.uploadLocalImages(id);
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
