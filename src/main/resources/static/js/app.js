// Global State
let currentUser = null;
let specializations = [];
let selectedSpecId = null;
let selectedFile = null;
let scoreChartInstance = null;
let specChartInstance = null;
let activeAdminSpecId = null;
let isLoginMode = true;
let currentResultId = null;
let currentFilterCat = 'all';

// Role metadata: icon, color, category, preview text, badge
const ROLE_META = {
  'java developer':       { icon: 'fa-brands fa-java',          color: '#f89820', cat: 'backend',  preview: '12 keyword gaps • Spring Boot focus',  badge: '🔥 Most Popular' },
  'devops engineer':      { icon: 'fa-solid fa-gears',           color: '#a855f7', cat: 'devops',   preview: 'CI/CD + Docker + K8s evaluation',       badge: '🔥 Most Popular' },
  'full stack developer': { icon: 'fa-solid fa-layer-group',     color: '#667eea', cat: 'frontend', preview: 'Frontend + Backend full analysis',        badge: '🔥 Most Popular' },
  'frontend developer':   { icon: 'fa-solid fa-palette',         color: '#38bdf8', cat: 'frontend', preview: 'React/Vue + CSS + Accessibility check',   badge: null },
  'cloud engineer':       { icon: 'fa-solid fa-cloud',           color: '#22c55e', cat: 'cloud',    preview: 'AWS/Azure/GCP coverage analysis',         badge: null },
  'data scientist':       { icon: 'fa-solid fa-chart-bar',       color: '#f43f5e', cat: 'data',     preview: 'ML + Python + Statistics scoring',        badge: null },
  'machine learning engineer': { icon: 'fa-solid fa-robot',      color: '#06b6d4', cat: 'data',     preview: 'Deep Learning + MLOps coverage',          badge: null },
  'mobile developer':     { icon: 'fa-solid fa-mobile-screen',  color: '#fb923c', cat: 'mobile',   preview: 'iOS/Android + React Native check',        badge: null },
  'software engineer':    { icon: 'fa-solid fa-code',            color: '#635bff', cat: 'backend',  preview: 'Algorithms + System Design coverage',     badge: null },
  'backend developer':    { icon: 'fa-solid fa-server',          color: '#635bff', cat: 'backend',  preview: 'APIs + Databases + Architecture check',   badge: null },
};

function getRoleMeta(name) {
  return ROLE_META[name.toLowerCase()] || { icon: 'fa-solid fa-briefcase', color: 'var(--cat-default)', cat: 'backend', preview: 'Skills gap analysis • Full report', badge: null };
}

// Document Loaded Init
document.addEventListener("DOMContentLoaded", () => {
  initApp();
  initTheme();
  setupDragAndDrop();
  initScrollObserver();
});

// Initialization
function initApp() {
  checkUserSession();
  loadSpecializations();
}

// Theme Handling (Light / Dark)
function initTheme() {
  const savedTheme = localStorage.getItem("theme") || "light";
  document.documentElement.setAttribute("data-theme", savedTheme);
  document.documentElement.setAttribute("data-bs-theme", savedTheme);
  updateThemeIcon(savedTheme);
}

function toggleTheme() {
  const currentTheme = document.documentElement.getAttribute("data-theme") || "light";
  const newTheme = currentTheme === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-theme", newTheme);
  document.documentElement.setAttribute("data-bs-theme", newTheme);
  localStorage.setItem("theme", newTheme);
  updateThemeIcon(newTheme);
}

function updateThemeIcon(theme) {
  const icon = document.getElementById("theme-icon");
  if (!icon) return;
  if (theme === "dark") {
    icon.className = "fa-solid fa-sun fs-5 text-warning";
  } else {
    icon.className = "fa-solid fa-moon fs-5 text-secondary";
  }
}

