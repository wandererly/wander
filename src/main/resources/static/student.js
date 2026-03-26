const state = {
  selectedCourseId: null,
  selectedCourseTitle: "",
  courses: [],
  page: 0,
  totalPages: 1,
  myPage: 0,
  myTotalPages: 1,
};

const el = (id) => document.getElementById(id);
const has = (id) => !!el(id);
const statusLabel = (s) =>
  s === "PUBLISHED" ? "已发布" : s === "DRAFT" ? "草稿" : s === "OFFLINE" ? "已下架" : s || "-";
const roleLabel = (r) => (r === "TEACHER" ? "教师" : r === "STUDENT" ? "学生" : r || "-");

async function loadCourses(keyword = "") {
  if (!has("course-list")) return;
  const list = el("course-list");
  list.innerHTML = "";
  try {
    const status = has("filter-status") ? el("filter-status").value : "";
    const query = new URLSearchParams({ page: String(state.page), size: "10" });
    if (keyword) query.set("keyword", keyword);
    if (status) query.set("status", status);
    const result = await request(`/api/courses?${query.toString()}`);
    state.courses = result.data.content || [];
    state.totalPages = result.data.totalPages || 1;
    if (has("page-info")) {
      el("page-info").textContent = `${state.page + 1} / ${state.totalPages}`;
    }
    if (state.courses.length === 0) {
      if (has("course-empty")) el("course-empty").classList.remove("hidden");
      return;
    }
    if (has("course-empty")) el("course-empty").classList.add("hidden");
    state.courses.forEach((course) => {
      const item = document.createElement("div");
      item.className = "course-item";
      item.innerHTML = `
        <h3>${course.title}</h3>
        <p class="muted">${course.description || "暂无简介"}</p>
        <div class="course-meta">
          <span>状态: ${statusLabel(course.status)}</span>
          <span>课程ID: ${course.id}</span>
        </div>
        <button class="btn ghost" data-course="${course.id}">查看详情</button>
      `;
      item.querySelector("button").addEventListener("click", () => {
        if (has("course-title")) {
          selectCourse(course);
        } else {
          window.location.href = `/student-progress.html?courseId=${course.id}`;
        }
      });
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

async function selectCourse(course) {
  state.selectedCourseId = course.id;
  state.selectedCourseTitle = course.title;
  if (has("course-title")) el("course-title").textContent = `${course.title} (ID ${course.id})`;
  if (has("course-desc")) el("course-desc").textContent = course.description || "暂无简介";
  if (has("course-status")) el("course-status").textContent = statusLabel(course.status);
  if (has("course-meta")) el("course-meta").classList.remove("hidden");
  if (has("course-actions")) el("course-actions").classList.remove("hidden");
  await loadSections();
  await loadProgress();
}

async function loadCourseById(courseId) {
  if (!courseId) return;
  try {
    const result = await request(`/api/courses/${courseId}`);
    await selectCourse(result.data);
  } catch (err) {
    if (has("progress-hint")) el("progress-hint").textContent = err.message;
  }
}

async function loadSections() {
  if (!has("section-list")) return;
  const list = el("section-list");
  list.innerHTML = "";
  if (!state.selectedCourseId) return;
  try {
    const result = await request(`/api/courses/${state.selectedCourseId}/sections`);
    const sections = result.data || [];
    if (sections.length === 0) {
      list.innerHTML = "<div class='muted'>该课程暂无章节</div>";
      return;
    }
    sections.forEach((section) => {
      const item = document.createElement("div");
      item.className = "section-item";
      item.dataset.sectionId = section.id;
      const contentLink = section.contentUrl
        ? `<a class="btn ghost" href="${section.contentUrl}" target="_blank" rel="noreferrer">打开内容</a>`
        : `<span class="muted">暂无内容链接</span>`;
      item.innerHTML = `
        <strong>${section.orderIndex}. ${section.title}</strong>
        <span class="muted">${section.contentUrl || "未设置内容链接"}</span>
        <div class="inline">
          <button class="btn primary btn-complete">完成</button>
          <button class="btn ghost btn-undo">取消完成</button>
          ${contentLink}
        </div>
      `;
      item.querySelector(".btn-complete").addEventListener("click", () => completeSection(section.id));
      item.querySelector(".btn-undo").addEventListener("click", () => undoSection(section.id));
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

async function loadProgress() {
  if (!has("progress-summary") || !has("progress-visual")) return;
  if (!state.selectedCourseId) return;
  const summary = el("progress-summary");
  const visual = el("progress-visual");
  summary.textContent = "-";
  visual.classList.add("hidden");
  try {
    const result = await request(`/api/enrollments/${state.selectedCourseId}/progress`);
    const data = result.data;
    summary.textContent = `已完成 ${data.completedSectionCount} 节，进度 ${data.progressPercent}%`;
    updateRing(data.progressPercent || 0);
    el("progress-percent").textContent = `${data.progressPercent || 0}%`;
    visual.classList.remove("hidden");
    markCompleted(data.sections || []);
  } catch (err) {
    summary.textContent = err.message;
    visual.classList.remove("hidden");
  }
}

function updateRing(percent) {
  if (!has("ring-progress")) return;
  const circle = el("ring-progress");
  const clamped = Math.max(0, Math.min(percent, 100));
  const circumference = 2 * Math.PI * 52;
  const offset = circumference - (clamped / 100) * circumference;
  circle.style.strokeDasharray = `${circumference}`;
  circle.style.strokeDashoffset = `${offset}`;
}

function markCompleted(sections) {
  const completedIds = new Set(sections.filter((s) => s.completed).map((s) => s.sectionId));
  document.querySelectorAll(".section-item").forEach((item) => {
    const id = Number(item.dataset.sectionId);
    if (completedIds.has(id)) {
      item.style.borderColor = "#1f5b8f";
    } else {
      item.style.borderColor = "#e4dbcf";
    }
  });
}

async function enrollCourse() {
  if (!state.selectedCourseId) return;
  try {
    await request(`/api/enrollments/${state.selectedCourseId}`, { method: "POST" });
    await loadProgress();
  } catch (err) {
    alert(err.message);
  }
}

async function completeSection(sectionId) {
  if (!state.selectedCourseId) return;
  try {
    await request(`/api/enrollments/${state.selectedCourseId}/sections/${sectionId}/complete`, {
      method: "POST",
    });
    await loadProgress();
  } catch (err) {
    alert(err.message);
  }
}

async function undoSection(sectionId) {
  if (!state.selectedCourseId) return;
  try {
    await request(`/api/enrollments/${state.selectedCourseId}/sections/${sectionId}/complete`, {
      method: "DELETE",
    });
    await loadProgress();
  } catch (err) {
    alert(err.message);
  }
}

function initEvents() {
  if (has("btn-search")) {
    el("btn-search").addEventListener("click", () => {
      state.page = 0;
      loadCourses(has("search-keyword") ? el("search-keyword").value.trim() : "");
    });
  }
  if (has("btn-prev")) {
    el("btn-prev").addEventListener("click", () => {
      if (state.page > 0) {
        state.page -= 1;
        loadCourses(has("search-keyword") ? el("search-keyword").value.trim() : "");
      }
    });
  }
  if (has("btn-next")) {
    el("btn-next").addEventListener("click", () => {
      if (state.page + 1 < state.totalPages) {
        state.page += 1;
        loadCourses(has("search-keyword") ? el("search-keyword").value.trim() : "");
      }
    });
  }
  if (has("filter-status")) {
    el("filter-status").addEventListener("change", () => {
      state.page = 0;
      loadCourses(has("search-keyword") ? el("search-keyword").value.trim() : "");
    });
  }
  if (has("btn-enroll")) el("btn-enroll").addEventListener("click", enrollCourse);
  if (has("btn-refresh-progress")) el("btn-refresh-progress").addEventListener("click", loadProgress);
  if (has("btn-logout")) el("btn-logout").addEventListener("click", logout);
  if (has("btn-role-request")) el("btn-role-request").addEventListener("click", submitRoleRequest);
  if (has("btn-refresh-notify")) el("btn-refresh-notify").addEventListener("click", loadNotifications);
  if (has("btn-refresh-my")) el("btn-refresh-my").addEventListener("click", () => loadMyCourses());
  if (has("btn-load-course")) {
    el("btn-load-course").addEventListener("click", () => {
      const value = el("progress-course-id").value.trim();
      if (!value) return;
      loadCourseById(value);
    });
  }
  if (has("btn-my-prev")) {
    el("btn-my-prev").addEventListener("click", () => {
      if (state.myPage > 0) {
        state.myPage -= 1;
        loadMyCourses();
      }
    });
  }
  if (has("btn-my-next")) {
    el("btn-my-next").addEventListener("click", () => {
      if (state.myPage + 1 < state.myTotalPages) {
        state.myPage += 1;
        loadMyCourses();
      }
    });
  }
}

async function init() {
  const user = await requireAuth("STUDENT");
  if (!user) return;
  if (has("student-user")) el("student-user").textContent = `${user.username} · 学生`;
  initEvents();
  if (has("course-list")) loadCourses();
  if (has("role-request-list")) loadMyRequests();
  if (has("notify-list")) loadNotifications();
  if (has("my-course-list")) loadMyCourses();
  if (has("progress-course-id")) {
    const params = new URLSearchParams(window.location.search);
    const courseId = params.get("courseId");
    if (courseId) {
      el("progress-course-id").value = courseId;
      loadCourseById(courseId);
    }
  }
}

async function submitRoleRequest() {
  if (!has("role-request-target")) return;
  const targetRole = el("role-request-target").value;
  const reason = has("role-request-reason") ? el("role-request-reason").value.trim() : "";
  const msg = el("role-request-message");
  msg.textContent = "";
  try {
    await request("/api/role-requests", {
      method: "POST",
      body: JSON.stringify({ targetRole, reason }),
    });
    msg.textContent = "申请已提交";
    msg.style.color = "#1f5b8f";
    if (has("role-request-reason")) el("role-request-reason").value = "";
  } catch (err) {
    msg.textContent = err.message;
    msg.style.color = "#ff7a59";
  }
}

async function loadMyRequests() {
  if (!has("role-request-list")) return;
  const list = el("role-request-list");
  list.innerHTML = "";
  try {
    const result = await request("/api/role-requests/my");
    const requests = result.data || [];
    if (requests.length === 0) {
      list.innerHTML = "<div class='muted'>暂无申请记录</div>";
      return;
    }
    requests.forEach((r) => {
      const item = document.createElement("div");
      item.className = "course-item";
      item.innerHTML = `
        <h3>申请 #${r.id}</h3>
        <div class="course-meta">
          <span>目标: ${roleLabel(r.targetRole)}</span>
          <span>状态: ${r.status === "PENDING" ? "待处理" : r.status === "APPROVED" ? "已通过" : "已拒绝"}</span>
        </div>
        <p class="muted">${r.reason || "无理由"}</p>
        <p class="muted">${r.adminNote ? "管理员备注: " + r.adminNote : ""}</p>
      `;
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

async function loadNotifications() {
  if (!has("notify-list")) return;
  const list = el("notify-list");
  list.innerHTML = "";
  try {
    const countResult = await request("/api/notifications/unread-count");
    if (has("notify-count")) el("notify-count").textContent = `未读 ${countResult.data}`;
    const result = await request("/api/notifications");
    const notifications = result.data || [];
    if (notifications.length === 0) {
      list.innerHTML = "<div class='muted'>暂无通知</div>";
      return;
    }
    notifications.forEach((n) => {
      const item = document.createElement("div");
      item.className = "course-item";
      item.innerHTML = `
        <h3>${n.title}</h3>
        <p class="muted">${n.content || ""}</p>
        <div class="course-meta">
          <span>${n.readFlag ? "已读" : "未读"}</span>
          <span>${new Date(n.createdAt).toLocaleString()}</span>
        </div>
        ${n.readFlag ? "" : "<button class='btn ghost btn-read'>标记已读</button>"}
      `;
      if (!n.readFlag) {
        item.querySelector(".btn-read").addEventListener("click", () => markRead(n.id));
      }
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

async function markRead(id) {
  try {
    await request(`/api/notifications/${id}/read`, { method: "PATCH" });
    loadNotifications();
  } catch (err) {
    alert(err.message);
  }
}

async function loadMyCourses() {
  if (!has("my-course-list")) return;
  const list = el("my-course-list");
  list.innerHTML = "";
  try {
    const query = new URLSearchParams({ page: String(state.myPage), size: "10" });
    const result = await request(`/api/courses/my?${query.toString()}`);
    const courses = result.data.content || [];
    state.myTotalPages = result.data.totalPages || 1;
    if (has("my-page-info")) el("my-page-info").textContent = `${state.myPage + 1} / ${state.myTotalPages}`;
    if (courses.length === 0) {
      el("my-course-empty").classList.remove("hidden");
      return;
    }
    el("my-course-empty").classList.add("hidden");
    courses.forEach((c) => {
      const item = document.createElement("div");
      item.className = "course-item";
      item.innerHTML = `
        <h3>${c.title}</h3>
        <p class="muted">${c.description || "暂无简介"}</p>
        <div class="course-meta">
          <span>状态: ${statusLabel(c.status)}</span>
          <span>进度: ${c.progressPercent || 0}%</span>
        </div>
        <button class="btn ghost">查看详情</button>
      `;
      item.querySelector("button").addEventListener("click", () => {
        if (has("course-title")) {
          selectCourse({
            id: c.courseId,
            title: c.title,
            description: c.description,
            status: c.status,
          });
        } else {
          window.location.href = `/student-progress.html?courseId=${c.courseId}`;
        }
      });
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

document.addEventListener("DOMContentLoaded", init);
