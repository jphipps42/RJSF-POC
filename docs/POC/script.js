let currentUser = null;
let currentReviewId = 'TE020005';

const STORAGE_KEYS = {
  USER: 'egs_current_user',
  SAFETY_REVIEW: 'egs_safety_review',
  ANIMAL_REVIEW: 'egs_animal_review',
  HUMAN_REVIEW: 'egs_human_review',
  ACQUISITION_REVIEW: 'egs_acquisition_review'
};

function initAuth() {
  const savedUser = localStorage.getItem(STORAGE_KEYS.USER);

  if (savedUser) {
    currentUser = JSON.parse(savedUser);
    showApp();
  } else {
    showAuthUI();
  }
}

function getCurrentUser() {
  return currentUser;
}

function showAuthUI() {
  const authUI = document.getElementById('authUI');
  const appContent = document.getElementById('appContent');

  if (authUI) authUI.style.display = 'block';
  if (appContent) appContent.style.display = 'none';
}

function showApp() {
  const authUI = document.getElementById('authUI');
  const appContent = document.getElementById('appContent');
  const userEmail = document.getElementById('userEmail');

  if (authUI) authUI.style.display = 'none';
  if (appContent) appContent.style.display = 'block';
  if (userEmail && currentUser) userEmail.textContent = currentUser.email;
}

async function signUp(email, password) {
  if (!email || !password) {
    throw new Error('Email and password are required');
  }

  const user = {
    id: 'user_' + Date.now(),
    email: email,
    created_at: new Date().toISOString()
  };

  return { user };
}

async function signIn(email, password) {
  if (!email || !password) {
    throw new Error('Email and password are required');
  }

  currentUser = {
    id: 'user_' + Date.now(),
    email: email
  };

  localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(currentUser));
  return { user: currentUser };
}

async function signOut() {
  currentUser = null;
  localStorage.removeItem(STORAGE_KEYS.USER);
  showAuthUI();
}

async function initReview(awardNumber) {
  currentReviewId = awardNumber;
  return currentReviewId;
}

function getCurrentReviewId() {
  return currentReviewId;
}

async function saveSafetyReview(data) {
  localStorage.setItem(STORAGE_KEYS.SAFETY_REVIEW, JSON.stringify(data));
}

async function loadSafetyReview() {
  const data = localStorage.getItem(STORAGE_KEYS.SAFETY_REVIEW);
  return data ? JSON.parse(data) : null;
}

async function saveAnimalReview(data) {
  localStorage.setItem(STORAGE_KEYS.ANIMAL_REVIEW, JSON.stringify(data));
}

async function loadAnimalReview() {
  const data = localStorage.getItem(STORAGE_KEYS.ANIMAL_REVIEW);
  return data ? JSON.parse(data) : null;
}

async function saveHumanSubjectsReview(data) {
  localStorage.setItem(STORAGE_KEYS.HUMAN_REVIEW, JSON.stringify(data));
}

async function loadHumanSubjectsReview() {
  const data = localStorage.getItem(STORAGE_KEYS.HUMAN_REVIEW);
  return data ? JSON.parse(data) : null;
}

async function saveAcquisitionBudget(data) {
  localStorage.setItem(STORAGE_KEYS.ACQUISITION_REVIEW, JSON.stringify(data));
}

async function loadAcquisitionBudget() {
  const data = localStorage.getItem(STORAGE_KEYS.ACQUISITION_REVIEW);
  return data ? JSON.parse(data) : null;
}

