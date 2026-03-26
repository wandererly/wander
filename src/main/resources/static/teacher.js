const el = (id) => document.getElementById(id);
const has = (id) => !!el(id);
const statusLabel = (s) =>
  s === "PUBLISHED" ? "已发布" : s === "DRAFT" ? "草稿" : s === "OFFLINE" ? "已下架" : s || "-";
const roleLabel = (r) => (r === "STUDENT" ? "学生" : r === "TEACHER" ? "教师" : r || "-");

function setTeacherStatus(message, isError) {
  const msg = el("teacher-message");
  if (!msg) return;
  msg.textContent = message || "";
  msg.style.color = isError ? "#ff7a59" : "#1f5b8f";
}

const state = {
  page: 0,
  totalPages: 1,
  courseOptions: new Map(),
  sectionOptions: new Map(),
  previewSections: [],
};

async function loadCourses() {
  if (!has("course-list")) return;
  const list = el("course-list");
  list.innerHTML = "";
  try {
    const keyword = has("teacher-search-keyword") ? el("teacher-search-keyword").value.trim() : "";
    const status = has("teacher-filter-status") ? el("teacher-filter-status").value : "";
    const query = new URLSearchParams({ page: String(state.page), size: "10" });
    if (keyword) query.set("keyword", keyword);
    if (status) query.set("status", status);
    const result = await request(`/api/courses/my-teaching?${query.toString()}`);
    const courses = result.data.content || [];
    state.totalPages = result.data.totalPages || 1;
    if (has("teacher-page-info")) {
      el("teacher-page-info").textContent = `${state.page + 1} / ${state.totalPages}`;
    }
    if (courses.length === 0) {
      list.innerHTML = "<div class='muted'>暂无课程</div>";
      return;
    }
    courses.forEach((course) => {
      const item = document.createElement("div");
      item.className = "course-item";
      item.innerHTML = `
        <h3>${course.title}</h3>
        <p class="muted">${course.description || "暂无简介"}</p>
        <div class="course-meta">
          <span>状态: ${statusLabel(course.status)}</span>
          <span>课程ID: ${course.id}</span>
        </div>
        <div class="inline">
          <button class="btn ghost btn-copy-id">复制ID</button>
          <button class="btn ghost btn-select">选择并进入</button>
          <a class="btn ghost" href="/teacher-course-manage.html?courseId=${course.id}">去管理</a>
          <a class="btn ghost" href="/teacher-course-preview.html?courseId=${course.id}">预览</a>
        </div>
      `;
      item.querySelector(".btn-copy-id").addEventListener("click", () => copyCourseId(course.id));
      item.querySelector(".btn-select").addEventListener("click", () => {
        saveSelectedCourse(course);
        window.location.href = `/teacher-course-manage.html?courseId=${course.id}`;
      });
      list.appendChild(item);
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

async function copyCourseId(id) {
  try {
    await navigator.clipboard.writeText(String(id));
    alert("课程 ID 已复制");
  } catch (err) {
    alert(`课程 ID: ${id}`);
  }
}

function saveSelectedCourse(course) {
  if (!course) return;
  const payload = {
    id: course.id,
    title: course.title,
    status: course.status,
  };
  localStorage.setItem("teacher_selected_course", JSON.stringify(payload));
}

function readSelectedCourse() {
  const raw = localStorage.getItem("teacher_selected_course");
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (err) {
    return null;
  }
}

function applySelectedCourse(course) {
  if (!course) return;
  if (has("teacher-course-id")) el("teacher-course-id").value = course.id;
  if (has("teacher-section-course-id")) el("teacher-section-course-id").value = course.id;
  if (has("selected-course-info")) {
    el("selected-course-info").textContent = `${course.title} · ID ${course.id} · ${statusLabel(course.status)}`;
  }
  saveSelectedCourse(course);
  loadSectionOptions(course.id);
}

async function loadCourseOptions() {
  if (!has("teacher-course-select")) return;
  const select = el("teacher-course-select");
  select.innerHTML = "<option value=\"\">请选择课程</option>";
  state.courseOptions.clear();
  try {
    const result = await request("/api/courses/my-teaching?page=0&size=50");
    const courses = result.data.content || [];
    courses.forEach((course) => {
      const option = document.createElement("option");
      option.value = course.id;
      option.textContent = `${course.title} (ID ${course.id})`;
      select.appendChild(option);
      state.courseOptions.set(String(course.id), course);
    });
    const preset = readSelectedCourse();
    if (preset && state.courseOptions.has(String(preset.id))) {
      select.value = String(preset.id);
      applySelectedCourse(state.courseOptions.get(String(preset.id)));
    }
  } catch (err) {
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "加载失败";
    select.appendChild(option);
  }
}

async function loadSectionOptions(courseId) {
  if (!has("teacher-section-select")) return;
  const select = el("teacher-section-select");
  select.innerHTML = "<option value=\"\">请选择章节</option>";
  state.sectionOptions.clear();
  if (!courseId) return;
  try {
    const result = await request(`/api/courses/${courseId}/sections`);
    const sections = result.data || [];
    sections.forEach((section) => {
      const option = document.createElement("option");
      option.value = section.id;
      option.textContent = `${section.orderIndex}. ${section.title} (ID ${section.id})`;
      select.appendChild(option);
      state.sectionOptions.set(String(section.id), section);
    });
    if (sections.length === 0) {
      const option = document.createElement("option");
      option.value = "";
      option.textContent = "暂无章节";
      select.appendChild(option);
    }
  } catch (err) {
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "加载失败";
    select.appendChild(option);
  }
}

function applySelectedSection(section) {
  if (!section) return;
  if (has("teacher-section-id")) el("teacher-section-id").value = section.id;
  if (has("teacher-section-title")) el("teacher-section-title").value = section.title || "";
  if (has("teacher-section-order")) el("teacher-section-order").value = section.orderIndex || "";
  if (has("teacher-section-url")) el("teacher-section-url").value = section.contentUrl || "";
  if (has("selected-section-info")) {
    el("selected-section-info").textContent = `${section.orderIndex}. ${section.title} · ID ${section.id}`;
  }
}

function updatePreviewRing(percent) {
  if (!has("student-view-ring")) return;
  const circle = el("student-view-ring");
  const clamped = Math.max(0, Math.min(percent, 100));
  const circumference = 2 * Math.PI * 52;
  const offset = circumference - (clamped / 100) * circumference;
  circle.style.strokeDasharray = `${circumference}`;
  circle.style.strokeDashoffset = `${offset}`;
  if (has("student-view-percent")) el("student-view-percent").textContent = `${clamped}%`;
}

async function createCourse() {
  if (!has("teacher-course-title")) return;
  const title = el("teacher-course-title").value.trim();
  const description = el("teacher-course-desc").value.trim();
  if (!title) {
    setTeacherStatus("请输入课程标题", true);
    return;
  }
  try {
    await request("/api/courses", {
      method: "POST",
      body: JSON.stringify({ title, description }),
    });
    setTeacherStatus("课程已创建", false);
    el("teacher-course-title").value = "";
    el("teacher-course-desc").value = "";
    await loadCourses();
  } catch (err) {
    setTeacherStatus(err.message, true);
  }
}

async function updateCourse() {
  if (!has("teacher-course-id")) return;
  const courseId = el("teacher-course-id").value.trim();
  if (!courseId) {
    setTeacherStatus("请输入课程 ID", true);
    return;
  }
  const title = el("teacher-course-new-title").value.trim();
  const description = el("teacher-course-new-desc").value.trim();
  const status = el("teacher-course-status").value;
  const payload = {};
  if (title) payload.title = title;
  if (description) payload.description = description;
  if (status) payload.status = status;
  try {
    await request(`/api/courses/${courseId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
    setTeacherStatus("课程已更新", false);
    await loadCourses();
  } catch (err) {
    setTeacherStatus(err.message, true);
  }
}

async function offlineCourse() {
  if (!has("teacher-course-id")) return;
  const courseId = el("teacher-course-id").value.trim();
  if (!courseId) {
    setTeacherStatus("请输入课程 ID", true);
    return;
  }
  try {
    await request(`/api/courses/${courseId}/offline`, { method: "PATCH" });
    setTeacherStatus("课程已下架", false);
    await loadCourses();
  } catch (err) {
    setTeacherStatus(err.message, true);
  }
}

async function deleteCourse() {
  if (!has("teacher-course-id")) return;
  const courseId = el("teacher-course-id").value.trim();
  if (!courseId) {
    setTeacherStatus("请输入课程 ID", true);
    return;
  }
  if (!confirm("确认删除课程？将同时删除章节和选课数据")) return;
  try {
    await request(`/api/courses/${courseId}`, { method: "DELETE" });
    setTeacherStatus("课程已删除", false);
    await loadCourses();
  } catch (err) {
    setTeacherStatus(err.message, true);
  }
}

async function createSection() {
  const courseId = has("teacher-section-course-id")
    ? el("teacher-section-course-id").value.trim()
    : has("teacher-course-select")
    ? el("teacher-course-select").value
    : "";
  const title = el("teacher-section-title").value.trim();
  const orderIndex = el("teacher-section-order").value.trim();
  const contentUrl = el("teacher-section-url").value.trim();
  if (!courseId || !title || !orderIndex) {
    setTeacherStatus("请输入课程 ID、章节标题、顺序", true);
    return;
  }
  try {
    await request(`/api/courses/${courseId}/sections`, {
      method: "POST",
      body: JSON.stringify({
        title,
        orderIndex: Number(orderIndex),
        contentUrl,
      }),
    });
    setTeacherStatus("章节已创建", false);
    if (has("btn-refresh-sections")) loadSectionsManage();
  } catch (err) {
    setTeacherStatus(err.message, true);
  }
}

async function updateSection() {
  const courseId = has("teacher-section-course-id")
    ? el("teacher-section-course-id").value.trim()
    : has("teacher-course-select")
    ? el("teacher-course-select").value
    : "";
  const sectionId = el("teacher-section-id").value.trim();
  const title = el("teacher-section-title").value.trim();
  const orderIndex = el("teacher-section-order").value.trim();
  const contentUrl = el("teacher-section-url").value.trim();
  if (!courseId || !sectionId || !title || !orderIndex) {
    setTeacherStatus("请输入课程 ID、章节 ID、标题和顺序", true);
    return;
  }
  try {
    await request(`/api/courses/${courseId}/sections/${sectionId}`, {
      method: "PUT",
      body: JSON.stringify({
        title,
        orderIndex: Number(orderIndex),
        contentUrl,
      }),
    });
    setTeacherStatus("章节已更新", false);
    if (has("btn-refresh-sections")) loadSectionsManage();
  } catch (err) {
    setTeacherStatus(err.message, true);
  }
}

async function deleteSection() {
  const courseId = has("teacher-section-course-id")
    ? el("teacher-section-course-id").value.trim()
    : has("teacher-course-select")
    ? el("teacher-course-select").value
    : "";
  const sectionId = el("teacher-section-id").value.trim();
  if (!courseId || !sectionId) {
    setTeacherStatus("请输入课程 ID 和章节 ID", true);
    return;
  }
  if (!confirm("确认删除章节？")) return;
  try {
    await request(`/api/courses/${courseId}/sections/${sectionId}`, { method: "DELETE" });
    setTeacherStatus("章节已删除", false);
    if (has("btn-refresh-sections")) loadSectionsManage();
  } catch (err) {
    setTeacherStatus(err.message, true);
  }
}

function initEvents() {
  if (has("btn-logout")) el("btn-logout").addEventListener("click", logout);
  if (has("btn-refresh-courses")) el("btn-refresh-courses").addEventListener("click", loadCourses);
  if (has("btn-teacher-search")) {
    el("btn-teacher-search").addEventListener("click", () => {
      state.page = 0;
      loadCourses();
    });
  }
  if (has("btn-teacher-prev")) {
    el("btn-teacher-prev").addEventListener("click", () => {
      if (state.page > 0) {
        state.page -= 1;
        loadCourses();
      }
    });
  }
  if (has("btn-teacher-next")) {
    el("btn-teacher-next").addEventListener("click", () => {
      if (state.page + 1 < state.totalPages) {
        state.page += 1;
        loadCourses();
      }
    });
  }
  if (has("teacher-filter-status")) {
    el("teacher-filter-status").addEventListener("change", () => {
      state.page = 0;
      loadCourses();
    });
  }
  if (has("btn-create-course")) el("btn-create-course").addEventListener("click", createCourse);
  if (has("btn-update-course")) el("btn-update-course").addEventListener("click", updateCourse);
  if (has("btn-offline-course")) el("btn-offline-course").addEventListener("click", offlineCourse);
  if (has("btn-delete-course")) el("btn-delete-course").addEventListener("click", deleteCourse);
  if (has("btn-create-section")) el("btn-create-section").addEventListener("click", createSection);
  if (has("btn-update-section")) el("btn-update-section").addEventListener("click", updateSection);
  if (has("btn-delete-section")) el("btn-delete-section").addEventListener("click", deleteSection);
  if (has("btn-role-request")) el("btn-role-request").addEventListener("click", submitRoleRequest);
  if (has("btn-refresh-notify")) el("btn-refresh-notify").addEventListener("click", loadNotifications);
  if (has("btn-load-courses")) el("btn-load-courses").addEventListener("click", loadCourseOptions);
  if (has("teacher-course-select")) {
    el("teacher-course-select").addEventListener("change", (e) => {
      const value = e.target.value;
      if (!value) return;
      const course = state.courseOptions.get(String(value));
      applySelectedCourse(course);
      if (has("btn-refresh-sections")) {
        loadSectionsManage();
      }
      if (has("preview-mode")) {
        loadPreview();
      }
    });
  }
  if (has("preview-mode")) {
    el("preview-mode").addEventListener("change", () => {
      renderStudentPreview(state.previewSections || []);
    });
  }
  if (has("teacher-section-select")) {
    el("teacher-section-select").addEventListener("change", (e) => {
      const value = e.target.value;
      if (!value) return;
      const section = state.sectionOptions.get(String(value));
      applySelectedSection(section);
    });
  }
  if (has("btn-refresh-sections")) el("btn-refresh-sections").addEventListener("click", loadSectionsManage);
}

async function init() {
  const user = await requireAuth("TEACHER");
  if (!user) return;
  if (has("teacher-user")) el("teacher-user").textContent = `${user.username} · 教师`;
  initEvents();
  if (has("course-list")) loadCourses();
  if (has("role-request-list")) loadMyRequests();
  if (has("notify-list")) loadNotifications();
  if (has("teacher-course-select")) loadCourseOptions();
  if (has("teacher-course-id")) {
    const params = new URLSearchParams(window.location.search);
    const courseId = params.get("courseId");
    if (courseId) {
      el("teacher-course-id").value = courseId;
    }
  }
  if (has("teacher-section-course-id")) {
    const params = new URLSearchParams(window.location.search);
    const courseId = params.get("courseId");
    if (courseId) {
      el("teacher-section-course-id").value = courseId;
    }
  }
  const preset = readSelectedCourse();
  if (preset && has("selected-course-info")) {
    el("selected-course-info").textContent = `${preset.title} · ID ${preset.id} · ${statusLabel(preset.status)}`;
  }
  if (preset && has("teacher-section-select")) {
    loadSectionOptions(preset.id);
  }
  if (has("preview-id")) {
    loadPreview();
  }
  if (has("section-list-manage")) {
    loadSectionsManage();
  }
}

async function loadSectionsManage() {
  if (!has("section-list-manage")) return;
  const courseId = has("teacher-course-select") ? el("teacher-course-select").value : "";
  const list = el("section-list-manage");
  list.innerHTML = "";
  if (!courseId) {
    list.innerHTML = "<div class='muted'>请先选择课程</div>";
    return;
  }
  try {
    const result = await request(`/api/courses/${courseId}/sections`);
    const sections = result.data || [];
    if (sections.length === 0) {
      list.innerHTML = "<div class='muted'>暂无章节</div>";
      return;
    }
    list.innerHTML = sections
      .map(
        (s) => `
        <div class="section-item" draggable="true" data-id="${s.id}">
          <div class="inline" style="justify-content: space-between;">
            <strong>${s.orderIndex}. ${s.title}</strong>
            <span class="muted">ID ${s.id}</span>
          </div>
          <span class="drag-handle">拖拽调整顺序</span>
          <label>
            标题
            <input data-field="title" data-id="${s.id}" type="text" value="${s.title || ""}" />
          </label>
          <label>
            顺序
            <input data-field="order" data-id="${s.id}" type="number" value="${s.orderIndex || 1}" />
          </label>
          <label>
            内容链接
            <input data-field="url" data-id="${s.id}" type="text" value="${s.contentUrl || ""}" />
          </label>
          <div class="inline">
            <button class="btn ghost" data-action="save" data-id="${s.id}">保存</button>
            <button class="btn danger" data-action="delete" data-id="${s.id}">删除</button>
          </div>
        </div>`
      )
      .join("");

    enableDragSort(list);
    updateOrderInputs(list);

    list.querySelectorAll("button[data-action='save']").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-id");
        const title = list.querySelector(`input[data-field='title'][data-id='${id}']`).value.trim();
        const orderIndex = Number(
          list.querySelector(`input[data-field='order'][data-id='${id}']`).value
        );
        const contentUrl = list.querySelector(`input[data-field='url'][data-id='${id}']`).value.trim();
        if (!title || !orderIndex) {
          setTeacherStatus("标题和顺序不能为空", true);
          return;
        }
        try {
          await request(`/api/courses/${courseId}/sections/${id}`, {
            method: "PUT",
            body: JSON.stringify({ title, orderIndex, contentUrl }),
          });
          setTeacherStatus("章节已更新", false);
        } catch (err) {
          setTeacherStatus(err.message, true);
        }
      });
    });

    list.querySelectorAll("button[data-action='delete']").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-id");
        if (!confirm("确认删除该章节？")) return;
        try {
          await request(`/api/courses/${courseId}/sections/${id}`, { method: "DELETE" });
          setTeacherStatus("章节已删除", false);
          loadSectionsManage();
        } catch (err) {
          setTeacherStatus(err.message, true);
        }
      });
    });
  } catch (err) {
    list.innerHTML = `<div class="muted">${err.message}</div>`;
  }
}

function enableDragSort(list) {
  let dragEl = null;
  list.querySelectorAll(".section-item").forEach((item) => {
    item.addEventListener("dragstart", () => {
      dragEl = item;
      item.classList.add("dragging");
    });
    item.addEventListener("dragend", () => {
      item.classList.remove("dragging");
      dragEl = null;
      updateOrderInputs(list);
      saveSectionOrder();
    });
    item.addEventListener("dragover", (e) => {
      e.preventDefault();
      const target = e.currentTarget;
      if (!dragEl || dragEl === target) return;
      const rect = target.getBoundingClientRect();
      const next = e.clientY - rect.top > rect.height / 2;
      list.insertBefore(dragEl, next ? target.nextSibling : target);
    });
  });
}

function updateOrderInputs(list) {
  const items = Array.from(list.querySelectorAll(".section-item"));
  items.forEach((item, index) => {
    const id = item.getAttribute("data-id");
    const orderInput = list.querySelector(`input[data-field='order'][data-id='${id}']`);
    if (orderInput) {
      orderInput.value = String(index + 1);
    }
  });
}

async function saveSectionOrder() {
  if (!has("section-list-manage")) return;
  const courseId = has("teacher-course-select") ? el("teacher-course-select").value : "";
  if (!courseId) {
    setTeacherStatus("请先选择课程", true);
    return;
  }
  const list = el("section-list-manage");
  const items = Array.from(list.querySelectorAll(".section-item"));
  if (items.length === 0) return;
  try {
    for (let index = 0; index < items.length; index += 1) {
      const id = items[index].getAttribute("data-id");
      const title = list.querySelector(`input[data-field='title'][data-id='${id}']`).value.trim();
      const contentUrl = list.querySelector(`input[data-field='url'][data-id='${id}']`).value.trim();
      const orderIndex = index + 1;
      await request(`/api/courses/${courseId}/sections/${id}`, {
        method: "PUT",
        body: JSON.stringify({ title, orderIndex, contentUrl }),
      });
    }
    setTeacherStatus("排序已自动保存", false);
    loadSectionsManage();
  } catch (err) {
    setTeacherStatus(err.message, true);
  }
}

async function loadPreview() {
  const params = new URLSearchParams(window.location.search);
  const courseId = params.get("courseId") || (has("teacher-course-select") ? el("teacher-course-select").value : "");
  if (!courseId) return;
  try {
    const detail = await request(`/api/courses/${courseId}`);
    const course = detail.data;
    if (has("preview-title")) el("preview-title").textContent = course.title;
    if (has("preview-desc")) el("preview-desc").textContent = course.description || "暂无简介";
    if (has("preview-status")) el("preview-status").textContent = statusLabel(course.status);
    if (has("preview-id")) el("preview-id").textContent = `课程 ID: ${course.id}`;
    if (has("student-view-title")) el("student-view-title").textContent = course.title;
    if (has("student-view-desc")) el("student-view-desc").textContent = course.description || "暂无简介";
    if (has("student-view-status")) el("student-view-status").textContent = statusLabel(course.status);
    if (has("student-view-id")) el("student-view-id").textContent = `课程 ID: ${course.id}`;
    const sectionsResult = await request(`/api/courses/${courseId}/sections`);
    const sections = sectionsResult.data || [];
    state.previewSections = sections;
    if (has("preview-sections")) {
      const box = el("preview-sections");
      if (sections.length === 0) {
        box.innerHTML = "<div class='muted'>暂无章节</div>";
      } else {
        box.innerHTML = sections
          .map(
            (s) => `
            <div class="section-item">
              <strong>${s.orderIndex}. ${s.title}</strong>
              <span class="muted">${s.contentUrl || "未设置内容链接"}</span>
              ${s.contentUrl ? `<a class="btn ghost" href="${s.contentUrl}" target="_blank" rel="noreferrer">打开内容</a>` : ""}
            </div>`
          )
          .join("");
      }
    }
    renderStudentPreview(sections);
  } catch (err) {
    if (has("preview-error")) el("preview-error").textContent = err.message;
  }
}

function renderStudentPreview(sections) {
  if (!has("student-view-sections")) return;
  const box = el("student-view-sections");
  const mode = has("preview-mode") ? el("preview-mode").value : "in-progress";
  if (!sections || sections.length === 0) {
    box.innerHTML = "<div class='muted'>暂无章节</div>";
    updatePreviewRing(0);
    if (has("student-view-summary")) {
      el("student-view-summary").textContent = "暂无章节";
    }
    return;
  }

  let completedCount = 0;
  if (mode === "completed") {
    completedCount = sections.length;
  } else if (mode === "in-progress") {
    completedCount = Math.min(1, sections.length);
  } else {
    completedCount = 0;
  }
  const percent = Math.round((completedCount * 100) / sections.length);
  updatePreviewRing(percent);
  if (has("student-view-summary")) {
    const statusText = mode === "not-enrolled" ? "未选课" : mode === "completed" ? "已完成" : "学习中";
    el("student-view-summary").textContent = `${statusText}：已完成 ${completedCount} / ${sections.length} 节，进度 ${percent}%（示例）`;
  }
  if (has("student-view-enroll-status")) {
    el("student-view-enroll-status").textContent =
      mode === "not-enrolled" ? "未选课" : mode === "completed" ? "已完成" : "学习中";
  }
  if (has("student-view-enroll")) {
    el("student-view-enroll").textContent = mode === "not-enrolled" ? "选课" : "已选课";
  }

  box.innerHTML = sections
    .map((s, index) => {
      const completed = index < completedCount;
      return `
      <div class="section-item ${completed ? "completed" : ""}">
        <strong>${s.orderIndex}. ${s.title}</strong>
        <span class="muted">${s.contentUrl || "未设置内容链接"}</span>
        <div class="inline">
          <button class="btn primary" disabled>${completed ? "已完成" : "完成"}</button>
          <button class="btn ghost" disabled>取消完成</button>
        </div>
      </div>`;
    })
    .join("");
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

document.addEventListener("DOMContentLoaded", init);