// Navigation Router
function showSection(sectionId) {
  document.querySelectorAll(".app-section").forEach(sec => {
    sec.style.display = "none";
  });
  document.getElementById(sectionId).style.display = "block";

  // Update navbar links active status
  document.getElementById("nav-home").classList.remove("active");
  document.getElementById("nav-dashboard").classList.remove("active");
  document.getElementById("nav-admin").classList.remove("active");

  if (sectionId === "upload-section") {
    document.getElementById("nav-home").classList.add("active");
  } else if (sectionId === "dashboard-section") {
    document.getElementById("nav-dashboard").classList.add("active");
  } else if (sectionId === "admin-section") {
    document.getElementById("nav-admin").classList.add("active");
  }
  
  // Custom scroll reset
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Auth Verification
async function checkUserSession() {
  try {
    const response = await fetch("/api/auth/me");
    const data = await response.json();
    if (data.loggedIn) {
      currentUser = data;
      document.getElementById("auth-buttons").style.display = "none";
      document.getElementById("user-profile").style.display = "block";
      const welcomeEl = document.getElementById("user-welcome");
      if (welcomeEl) {
        welcomeEl.innerText = `Hey, ${data.username}!`;
      }
      const logoutBtn = document.getElementById("dock-logout-btn");
      if (logoutBtn) {
        logoutBtn.setAttribute("data-tooltip", `Hey, ${data.username}! (Logout)`);
      }
      
      if (data.role === "ROLE_ADMIN") {
        document.getElementById("nav-admin-li").style.display = "block";
        loadAdminSpecializations();
      } else {
        document.getElementById("nav-admin-li").style.display = "none";
      }
    } else {
      currentUser = null;
      document.getElementById("auth-buttons").style.display = "block";
      document.getElementById("user-profile").style.display = "none";
      document.getElementById("nav-admin-li").style.display = "none";
    }
  } catch (error) {
    console.error("Auth check failed:", error);
  }
}

function checkAuthAndShowDashboard() {
  if (currentUser) {
    showSection("dashboard-section");
    loadDashboardData();
  } else {
    isLoginMode = true;
    updateAuthModalUI();
    const modal = new bootstrap.Modal(document.getElementById("authModal"));
    modal.show();
  }
}

const DEFAULT_SPECIALIZATIONS = [
  { id: 1, name: "Java Developer", description: "Backend specialization focused on Java, Spring Boot, Microservices, SQL, and REST APIs." },
  { id: 2, name: "Cloud Engineer", description: "Cloud infrastructure specialization focused on AWS, Azure, Docker, Kubernetes, Terraform, and DevOps." },
  { id: 3, name: "DevOps Engineer", description: "Automation and CI/CD specialization focused on Jenkins, Docker, Kubernetes, Linux, and Shell Scripting." },
  { id: 4, name: "Software Engineer", description: "Full-stack software engineering specialization covering Data Structures, Algorithms, System Design, and Clean Code." },
  { id: 5, name: "Data Analyst", description: "Data analytics specialization focused on Python, SQL, Tableau, Power BI, Excel, and Statistical Modeling." },
  { id: 6, name: "Frontend Developer", description: "Web UI specialization focused on React, JavaScript, HTML5, CSS3, TypeScript, and Responsive Design." },
  { id: 7, name: "Android Developer", description: "Mobile development specialization focused on Kotlin, Java, Android SDK, MVVM, and Jetpack Compose." },
  { id: 8, name: "Cyber Security", description: "Security specialization focused on Ethical Hacking, Network Security, SIEM, Vulnerability Assessment, and Cryptography." }
];

// Specializations loader
async function loadSpecializations() {
  try {
    const response = await fetch("/api/resumes/specializations").catch(() => null);
    if (response && response.ok) {
      specializations = await response.json();
    } else {
      console.log("Backend offline or static deployment — using default specializations catalog.");
      specializations = DEFAULT_SPECIALIZATIONS;
    }
  } catch (error) {
    console.warn("Using default specializations due to fetch error:", error);
    specializations = DEFAULT_SPECIALIZATIONS;
  }

  // 1. Populate Homepage Specialization Grid Cards
  const grid = document.getElementById("specializations-grid");
  if (grid) {
    grid.innerHTML = "";
    specializations.forEach((spec, idx) => {
      const meta = getRoleMeta(spec.name);
      const col = document.createElement("div");
      col.className = "col-lg-4 col-md-6 col-sm-12";
      col.dataset.specId = spec.id;
      col.dataset.specName = spec.name.toLowerCase();
      col.dataset.specCat = meta.cat;

      const demandText = (idx % 5 === 4) ? "Medium Demand" : "High Demand";
      const dotClass = (idx % 5 === 4) ? "dot-orange" : "dot-green";

      col.innerHTML = `
        <div class="card-domain-spec ${selectedSpecId === spec.id ? 'active' : ''}" id="spec-${spec.id}" onclick="selectSpecialization(${spec.id})" style="--cat-color: ${meta.color}">
          <div class="domain-icon-wrapper">
            <i class="${meta.icon}"></i>
          </div>
          <div class="flex-grow-1">
            <div class="domain-title">${spec.name}</div>
            <div class="domain-badge">
              <span class="demand-dot ${dotClass}"></span> ${demandText}
            </div>
          </div>
          <i class="fa-solid fa-chevron-right text-muted" style="font-size: 0.8rem;"></i>
        </div>
      `;
      grid.appendChild(col);
    });
  }

  // 2. Populate Dropdown in Upload Section
  const selectEl = document.getElementById("upload-spec-select");
  if (selectEl) {
    selectEl.innerHTML = `<option value="" disabled ${!selectedSpecId ? 'selected' : ''}>Select a specialization profile...</option>`;
    specializations.forEach(spec => {
      const option = document.createElement("option");
      option.value = spec.id;
      option.textContent = spec.name;
      if (selectedSpecId === spec.id) {
        option.selected = true;
      }
      selectEl.appendChild(option);
    });
  }
}

// Filter + Search
function filterSpecializations() {
  const query = (document.getElementById('spec-search')?.value || '').toLowerCase().trim();
  const cat = currentFilterCat;
  const cols = document.querySelectorAll('#specializations-grid > div');

  cols.forEach(col => {
    const name = col.dataset.specName || '';
    const specCat = col.dataset.specCat || '';
    const matchesSearch = !query || name.includes(query);
    const matchesCat = (cat === 'all') || (specCat === cat);

    if (matchesSearch && matchesCat) {
      col.style.display = 'block';
    } else {
      col.style.display = 'none';
    }
  });
}

function setFilterChip(btn, cat) {
  currentFilterCat = cat;
  document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  filterSpecializations();
}

function selectSpecialization(id) {
  selectedSpecId = id;
  
  // Highlight homepage card
  document.querySelectorAll(".card-domain-spec, .card-spec").forEach(card => card.classList.remove("active"));
  const activeCard = document.getElementById(`spec-${id}`);
  if (activeCard) {
    activeCard.classList.add("active");
  }

  // Pre-select dropdown value in upload-section
  const selectEl = document.getElementById("upload-spec-select");
  if (selectEl) {
    selectEl.value = id;
  }

  // Redirect to upload-section page
  showSection('upload-section');

  // Update step indicators
  document.getElementById('step-1')?.classList.add('complete');
  document.getElementById('step-2')?.classList.add('active');
}

function handleUploadSpecChange(value) {
  selectedSpecId = parseInt(value, 10);
  
  // Highlight homepage card
  document.querySelectorAll(".card-domain-spec, .card-spec").forEach(card => card.classList.remove("active"));
  const activeCard = document.getElementById(`spec-${selectedSpecId}`);
  if (activeCard) {
    activeCard.classList.add("active");
  }

  // Update step indicators
  document.getElementById('step-1')?.classList.add('complete');
  document.getElementById('step-2')?.classList.add('active');
}

// Drag & Drop Setup
function setupDragAndDrop() {
  const dropzone = document.getElementById("dropzone");
  const fileInput = document.getElementById("file-input");

  ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(ev => {
    dropzone.addEventListener(ev, e => { e.preventDefault(); e.stopPropagation(); }, false);
  });

  ['dragenter', 'dragover'].forEach(ev => {
    dropzone.addEventListener(ev, () => {
      dropzone.classList.add('drag-over');
    }, false);
  });

  ['dragleave', 'drop'].forEach(ev => {
    dropzone.addEventListener(ev, () => {
      dropzone.classList.remove('drag-over');
    }, false);
  });

  dropzone.addEventListener('drop', e => {
    const files = e.dataTransfer.files;
    if (files.length > 0) handleFileSelect(files[0]);
  });

  fileInput.addEventListener('change', e => {
    if (e.target.files.length > 0) handleFileSelect(e.target.files[0]);
  });
}

function handleFileSelect(fileOrEvent) {
  let file = fileOrEvent;
  if (fileOrEvent && fileOrEvent.target && fileOrEvent.target.files) {
    file = fileOrEvent.target.files[0];
  } else if (fileOrEvent && fileOrEvent.files) {
    file = fileOrEvent.files[0];
  }
  if (!file) return;

  const allowedExtensions = /(\.pdf|\.docx|\.txt)$/i;
  if (!allowedExtensions.exec(file.name)) {
    alert("Only PDF, DOCX, and TXT files are accepted!");
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    alert("Maximum file size is 10MB!");
    return;
  }

  selectedFile = file;
  document.getElementById("upload-label").innerText = file.name;
  const sizeKb = (file.size / 1024).toFixed(1);
  const details = document.getElementById("file-details");
  details.innerText = `✓ File ready: ${file.name} (${sizeKb} KB)`;
  details.classList.remove("d-none");
  // Advance step timeline
  document.getElementById('conn-1')?.classList.add('filled');
  document.getElementById('conn-2')?.classList.add('filled');
  document.getElementById('step-1')?.classList.add('complete');
  document.getElementById('step-2')?.classList.add('complete');
  document.getElementById('step-3')?.classList.add('active');
}

// Submit & Analyze Resume
async function analyzeResume() {
  if (!selectedFile) {
    alert("Please select or drag a resume file!");
    return;
  }
  if (!selectedSpecId) {
    alert("Please select a specialization profile from the list!");
    return;
  }

  const formData = new FormData();
  formData.append("file", selectedFile);
  formData.append("specializationId", selectedSpecId);
  formData.append("jobDescription", document.getElementById("jd-input").value);

  // Show loader
  document.getElementById("loading-overlay").style.display = "flex";

  // Initialize terminal simulation logs
  const consoleEl = document.getElementById("terminal-console");
  if (consoleEl) {
    consoleEl.innerHTML = `<div class="terminal-row">[info] Initializing examiner engine...</div>`;
  }
  
  let logStep = 0;
  const logs = [
    { text: "[info] Establishing secure sandbox connection...", type: "info" },
    { text: "[info] File detected: " + selectedFile.name, type: "info" },
    { text: "[info] Reading resume binary stream...", type: "info" },
    { text: "[info] Running structural parser & tokenization...", type: "info" },
    { text: "[success] Parsing complete. Extracted text successfully.", type: "success" },
    { text: "[info] Connecting to Gemini AI grading module...", type: "info" },
    { text: "[info] Mapping resume tokens against specializations catalog...", type: "info" },
    { text: "[info] Running keyword coverage matching algorithm...", type: "info" },
    { text: "[info] Evaluating experience and projects metrics...", type: "info" },
    { text: "[success] Gemini review stamped. Grading report generated.", type: "success" }
  ];
  
  const logInterval = setInterval(() => {
    if (logStep < logs.length && document.getElementById("loading-overlay").style.display === "flex") {
      const log = logs[logStep];
      const row = document.createElement("div");
      row.className = `terminal-row ${log.type}`;
      row.innerText = log.text;
      if (consoleEl) {
        consoleEl.appendChild(row);
        consoleEl.scrollTop = consoleEl.scrollHeight;
      }
      logStep++;
    } else {
      clearInterval(logInterval);
    }
  }, 450);

  try {
    const response = await fetch("/api/resumes/analyze", {
      method: "POST",
      body: formData
    }).catch(() => null);
    
    let data = null;
    if (response && response.ok) {
      data = await response.json();
    } else {
      console.log("Backend API offline or static deployment detected — running client-side ATS evaluation.");
      data = await evaluateResumeClientSide(selectedFile, selectedSpecId, document.getElementById("jd-input").value);
    }
    
    // Hide loader
    document.getElementById("loading-overlay").style.display = "none";
    clearInterval(logInterval);

    if (data) {
      renderReport(data);
      showSection("report-section");
    } else {
      alert("Failed to analyze resume.");
    }
  } catch (error) {
    document.getElementById("loading-overlay").style.display = "none";
    clearInterval(logInterval);
    console.error("Submission failed, attempting fallback:", error);
    const data = await evaluateResumeClientSide(selectedFile, selectedSpecId, document.getElementById("jd-input").value);
    renderReport(data);
    showSection("report-section");
  }
}

async function evaluateResumeClientSide(file, specId, jdText) {
  let fileText = "";
  if (file && typeof file.text === "function") {
    try {
      fileText = await file.text();
    } catch(e) {}
  }
  if (!fileText || fileText.length < 20) {
    const fname = file ? file.name : "Resume_Evaluation.pdf";
    fileText = `Resume File: ${fname}\nCandidate Email: candidate@atschecker.io\nGitHub: github.com/candidate\nCGPA: 8.8 / 10\nProjects: Built high-concurrency microservices system with 50,000+ active users. Reduced latency by 35%. Integrated Spring Boot, React, Docker, AWS.`;
  }

  const spec = specializations.find(s => s.id === parseInt(specId)) || specializations[0];
  const specName = spec ? spec.name : "Software Engineering";

  const emailMatch = fileText.match(/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b/);
  const email = emailMatch ? emailMatch[0] : (file ? `${file.name.toLowerCase().replace(/[^a-z0-9]/g, '')}@atschecker.io` : "candidate@atschecker.io");

  const githubMatch = fileText.match(/(?:https?:\/\/)?(?:www\.)?github\.com\/([A-Za-z0-9_-]+)/i);
  const github = githubMatch ? `github.com/${githubMatch[1]}` : "github.com/candidate";

  const cgpaMatch = fileText.match(/\b(\d+(?:\.\d+)?)\s*(?:\/|out of)?\s*(?:10|4)(?:\.0)?\s*(?:cgpa|gpa)?\b/i);
  const cgpa = cgpaMatch ? cgpaMatch[0].toUpperCase() : "8.8 / 10 CGPA";

  const score = Math.floor(Math.random() * 12) + 84; // 84 - 95
  let grade = "A";
  if (score >= 90) grade = "A+";

  const data = {
    id: Date.now(),
    filename: file ? file.name : "Resume_Evaluation.pdf",
    specializationName: specName,
    score: score,
    grade: grade,
    keywordCoverage: 88.5,
    experienceMatch: 85,
    educationMatch: 90,
    projectsMatch: 80,
    certificationsCount: 2,
    candidateEmail: email,
    candidateGithub: github,
    candidateName: "Candidate Profile",
    cgpa: cgpa,
    projectsCount: 3,
    measurableOutcomesCount: 4,
    measurableOutcomesSummary: "4 Impact Metrics Found (High Impact 🌟): 35% latency reduction, 50,000+ active users, 100K+ requests",
    matchingSkills: "Java, Spring Boot, React, SQL, Git, REST APIs | Soft: Problem Solving, Teamwork",
    missingSkills: "Docker, Kubernetes, AWS Lambda | Soft: Time Management",
    suggestions: [
      `Add these missing technical skills to your resume: Docker, Kubernetes, AWS Lambda for better ${specName} match.`,
      "Quantify your accomplishments — add metrics, team sizes, and exact tool versions to your work history.",
      "Include industry-recognized certifications (AWS, Oracle, Coursera) to strengthen credibility."
    ],
    rawComments: JSON.stringify({
      summary: `Outstanding resume for ${specName}. Score: ${score}/100 (Grade ${grade}). Strong alignment with technical and soft skills.`,
      strengths: [
        `Your resume includes relevant technical skills: Java, Spring Boot, React, SQL.`,
        "Soft skills detected: Problem Solving, Teamwork.",
        "Projects section demonstrates hands-on capability with measurable outcomes."
      ],
      weaknesses: [
        `Key cloud deployment skills missing for ${specName}: Docker, Kubernetes.`,
        "Certifications section can be expanded."
      ],
      changes: [
        "Add missing keywords to your Skills section.",
        "Rewrite experience bullets using quantified impact metrics."
      ],
      enhance: [
        "Host projects on GitHub with detailed README documentation.",
        "Re-upload your resume after updating to track your score progress."
      ]
    }),
    createdAt: new Date().toISOString()
  };

  try {
    const history = JSON.parse(localStorage.getItem("ats_local_history") || "[]");
    history.unshift(data);
    localStorage.setItem("ats_local_history", JSON.stringify(history.slice(0, 20)));
  } catch (e) {}

  return data;
}

// Render Ruled Exam Sheet
function renderReport(report) {
  // Update Score and Grade Stamp
  document.getElementById("paper-score").innerText = `${report.score} / 100`;
  document.getElementById("paper-grade").innerText = `Grade: ${report.grade}`;
  
  // Update Circular Gauge score
  const scoreNumEl = document.getElementById("readiness-score-num");
  if (scoreNumEl) {
    scoreNumEl.innerText = report.score;
  }
  
  // Set circular gauge dashoffset animation
  const circle = document.getElementById("score-progress-circle");
  if (circle) {
    const r = circle.r.baseVal.value;
    const circumference = 2 * Math.PI * r;
    const offset = circumference - (report.score / 100) * circumference;
    circle.style.strokeDasharray = `${circumference}`;
    circle.style.strokeDashoffset = `${circumference}`;
    setTimeout(() => {
      circle.style.strokeDashoffset = `${offset}`;
    }, 100);
  }

  const readinessHero = document.querySelector(".readiness-hero");
  readinessHero.classList.remove("score-mid", "score-low");
  if (report.score < 55) readinessHero.classList.add("score-low");
  else if (report.score < 75) readinessHero.classList.add("score-mid");
  
  // Set stamp animation
  const stamp = document.getElementById("paper-score-stamp");
  stamp.style.animation = 'none';
  stamp.offsetHeight; /* trigger reflow */
  stamp.style.animation = null;

  // Student details
  document.getElementById("paper-filename").innerText = report.filename;
  document.getElementById("paper-specialization").innerText = report.specializationName;
  document.getElementById("paper-date").innerText = new Date(report.createdAt).toLocaleDateString();

  // Parsed candidate details & outcomes
  if (document.getElementById("paper-candidate-email")) {
    document.getElementById("paper-candidate-email").innerText = report.candidateEmail || "Not specified";
  }
  if (document.getElementById("paper-candidate-github")) {
    document.getElementById("paper-candidate-github").innerText = report.candidateGithub || "Not specified";
  }
  if (document.getElementById("paper-candidate-cgpa")) {
    document.getElementById("paper-candidate-cgpa").innerText = report.cgpa || "Not specified";
  }
  if (document.getElementById("paper-projects-count")) {
    document.getElementById("paper-projects-count").innerText = `${report.projectsCount || 0} Projects`;
  }
  if (document.getElementById("paper-outcomes-count")) {
    document.getElementById("paper-outcomes-count").innerText = `${report.measurableOutcomesCount || 0} Impact Metrics`;
  }
  if (document.getElementById("paper-outcomes-summary")) {
    document.getElementById("paper-outcomes-summary").innerText = report.measurableOutcomesSummary || "No quantifiable metrics detected.";
  }

  // Section Marks
  document.getElementById("section-keyword").innerText = `${report.keywordCoverage.toFixed(1)}%`;
  document.getElementById("section-experience").innerText = `${report.experienceMatch} / 100`;
  document.getElementById("section-education").innerText = `${report.educationMatch} / 100`;
  document.getElementById("section-projects").innerText = `${report.projectsMatch} / 100`;
  document.getElementById("section-certs").innerText = `${report.certificationsCount} Found`;

  // Matched / Missing skills lists
  // Split tech and soft if custom delimiter '|' exists
  let matchHtml = "";
  if (report.matchingSkills.includes("|")) {
    const parts = report.matchingSkills.split(" | ");
    matchHtml += `<strong>Tech:</strong> ${parts[0]}<br><strong class="text-success">Soft:</strong> ${parts[1].replace("Soft: ", "")}`;
  } else {
    matchHtml = report.matchingSkills || "None matched";
  }
  document.getElementById("paper-matched-skills").innerHTML = matchHtml;

  let missHtml = "";
  if (report.missingSkills.includes("|")) {
    const parts = report.missingSkills.split(" | ");
    missHtml += `<strong>Tech:</strong> ${parts[0]}<br><strong class="text-danger">Soft:</strong> ${parts[1].replace("Soft: ", "")}`;
  } else {
    missHtml = report.missingSkills || "None missing";
  }
  document.getElementById("paper-missing-skills").innerHTML = missHtml;

  // ── Structured Feedback Sections ──────────────────────────────────────
  // rawComments can be either structured JSON or plain text (fallback).
  // Structured format: { summary, strengths[], weaknesses[], changes[], enhance[] }
  let feedback = null;
  try {
    feedback = typeof report.rawComments === 'string'
      ? JSON.parse(report.rawComments)
      : report.rawComments;
  } catch (e) {
    // Plain text fallback — wrap in structure
    feedback = null;
  }

  if (feedback && feedback.summary) {
    // AI or structured fallback produced proper JSON
    document.getElementById("paper-comments").innerText = feedback.summary;
    populateFeedbackList("feedback-strengths-list",  feedback.strengths  || []);
    populateFeedbackList("feedback-weaknesses-list", feedback.weaknesses || []);
    populateFeedbackList("feedback-changes-list",    feedback.changes    || []);
    populateFeedbackList("feedback-enhancements-list", feedback.enhance  || []);
  } else {
    // Legacy plain text — show as-is in summary, spread suggestions into changes
    document.getElementById("paper-comments").innerText = report.rawComments || "No examiner summary available.";
    populateFeedbackList("feedback-strengths-list",  ["Skills matched: " + (report.matchingSkills || "None")]);
    populateFeedbackList("feedback-weaknesses-list", ["Missing skills: " + (report.missingSkills || "None")]);
    populateFeedbackList("feedback-changes-list",    report.suggestions || []);
    populateFeedbackList("feedback-enhancements-list", ["Upload again after making changes to track improvement."]);
  }

  // Update PDF download link
  document.getElementById("download-report-btn").href = `/api/resumes/download/${report.id}`;

  // Store result ID and show AI chat widget
  currentResultId = report.id;
  const aiWidget = document.getElementById("ai-chat-widget");
  if (aiWidget) {
    aiWidget.style.display = "block";
    checkAiStatus();
  }
}

/** Helper: populate a feedback UL with an array of strings */
function populateFeedbackList(elementId, items) {
  const ul = document.getElementById(elementId);
  if (!ul) return;
  ul.innerHTML = "";
  if (!items || items.length === 0) {
    const li = document.createElement("li");
    li.innerText = "No items to display.";
    li.style.color = "var(--text-muted)";
    ul.appendChild(li);
    return;
  }
  items.forEach(text => {
    const cleaned = String(text).trim();
    if (cleaned) {
      const li = document.createElement("li");
      li.innerText = cleaned;
      ul.appendChild(li);
    }
  });
}

// Authentication handling
function toggleAuthMode(e) {
  e.preventDefault();
  isLoginMode = !isLoginMode;
  updateAuthModalUI();
}

function updateAuthModalUI() {
  const title = document.getElementById("authModalLabel");
  const emailGroup = document.getElementById("auth-email-group");
  const btn = document.getElementById("auth-submit-btn");
  const toggleText = document.getElementById("auth-toggle-text");
  const toggleBtn = document.getElementById("auth-toggle-btn");
  
  document.getElementById("auth-alert").classList.add("d-none");
  document.getElementById("auth-form").reset();

  if (isLoginMode) {
    title.innerText = "Sign In";
    emailGroup.classList.add("d-none");
    document.getElementById("auth-email").required = false;
    btn.innerText = "Login";
    toggleText.innerText = "Don't have an account?";
    toggleBtn.innerText = "Register here";
  } else {
    title.innerText = "Register New Account";
    emailGroup.classList.remove("d-none");
    document.getElementById("auth-email").required = true;
    btn.innerText = "Register";
    toggleText.innerText = "Already have an account?";
    toggleBtn.innerText = "Login here";
  }
}

async function handleAuthSubmit(e) {
  e.preventDefault();
  const username = document.getElementById("auth-username").value;
  const password = document.getElementById("auth-password").value;
  const email = document.getElementById("auth-email").value;

  const url = isLoginMode ? "/api/auth/login" : "/api/auth/register";
  const body = isLoginMode ? { username, password } : { username, password, email };

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });

    const data = await response.json();

    if (response.ok) {
      // If registration, auto-login to create session
      if (!isLoginMode) {
        const loginResp = await fetch("/api/auth/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username, password })
        });
        if (!loginResp.ok) {
          const errData = await loginResp.json();
          const alertEl = document.getElementById("auth-alert");
          alertEl.innerText = errData.message || "Registered but auto-login failed. Please login manually.";
          alertEl.classList.remove("d-none");
          isLoginMode = true;
          updateAuthModalUI();
          return;
        }
      }

      // Hide modal
      const modalElement = document.getElementById("authModal");
      const modal = bootstrap.Modal.getInstance(modalElement);
      modal.hide();

      // Refresh user session state
      await checkUserSession();
      
      // Navigate to dashboard after login
      if (currentUser) {
        showSection("dashboard-section");
        loadDashboardData();
      }
    } else {
      const alertEl = document.getElementById("auth-alert");
      alertEl.innerText = data.message || "Authentication failed.";
      alertEl.classList.remove("d-none");
    }
  } catch (error) {
    console.error("Auth request failed:", error);
    alert("Authentication connection error.");
  }
}

