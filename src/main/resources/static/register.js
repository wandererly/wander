const msg = document.getElementById("reg-message");

function setMessage(text, isError) {
  msg.textContent = text || "";
  msg.style.color = isError ? "#ff7a59" : "#1f5b8f";
}

async function registerUser() {
  const username = document.getElementById("reg-username").value.trim();
  const email = document.getElementById("reg-email").value.trim();
  const password = document.getElementById("reg-password").value.trim();
  const role = document.getElementById("reg-role").value;
  if (!username || !email || !password) {
    setMessage("请填写完整信息", true);
    return;
  }
  try {
    await request("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, email, password, role }),
    });
    setMessage("注册成功，请登录", false);
    setTimeout(() => {
      window.location.href = "/login.html";
    }, 800);
  } catch (err) {
    setMessage(err.message, true);
  }
}

document.getElementById("btn-register").addEventListener("click", registerUser);

function initRole() {
  const params = new URLSearchParams(window.location.search);
  const role = params.get("role");
  if (role === "STUDENT" || role === "TEACHER") {
    const select = document.getElementById("reg-role");
    select.value = role;
    select.disabled = true;
  }
}

document.addEventListener("DOMContentLoaded", initRole);
