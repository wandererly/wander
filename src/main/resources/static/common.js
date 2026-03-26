const API_BASE = "";

function getToken() {
  return localStorage.getItem("wander_token") || "";
}

function setToken(token) {
  if (token) {
    localStorage.setItem("wander_token", token);
  } else {
    localStorage.removeItem("wander_token");
  }
}

function headers() {
  const h = { "Content-Type": "application/json" };
  const token = getToken();
  if (token) {
    h.Authorization = `Bearer ${token}`;
  }
  return h;
}

async function request(url, options = {}) {
  const res = await fetch(API_BASE + url, {
    ...options,
    headers: { ...headers(), ...(options.headers || {}) },
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok || data.success === false) {
    const msg = data.message || "请求失败";
    throw new Error(msg);
  }
  return data;
}

async function getCurrentUser() {
  const result = await request("/api/users/me");
  return result.data;
}

async function requireAuth(role) {
  const token = getToken();
  if (!token) {
    window.location.href = "/login.html";
    return null;
  }
  try {
    const user = await getCurrentUser();
    if (role && user.role !== role) {
      if (user.role === "ADMIN") window.location.href = "/admin.html";
      if (user.role === "TEACHER") window.location.href = "/teacher.html";
      if (user.role === "STUDENT") window.location.href = "/student.html";
      return null;
    }
    return user;
  } catch (err) {
    setToken("");
    window.location.href = "/login.html";
    return null;
  }
}

function logout() {
  setToken("");
  window.location.href = "/login.html";
}
