import { request } from "../../utils/request";
import { getUserId, fetchMe } from "../../utils/me";
import { formatTime } from "../../utils/format";
import type { MessageListData, MessageView } from "../../types/api";

interface MsgItem extends MessageView {
  mine: boolean;
  timeLabel: string;
  typeLabel: string;
}

const QUICK: Array<{ type: string; label: string; preset: string }> = [
  { type: "SUGGEST_TIME", label: "建议时间", preset: "建议交接时间为：" },
  { type: "SUGGEST_PLACE", label: "建议地点", preset: "建议在以下公共区域交接：" },
  { type: "LATE_REMINDER", label: "迟到提醒", preset: "提醒：我可能会迟到，请稍等。" },
];

Page({
  data: {
    conversationId: "",
    peerId: "",
    items: [] as MsgItem[],
    messageType: "TEXT",
    content: "",
    quicks: QUICK,
    sending: false,
  },

  onLoad(options: any) {
    this.setData({ conversationId: options.conversationId || "", peerId: options.peerId || "" });
  },

  onShow() {
    this.load();
  },

  async load() {
    try {
      let uid = getUserId();
      if (!uid) {
        try {
          await fetchMe();
          uid = getUserId();
        } catch {
          /* 忽略 */
        }
      }
      const data = await request<MessageListData>({ url: `/conversations/${this.data.conversationId}/messages` });
      this.setData({
        items: data.items.map((m) => ({
          ...m,
          mine: m.senderId === uid,
          timeLabel: formatTime(m.createdAt),
          typeLabel: m.type === "TEXT" ? "" : m.type,
        })),
      });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },

  onInput(e: any) {
    this.setData({ content: e.detail.value });
  },

  onQuick(e: any) {
    const idx = Number(e.currentTarget.dataset.idx);
    const q = QUICK[idx];
    this.setData({ messageType: q.type, content: q.preset });
  },

  async send() {
    const content = this.data.content.trim();
    if (!content) {
      wx.showToast({ title: "请输入消息内容", icon: "none" });
      return;
    }
    this.setData({ sending: true });
    try {
      await request<MessageView>({
        url: `/conversations/${this.data.conversationId}/messages`,
        method: "POST",
        data: { type: this.data.messageType, content },
      });
      this.setData({ content: "", messageType: "TEXT" });
      this.load();
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    } finally {
      this.setData({ sending: false });
    }
  },

  async onBlock() {
    if (!this.data.peerId) {
      wx.showToast({ title: "当前无法识别对方", icon: "none" });
      return;
    }
    const res = await new Promise<boolean>((r) =>
      wx.showModal({ title: "拉黑对方", content: "拉黑后对方无法再向你发送消息或申请。", success: (m) => r(m.confirm), fail: () => r(false) })
    );
    if (!res) return;
    try {
      await request<unknown>({ url: "/blocks", method: "POST", data: { blockedUserId: this.data.peerId } });
      wx.showToast({ title: "已拉黑", icon: "success" });
    } catch (e: any) {
      wx.showToast({ title: e.message, icon: "none" });
    }
  },
});
