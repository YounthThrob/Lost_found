# Proposal

## Why

当前「学校/校区」只是自由文本字符串：用户档案存一份 `school_name/campus_name`，每篇信息又独立存一份 `school/campus`，没有实体身份、没有别名归一。这导致同一所学校因写法不同被切成碎片，「同校」筛选、按校聚合、访问控制全部失效；且「自报」被做了两次、互不约束。要落地，必须先把学校/校区升级为带 ID 的规范化实体，再让用户从目录选择，而不是手敲文本。

## What Changes

- **BREAKING** 新增「学校/校区目录」能力：规范化实体 `School{id,name,aliases,source}` 与 `Campus{id,schoolId,name,status}`；学校名单预置（教育部名单），校区按需申请 + 运营审核后进入目录。
- **BREAKING** 用户自报学校/校区从自由文本改为从目录选择：`CampusProfile` 的 `school_name/campus_name` 改为 `schoolId/campusId`；仍标记 `SELF_DECLARED`，仍保留「用户自报，未经校方认证」的非官方边界。
- **BREAKING** 信息 `ItemPost` 的 `school/campus` 改为 `schoolId/campusId`，发布默认继承作者档案（跨校相关可显式覆盖）；公开响应附带 `schoolName/campusName` 用于展示。
- **BREAKING** 搜索筛选 `/posts` 的 `school/campus` 参数改为 `schoolId/campusId`。
- 小程序「我的」页新增「基本信息」区块展示学校/校区，并提供从目录选择的选择器（仅 UI 文案改名，内部 `self_declared`/`SELF_DECLARED` 语义保留）。

## Capabilities

### New Capabilities

- `school-campus-directory`: 学校/校区规范化实体与目录，含别名归一、学校预置、校区按需申请与运营审核、供选择器使用的目录查询。

### Modified Capabilities

- `self-declared-campus`: 自报学校/校区从自由文本改为从目录选择 `schoolId/campusId`，仍保持非认证边界。
- `lost-found-listing`: 信息 `school/campus` 改为 `schoolId/campusId` 并默认继承作者档案；搜索筛选参数同步改为 ID。
- `public-browsing`: 公开端点字段 `school/campus` 改为 `schoolId/schoolName/campusId/campusName`。

## Impact

- 契约：`openapi/product-api.yaml`（`CampusProfileUpdate`/`CampusProfileResponse`、`ItemPostCreate`/`ItemPostUpdate`/`ItemPostView`、`/posts` 参数）；`contracts/error-codes.yaml` 评估是否新增「学校/校区不存在」错误码，倾向复用 `VALIDATION_FAILED`/`NOT_FOUND`。
- 双后端：Java/Python 的 account 与 listing 模块数据模型、内存存储；契约测试向量更新（Java == Python == expected）。
- 数据：教育部高校名单种子数据导入（学校实体）；校区按需生长。
- 小程序：campus 页 / 我的页、学校选择器、发帖继承逻辑。
- admin-web：校区申请审核入口（MVP 可复用 operations-management 审核）。