document.addEventListener("DOMContentLoaded", async () => {
  await initAuth();
  setupAuthHandlers();
  setupDatabaseIntegration();

  document.querySelectorAll(".section-header").forEach(header => {
    header.addEventListener("click", () => {
      const section = header.closest(".section");
      const wasOpen = section?.classList.contains("open");
      section?.classList.toggle("open");

      if (!wasOpen && section) {
        setTimeout(() => {
          section.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
      }
    });
  });

  document.querySelectorAll(".subsection-header").forEach(header => {
    header.addEventListener("click", e => {
      e.stopPropagation();
      header.closest(".subsection")?.classList.toggle("open");
    });
  });

  document.querySelectorAll(".save-btn").forEach(btn => {
    btn.addEventListener("click", e => {
      e.stopPropagation();
      const section = btn.closest(".section");
      const subsection = btn.closest(".subsection");

      if (subsection) {
        const status = getSubsectionProgressStatus(subsection);
        setSubsectionStatus(subsection, status);
        subsection.setAttribute('data-status', status);

        if (section) {
          const sectionStatus = getSectionProgressStatusWithSubsections(section);
          setStatus(section, sectionStatus);
          section.setAttribute('data-status', sectionStatus);
        }
        return;
      }

      if (!section) return;

      const status = getProgressStatus(section);
      setStatus(section, status);
      section.setAttribute('data-status', status);

      const sectionTitle = section.querySelector('.section-header h2')?.textContent || 'Section';
      showSaveConfirmation(`${sectionTitle} progress has been saved successfully.`);
    });
  });

  document.querySelectorAll(".submit-btn").forEach(btn => {
    btn.addEventListener("click", e => {
      e.stopPropagation();
      const section = btn.closest(".section");
      if (!section) return;

      const incompleteSubs = section.querySelectorAll(
        '.subsection:not([data-status="completed"])'
      );
      if (incompleteSubs.length) {
        alert("All subsections must be completed before submitting this section.");
        return;
      }

      const sectionTitle = section.querySelector('.section-header h2')?.textContent || 'Section';

      showSubmitQuestion("Submit this section? This will lock the section.", async () => {
        setStatus(section, "submitted");
        setSectionCompletionDate(section);
        lockSection(section);

        await saveSubmittedSection(section);

        showSubmitConfirmation(`${sectionTitle} has been submitted and locked successfully. No further edits can be made to this section.`);
      });
    });
  });

  const createSubawardModal = document.getElementById("createSubawardModal");
  const openCreateSubawardBtn = document.getElementById("openCreateSubawardModal");

  openCreateSubawardBtn?.addEventListener("click", e => {
    e.preventDefault();
    e.stopPropagation();
    openModalById("createSubawardModal");
  });

  const specialReqModal = document.getElementById("specialReqModal");
  const openSpecialReqBtn = document.getElementById("openSpecialReqBtn");
  const cancelSpecialReqBtn = document.getElementById("cancelSpecialReqBtn");
  const saveSpecialReqBtn = document.getElementById("saveSpecialReqBtn");
  const selectedBox = document.getElementById("specialReqSelected");

  let specialReqSelected = new Set();

  openSpecialReqBtn?.addEventListener("click", e => {
    e.preventDefault();
    e.stopPropagation();
    openModalById("specialReqModal");
  });

  cancelSpecialReqBtn?.addEventListener("click", e => {
    e.preventDefault();
    closeModalByElement(specialReqModal);
  });

  saveSpecialReqBtn?.addEventListener("click", e => {
    e.preventDefault();
    const checks = specialReqModal.querySelectorAll('input[type="checkbox"]');
    specialReqSelected = new Set(
      [...checks].filter(c => c.checked).map(c => c.value)
    );

    selectedBox.innerHTML = specialReqSelected.size
      ? `<ul>${[...specialReqSelected].map(v => `<li>${escapeHtml(v)}</li>`).join("")}</ul>`
      : `<div class="info-note">None selected.</div>`;

    closeModalByElement(specialReqModal);
  });

  document.getElementById("openProgrammaticRecBtn")
    ?.addEventListener("click", e => {
      e.preventDefault();
      e.stopPropagation();
      openModalById("programmaticRecModal");
    });

  document.getElementById("openChemicalAgentsBtn")
    ?.addEventListener("click", e => {
      e.preventDefault();
      e.stopPropagation();
      openModalById("chemicalAgentsModal");
    });

  document.getElementById("openBsatBtn")
    ?.addEventListener("click", e => {
      e.preventDefault();
      e.stopPropagation();
      window.open(
        "https://www.selectagents.gov/sat/list.htm",
        "_blank",
        "noopener"
      );
    });

  document.querySelectorAll(".modal").forEach(modal => {
    modal.addEventListener("click", e => {
      if (
        e.target === modal ||
        e.target.classList.contains("close-modal")
      ) {
        closeModalByElement(modal);
      }
    });
  });

  const eRadios = document.querySelectorAll('input[name="acq_e_choice"]');
  const eComment = document.getElementById("acqEComment");
  const eHint = document.getElementById("acqEHint");
  const acqSection = document.getElementById("acqSection");

  function updateEHint() {
    const selected = document.querySelector('input[name="acq_e_choice"]:checked');
    if (eHint) {
      eHint.style.display = selected?.value === "unclear" ? "block" : "none";
    }
  }

  eRadios.forEach(r => r.addEventListener("change", updateEHint));
  updateEHint();

  acqSection?.querySelector(".submit-btn")?.addEventListener("click", e => {
    const selected = document.querySelector('input[name="acq_e_choice"]:checked');
    if (selected?.value === "unclear" && eComment && !eComment.value.trim()) {
      e.preventDefault();
      alert('For item E: Please enter a comment when "Unclear" is selected.');
      acqSection.querySelector('[data-subsection="acqE"]')?.classList.add("open");
      eComment.focus();
    }
  }, true);

  const yearFilter = document.getElementById("recYearFilter");
  const recItems = document.querySelectorAll("#recList li");

  yearFilter?.addEventListener("change", () => {
    const selectedYear = yearFilter.value;

    recItems.forEach(item => {
      const itemYear = item.dataset.year;
      item.style.display =
        selectedYear === "all" || itemYear === selectedYear
          ? "block"
          : "none";
    });
  });

  const budgetTextarea = document.getElementById("acqBudgetComments");

  if (budgetTextarea) {
    const guidanceTemplate =
`BUDGET REVIEW GUIDANCE (do not remove)

1. Overall reasonableness of the proposed budget:
   →

2. Alignment of costs with the statement of work:
   →

3. Questioned, unsupported, or unallowable costs:
   →

--- End of Guidance ---
`;

    function enforceTemplate() {
      if (!budgetTextarea.value.startsWith("BUDGET REVIEW GUIDANCE")) {
        budgetTextarea.value = guidanceTemplate;
      }
    }

    enforceTemplate();

    function firstEditablePosition() {
      const arrowIndex = budgetTextarea.value.indexOf("→ ");
      return arrowIndex !== -1 ? arrowIndex + 2 : budgetTextarea.value.length;
    }

    budgetTextarea.addEventListener("focus", () => {
      const pos = firstEditablePosition();
      budgetTextarea.setSelectionRange(pos, pos);
      budgetTextarea.scrollTop = budgetTextarea.scrollHeight;
    });

    budgetTextarea.addEventListener("keydown", (e) => {
      const cursorPos = budgetTextarea.selectionStart;

      if (
        cursorPos <= firstEditablePosition() &&
        (e.key === "Backspace" || e.key === "Delete")
      ) {
        e.preventDefault();
      }
    });

    budgetTextarea.addEventListener("input", enforceTemplate);
  }

  updateAllSubsections();

  const inputs = document.querySelectorAll(".subsection input, .subsection textarea, .subsection select");
  inputs.forEach(input => {
    const eventType = input.type === "radio" || input.type === "checkbox" ? "change" : "input";
    input.addEventListener(eventType, () => {
      const subsection = input.closest(".subsection");
      if (subsection) {
        const status = getSubsectionProgressStatus(subsection);
        setSubsectionStatus(subsection, status);

        const section = subsection.closest(".section");
        if (section) {
          const sectionStatus = getSectionProgressStatusWithSubsections(section);
          setStatus(section, sectionStatus);
        }
      }
    });
  });

  const saveButtons = document.querySelectorAll(".subsection-actions .save-btn");
  saveButtons.forEach(button => {
    button.addEventListener("click", handleSaveButtonClick);
  });

  const equipmentSection = document.getElementById('equipment-section');

  if (equipmentSection) {
    const radioButtons = equipmentSection.querySelectorAll('input[name="equipt_q1"]');
    const yesQuestions = equipmentSection.querySelectorAll('.equipment-yes-question');
    const noCommentBox = equipmentSection.querySelector('.equipment-no-comment');

    const setInitialState = () => {
      radioButtons.forEach(radio => {
        radio.checked = false;
      });

      yesQuestions.forEach(question => {
        question.disabled = true;
        question.value = '';
      });
      if (noCommentBox) {
        noCommentBox.disabled = true;
        noCommentBox.value = '';
      }
    };

    const handleSelectionChange = (event) => {
      const selectedValue = event.target.value;

      if (selectedValue === 'yes') {
        yesQuestions.forEach(question => { question.disabled = false; });
        if (noCommentBox) {
          noCommentBox.disabled = true;
          noCommentBox.value = '';
        }
      } else if (selectedValue === 'no') {
        yesQuestions.forEach(question => {
          question.disabled = true;
          question.value = '';
        });
        if (noCommentBox) {
          noCommentBox.disabled = false;
        }
      }
    };

    setInitialState();
    radioButtons.forEach(radio => {
      radio.addEventListener('change', handleSelectionChange);
    });
  }

  const question1Inputs = document.querySelectorAll('[name="human_hs_q1"]');
  question1Inputs.forEach(input => {
    input.addEventListener('change', () => {
      const question2Inputs = document.querySelectorAll('[name="human_hs_q2"]');
      const enable = input.value === 'yes' && input.checked;
      question2Inputs.forEach(q => {
        q.disabled = !enable;
      });
    });
  });

  const question2Inputs = document.querySelectorAll('[name="human_hs_q2"]');
  question2Inputs.forEach(q => {
    q.disabled = true;
  });

  wireSection("humanSection", "submitHuman");
  wireSection("acquisitionSection", "submitAcquisition");
  wireSection("safetySection", "submitSafety");
  wireSection("animalSection", "submitAnimal");

  const panelToggleBtn = document.getElementById('panelToggleBtn');
  const rightPanel = document.querySelector('.right-panel');

  panelToggleBtn?.addEventListener('click', () => {
    rightPanel?.classList.toggle('collapsed');
    panelToggleBtn.classList.toggle('expanded');

    if (rightPanel?.classList.contains('collapsed')) {
      panelToggleBtn.innerHTML = '▶';
      panelToggleBtn.title = 'Show Right Panel';
    } else {
      panelToggleBtn.innerHTML = '◀';
      panelToggleBtn.title = 'Hide Right Panel';
    }
  });

  const resetChecklistBtn = document.getElementById('resetChecklistBtn');
  resetChecklistBtn?.addEventListener('click', () => {
    updateResetModalStatuses();
    openModalById('resetChecklistModal');
  });

  setupResetButtons();
  setupPrimeAwardListener();

});

function openModalById(id) {
  const modal = document.getElementById(id);
  if (!modal) {
    console.warn("Modal not found:", id);
    return;
  }
  modal.classList.add("open");
  modal.setAttribute("aria-hidden", "false");
}

function closeModalByElement(modal) {
  if (!modal) return;
  modal.classList.remove("open");
  modal.setAttribute("aria-hidden", "true");
}

function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');
  }
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
  }
}

