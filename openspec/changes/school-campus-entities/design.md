# Design

## Context

当前 `school_name/campus_name`（用户档案）与 `school/campus`（信息）都是自由文本，无实体、无别名归一；存储仍是 walking skeleton 的内存实现（重启即丢），无持久化数据需要迁移。契约优先、双后端（Java/Python）等价、非官方边界（自报学校必须标注「用户自报，未经校方认证」）是硬约束。

## Goals / Non-Goals

**Goals:**

- 建立 `School`/`Campus` 规范化实体，主键 UUIDv7，别名归一，让「同校」可比较、可按校筛选/聚合。
- 用户从目录选择学校/校区（不再手敲），信息默认继承作者学校。
- 校区按需申请 + 运营审核的完整生命周期（PENDING → ACTIVE/REJECTED）。

**Non-Goals:**

- 不做全国校区全量预置（无权威/授权干净的源，且 MVP 用不上）；只预置学校，校区按需生长。
- 不把「选择学校」变成「身份核验」——目录只解决身份/去重，不解决信任；`SELF_DECLARED` 与非官方边界保持不变。
- 不做硬性「同校才可见」的访问墙（本次仅规范化，访问控制另议）。

## Decisions

### 1. 实体模型与反规范化展示

- `School{id(UUIDv7), name, aliases[], source(SEED|USER_SUBMITTED), enabled, createdAt}`；`Campus{id(UUIDv7), schoolId, name, status(PENDING|ACTIVE|REJECTED), createdAt}`。
- `CampusProfile` 存 `schoolId`（+可选 `campusId`），响应附带 `schoolName/campusName`（从目录实时解析）。
- `ItemPost` 存 `schoolId/campusId`，并在发布时**快照** `schoolName/campusName` 到信息上，公开响应直接带名称。
  - 备选：只存 ID、公开响应让客户端二次查目录——被否，匿名浏览会多一次往返且增加前端复杂度。快照的权威判断仍靠 ID，名称仅是展示。

### 2. 继承规则

`ItemPostCreate.schoolId` 改为可选：未提供时服务端解析为作者档案的 `schoolId`；作者未设置学校且未提供 `schoolId` → `VALIDATION_FAILED`(422)。`campusId` 始终可选，提供时须属于该 `schoolId` 且 `ACTIVE`。

### 3. 目录端点鉴权

新增目录端点走 `bearerAuth`（已登录），**不进** `public-browsing` 的匿名清单——目录查询只服务于登录后的选择器，避免扩大匿名公开面。

### 4. 错误码复用，不新增

- 无效/停用 `schoolId`、`campusId` 不属于学校、校区重名 → `VALIDATION_FAILED`(422)。
- 查询不存在学校的校区、为不存在学校申请校区 → `NOT_FOUND`(404)。
- 理由：保持双后端契约等价面最小，与既有 profile/avatar 的复用策略一致。

### 5. 校区审核归属 operations

校区审核端点放运营模块（`/admin/**`，operator 角色鉴权，复用现有运营体系），不单独造鉴权体系。MVP 仅提供 API，admin-web 图形界面可选后补。

### 6. 学校种子

教育部《全国普通高等学校名单》整理为静态资源，两个后端各自打包，启动时加载进内存目录（walking skeleton）。别名表人工维护，MVP 至少覆盖试点学校的常见别名。

## Risks / Trade-offs

- [学校种子覆盖不完整 / 校区口径不一致] → 校区按需申请 + 运营审核兜底；学校 scope（是否含高职/独立学院/成人）上线前确认。
- [教育部名单的授权与口径] → 上线前核对使用条款；MVP 可先只预置试点学校，降低合规与维护面。
- [schoolName 快照可能随学校更名过期] → 可接受：展示用，权威判断一律用 ID。
- [内存存储重启丢目录/学校] → 与「持久化」P0 一并解决，本次沿用内存实现，不另做持久化。

## Migration Plan

1. 契约优先：改 `openapi/product-api.yaml`（目录端点 + CampusProfile/ItemPost schema + `/posts` 参数）、评估 `error-codes.yaml`、更新契约测试向量。
2. 双后端实现（目录存储/服务/路由、account、listing、operations 审核），跑契约测试确保 Java == Python == expected。
3. 小程序：campus 页改「基本信息」、学校选择器、发帖继承；UI 文案去掉「自报学校和校区」区块名，学校值保留「用户自报，未经校方认证」小字。
4. 破坏性变更一次性切换：当前无持久化生产数据，无需灰度；自由文本旧值随内存重启自然失效。

## Open Questions

- 学校种子 scope（普通高校 / 是否含高职、独立学院、成人高校）——可后定，不影响 spec 与任务拆分。
- 校区审核是否接入 admin-web 图形界面——MVP 仅 API，后补。
