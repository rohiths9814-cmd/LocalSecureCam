const API = "http://192.168.31.85:8080"; // change port if needed

async function loadHealth() {
  const res = await fetch(`${API}/health`);
  const data = await res.json();

  document.getElementById("disk").innerText =
    `💾 Disk free: ${data.diskFreePercent}%`;

  renderCameras(data.cameras);
}

function renderCameras(cameras) {
  const container = document.getElementById("cameras");
  container.innerHTML = "";

  for (const cam in cameras) {
    const status = cameras[cam];

    const div = document.createElement("div");
    div.className = "camera";

    div.innerHTML = `
      <div><b>${cam}</b></div>
      <div class="status">Status: ${status}</div>
      <button onclick="startCam('${cam}')">Start</button>
      <button onclick="stopCam('${cam}')">Stop</button>
      <br/>
      <img src="${API}/live/${cam}" width="100%" />
    `;

    container.appendChild(div);
  }
}

async function startCam(cam) {
  await fetch(`${API}/camera/${cam}/start`);
  setTimeout(loadHealth, 1000);
}

async function stopCam(cam) {
  await fetch(`${API}/camera/${cam}/stop`);
  setTimeout(loadHealth, 1000);
}

loadHealth();
setInterval(loadHealth, 5000);