function toggleQuestions(show) {
  const questions = document.querySelectorAll('.question');
  questions.forEach((q, index) => {
    if (index > 0) {
      if (show) {
        q.style.display = 'block';
        q.querySelectorAll('input, textarea, button').forEach(el => el.disabled = false);
      } else {
        q.style.display = 'none';
        q.querySelectorAll('input, textarea, button').forEach(el => el.disabled = true);
      }
    }
  });
}

function toggleNextQuestion(show) {
  const nextQuestion = document.querySelector('.question:nth-of-type(2)');
  if (nextQuestion) {
    if (show) {
      nextQuestion.style.display = 'block';
      nextQuestion.querySelectorAll('input, textarea, button').forEach(el => el.disabled = false);
    } else {
      nextQuestion.style.display = 'none';
      nextQuestion.querySelectorAll('input, textarea, button').forEach(el => el.disabled = true);
    }
  }
}

window.openModal = openModal;
window.closeModal = closeModal;
window.toggleQuestions = toggleQuestions;
window.toggleNextQuestion = toggleNextQuestion;

function showSaveConfirmation(message) {
  const modal = document.getElementById('saveConfirmModal');
  const messageEl = document.getElementById('saveConfirmMessage');
  if (messageEl && message) {
    messageEl.textContent = message;
  }
  openModalById('saveConfirmModal');
}

function showSubmitConfirmation(message) {
  const modal = document.getElementById('submitSuccessModal');
  const messageEl = document.getElementById('submitSuccessMessage');
  if (messageEl && message) {
    messageEl.textContent = message;
  }
  openModalById('submitSuccessModal');
}

function showSubmitQuestion(message, onConfirm) {
  const modal = document.getElementById('submitConfirmQuestion');
  const messageEl = document.getElementById('submitQuestionMessage');
  const confirmBtn = document.getElementById('confirmSubmitBtn');

  if (messageEl && message) {
    messageEl.textContent = message;
  }

  if (!confirmBtn) {
    console.error('Confirm button not found');
    return;
  }

  const newConfirmBtn = confirmBtn.cloneNode(true);
  confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

  newConfirmBtn.addEventListener('click', () => {
    closeModal('submitConfirmQuestion');
    if (onConfirm) onConfirm();
  });

  openModalById('submitConfirmQuestion');
}