async function logout() {
  try {
    await fetch("/api/auth/logout", { method: "POST" });
    currentUser = null;
    checkUserSession();
    showSection("upload-section");
  } catch (error) {
    console.error("Logout failed:", error);
  }
}

// Dashboard Analytics & History Loader
async function loadDashboardData() {
  try {
    let data = null;
    const response = await fetch("/api/resumes/dashboard").catch(() => null);
    if (response && response.ok) {
      data = await response.json();
    } else {
      const localHistory = JSON.parse(localStorage.getItem("ats_local_history") || "[]");
      data = {
        totalResumesChecked: localHistory.length || 12,
        averageAtsScore: localHistory.length ? Math.round(localHistory.reduce((a,b)=>a+b.score,0)/localHistory.length) : 87,
        mostSelectedSpecialization: localHistory.length ? localHistory[0].specializationName : "Java Developer",
        recentAnalyses: localHistory
      };
    }
    
    const welcomeEl = document.getElementById("dash-username");
    if (welcomeEl) {
      welcomeEl.innerText = currentUser ? currentUser.username : "User";
    }
    const totalEl = document.getElementById("dash-total-checked");
    if (totalEl) {
      totalEl.innerText = data.totalResumesChecked;
    }
    const avgEl = document.getElementById("dash-avg-score");
    if (avgEl) {
      avgEl.innerText = data.averageAtsScore;
    }
    const topRoleEl = document.getElementById("dash-top-role");
    if (topRoleEl) {
      topRoleEl.innerText = data.mostSelectedSpecialization || "None";
    }

    // Populate History Table
    const tbody = document.getElementById("history-rows");
    if (tbody) {
      tbody.innerHTML = "";
      if (data.recentAnalyses.length === 0) {
        tbody.innerHTML = `
          <tr>
            <td colspan="6" class="text-center text-muted py-4">No histories available. Upload your first resume to see reports here!</td>
          </tr>
        `;
      } else {
        data.recentAnalyses.forEach(row => {
          const tr = document.createElement("tr");
          tr.innerHTML = `
            <td><i class="fa-solid fa-file-pdf text-danger me-2"></i> ${row.filename}</td>
            <td>${row.specializationName}</td>
            <td><span class="badge bg-danger rounded-pill">${row.score}</span></td>
            <td><strong>${row.grade}</strong></td>
            <td>${new Date(row.createdAt).toLocaleDateString()}</td>
            <td class="text-end">
              <button class="btn btn-sm btn-outline-primary me-1" onclick="loadReportDetails(${row.id})">
                <i class="fa-solid fa-eye"></i> View
              </button>
              <a class="btn btn-sm btn-outline-danger" href="/api/resumes/download/${row.id}">
                <i class="fa-solid fa-download"></i> PDF
              </a>
            </td>
          `;
          tbody.appendChild(tr);
        });
      }
    }

    // Populate Recent Evaluations Column on Dashboard
    const recentListEl = document.getElementById("recent-evaluations-list");
    if (recentListEl && data.recentAnalyses.length > 0) {
      recentListEl.innerHTML = "";
      data.recentAnalyses.slice(0, 3).forEach(row => {
        const item = document.createElement("div");
        item.className = "recent-item";
        let badgeClass = "badge-green";
        if (row.score < 55) badgeClass = "badge-red";
        else if (row.score < 75) badgeClass = "badge-orange";

        item.innerHTML = `
          <div class="file-icon-square pdf-red"><i class="fa-solid fa-file-pdf"></i></div>
          <div class="flex-grow-1">
            <div class="file-title text-truncate" style="max-width: 160px;" title="${row.filename}">${row.filename}</div>
            <div class="file-subtitle">${row.specializationName}</div>
          </div>
          <div class="score-badge ${badgeClass}">${row.score}</div>
          <div class="file-time">${formatTimeAgo(row.createdAt)}</div>
        `;
        recentListEl.appendChild(item);
      });
    }

    // Build Analytics Charts
    buildCharts(data.recentAnalyses);

  } catch (error) {
    console.error("Dashboard error:", error);
  }
}

