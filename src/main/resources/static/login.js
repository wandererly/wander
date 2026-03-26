const msg = document.getElementById("login-message");

function setMessage(text, isError) {
  msg.textContent = text || "";
  msg.style.color = isError ? "#ff7a59" : "#1f5b8f";
}

async function login() {
  const username = document.getElementById("login-username").value.trim();
  const password = document.getElementById("login-password").value.trim();
  if (!username || !password) {
    setMessage("请输入用户名和密码", true);
    return;
  }
  try {
    const result = await request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
    setToken(result.data.token);
    const me = await getCurrentUser();
    if (me.role === "ADMIN") window.location.href = "/admin.html";
    if (me.role === "TEACHER") window.location.href = "/teacher.html";
    if (me.role === "STUDENT") window.location.href = "/student.html";
  } catch (err) {
    setMessage(err.message, true);
  }
}

document.getElementById("btn-login").addEventListener("click", login);
