# Tasks

## 1. 契约（契约优先）

- [x] 1.1 更新 `openapi/product-api.yaml`：新增 `School`/`Campus` schema 与目录端点（`GET /directory/schools`、`GET /directory/schools/{schoolId}/campuses`、`POST /directory/schools/{schoolId}/campuses`、`POST /admin/campuses/{campusId}/review`），并将 `CampusProfileUpdate/Response`、`ItemPostCreate/Update/View` 的 `school/campus` 改为 `schoolId/campusId`（响应附 `schoolName/campusName`），`/posts` 参数 `school/campus` 改为 `schoolId/campusId`。验证：全文无残留的 `school/campus` 自由文本字段引用。
- [x] 1.2 确认 `contracts/error-codes.yaml` 复用 `VALIDATION_FAILED`/`NOT_FOUND`（不新增错误码），并核对既有码描述仍准确。验证：契约与实现一致，无未声明的新错误码。
- [x] 1.3 更新 `contracts/run_contract_tests.py`：新增目录查询、校区申请/审核、schoolId 继承、按 schoolId 筛选、无效 schoolId 返回 422 的 EXPECTED 向量。验证：新增向量与 spec 各 Scenario 一一对应。

## 2. Python 后端

- [x] 2.1 新增目录实体与内存存储（`School`/`Campus`，含 aliases、status、source）及学校种子加载。验证：`backend-python/.venv/Scripts/python.exe -m pytest` 目录存储单测通过。
- [x] 2.2 新增目录路由与服务校验：学校搜索（名称/别名包含匹配、分页、仅 enabled）、校区查询（仅 ACTIVE、学校不存在 404）、校区申请（重名 422、学校不存在 404）、运营审核（PENDING→ACTIVE/REJECTED）。验证：curl 各端点行为符合 spec。
- [x] 2.3 改造 account 模块：`CampusProfile` 由 `school_name/campus_name` 改为 `schoolId/campusId`，设置接口校验 schoolId 已启用、campusId 属于该校且 ACTIVE，响应附 `schoolName/campusName`。验证：`/me/campus-profile` PUT 非法 schoolId 返回 422。
- [x] 2.4 改造 listing 模块：`ItemPost` 改为 `schoolId/campusId` + `schoolName/campusName` 快照，创建时未提供 schoolId 则继承作者档案，作者无学校且未提供则 422。验证：发帖继承与显式覆盖两条路径正确。
- [x] 2.5 打包教育部学校种子静态资源并启动加载，覆盖试点学校与常见别名。验证：启动后 `GET /directory/schools?keyword=试点校` 能命中种子学校。

## 3. Java 后端（镜像 Python）

- [x] 3.1 新增目录实体与内存存储 + 种子加载。验证：`JAVA_HOME=/d/develop/Java/jdk-21 mvn -q test` 通过。
- [x] 3.2 新增目录路由与服务校验（与 Python 同行为）。验证：目录端点行为与 Python 一致。
- [x] 3.3 改造 account：`CampusProfile` 改 `schoolId/campusId`，校验并附名称。验证：非法 schoolId 返回 422。
- [x] 3.4 改造 listing：`ItemPost` 改 `schoolId/campusId` + 名称快照 + 继承。验证：继承与覆盖路径正确。

## 4. 双后端契约等价

- [x] 4.1 构建 Java 并运行 `contracts/run_contract_tests.py`，确认 Java == Python == expected 全部通过。验证：契约测试全绿、无失败向量。

## 5. 小程序

- [x] 5.1 将 campus 页改造为「基本信息」区块：学校/校区从目录搜索选择（调用目录端点），学校值旁保留「用户自报，未经校方认证」小字。验证：选择器能搜索并选中种子学校。
- [x] 5.2 我的页展示学校/校区（基本信息），点击进入编辑。验证：我的页正确显示所选学校/校区。
- [x] 5.3 发帖页 schoolId 预填继承用户档案，跨校场景可改选。验证：发帖默认带入本人学校，可改选。
- [x] 5.4 运行 `node node_modules/typescript/bin/tsc` 确认类型通过。验证：tsc 无错误。

## Workflow follow-up

- admin-web 校区审核图形界面为可选项，MVP 仅提供审核 API（见 design.md 决策 5）。
- 完成后按项目规范 `/opsx:archive` 归档本 change。