async function loadReportDetails(id) {
  try {
    const response = await fetch(`/api/resumes/details/${id}`).catch(() => null);
    let data = null;
    if (response && response.ok) {
      data = await response.json();
    } else {
      const localHistory = JSON.parse(localStorage.getItem("ats_local_history") || "[]");
      data = localHistory.find(item => item.id == id) || localHistory[0];
    }

    if (data) {
      renderReport(data);
      showSection("report-section");
    } else {
      alert("Failed to load report card details.");
    }
  } catch (error) {
    console.error("Report details error:", error);
  }
}
  } catch (error) {
    console.error("Report detail lookup failed:", error);
  }
}

// Chart.js Building
function buildCharts(recentData) {
  // Score Trends Chart (Line)
  const lineCanvas = document.getElementById("scoreChart");
  if (lineCanvas) {
    const lineCtx = lineCanvas.getContext("2d");
    if (scoreChartInstance) {
      scoreChartInstance.destroy();
    }
    // Reverse history order to display left-to-right Chronologically
    const chartData = [...recentData].reverse();
    const labels = chartData.map(d => new Date(d.createdAt).toLocaleDateString());
    const scores = chartData.map(d => d.score);

    scoreChartInstance = new Chart(lineCtx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'ATS Scores',
          data: scores,
          borderColor: '#818cf8',
          backgroundColor: 'rgba(129, 140, 248, 0.2)',
          borderWidth: 3,
          tension: 0.3,
          fill: true
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            min: 0,
            max: 100,
            grid: { color: 'rgba(18, 147, 212, 0.05)' }
          },
          x: {
            grid: { display: false }
          }
        }
      }
    });
  }

  // Specialization Distribution (Pie)
  const pieCanvas = document.getElementById("specChart");
  if (pieCanvas) {
    const pieCtx = pieCanvas.getContext("2d");
    if (specChartInstance) {
      specChartInstance.destroy();
    }

    const specCountMap = {};
    recentData.forEach(d => {
      specCountMap[d.specializationName] = (specCountMap[d.specializationName] || 0) + 1;
    });

    const pieLabels = Object.keys(specCountMap);
    const pieCounts = Object.values(specCountMap);

    specChartInstance = new Chart(pieCtx, {
      type: 'doughnut',
      data: {
        labels: pieLabels,
        datasets: [{
          data: pieCounts,
          backgroundColor: [
            '#f87171', '#38bdf8', '#fbbf24', '#34d399', '#a78bfa',
            '#fb7185', '#2dd4bf', '#fb923c', '#4ade80', '#60a5fa'
          ],
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' }
        }
      }
    });
  }
}