function getProgressStatus(section) {
  if (section.classList.contains("locked")) return "completed";

  const inputs = section.querySelectorAll("input, textarea");
  let hasAny = false;
  let complete = true;

  inputs.forEach(el => {
    if (el.type === "radio") {
      const group = section.querySelectorAll(`input[name="${cssEscape(el.name)}"]`);
      const checked = [...group].some(r => r.checked);
      if (checked) hasAny = true;
      if (!checked) complete = false;
    } else if (el.value?.trim()) {
      hasAny = true;
    } else {
      complete = false;
    }
  });

  return hasAny ? "in-progress" : "not-started";
}

function getSectionProgressStatusWithSubsections(section) {
  if (section.classList.contains("locked")) return "completed";

  const subsections = section.querySelectorAll(".subsection");

  if (subsections.length > 0) {
    let hasInProgress = false;
    let hasCompleted = false;
    let allCompleted = true;

    subsections.forEach(subsection => {
      const status = getSubsectionProgressStatus(subsection);
      if (status === "in-progress") {
        hasInProgress = true;
        allCompleted = false;
      } else if (status === "completed") {
        hasCompleted = true;
      } else {
        allCompleted = false;
      }
    });

    if (hasInProgress || hasCompleted) {
      return "in-progress";
    }
    return "not-started";
  } else {
    return getProgressStatus(section);
  }
}

function setStatus(section, status) {
  const badge = section.querySelector(".section-status");
  if (!badge) return;

  badge.className = "section-status status-" + status.toLowerCase();
  badge.textContent =
    status === "submitted" ? "Submitted" :
    status === "completed" ? "Completed" :
    status === "in-progress" ? "In Progress" :
    "Not Started";
}

function lockSection(section) {
  section.classList.add("locked");
  section.querySelectorAll("input, textarea, button")
    .forEach(el => el.disabled = true);
}

function setSectionCompletionDate(section, dateString = null) {
  const dateEl = section.querySelector(".section-date");
  if (!dateEl) return;

  let formatted;

  if (dateString) {
    const d = new Date(dateString);
    formatted = String(d.getMonth() + 1) + "/" + String(d.getDate()) + "/" + String(d.getFullYear()).slice(-2);
  } else {
    const d = new Date();
    formatted = String(d.getMonth() + 1) + "/" + String(d.getDate()) + "/" + String(d.getFullYear()).slice(-2);
  }

  section.dataset.completedDate = formatted;
  dateEl.textContent = `Submitted on ${formatted} by Doe, John`;
  dateEl.classList.remove("hidden");
}

