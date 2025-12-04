document.addEventListener("DOMContentLoaded", function () {
  const nameSpan = document.getElementById("staffName");
  const idSpan = document.getElementById("staffId");
  const btnOrderConfirm = document.getElementById("btnOrderConfirm");
  const btnInvoice = document.getElementById("btnInvoice");
  const btnDisplay = document.getElementById("btnDisplay");
  const btnLogout = document.getElementById("btnLogout");

  const staffName = sessionStorage.getItem("staffname");
  const staffId = sessionStorage.getItem("staffid");

  // If not logged in, send back to login page
  if (!staffId) {
    window.location.href = "staff_login.html";
    return;
  }

  // Show staff info
  nameSpan.textContent = staffName || "Staff Member";
  idSpan.textContent = staffId;

  // Go to Order Confirmation
  btnOrderConfirm.addEventListener("click", () => {
    window.location.href = "Order_Confirm.html";
  });

  // Go to Invoice Generation
  btnInvoice.addEventListener("click", () => {
    window.location.href = "invoice_generte.html"; // use your existing filename
  });

  // Go to Invoice Generation
  btnDisplay.addEventListener("click", () => {
    window.location.href = "uc5.html"; // use your existing filename
  });


  // Log out
  btnLogout.addEventListener("click", () => {
    sessionStorage.clear();
    window.location.href = "staff_login.html";
  });
});