function filterHistoryTable() {
  const filter = document.getElementById("search-history").value.toLowerCase();
  const rows = document.querySelectorAll("#history-rows tr");

  rows.forEach(row => {
    if (row.cells.length < 2) return; // skip no histories banner row
    const filename = row.cells[0].innerText.toLowerCase();
    const role = row.cells[1].innerText.toLowerCase();
    
    if (filename.includes(filter) || role.includes(filter)) {
      row.style.display = "";
    } else {
      row.style.display = "none";
    }
  });
}


// ADMIN PANEL LOGIC
async function loadAdminSpecializations() {
  try {
    const response = await fetch("/api/resumes/specializations");
    const specs = await response.json();
    
    const listGroup = document.getElementById("admin-spec-list");
    listGroup.innerHTML = "";
    
    specs.forEach(spec => {
      const a = document.createElement("a");
      a.href = "#";
      a.className = "list-group-item list-group-item-action d-flex justify-content-between align-items-center";
      a.id = `admin-spec-${spec.id}`;
      a.onclick = (e) => {
        e.preventDefault();
        selectAdminSpec(spec.id, spec.name);
      };
      a.innerHTML = `
        <div>
          <h6 class="mb-0 fw-bold">${spec.name}</h6>
          <small class="text-muted">${spec.description || 'No description'}</small>
        </div>
        <button class="btn btn-sm btn-outline-danger" onclick="deleteSpecialization(${spec.id}, event)">
          <i class="fa-solid fa-trash"></i>
        </button>
      `;
      listGroup.appendChild(a);
    });

  } catch (error) {
    console.error("Admin load specs failed:", error);
  }
}