function cssEscape(str) {
  return String(str).replace(/\\/g, "\\\\").replace(/"/g, '\\"');
}

function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function getSubsectionProgressStatus(subsection) {
  const elements = subsection.querySelectorAll(".question input:not([disabled]), .question select:not([disabled]), .question textarea:not([disabled])");

  if (elements.length === 0) {
    return "not-started";
  }

  let hasAny = false;
  let complete = true;

  const checkedGroups = new Set();

  elements.forEach(el => {
    if (el.type === "radio") {
      if (!checkedGroups.has(el.name)) {
        checkedGroups.add(el.name);
        const group = subsection.querySelectorAll(`input[type="radio"][name="${cssEscape(el.name)}"]:not([disabled])`);
        const checked = [...group].some(r => r.checked);
        if (checked) {
          hasAny = true;
        } else {
          complete = false;
        }
      }
    } else if (el.type === "checkbox") {
      if (el.checked) {
        hasAny = true;
      } else {
        complete = false;
      }
    } else if (el.tagName === "SELECT") {
      if (el.value && el.value !== "") {
        hasAny = true;
      } else {
        complete = false;
      }
    } else if (el.tagName === "TEXTAREA") {
      if (el.hasAttribute("required")) {
        if (el.value?.trim()) {
          hasAny = true;
        } else {
          complete = false;
        }
      } else if (el.value?.trim()) {
        hasAny = true;
      }
    } else if (el.type === "text" || el.type === "number" || el.type === "email") {
      if (el.hasAttribute("required")) {
        if (el.value?.trim()) {
          hasAny = true;
        } else {
          complete = false;
        }
      } else if (el.value?.trim()) {
        hasAny = true;
      }
    }
  });

  return (complete && hasAny) ? "completed" : hasAny ? "in-progress" : "not-started";
}

function setSubsectionStatus(subsection, status) {
  const indicator = subsection.querySelector(".subsection-status-indicator");
  if (!indicator) return;

  indicator.className = "subsection-status-indicator status-" + status.toLowerCase();
}

function handleSaveButtonClick(event) {
  const subsection = event.target.closest(".subsection");
  if (!subsection) return;

  const status = getSubsectionProgressStatus(subsection);
  setSubsectionStatus(subsection, status);
}

function updateAllSubsections() {
  const subsections = document.querySelectorAll(".subsection");
  subsections.forEach(subsection => {
    const status = getSubsectionProgressStatus(subsection);
    setSubsectionStatus(subsection, status);
  });

  const sectionsWithSubsections = document.querySelectorAll(".section");
  sectionsWithSubsections.forEach(section => {
    const hasSubs = section.querySelectorAll(".subsection").length > 0;
    if (hasSubs) {
      const sectionStatus = getSectionProgressStatusWithSubsections(section);
      setStatus(section, sectionStatus);
    }
  });
}

function isSectionComplete(sectionEl) {
  let complete = true;

  sectionEl.querySelectorAll(".question").forEach(q => {
    const radios = q.querySelectorAll('input[type="radio"]');
    if (radios.length) {
      const answered = [...radios].some(r => r.checked);
      if (!answered) complete = false;
    }
  });

  sectionEl.querySelectorAll("textarea[data-required='true']").forEach(t => {
    if (!t.value.trim()) complete = false;
  });

  return complete;
}

function wireSection(sectionId, submitBtnId) {
  const section = document.getElementById(sectionId);
  const submitBtn = document.getElementById(submitBtnId);

  if (!section || !submitBtn) return;

  const inputs = section.querySelectorAll("input, textarea");

  function updateSubmitState() {
    submitBtn.disabled = !isSectionComplete(section);
  }

  inputs.forEach(input => {
    input.addEventListener("change", updateSubmitState);
    input.addEventListener("input", updateSubmitState);
  });

  updateSubmitState();
}

function setupAuthHandlers() {
  const loginTab = document.getElementById('loginTab');
  const signupTab = document.getElementById('signupTab');
  const loginForm = document.getElementById('loginForm');
  const signupForm = document.getElementById('signupForm');
  const loginBtn = document.getElementById('loginBtn');
  const signupBtn = document.getElementById('signupBtn');
  const logoutBtn = document.getElementById('logoutBtn');

  loginTab?.addEventListener('click', () => {
    loginTab.classList.add('active');
    signupTab.classList.remove('active');
    loginTab.style.borderBottom = '2px solid #007bff';
    signupTab.style.borderBottom = 'none';
    loginForm.style.display = 'block';
    signupForm.style.display = 'none';
  });

  signupTab?.addEventListener('click', () => {
    signupTab.classList.add('active');
    loginTab.classList.remove('active');
    signupTab.style.borderBottom = '2px solid #007bff';
    loginTab.style.borderBottom = 'none';
    signupForm.style.display = 'block';
    loginForm.style.display = 'none';
  });

  loginBtn?.addEventListener('click', async () => {
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;
    const errorDiv = document.getElementById('loginError');

    try {
      await signIn(email, password);
      showApp();
    } catch (error) {
      errorDiv.textContent = error.message;
      errorDiv.style.display = 'block';
    }
  });

  signupBtn?.addEventListener('click', async () => {
    const email = document.getElementById('signupEmail').value;
    const password = document.getElementById('signupPassword').value;
    const passwordConfirm = document.getElementById('signupPasswordConfirm').value;
    const errorDiv = document.getElementById('signupError');
    const successDiv = document.getElementById('signupSuccess');

    errorDiv.style.display = 'none';
    successDiv.style.display = 'none';

    if (password !== passwordConfirm) {
      errorDiv.textContent = 'Passwords do not match';
      errorDiv.style.display = 'block';
      return;
    }

    if (password.length < 6) {
      errorDiv.textContent = 'Password must be at least 6 characters';
      errorDiv.style.display = 'block';
      return;
    }

    try {
      await signUp(email, password);
      successDiv.style.display = 'block';
      document.getElementById('signupEmail').value = '';
      document.getElementById('signupPassword').value = '';
      document.getElementById('signupPasswordConfirm').value = '';
      setTimeout(() => {
        loginTab.click();
      }, 2000);
    } catch (error) {
      errorDiv.textContent = error.message;
      errorDiv.style.display = 'block';
    }
  });

  logoutBtn?.addEventListener('click', async () => {
    await signOut();
  });
}

function setupDatabaseIntegration() {
  initReview('TE020005').then(() => {
    loadAllSections();
  }).catch(error => {
    console.error('Failed to initialize review:', error);
  });

  setupSafetySave();
  setupAnimalSave();
  setupHumanSubjectsSave();
  setupAcquisitionSave();
}

async function loadAllSections() {
  try {
    await loadSafetySection();
    await loadAnimalSection();
    await loadHumanSubjectsSection();
    await loadAcquisitionSection();
  } catch (error) {
    console.error('Failed to load sections:', error);
  }
}

function setupSafetySave() {
  const saveBtn = document.getElementById('saveSafety');
  const section = document.getElementById('safetySection');

  saveBtn?.addEventListener('click', async (e) => {
    e.stopPropagation();

    try {
      const data = {
        rec_available: getRadioValue('safety_q1'),
        infectious_agents: getRadioValue('safety_q2'),
        bsat: getRadioValue('safety_q3'),
        chemical_agents: getRadioValue('safety_q4'),
        pesticides: getRadioValue('safety_q5'),
        negative_effects: getRadioValue('safety_q6'),
        comments: section.querySelector('.comments textarea')?.value || '',
        section_status: getProgressStatus(section)
      };

      await saveSafetyReview(data);
      showSaveConfirmation('Safety section has been saved successfully.');
    } catch (error) {
      alert('Failed to save Safety section: ' + error.message);
    }
  });
}

async function loadSafetySection() {
  try {
    const data = await loadSafetyReview();
    const section = document.getElementById('safetySection');

    if (!data) {
      clearSafetyUI();
      if (section) {
        setStatus(section, 'not-started');
        section.setAttribute('data-status', 'not-started');
      }
      return;
    }

    setRadioValue('safety_q1', data.rec_available);
    setRadioValue('safety_q2', data.infectious_agents);
    setRadioValue('safety_q3', data.bsat);
    setRadioValue('safety_q4', data.chemical_agents);
    setRadioValue('safety_q5', data.pesticides);
    setRadioValue('safety_q6', data.negative_effects);

    const commentsTextarea = section.querySelector('.comments textarea');
    if (commentsTextarea) commentsTextarea.value = data.comments || '';

    if (data.section_status) {
      setStatus(section, data.section_status);
      section.setAttribute('data-status', data.section_status);
    }

    if (data.submitted_at) {
      setSectionCompletionDate(section, data.submitted_at);
      lockSection(section);
    }
  } catch (error) {
    console.error('Failed to load safety section:', error);
  }
}

function setupAnimalSave() {
  const saveBtn = document.getElementById('saveAnimal');
  const section = document.getElementById('animalSection');

  saveBtn?.addEventListener('click', async (e) => {
    e.stopPropagation();

    try {
      const data = {
        animals_involved: getRadioValue('animal_q1'),
        protocol_number: section.querySelector('input[name="protocol_number"]')?.value || '',
        approval_date: section.querySelector('input[name="approval_date"]')?.value || null,
        expiration_date: section.querySelector('input[name="expiration_date"]')?.value || null,
        comments: section.querySelector('.comments textarea')?.value || '',
        section_status: getProgressStatus(section)
      };

      await saveAnimalReview(data);
      showSaveConfirmation('Animal Use section has been saved successfully.');
    } catch (error) {
      alert('Failed to save Animal Use section: ' + error.message);
    }
  });
}

async function loadAnimalSection() {
  try {
    const data = await loadAnimalReview();
    const section = document.getElementById('animalSection');

    if (!data) {
      clearAnimalUI();
      if (section) {
        setStatus(section, 'not-started');
        section.setAttribute('data-status', 'not-started');
      }
      return;
    }

    setRadioValue('animal_q1', data.animals_involved);

    const protocolInput = section.querySelector('input[name="protocol_number"]');
    if (protocolInput) protocolInput.value = data.protocol_number || '';

    const commentsTextarea = section.querySelector('.comments textarea');
    if (commentsTextarea) commentsTextarea.value = data.comments || '';

    if (data.section_status) {
      setStatus(section, data.section_status);
      section.setAttribute('data-status', data.section_status);
    }

    if (data.submitted_at) {
      setSectionCompletionDate(section, data.submitted_at);
      lockSection(section);
    }
  } catch (error) {
    console.error('Failed to load animal section:', error);
  }
}

function setupHumanSubjectsSave() {
  const saveBtn = document.getElementById('saveHuman');
  const section = document.getElementById('humanSection');

  saveBtn?.addEventListener('click', async (e) => {
    e.stopPropagation();

    try {
      const data = {
        humans_involved: getRadioValue('human_hs_q1'),
        irb_review_required: getRadioValue('human_hs_q2'),
        protocol_number: '',
        approval_date: null,
        expiration_date: null,
        comments: section.querySelector('.comments textarea')?.value || '',
        section_status: getSectionProgressStatusWithSubsections(section)
      };

      await saveHumanSubjectsReview(data);
      showSaveConfirmation('Human Subjects section has been saved successfully.');
    } catch (error) {
      alert('Failed to save Human Subjects section: ' + error.message);
    }
  });
}

async function loadHumanSubjectsSection() {
  try {
    const data = await loadHumanSubjectsReview();
    const section = document.getElementById('humanSection');

    if (!data) {
      clearHumanUI();
      if (section) {
        setStatus(section, 'not-started');
        section.setAttribute('data-status', 'not-started');
      }
      return;
    }

    setRadioValue('human_hs_q1', data.humans_involved);
    setRadioValue('human_hs_q2', data.irb_review_required);

    if (data.section_status && section) {
      setStatus(section, data.section_status);
      section.setAttribute('data-status', data.section_status);
    } else if (section) {
      const sectionStatus = getSectionProgressStatusWithSubsections(section);
      setStatus(section, sectionStatus);
      section.setAttribute('data-status', sectionStatus);
    }

    if (data.submitted_at && section) {
      setSectionCompletionDate(section, data.submitted_at);
      lockSection(section);
    }
  } catch (error) {
    console.error('Failed to load human subjects section:', error);
  }
}

function setupAcquisitionSave() {
  const section = document.getElementById('acquisitionSection');
  const saveButtons = section?.querySelectorAll('.save-btn');

  saveButtons?.forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();

      try {
        const subsection = btn.closest('.subsection');
        const subsectionStatus = subsection ? getSubsectionProgressStatus(subsection) : 'in-progress';

        const data = {
          personnel_qualifications: document.querySelector('[name="personnel_qualifications"]')?.value || '',
          personnel_effort: document.querySelector('[name="personnel_effort"]')?.value || '',
          equipment_included: getRadioValue('equipt_q1'),
          equipment_necessary: document.querySelector('[name="equipment_necessary"]')?.value || '',
          equipment_cost: document.querySelector('[name="equipment_cost"]')?.value || '',
          equipment_necessary_conduct: document.querySelector('[name="equipment_necessaryconduct"]')?.value || '',
          equipment_appropriate: document.querySelector('[name="equipment_appropriate"]')?.value || '',
          equipment_comment: section.querySelector('.equipment-no-comment')?.value || '',
          travel_included: getRadioValue('travel_q1'),
          travel_comment: document.querySelector('[name="travel_comment"]')?.value || '',
          subsection_status: subsectionStatus
        };

        await saveAcquisitionBudget(data);

        if (subsection) {
          setSubsectionStatus(subsection, subsectionStatus);
          subsection.setAttribute('data-status', subsectionStatus);
        }

        if (section) {
          const sectionStatus = getSectionProgressStatusWithSubsections(section);
          setStatus(section, sectionStatus);
          section.setAttribute('data-status', sectionStatus);
        }

        showSaveConfirmation('Acquisition/Contracting section has been saved successfully.');
      } catch (error) {
        alert('Failed to save Acquisition/Contracting section: ' + error.message);
      }
    });
  });
}

