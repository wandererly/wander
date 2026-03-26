const el = (id) => document.getElementById(id);
const has = (id) => !!el(id);
const msg = has("admin-message") ? el("admin-message") : null;
const roleLabel = (r) => (r === "STUDENT" ? "学生" : r === "TEACHER" ? "教师" : r === "ADMIN" ? "管理员" : r || "-");
const statusLabel = (s) => (s === "NORMAL" ? "正常" : s === "BANNED" ? "禁用" : s || "-");

const state = {
  page: 0,
  totalPages: 1,
  auditPage: 0,
  auditTotalPages: 1,
};

function setMessage(text, isError) {
  if (!msg) return;
  msg.textContent = text || "";
  msg.style.color = isError ? "#ff7a59" : "#1f5b8f";
}

async function updateRole() {
  if (!has("admin-user-id")) return;
  const userId = el("admin-user-id").value.trim();
  const role = el("admin-user-role").value;
  if (!userId) {
    setMessage("请输入用户 ID", true);
    return;
  }
  if (!confirm("确认更新用户角色？")) return;
  try {
    await request(`/api/admin/users/${userId}/role`, {
      method: "PATCH",
      body: JSON.stringify({ role }),
    });
    setMessage("角色已更新", false);
  } catch (err) {
    setMessage(err.message, true);
  }
}

function initEvents() {
  if (has("btn-logout")) el("btn-logout").addEventListener("click", logout);
  if (has("btn-update-role")) el("btn-update-role").addEventListener("click", updateRole);
  if (has("btn-refresh-requests")) el("btn-refresh-requests").addEventListener("click", loadRequests);
  if (has("btn-refresh-users")) el("btn-refresh-users").addEventListener("click", loadUsers);
  if (has("btn-user-search")) {
    el("btn-user-search").addEventListener("click", () => {
      state.page = 0;
      loadUsers();
    });
  }
  if (has("btn-user-prev")) {
    el("btn-user-prev").addEventListener("click", () => {
      if (state.page > 0) {
        state.page -= 1;
        loadUsers();
      }
    });
  }
  if (has("btn-user-next")) {
    el("btn-user-next").addEventListener("click", () => {
      if (state.page + 1 < state.totalPages) {
        state.page += 1;
        loadUsers();
      }
    });
  }
  if (has("user-role-filter")) {
    el("user-role-filter").addEventListener("change", () => {
      state.page = 0;
      loadUsers();
    });
  }
  if (has("user-status-filter")) {
    el("user-status-filter").addEventListener("change", () => {
      state.page = 0;
      loadUsers();
    });
  }
  if (has("btn-batch-role")) el("btn-batch-role").addEventListener("click", batchRole);
  if (has("btn-batch-status")) el("btn-batch-status").addEventListener("click", batchStatus);
  if (has("btn-user-detail")) el("btn-user-detail").addEventListener("click", loadUserDetail);
  if (has("btn-audit-search")) {
    el("btn-audit-search").addEventListener("click", () => {
      state.auditPage = 0;
      loadAuditLogs();
    });
  }
  if (has("btn-audit-export")) el("btn-audit-export").addEventListener("click", exportAuditLogs);
  if (has("btn-audit-prev")) {
    el("btn-audit-prev").addEventListener("click", () => {
      if (state.auditPage > 0) {
        state.auditPage -= 1;
        loadAuditLogs();
      }
    });
  }
  if (has("btn-audit-next")) {
    el("btn-audit-next").addEventListener("click", () => {
      if (state.auditPage + 1 < state.auditTotalPages) {
        state.auditPage += 1;
        loadAuditLogs();
      }
    });
  }
  if (has("btn-create-user")) el("btn-create-user").addEventListener("click", createUser);
}

async function init() {
  const user = await requireAuth("ADMIN");
  if (!user) return;
  if (has("admin-user")) el("admin-user").textContent = `${user.username} · ${roleLabel(user.role)}`;
  initEvents();
  if (has("request-list")) loadRequests();
  if (has("user-list")) loadUsers();
  if (has("audit-list")) loadAuditLogs();
}

