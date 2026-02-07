// All APIs are relative (same origin)
const REFRESH_INTERVAL = 5000; // 5 seconds

async function loadDashboard() {
  try {
    await loadHealth();
    await loadCameras();
  } catch (e) {
    console.error("Dashboard load failed", e);
  }
}

// ===================== HEALTH =====================
async function loadHealth() {
  const res = await fetch("/health");
  const data = await res.json();

  const disk = document.getElementById("disk");
  disk.innerText = `💾 Disk free: ${data.diskFreePercent}%`;
}

// ===================== CAMERAS =====================
async function loadCameras() {
  const res = await fetch("/cameras");
  const cameras = await res.json();

  const container = document.getElementById("cameras");
  container.innerHTML = "";

  const keys = Object.keys(cameras);
  if (keys.length === 0) {
    container.innerHTML = "<p>No cameras registered</p>";
    return;
  }

  for (const cam of keys) {
    renderCamera(cam, cameras[cam], container);
  }
}

function renderCamera(cam, info, container) {
  const state = info.state;
  const lastChange = new Date(info.lastChange).toLocaleTimeString();

  let color = "#888";
  if (state === "RECORDING") color = "#2ecc71";
  if (state === "RESTARTING") color = "#f1c40f";
  if (state === "STOPPED") color = "#e74c3c";

  const div = document.createElement("div");
  div.className = "camera";

  div.innerHTML = `
    <div style="display:flex; justify-content:space-between; align-items:center;">
      <b>${cam}</b>
      <span style="color:${color}; font-weight:bold;">${state}</span>
    </div>

    <div style="font-size:12px; color:#aaa;">
      Last change: ${lastChange}
    </div>

    <div style="margin-top:8px;">
      <button onclick="startCam('${cam}')">▶ Start</button>
      <button onclick="stopCam('${cam}')">⏹ Stop</button>
    </div>

    <div style="margin-top:10px;">
      <img
        src="/live/${cam}"
        alt="Live ${cam}"
        style="width:100%; border-radius:6px; background:#000;"
        onerror="this.style.display='none'"
      />
    </div>
  `;

  container.appendChild(div);
}

// ===================== ACTIONS =====================
async function startCam(cam) {
  await fetch(`/camera/${cam}/start`);
  setTimeout(loadDashboard, 1000);
}

async function stopCam(cam) {
  await fetch(`/camera/${cam}/stop`);
  setTimeout(loadDashboard, 1000);
}

// ===================== INIT =====================
loadDashboard();
setInterval(loadDashboard, REFRESH_INTERVAL);