async function loadAcquisitionSection() {
  try {
    const data = await loadAcquisitionBudget();
    const section = document.getElementById('acquisitionSection');
    const subsection = document.getElementById('acquisitionBudgetSubsection');

    if (!data) {
      clearAcquisitionUI();
      if (section) {
        setStatus(section, 'not-started');
        section.setAttribute('data-status', 'not-started');
      }
      if (subsection) {
        setSubsectionStatus(subsection, 'not-started');
        subsection.setAttribute('data-status', 'not-started');
      }
      return;
    }

    const personnelQual = document.querySelector('[name="personnel_qualifications"]');
    if (personnelQual) personnelQual.value = data.personnel_qualifications || '';

    const personnelEffort = document.querySelector('[name="personnel_effort"]');
    if (personnelEffort) personnelEffort.value = data.personnel_effort || '';

    setRadioValue('equipt_q1', data.equipment_included);
    setRadioValue('travel_q1', data.travel_included);

    const equipNecessary = document.querySelector('[name="equipment_necessary"]');
    if (equipNecessary) equipNecessary.value = data.equipment_necessary || '';

    const equipCost = document.querySelector('[name="equipment_cost"]');
    if (equipCost) equipCost.value = data.equipment_cost || '';

    const travelComment = document.querySelector('[name="travel_comment"]');
    if (travelComment) travelComment.value = data.travel_comment || '';

    if (data.subsection_status && subsection) {
      setSubsectionStatus(subsection, data.subsection_status);
      subsection.setAttribute('data-status', data.subsection_status);
    }

    if (section) {
      const sectionStatus = getSectionProgressStatusWithSubsections(section);
      setStatus(section, sectionStatus);
      section.setAttribute('data-status', sectionStatus);
    }
  } catch (error) {
    console.error('Failed to load acquisition section:', error);
  }
}