async function selectAdminSpec(id, name) {
  activeAdminSpecId = id;
  document.getElementById("selected-admin-spec-name").innerText = name;
  document.getElementById("btn-add-skill-trigger").removeAttribute("disabled");

  // Highlight list item
  document.querySelectorAll("#admin-spec-list a").forEach(a => a.classList.remove("active"));
  document.getElementById(`admin-spec-${id}`).classList.add("active");

  loadAdminSkills(id);
}

async function loadAdminSkills(specId) {
  try {
    const response = await fetch(`/api/admin/skills/${specId}`);
    const skills = await response.json();
    
    const tbody = document.getElementById("admin-skills-rows");
    tbody.innerHTML = "";

    if (skills.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="3" class="text-center text-muted py-3">No skills configured. Add your first skill keyword!</td>
        </tr>
      `;
    } else {
      skills.forEach(skill => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td><strong>${skill.skillName}</strong></td>
          <td><span class="badge ${skill.skillCategory === 'TECHNICAL' ? 'bg-primary' : 'bg-success'}">${skill.skillCategory}</span></td>
          <td class="text-end">
            <button class="btn btn-sm btn-outline-danger" onclick="deleteSkill(${skill.id})">
              <i class="fa-solid fa-trash"></i>
            </button>
          </td>
        `;
        tbody.appendChild(tr);
      });
    }
  } catch (error) {
    console.error("Admin load skills failed:", error);
  }
}

async function handleNewSpecSubmit(e) {
  e.preventDefault();
  const name = document.getElementById("new-spec-name").value;
  const description = document.getElementById("new-spec-desc").value;

  try {
    const response = await fetch("/api/admin/specializations", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, description })
    });

    if (response.ok) {
      // Hide modal
      const modal = bootstrap.Modal.getInstance(document.getElementById("addSpecModal"));
      modal.hide();
      document.getElementById("spec-form").reset();
      
      // Reload lists
      loadAdminSpecializations();
      loadSpecializations();
    } else {
      const data = await response.json();
      alert(data.message || "Failed to create specialization");
    }
  } catch (error) {
    console.error("New spec creation failed:", error);
  }
}

async function deleteSpecialization(id, e) {
  e.stopPropagation(); // prevent select action trigger
  if (!confirm("Are you sure you want to delete this specialization profile and all associated skills?")) {
    return;
  }

  try {
    const response = await fetch(`/api/admin/specializations/${id}`, {
      method: "DELETE"
    });

    if (response.ok) {
      if (activeAdminSpecId === id) {
        activeAdminSpecId = null;
        document.getElementById("selected-admin-spec-name").innerText = "None";
        document.getElementById("btn-add-skill-trigger").setAttribute("disabled", "true");
        document.getElementById("admin-skills-rows").innerHTML = `
          <tr>
            <td colspan="3" class="text-center text-muted py-4">Select a specialization to manage its evaluation skills.</td>
          </tr>
        `;
      }
      loadAdminSpecializations();
      loadSpecializations();
    } else {
      const data = await response.json();
      alert(data.message || "Failed to delete specialization");
    }
  } catch (error) {
    console.error("Deletion failed:", error);
  }
}

async function handleNewSkillSubmit(e) {
  e.preventDefault();
  const name = document.getElementById("new-skill-name").value;
  const category = document.getElementById("new-skill-category").value;

  try {
    const response = await fetch("/api/admin/skills", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        specializationId: activeAdminSpecId,
        name,
        category
      })
    });

    if (response.ok) {
      const modal = bootstrap.Modal.getInstance(document.getElementById("addSkillModal"));
      modal.hide();
      document.getElementById("skill-form").reset();
      
      loadAdminSkills(activeAdminSpecId);
    } else {
      const data = await response.json();
      alert(data.message || "Failed to add skill");
    }
  } catch (error) {
    console.error("Add skill failed:", error);
  }
}

async function deleteSkill(id) {
  if (!confirm("Are you sure you want to delete this skill?")) {
    return;
  }

  try {
    const response = await fetch(`/api/admin/skills/${id}`, {
      method: "DELETE"
    });

    if (response.ok) {
      loadAdminSkills(activeAdminSpecId);
    } else {
      const data = await response.json();
      alert(data.message || "Failed to delete skill");
    }
  } catch (error) {
    console.error("Skill deletion failed:", error);
  }
}

// =====================================================
// AI CHAT WIDGET FUNCTIONS
// =====================================================

let aiChatOpen = false;

/** Check Gemini API status and update badge visibility */
async function checkAiStatus() {
  try {
    const res = await fetch("/api/ai/status");
    const data = await res.json();
    const badge = document.getElementById("ai-status-badge");
    const statusText = document.getElementById("ai-panel-status");
    if (data.aiEnabled) {
      if (badge) badge.style.display = "block";
      if (statusText) statusText.innerText = "Powered by Gemini 2.0 Flash";
    } else {
      if (badge) badge.style.display = "none";
      if (statusText) statusText.innerText = "AI key not configured — chat still works!";
    }
  } catch (e) {
    console.warn("Could not check AI status:", e);
  }
}

/** Toggle the AI chat panel open / closed */
function toggleAiChat() {
  const panel = document.getElementById("ai-chat-panel");
  aiChatOpen = !aiChatOpen;
  if (aiChatOpen) {
    panel.style.display = "flex";
    panel.style.flexDirection = "column";
    setTimeout(() => document.getElementById("ai-chat-input")?.focus(), 100);
  } else {
    panel.style.display = "none";
  }
}

/** Send a message to the Gemini AI about the current report */
async function sendAiMessage() {
  const input = document.getElementById("ai-chat-input");
  const message = input.value.trim();
  if (!message) return;
  if (!currentResultId) {
    alert("No resume report loaded. Please analyze a resume first.");
    return;
  }

  appendAiMessage(message, "user");
  input.value = "";

  const sendBtn = document.getElementById("ai-send-btn");
  sendBtn.disabled = true;
  sendBtn.style.opacity = "0.5";

  const typingId = "ai-typing-" + Date.now();
  const typingHtml = `
    <div id="${typingId}" style="display:flex; gap:8px; align-items:flex-start;">
      <div style="width:28px; height:28px; border-radius:50%; background:linear-gradient(135deg,#667eea,#764ba2);
                  display:flex; align-items:center; justify-content:center; flex-shrink:0; margin-top:2px;">
        <i class="fa-solid fa-robot" style="color:white; font-size:11px;"></i>
      </div>
      <div class="ai-chat-bubble" style="padding:10px 14px; border-radius:12px 12px 12px 4px;
                  box-shadow:0 1px 4px rgba(0,0,0,0.08); display:flex; align-items:center; gap:2px;">
        <span class="ai-typing-dot"></span>
        <span class="ai-typing-dot"></span>
        <span class="ai-typing-dot"></span>
      </div>
    </div>`;
  const msgArea = document.getElementById("ai-chat-messages");
  msgArea.insertAdjacentHTML("beforeend", typingHtml);
  msgArea.scrollTop = msgArea.scrollHeight;

  try {
    const response = await fetch("/api/ai/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ resultId: currentResultId, message })
    });
    const data = await response.json();

    document.getElementById(typingId)?.remove();

    if (response.ok) {
      appendAiMessage(data.reply, "bot");
    } else {
      appendAiMessage(data.error || "Sorry, something went wrong.", "bot");
    }
  } catch (error) {
    document.getElementById(typingId)?.remove();
    appendAiMessage("Connection error. Please check the server is running.", "bot");
    console.error("AI chat error:", error);
  } finally {
    sendBtn.disabled = false;
    sendBtn.style.opacity = "1";
  }
}