async function loadRequests() {
  if (!has("request-list")) return;
  const list = el("request-list");
  list.innerHTML = "";
  try {
    const result = await request("/api/admin/users/role-requests?status=PENDING");
    const requests = result.data || [];
    if (requests.length === 0) {
      list.innerHTML = "<div class='muted'>暂无待处理申请</div>";
      return;
    }
    requests.forEach((r) => {
      const item = document.createElement("div");
      item.className = "course-item";
      item.innerHTML = `
        <h3>申请 #${r.id}</h3>
        <p class="muted">用户 ${r.username} (ID ${r.userId})</p>
        <div class="course-meta">
          <span>当前: ${roleLabel(r.currentRole)}</span>
          <span>目标: ${roleLabel(r.targetRole)}</span>
        </div>
        <p class="muted">${r.reason || "无理由"}</p>
        <p class="muted">${r.adminNote ? "管理员备注: " + r.adminNote : ""}</p>
        <div class="inline">
          <button class="btn primary btn-approve">通过</button>
          <button class="btn danger btn-reject">拒绝</button>
        </div>
      `;
      item.querySelector(".btn-approve").addEventListener("click", () => approveRequest(r.id));
      item.querySelector(".btn-reject").addEventListener("click", () => rejectRequest(r.id));
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

async function approveRequest(id) {
  if (!confirm("确认通过该申请？")) return;
  try {
    const note = has("admin-note") ? el("admin-note").value.trim() : "";
    await request(`/api/admin/users/role-requests/${id}/approve`, {
      method: "PATCH",
      body: JSON.stringify({ adminNote: note }),
    });
    loadRequests();
    loadUsers();
  } catch (err) {
    alert(err.message);
  }
}

async function rejectRequest(id) {
  if (!confirm("确认拒绝该申请？")) return;
  try {
    const note = has("admin-note") ? el("admin-note").value.trim() : "";
    await request(`/api/admin/users/role-requests/${id}/reject`, {
      method: "PATCH",
      body: JSON.stringify({ adminNote: note }),
    });
    loadRequests();
  } catch (err) {
    alert(err.message);
  }
}

async function loadUsers() {
  if (!has("user-list")) return;
  const list = el("user-list");
  list.innerHTML = "";
  try {
    const keyword = has("user-keyword") ? el("user-keyword").value.trim() : "";
    const role = has("user-role-filter") ? el("user-role-filter").value : "";
    const status = has("user-status-filter") ? el("user-status-filter").value : "";
    const query = new URLSearchParams({ page: String(state.page), size: "10" });
    if (keyword) query.set("keyword", keyword);
    if (role) query.set("role", role);
    if (status) query.set("status", status);
    const result = await request(`/api/admin/users?${query.toString()}`);
    const users = result.data.content || [];
    state.totalPages = result.data.totalPages || 1;
    if (has("user-page-info")) el("user-page-info").textContent = `${state.page + 1} / ${state.totalPages}`;
    if (users.length === 0) {
      list.innerHTML = "<div class='muted'>暂无用户</div>";
      return;
    }
    users.forEach((u) => {
      const item = document.createElement("div");
      item.className = "course-item";
      item.innerHTML = `
        <h3>${u.username}</h3>
        <p class="muted">${u.email}</p>
        <div class="course-meta">
          <span>角色: ${roleLabel(u.role)}</span>
          <span>状态: ${statusLabel(u.status)}</span>
          <span>ID: ${u.id}</span>
        </div>
        <div class="inline">
          <select class="role-select">
            <option value="STUDENT" ${u.role === "STUDENT" ? "selected" : ""}>学生</option>
            <option value="TEACHER" ${u.role === "TEACHER" ? "selected" : ""}>教师</option>
            <option value="ADMIN" ${u.role === "ADMIN" ? "selected" : ""}>管理员</option>
          </select>
          <button class="btn ghost btn-set-role">更新角色</button>
          <select class="status-select">
            <option value="NORMAL" ${u.status === "NORMAL" ? "selected" : ""}>正常</option>
            <option value="BANNED" ${u.status === "BANNED" ? "selected" : ""}>禁用</option>
          </select>
          <button class="btn ghost btn-set-status">更新状态</button>
          <button class="btn danger btn-reset-pwd">重置密码</button>
          <button class="btn danger btn-delete-user">删除用户</button>
        </div>
      `;
      item.querySelector(".btn-set-role").addEventListener("click", () => updateUserRole(u.id, item));
      item.querySelector(".btn-set-status").addEventListener("click", () => updateUserStatus(u.id, item));
      item.querySelector(".btn-reset-pwd").addEventListener("click", () => resetUserPassword(u.id));
      item.querySelector(".btn-delete-user").addEventListener("click", () => deleteUser(u.id));
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

async function updateUserRole(userId, item) {
  const role = item.querySelector(".role-select").value;
  if (!confirm("确认更新用户角色？")) return;
  try {
    await request(`/api/admin/users/${userId}/role`, {
      method: "PATCH",
      body: JSON.stringify({ role }),
    });
    loadUsers();
  } catch (err) {
    alert(err.message);
  }
}

async function updateUserStatus(userId, item) {
  const status = item.querySelector(".status-select").value;
  if (!confirm("确认更新用户状态？")) return;
  try {
    await request(`/api/admin/users/${userId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
    loadUsers();
  } catch (err) {
    alert(err.message);
  }
}

async function resetUserPassword(userId) {
  const pwd = prompt("输入新密码");
  if (!pwd) return;
  if (!confirm("确认重置密码？")) return;
  try {
    await request(`/api/admin/users/${userId}/password`, {
      method: "PATCH",
      body: JSON.stringify({ password: pwd }),
    });
    alert("密码已重置");
  } catch (err) {
    alert(err.message);
  }
}

async function deleteUser(userId) {
  if (!confirm("确认删除用户？将清理相关数据")) return;
  try {
    await request(`/api/admin/users/${userId}`, { method: "DELETE" });
    loadUsers();
  } catch (err) {
    alert(err.message);
  }
}

function parseIds(input) {
  return input
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
    .map((s) => Number(s))
    .filter((n) => Number.isFinite(n));
}

async function batchRole() {
  if (!has("batch-user-ids")) return;
  const ids = parseIds(el("batch-user-ids").value);
  const role = el("batch-role").value;
  const msg = el("batch-message");
  if (ids.length === 0) {
    msg.textContent = "请输入有效用户 ID";
    msg.style.color = "#ff7a59";
    return;
  }
  if (!confirm("确认批量更新角色？")) return;
  try {
    await request("/api/admin/users/batch/role", {
      method: "POST",
      body: JSON.stringify({ userIds: ids, role }),
    });
    msg.textContent = "批量角色更新完成";
    msg.style.color = "#1f5b8f";
    loadUsers();
  } catch (err) {
    msg.textContent = err.message;
    msg.style.color = "#ff7a59";
  }
}

async function batchStatus() {
  if (!has("batch-user-ids")) return;
  const ids = parseIds(el("batch-user-ids").value);
  const status = el("batch-status").value;
  const msg = el("batch-message");
  if (ids.length === 0) {
    msg.textContent = "请输入有效用户 ID";
    msg.style.color = "#ff7a59";
    return;
  }
  if (!confirm("确认批量更新状态？")) return;
  try {
    await request("/api/admin/users/batch/status", {
      method: "POST",
      body: JSON.stringify({ userIds: ids, status }),
    });
    msg.textContent = "批量状态更新完成";
    msg.style.color = "#1f5b8f";
    loadUsers();
  } catch (err) {
    msg.textContent = err.message;
    msg.style.color = "#ff7a59";
  }
}

async function loadUserDetail() {
  if (!has("detail-user-id")) return;
  const userId = el("detail-user-id").value.trim();
  const box = el("user-detail");
  box.innerHTML = "";
  if (!userId) {
    box.innerHTML = "<div class='muted'>请输入用户 ID</div>";
    return;
  }
  try {
    const result = await request(`/api/admin/users/${userId}/detail`);
    const u = result.data;
    const courses = u.courses || [];
    const courseHtml = courses
      .map(
        (c) =>
          `<div class="course-item">
            <strong>${c.title}</strong>
            <div class="course-meta">
              <span>ID ${c.courseId}</span>
              <span>进度 ${c.progressPercent || 0}%</span>
            </div>
            <div class="inline">
              <a class="btn ghost" href="/student.html" target="_blank">打开学生主页</a>
            </div>
          </div>`
      )
      .join("");
    box.innerHTML = `
      <div class="course-item">
        <h3>${u.username}</h3>
        <p class="muted">${u.email}</p>
        <div class="course-meta">
          <span>角色: ${roleLabel(u.role)}</span>
          <span>状态: ${statusLabel(u.status)}</span>
          <span>ID: ${u.id}</span>
          <span>创建: ${u.createdAt ? new Date(u.createdAt).toLocaleString() : "-"}</span>
          <span>更新: ${u.updatedAt ? new Date(u.updatedAt).toLocaleString() : "-"}</span>
          <span>最近学习: ${u.lastLearningAt ? new Date(u.lastLearningAt).toLocaleString() : "-"}</span>
        </div>
      </div>
      <div class="grid">${courseHtml || "<div class='muted'>暂无课程记录</div>"}</div>
    `;
  } catch (err) {
    box.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

async function loadAuditLogs() {
  if (!has("audit-list")) return;
  const list = el("audit-list");
  list.innerHTML = "";
  try {
    const keyword = has("audit-keyword") ? el("audit-keyword").value.trim() : "";
    const action = has("audit-action") ? el("audit-action").value.trim() : "";
    const targetType = has("audit-target") ? el("audit-target").value.trim() : "";
    const query = new URLSearchParams({ page: String(state.auditPage), size: "10" });
    if (keyword) query.set("keyword", keyword);
    if (action) query.set("action", action);
    if (targetType) query.set("targetType", targetType);
    const result = await request(`/api/admin/users/audit-logs?${query.toString()}`);
    const logs = result.data.content || [];
    state.auditTotalPages = result.data.totalPages || 1;
    if (has("audit-page-info")) el("audit-page-info").textContent = `${state.auditPage + 1} / ${state.auditTotalPages}`;
    if (logs.length === 0) {
      list.innerHTML = "<div class='muted'>暂无日志</div>";
      return;
    }
    logs.forEach((l) => {
      const item = document.createElement("div");
      item.className = "course-item";
      item.innerHTML = `
        <h3>${l.action}</h3>
        <p class="muted">${l.detail || ""}</p>
        <div class="course-meta">
          <span>管理员: ${l.adminUsername}</span>
          <span>目标: ${l.targetType || "-"} ${l.targetId || ""}</span>
          <span>${new Date(l.createdAt).toLocaleString()}</span>
        </div>
      `;
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

function exportAuditLogs() {
  if (!has("audit-keyword")) return;
  const keyword = el("audit-keyword").value.trim();
  const action = el("audit-action").value.trim();
  const targetType = el("audit-target").value.trim();
  const query = new URLSearchParams();
  if (keyword) query.set("keyword", keyword);
  if (action) query.set("action", action);
  if (targetType) query.set("targetType", targetType);
  const url = `/api/admin/users/audit-logs/export?${query.toString()}`;
  window.open(url, "_blank");
}

async function createUser() {
  if (!has("create-username")) return;
  const username = el("create-username").value.trim();
  const email = el("create-email").value.trim();
  const password = el("create-password").value.trim();
  const role = el("create-role").value;
  const status = el("create-status").value;
  const msg = el("create-message");
  if (!username || !email || !password) {
    msg.textContent = "请填写完整信息";
    msg.style.color = "#ff7a59";
    return;
  }
  try {
    await request("/api/admin/users", {
      method: "POST",
      body: JSON.stringify({ username, email, password, role, status }),
    });
    msg.textContent = "用户已创建";
    msg.style.color = "#1f5b8f";
    el("create-username").value = "";
    el("create-email").value = "";
    el("create-password").value = "";
    loadUsers();
  } catch (err) {
    msg.textContent = err.message;
    msg.style.color = "#ff7a59";
  }
}

document.addEventListener("DOMContentLoaded", init);