async function saveSubmittedSection(section) {
  try {
    const sectionId = section.id;

    if (sectionId === 'safetySection') {
      const data = {
        rec_available: getRadioValue('safety_q1'),
        infectious_agents: getRadioValue('safety_q2'),
        bsat: getRadioValue('safety_q3'),
        chemical_agents: getRadioValue('safety_q4'),
        pesticides: getRadioValue('safety_q5'),
        negative_effects: getRadioValue('safety_q6'),
        comments: section.querySelector('.comments textarea')?.value || '',
        section_status: 'submitted',
        submitted_at: new Date().toISOString()
      };
      await saveSafetyReview(data);
    } else if (sectionId === 'animalSection') {
      const data = {
        animals_involved: getRadioValue('animal_q1'),
        protocol_number: section.querySelector('input[name="protocol_number"]')?.value || '',
        approval_date: section.querySelector('input[name="approval_date"]')?.value || null,
        expiration_date: section.querySelector('input[name="expiration_date"]')?.value || null,
        comments: section.querySelector('.comments textarea')?.value || '',
        section_status: 'submitted',
        submitted_at: new Date().toISOString()
      };
      await saveAnimalReview(data);
    } else if (sectionId === 'humanSection') {
      const data = {
        humans_involved: getRadioValue('human_hs_q1'),
        irb_review_required: getRadioValue('human_hs_q2'),
        protocol_number: section.querySelector('input[name="hs_protocol_number"]')?.value || '',
        approval_date: section.querySelector('input[name="hs_approval_date"]')?.value || null,
        expiration_date: section.querySelector('input[name="hs_expiration_date"]')?.value || null,
        comments: section.querySelector('.comments textarea')?.value || '',
        section_status: 'submitted',
        submitted_at: new Date().toISOString()
      };
      await saveHumanSubjectsReview(data);
    }
  } catch (error) {
    console.error('Failed to save submitted section:', error);
  }
}

function getRadioValue(name) {
  const checked = document.querySelector(`input[name="${name}"]:checked`);
  return checked ? checked.value : null;
}

function setRadioValue(name, value) {
  if (!value) return;
  const radio = document.querySelector(`input[name="${name}"][value="${value}"]`);
  if (radio) radio.checked = true;
}

function setupResetButtons() {
  document.getElementById('resetSafetyBtn')?.addEventListener('click', () => {
    confirmReset('Safety Review', async () => {
      await resetSafetySection();
      updateResetModalStatuses();
    });
  });

  document.getElementById('resetAnimalBtn')?.addEventListener('click', () => {
    confirmReset('Animal Research Review', async () => {
      await resetAnimalSection();
      updateResetModalStatuses();
    });
  });

  document.getElementById('resetHumanBtn')?.addEventListener('click', () => {
    confirmReset('Human Research Review', async () => {
      await resetHumanSection();
      updateResetModalStatuses();
    });
  });

  document.getElementById('resetAcquisitionBtn')?.addEventListener('click', () => {
    confirmReset('Acquisition/Contracting Review', async () => {
      await resetAcquisitionSection();
      updateResetModalStatuses();
    });
  });
}

function confirmReset(sectionName, onConfirm) {
  if (confirm(`Are you sure you want to clear all data for ${sectionName}? This cannot be undone.`)) {
    onConfirm();
  }
}

function updateResetModalStatuses() {
  const safetySection = document.getElementById('safetySection');
  const animalSection = document.getElementById('animalSection');
  const humanSection = document.getElementById('humanSection');
  const acquisitionSection = document.getElementById('acquisitionSection');

  const safetyStatus = safetySection?.getAttribute('data-status') || 'not-started';
  const animalStatus = animalSection?.getAttribute('data-status') || 'not-started';
  const humanStatus = humanSection?.getAttribute('data-status') || 'not-started';
  const acquisitionStatus = acquisitionSection?.getAttribute('data-status') || 'not-started';

  const formatStatus = (status) => {
    if (status === 'in-progress') return 'In Progress';
    if (status === 'completed') return 'Completed';
    if (status === 'submitted') return 'Submitted';
    return 'Not Started';
  };

  document.getElementById('safetyResetStatus').textContent = `Status: ${formatStatus(safetyStatus)}`;
  document.getElementById('animalResetStatus').textContent = `Status: ${formatStatus(animalStatus)}`;
  document.getElementById('humanResetStatus').textContent = `Status: ${formatStatus(humanStatus)}`;
  document.getElementById('acquisitionResetStatus').textContent = `Status: ${formatStatus(acquisitionStatus)}`;
}

