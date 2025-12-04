// Supabase setup
const SUPABASE_URL = "https://bmqvkxfvljxlgynxruga.supabase.co";
const SUPABASE_KEY =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";

const supabaseClient = window.supabase.createClient(SUPABASE_URL, SUPABASE_KEY);

document.addEventListener("DOMContentLoaded", function () {
  const loginForm = document.getElementById("loginForm");
  const loginMessage = document.getElementById("loginMessage");

  function showMessage(text, type) {
    loginMessage.textContent = text;
    loginMessage.className = "message " + (type || "");
  }

  async function login(event) {
    event.preventDefault();

    const staffid = loginForm.username.value.trim();
    const password = loginForm.password.value.trim();

    if (!staffid || !password) {
      showMessage("Please enter both Staff ID and password.", "error");
      return;
    }

    showMessage("Checking credentials...");

    // Pull staff row from inhouse_staff
    const { data, error } = await supabaseClient
      .from("inhouse_staff")
      .select("*")
      .eq("staffid", staffid)
      .maybeSingle();

    if (error) {
      console.error(error);
      showMessage("Server error. Try again.", "error");
      return;
    }

    if (!data) {
      showMessage("Invalid Staff ID.", "error");
      return;
    }

    // SIMPLE PASSWORD CHECK (only works if storing plain text)
    if (data.password !== password) {
      showMessage("Incorrect password.", "error");
      return;
    }

    // Save staff info
    sessionStorage.setItem("staffid", data.staffid);
    sessionStorage.setItem("staffname", data.name);

    showMessage("Login successful. Redirecting...", "success");

    setTimeout(() => {
      window.location.href = "staff_menu.html";
    }, 800);
  }

  loginForm.addEventListener("submit", login);
});