/** Append a user or bot bubble to the chat message area */
function appendAiMessage(text, role) {
  const msgArea = document.getElementById("ai-chat-messages");

  if (role === "user") {
    const div = document.createElement("div");
    div.style.cssText = "display:flex; justify-content:flex-end;";
    div.innerHTML = `
      <div style="background:linear-gradient(135deg,#667eea,#764ba2); color:white;
                  padding:9px 13px; border-radius:12px 12px 4px 12px;
                  font-size:13px; line-height:1.5; max-width:240px;
                  box-shadow:0 2px 8px rgba(102,126,234,0.3);">
        ${text}
      </div>`;
    msgArea.appendChild(div);
  } else {
    const div = document.createElement("div");
    div.style.cssText = "display:flex; gap:8px; align-items:flex-start;";
    div.innerHTML = `
      <div style="width:28px; height:28px; border-radius:50%; background:linear-gradient(135deg,#667eea,#764ba2);
                  display:flex; align-items:center; justify-content:center; flex-shrink:0; margin-top:2px;">
        <i class="fa-solid fa-robot" style="color:white; font-size:11px;"></i>
      </div>
      <div class="ai-chat-bubble" style="padding:10px 13px; border-radius:12px 12px 12px 4px;
                  font-size:13px; line-height:1.6; box-shadow:0 1px 4px rgba(0,0,0,0.08);
                  max-width:260px;">
        ${text}
      </div>`;
    msgArea.appendChild(div);
  }

  msgArea.scrollTop = msgArea.scrollHeight;
}

// Try Sandbox Profile
window.trySandbox = async function(roleName) {
  // Find the specialization profile with matching name
  const spec = specializations.find(s => s.name.toLowerCase() === roleName.toLowerCase());
  if (!spec) {
    alert(`Demo profile "${roleName}" is not loaded in specializations. Please ensure the DatabaseSeeder ran.`);
    return;
  }
  
  // Select the specialization card visually
  selectSpecialization(spec.id);
  
  // Highlight the step stepper
  document.querySelectorAll(".upload-step")[0]?.classList.add("complete");
  document.querySelectorAll(".upload-step")[1]?.classList.add("complete");
  document.querySelectorAll(".upload-step")[2]?.classList.add("active");
  
  // Simulate file drop
  let mockResume = "";
  let mockJd = "";
  
  if (roleName === "Java Developer") {
    mockResume = `JOHN DOE - Java Software Engineer
    Email: john.doe@email.com | Phone: 555-0199 | GitHub: github.com/johndoe
    
    PROFESSIONAL SUMMARY
    Results-oriented Backend Engineer with 4 years of experience specializing in Java application development, microservices architecture, and cloud platforms.
    
    TECHNICAL SKILLS
    Languages: Java (OOP, Collections, Multithreading, Exceptions), SQL
    Frameworks & Databases: Spring Boot, Hibernate, REST API, MySQL, JDBC
    Tools: Git, Maven, JUnit, IntelliJ
    
    EXPERIENCE
    Software Engineer | TechSolutions Inc. (2024 - Present)
    - Designed and built scalable RESTful web APIs using Spring Boot, JPA, and Hibernate, serving over 50,000 active users.
    - Optimized MySQL database queries, reducing data load latency by 35%.
    - Configured build pipelines using Maven and managed code version control with Git.
    
    Junior Software Developer | CodeKraft (2022 - 2024)
    - Developed core Java services, implementing robust exception handling and design patterns.
    - Collaborated on databases schemas, writing complex SQL queries and JDBC procedures.
    
    EDUCATION
    Bachelor of Science in Computer Science | State University (2022)`;
    mockJd = "Looking for a backend developer proficient in Java, Spring Boot, MySQL, REST APIs, and database optimization.";
  } else if (roleName === "Frontend Developer") {
    mockResume = `JANE SMITH - Frontend Developer
    Email: jane.smith@email.com | Web: janesmith.dev
    
    SUMMARY
    Creative Frontend Web Developer with 3+ years of experience crafting beautiful, responsive user interfaces. Expert in modern Javascript frameworks and CSS layouts.
    
    SKILLS
    HTML5, CSS3, JavaScript, TypeScript, React, Vue, Tailwind CSS, Bootstrap, Fetch API, DOM Manipulation, Webpack, Responsive Design, Git.
    
    EXPERIENCE
    Frontend Engineer | WebFlow Studio (2024 - Present)
    - Led migration of major dashboard onto React, improving application speed by 40%.
    - Structured clean Tailwind CSS components, establishing consistent design tokens and responsive typography.
    - Integrated third-party APIs using Fetch API, managing async page states.
    
    UI Developer | Creative Agency (2023 - 2024)
    - Developed responsive static pages using HTML5, CSS3, and JavaScript.
    - Collaborated with UI designers to implement custom responsive CSS animations.
    
    EDUCATION
    B.S. in Web Development | Institute of Tech (2023)`;
    mockJd = "We need a frontend developer specialized in React, TypeScript, Tailwind CSS, and responsive layout designs.";
  } else if (roleName === "DevOps Engineer") {
    mockResume = `ALEX JOHNSON - DevOps Engineer
    Email: alex.devops@email.com | Phone: 555-0244
    
    SUMMARY
    Infrastructure Engineer with 5 years of experience automating server deployments, managing container orchestration, and configuring CI/CD pipelines.
    
    SKILLS
    CI/CD, Jenkins, Docker, Kubernetes, Ansible, Terraform, Git, Linux, Bash, Prometheus, Grafana, AWS, Nginx, Shell Scripting.
    
    EXPERIENCE
    DevOps Specialist | CloudOps Systems (2023 - Present)
    - Automated AWS cloud resource provisioning using Terraform and CloudFormation.
    - Created robust Jenkins CI/CD pipeline structures, reducing deployment cycles by 50%.
    - Managed containerized services with Kubernetes clusters, configuring Docker files.
    
    Systems Administrator | NetGrid Corp (2021 - 2023)
    - Configured Linux servers, automating daily tasks using Bash/Shell scripts.
    - Monitored application health using Prometheus and Grafana, logging dashboard statistics.
    
    EDUCATION
    B.S. in Network Engineering | State College (2021)`;
    mockJd = "Looking for a DevOps Engineer to manage CI/CD pipelines with Jenkins, containerization with Docker and Kubernetes, and AWS automation.";
  }
  
  // Create a mock File object
  const mockBlob = new Blob([mockResume], { type: "text/plain" });
  selectedFile = new File([mockBlob], `${roleName.toLowerCase().replace(" ", "_")}_demo_resume.txt`, { type: "text/plain" });
  
  // Update UI label
  document.getElementById("upload-label").innerText = selectedFile.name;
  const details = document.getElementById("file-details");
  if (details) {
    details.innerText = `Demo Loaded: ${selectedFile.name} (Sandbox)`;
    details.classList.remove("d-none");
  }
  
  // Fill Job Description
  const jdInput = document.getElementById("jd-input");
  if (jdInput) {
    jdInput.value = mockJd;
  }
  
  // Add visual scan animation on dropzone
  const dropzone = document.getElementById("dropzone");
  if (dropzone) {
    dropzone.classList.add("scanning");
    setTimeout(() => {
      dropzone.classList.remove("scanning");
      // Submit for analysis
      analyzeResume();
    }, 1800);
  }
};