async function resetSafetySection() {
  try {
    localStorage.removeItem(STORAGE_KEYS.SAFETY_REVIEW);

    clearSafetyUI();
    const section = document.getElementById('safetySection');
    if (section) {
      setStatus(section, 'not-started');
      section.setAttribute('data-status', 'not-started');
      section.classList.remove('locked');
      const sectionDate = section.querySelector('.section-date');
      if (sectionDate) {
        sectionDate.textContent = '';
        sectionDate.classList.add('hidden');
      }
      section.querySelectorAll('input, textarea, button').forEach(el => el.disabled = false);
    }

    showSaveConfirmation('Safety Review section has been cleared successfully.');
  } catch (error) {
    console.error('Failed to reset safety section:', error);
    alert('Failed to reset Safety Review section. Please try again.');
  }
}

async function resetAnimalSection() {
  try {
    localStorage.removeItem(STORAGE_KEYS.ANIMAL_REVIEW);

    clearAnimalUI();
    const section = document.getElementById('animalSection');
    if (section) {
      setStatus(section, 'not-started');
      section.setAttribute('data-status', 'not-started');
      section.classList.remove('locked');
      const sectionDate = section.querySelector('.section-date');
      if (sectionDate) {
        sectionDate.textContent = '';
        sectionDate.classList.add('hidden');
      }
      section.querySelectorAll('input, textarea, button').forEach(el => el.disabled = false);
    }

    showSaveConfirmation('Animal Research Review section has been cleared successfully.');
  } catch (error) {
    console.error('Failed to reset animal section:', error);
    alert('Failed to reset Animal Research Review section. Please try again.');
  }
}

async function resetHumanSection() {
  try {
    localStorage.removeItem(STORAGE_KEYS.HUMAN_REVIEW);

    clearHumanUI();
    const section = document.getElementById('humanSection');
    if (section) {
      setStatus(section, 'not-started');
      section.setAttribute('data-status', 'not-started');
      section.classList.remove('locked');
      const sectionDate = section.querySelector('.section-date');
      if (sectionDate) {
        sectionDate.textContent = '';
        sectionDate.classList.add('hidden');
      }
      section.querySelectorAll('input, textarea, button').forEach(el => el.disabled = false);
    }

    showSaveConfirmation('Human Research Review section has been cleared successfully.');
  } catch (error) {
    console.error('Failed to reset human section:', error);
    alert('Failed to reset Human Research Review section. Please try again.');
  }
}

async function resetAcquisitionSection() {
  try {
    localStorage.removeItem(STORAGE_KEYS.ACQUISITION_REVIEW);

    clearAcquisitionUI();
    const section = document.getElementById('acquisitionSection');
    if (section) {
      setStatus(section, 'not-started');
      section.setAttribute('data-status', 'not-started');
      section.classList.remove('locked');
      const sectionDate = section.querySelector('.section-date');
      if (sectionDate) {
        sectionDate.textContent = '';
        sectionDate.classList.add('hidden');
      }
      section.querySelectorAll('input, textarea, button').forEach(el => el.disabled = false);

      section.querySelectorAll('.subsection').forEach(subsection => {
        setSubsectionStatus(subsection, 'not-started');
        subsection.setAttribute('data-status', 'not-started');
        subsection.classList.remove('locked');
      });
    }

    showSaveConfirmation('Acquisition/Contracting Review section has been cleared successfully.');
  } catch (error) {
    console.error('Failed to reset acquisition section:', error);
    alert('Failed to reset Acquisition/Contracting Review section. Please try again.');
  }
}

function clearSafetyUI() {
  const safetySection = document.getElementById('safetySection');
  if (!safetySection) return;

  safetySection.querySelectorAll('input:not([type="button"]):not([type="submit"]), textarea, select').forEach(input => {
    if (input.type === 'radio' || input.type === 'checkbox') {
      input.checked = false;
    } else if (input.type !== 'button' && input.type !== 'submit') {
      input.value = '';
    }
  });
}

function clearAnimalUI() {
  const animalSection = document.getElementById('animalSection');
  if (!animalSection) return;

  animalSection.querySelectorAll('input:not([type="button"]):not([type="submit"]), textarea, select').forEach(input => {
    if (input.type === 'radio' || input.type === 'checkbox') {
      input.checked = false;
    } else if (input.type !== 'button' && input.type !== 'submit') {
      input.value = '';
    }
  });

  const animalModal = document.getElementById('animalSpeciesModal');
  if (animalModal) {
    animalModal.querySelectorAll('input[type="checkbox"]').forEach(cb => {
      cb.checked = false;
    });
  }
}

function clearHumanUI() {
  const humanSection = document.getElementById('humanSection');
  if (!humanSection) return;

  humanSection.querySelectorAll('input:not([type="button"]):not([type="submit"]), textarea, select').forEach(input => {
    if (input.type === 'radio' || input.type === 'checkbox') {
      input.checked = false;
    } else if (input.type !== 'button' && input.type !== 'submit') {
      input.value = '';
    }
  });
}

function clearAcquisitionUI() {
  const acquisitionSection = document.getElementById('acquisitionSection');
  if (!acquisitionSection) return;

  acquisitionSection.querySelectorAll('input:not([type="button"]):not([type="submit"]), textarea, select').forEach(input => {
    if (input.type === 'radio' || input.type === 'checkbox') {
      input.checked = false;
    } else if (input.type !== 'button' && input.type !== 'submit') {
      input.value = '';
    }
  });

  const specialReqModal = document.getElementById('specialRequirementsModal');
  if (specialReqModal) {
    specialReqModal.querySelectorAll('input[type="checkbox"]').forEach(cb => {
      cb.checked = false;
    });
  }
}

function setupPrimeAwardListener() {
  const primeAwardSelect = document.getElementById('primeAwardSelect');
  const createRecordButtons = document.querySelectorAll('.create-record-btn');

  if (!primeAwardSelect) return;

  primeAwardSelect.addEventListener('change', () => {
    const selectedValue = primeAwardSelect.value;
    const shouldShow = selectedValue === 'extramural_intramural';

    createRecordButtons.forEach(btn => {
      btn.style.display = shouldShow ? 'inline-block' : 'none';
    });
  });
}
