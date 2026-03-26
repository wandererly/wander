const el = (id) => document.getElementById(id);

function roleLabel(role) {
  if (role === "ADMIN") return "管理员";
  if (role === "TEACHER") return "教师";
  if (role === "STUDENT") return "学生";
  return role || "-";
}

function statusLabel(status) {
  if (status === "NORMAL") return "正常";
  if (status === "BANNED") return "禁用";
  return status || "-";
}

function setMessage(text, isError) {
  const msg = el("profile-message");
  msg.textContent = text || "";
  msg.style.color = isError ? "#ff7a59" : "#1f5b8f";
}

async function uploadAvatar() {
  const fileInput = el("profile-avatar-file");
  const file = fileInput.files && fileInput.files[0];
  if (!file) {
    setMessage("请选择头像文件", true);
    return;
  }
  const formData = new FormData();
  formData.append("file", file);
  const token = getToken();
  try {
    const res = await fetch("/api/users/me/avatar", {
      method: "POST",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok || data.success === false) {
      throw new Error(data.message || "上传失败");
    }
    const url = data.data && data.data.avatarUrl ? data.data.avatarUrl : "";
    if (url) {
      el("profile-avatar").value = url;
    }
    setMessage("头像已上传", false);
    await loadProfile();
  } catch (err) {
    setMessage(err.message, true);
  }
}

async function loadProfile() {
  const user = await requireAuth();
  if (!user) return;
  el("profile-user").textContent = `${user.username} · ${roleLabel(user.role)}`;
  const info = el("profile-info");
  const avatarUrl = user.avatarUrl || "";
  const nickname = user.nickname || user.username || "-";
  info.innerHTML = `
    <div class="course-item">
      <div class="inline">
        <img class="avatar" src="${avatarUrl || "https://dummyimage.com/128x128/e7e0d6/6b5f54&text=U"}" alt="avatar" />
        <div>
          <h3>${nickname}</h3>
          <p class="muted">${user.username}</p>
        </div>
      </div>
      <p class="muted">${user.email}</p>
      <div class="course-meta">
        <span>角色: ${roleLabel(user.role)}</span>
        <span>状态: ${statusLabel(user.status)}</span>
        <span>ID: ${user.id}</span>
      </div>
      <div class="course-meta">
        <span>昵称: ${user.nickname || "-"}</span>
        <span>手机: ${user.phone || "-"}</span>
      </div>
      <div class="course-meta">
        <span>创建: ${user.createdAt ? new Date(user.createdAt).toLocaleString() : "-"}</span>
        <span>更新: ${user.updatedAt ? new Date(user.updatedAt).toLocaleString() : "-"}</span>
      </div>
    </div>
  `;

  const home = el("btn-home");
  if (user.role === "ADMIN") home.href = "/admin.html";
  if (user.role === "TEACHER") home.href = "/teacher.html";
  if (user.role === "STUDENT") home.href = "/student.html";

  el("profile-nickname").value = user.nickname || "";
  el("profile-phone").value = user.phone || "";
  el("profile-avatar").value = user.avatarUrl || "";
  el("profile-email").value = user.email || "";
  el("profile-username").value = "";
}

async function updateProfile() {
  const username = el("profile-username").value.trim();
  const nickname = el("profile-nickname").value.trim();
  const phone = el("profile-phone").value.trim();
  const avatarUrl = el("profile-avatar").value.trim();
  const email = el("profile-email").value.trim();
  const password = el("profile-password").value.trim();
  const confirm = el("profile-password-confirm").value.trim();

  if (!email && !password && !nickname && !phone && !avatarUrl && !username) {
    setMessage("请填写要修改的字段", true);
    return;
  }
  if (username && (username.length < 4 || username.length > 16)) {
    setMessage("用户名长度需为 4-16 位", true);
    return;
  }
  if (password && password.length < 6) {
    setMessage("密码至少 6 位", true);
    return;
  }
  if (password && password !== confirm) {
    setMessage("两次输入的密码不一致", true);
    return;
  }

  const payload = {};
  if (email) payload.email = email;
  if (password) payload.password = password;
  if (username) payload.username = username;
  if (nickname) payload.nickname = nickname;
  if (phone) payload.phone = phone;
  if (avatarUrl) payload.avatarUrl = avatarUrl;

  try {
    await request("/api/users/me", {
      method: "PUT",
      body: JSON.stringify(payload),
    });
    setMessage("已保存", false);
    el("profile-nickname").value = "";
    el("profile-phone").value = "";
    el("profile-avatar").value = "";
    el("profile-username").value = "";
    el("profile-password").value = "";
    el("profile-password-confirm").value = "";
    await loadProfile();
    if (username) {
      setMessage("用户名已更新，请重新登录", false);
      setTimeout(() => {
        logout();
      }, 800);
    }
  } catch (err) {
    setMessage(err.message || "保存失败", true);
  }
}

function initEvents() {
  el("btn-logout").addEventListener("click", logout);
  el("btn-update-profile").addEventListener("click", updateProfile);
  el("btn-upload-avatar").addEventListener("click", uploadAvatar);
}

document.addEventListener("DOMContentLoaded", () => {
  initEvents();
  loadProfile();
});