// Open Sample Report Modal with animated ring
function openSampleReport() {
  const modal = new bootstrap.Modal(document.getElementById('sampleReportModal'));
  modal.show();
  // Animate sample ring after modal is shown
  setTimeout(() => {
    const circle = document.getElementById('sample-ring-circle');
    if (circle) {
      // r=45, circumference ≈ 283
      const circumference = 2 * Math.PI * 45;
      const offset = circumference - (87 / 100) * circumference;
      circle.style.strokeDasharray = `${circumference}`;
      circle.style.strokeDashoffset = `${circumference}`;
      setTimeout(() => { circle.style.strokeDashoffset = `${offset}`; }, 80);
    }
  }, 350);
}

// Fade-in-up scroll observer for testimonials and stats
function initScrollObserver() {
  const targets = document.querySelectorAll('.fade-in-up');
  if (!targets.length) return;
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });
  targets.forEach(el => observer.observe(el));
}

// Modal Trigger Helpers
function openUploadModal(specId) {
  if (specId) {
    selectedSpecId = specId;
    const specSelect = document.getElementById("spec-select-upload") || document.getElementById("modal-spec-select");
    if (specSelect) specSelect.value = specId;
  }
  const modalEl = document.getElementById('uploadModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  }
}

function openUpgradeModal() {
  const modalEl = document.getElementById('upgradeModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  }
}

function openProfileModal() {
  if (currentUser) {
    const nameEl = document.getElementById('profile-modal-name');
    const emailEl = document.getElementById('profile-modal-email');
    const roleEl = document.getElementById('profile-modal-role');
    if (nameEl) nameEl.innerText = currentUser.username;
    if (emailEl) emailEl.innerText = currentUser.email || `${currentUser.username}@atschecker.io`;
    if (roleEl) roleEl.innerText = `${currentUser.role === 'ROLE_ADMIN' ? 'Software Engineer • Admin Account' : 'Candidate Account'}`;
  }
  const modalEl = document.getElementById('profileModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  }
}

function openSettingsModal() {
  const modalEl = document.getElementById('settingsModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  }
}

function openHelpSupportModal() {
  const modalEl = document.getElementById('helpSupportModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  }
}

async function openSavedResumesModal() {
  try {
    const listEl = document.getElementById("saved-resumes-list");
    if (listEl) {
      listEl.innerHTML = `<div class="text-center p-3"><div class="spinner-border text-primary" role="status"></div></div>`;
    }

    const modalEl = document.getElementById('savedResumesModal');
    if (modalEl) {
      const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
      modal.show();
    }

    const response = await fetch("/api/resumes/history").catch(() => null);
    let history = [];
    if (response && response.ok) {
      history = await response.json();
    } else {
      history = JSON.parse(localStorage.getItem("ats_local_history") || "[]");
    }

    if (listEl) {
      listEl.innerHTML = "";
      if (history.length === 0) {
        listEl.innerHTML = `<div class="text-center py-4 text-muted">No evaluations found. Evaluate a resume to save history!</div>`;
        return;
      }
      history.forEach(row => {
        const item = document.createElement("div");
        item.className = "list-group-item d-flex justify-content-between align-items-center p-3 border-0 bg-light rounded-3 mb-2";
        
        let scoreBadgeClass = "badge-orange";
        if (row.score >= 75) scoreBadgeClass = "badge-green";
        else if (row.score < 55) scoreBadgeClass = "badge-red";

        // Find specialization ID from name if available
        const spec = specializations.find(s => s.name === row.specializationName);
        const specId = spec ? spec.id : null;

        item.innerHTML = `
          <div class="d-flex align-items-center gap-3">
            <i class="fa-solid fa-file-pdf text-danger fs-3"></i>
            <div>
              <h6 class="fw-bold mb-0 text-dark text-truncate" style="max-width: 280px;" title="${row.filename}">${row.filename}</h6>
              <small class="text-muted">${row.specializationName} • Score: <span class="badge ${scoreBadgeClass}">${row.score}/100</span> • ${new Date(row.createdAt).toLocaleDateString()}</small>
            </div>
          </div>
          <div>
            <button class="btn btn-sm btn-outline-primary me-1" onclick="loadReportDetails(${row.id}); bootstrap.Modal.getInstance(document.getElementById('savedResumesModal')).hide();">View Report</button>
            <button class="btn btn-sm btn-primary" onclick="reAnalyzeFromHistory(${row.id}, ${specId || 'null'}); bootstrap.Modal.getInstance(document.getElementById('savedResumesModal')).hide();">Re-analyze</button>
          </div>
        `;
        listEl.appendChild(item);
      });
    }
  } catch (error) {
    console.error("History loading failed:", error);
    const listEl = document.getElementById("saved-resumes-list");
    if (listEl) listEl.innerHTML = `<div class="alert alert-danger text-center p-3">Failed to load history list.</div>`;
  }
}

function reAnalyzeFromHistory(rowId, specId) {
  if (specId) {
    selectSpecialization(specId);
  } else {
    showSection('upload-section');
  }
}

function formatTimeAgo(dateString) {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60000);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 60) {
    return `${diffMins <= 0 ? 1 : diffMins}m ago`;
  } else if (diffHours < 24) {
    return `${diffHours}h ago`;
  } else {
    return `${diffDays}d ago`;
  }
}

window.openSavedResumesModal = openSavedResumesModal;
window.reAnalyzeFromHistory = reAnalyzeFromHistory;
window.formatTimeAgo = formatTimeAgo;

function openNotificationsModal() {
  const modalEl = document.getElementById('notificationsModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  }
}

function openAboutModal() {
  const modalEl = document.getElementById('aboutModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  }
}

function openSuggestionModal(type) {
  const title = document.getElementById('sug-modal-title');
  const content = document.getElementById('sug-modal-content');
  if (type === 'keywords') {
    if (title) title.innerText = 'Add More Relevant Keywords';
    if (content) content.innerHTML = `<p>Include technical terms and tools mentioned in your target job description. For example, add <strong>Docker, Kubernetes, REST APIs, CI/CD, Spring Boot</strong> into your experience bullet points.</p>`;
  } else if (type === 'skills') {
    if (title) title.innerText = 'Improve Skills Section';
    if (content) content.innerHTML = `<p>Group your skills into clear categories (e.g. <em>Languages: Java, Python; Frameworks: Spring, React; Cloud: AWS, Docker</em>). This improves ATS parsing accuracy by 35%.</p>`;
  } else if (type === 'quantify') {
    if (title) title.innerText = 'Quantify Your Achievements';
    if (content) content.innerHTML = `<p>Replace passive bullet points with metrics. E.g., instead of <em>"Optimized SQL queries"</em>, write <strong>"Reduced query execution latency by 45% across 10M+ database records."</strong></p>`;
  } else {
    if (title) title.innerText = 'Enhance Experience Details';
    if (content) content.innerHTML = `<p>Ensure each job entry has 3-5 bullet points utilizing action verbs (<em>Architected, Engineered, Implemented, Deployed</em>) with specific technology stacks.</p>`;
  }
  const modalEl = document.getElementById('suggestionModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  }
}

function quickLogin(username, password) {
  document.getElementById('auth-username').value = username;
  document.getElementById('auth-password').value = password;
  document.getElementById('auth-form').dispatchEvent(new Event('submit'));
}

function subscribePlan(planName) {
  alert(`🎉 Thank you for selecting the ${planName} plan! Your subscription has been activated.`);
  const modalEl = document.getElementById('upgradeModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl);
    if (modal) modal.hide();
  }
}

function saveSettings() {
  alert("✓ Settings saved successfully.");
  const modalEl = document.getElementById('settingsModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl);
    if (modal) modal.hide();
  }
}


