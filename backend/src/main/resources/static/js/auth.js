// Authentication and Authorization Utilities

// Check if user is authenticated
function isAuthenticated() {
    return authAPI.isAuthenticated();
}

// Get current user information
function getCurrentUser() {
    return authAPI.getCurrentUser();
}

// Check if user has specific role
function hasRole(role) {
    const user = getCurrentUser();
    return user && user.role === role;
}

// Protect page - redirect if not authenticated
function requireAuth() {
    if (!isAuthenticated()) {
        window.location.replace('/login.html');
        return false;
    }
    return true;
}

// Protect page with role check
function requireRole(role) {
    if (!requireAuth()) {
        return false;
    }

    if (!hasRole(role)) {
        showNotification('Access denied. Insufficient permissions.', 'error');
        setTimeout(() => {
            window.location.replace('/index.html');
        }, 2000);
        return false;
    }

    return true;
}

// Redirect based on user role
function redirectToDashboard() {
    const user = getCurrentUser();

    if (!user) {
        window.location.href = '/login.html';
        return;
    }

    switch (user.role) {
        case 'CUSTOMER':
            window.location.replace('/customer/dashboard.html');
            break;
        case 'MANAGER_ADMIN':
            window.location.replace('/manager/dashboard.html');
            break;
        case 'OWNER_ADMIN':
            window.location.replace('/owner/dashboard.html');
            break;
        default:
            window.location.replace('/index.html');
    }
}

// Update navigation based on authentication status
function updateNavigation() {
    const user = getCurrentUser();
    const navMenu = document.querySelector('.nav-menu');
    const authPlaceholder = document.getElementById('auth-nav-placeholder');

    if (!navMenu) return;

    // Handle mobile menu toggle
    const hamburger = document.getElementById('hamburger');
    if (hamburger) {
        hamburger.onclick = () => {
            hamburger.classList.toggle('active');
            navMenu.classList.toggle('active');
        };
    }

    // Close menu when clicking links
    navMenu.querySelectorAll('.nav-link').forEach(link => {
        link.onclick = () => {
            if (hamburger) hamburger.classList.remove('active');
            navMenu.classList.remove('active');
        };
    });

    let authHTML = '';
    if (user) {
        authHTML = `
            <li class="nav-item user-dropdown">
                <div class="user-trigger">
                    <span class="nav-user-greeting">
                        Hi, ${user.fullName.split(' ')[0]}
                        <i class="dropdown-arrow"></i>
                    </span>
                </div>
                <div class="dropdown-content">
                    <a href="#" onclick="goToDashboard()">
                        <span style="font-size: 1.2rem;">📊</span> Dashboard
                    </a>
                    <a href="#" onclick="logout()" class="logout-link">
                        <span style="font-size: 1.2rem;">🚪</span> Logout
                    </a>
                </div>
            </li>
        `;
    } else {
        authHTML = `
            <li class="nav-item">
                <a href="/login.html" class="btn btn-primary btn-sm" style="color: white;">Login</a>
            </li>
        `;
    }

    if (authPlaceholder) {
        authPlaceholder.innerHTML = authHTML;
    } else {
        // Fallback for older pages
        let authSection = navMenu.querySelector('.nav-auth');
        if (!authSection) {
            authSection = document.createElement('div');
            authSection.className = 'nav-auth';
            navMenu.appendChild(authSection);
        }
        authSection.innerHTML = authHTML;
    }
}

// Logout function
// Logout function
function logout() {
    // Create Modal Element
    const modalOverlay = document.createElement('div');
    modalOverlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.4);
        backdrop-filter: blur(4px);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 100000;
        opacity: 0;
        transition: opacity 0.3s ease;
    `;

    const modalContent = document.createElement('div');
    modalContent.style.cssText = `
        background: white;
        padding: 2rem;
        border-radius: 16px;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
        max-width: 400px;
        width: 90%;
        text-align: center;
        transform: scale(0.9);
        transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    `;

    modalContent.innerHTML = `
        <div style="font-size: 3rem; margin-bottom: 1rem;">👋</div>
        <h3 style="margin-bottom: 0.5rem; color: #1e293b;">Sign Out?</h3>
        <p style="color: #64748b; margin-bottom: 2rem;">Are you sure you want to end your session?</p>
        <div style="display: flex; gap: 1rem; justify-content: center;">
            <button id="cancel-logout" style="padding: 0.75rem 1.5rem; border: 1px solid #cbd5e1; background: white; color: #475569; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 0.95rem; transition: all 0.2s;">Cancel</button>
            <button id="confirm-logout" style="padding: 0.75rem 1.5rem; border: none; background: #ef4444; color: white; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 0.95rem; box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3); transition: all 0.2s;">Logout</button>
        </div>
    `;

    modalOverlay.appendChild(modalContent);
    document.body.appendChild(modalOverlay);

    // Annimate In
    requestAnimationFrame(() => {
        modalOverlay.style.opacity = '1';
        modalContent.style.transform = 'scale(1)';
    });

    // Handle Actions
    const closeModal = () => {
        modalOverlay.style.opacity = '0';
        modalContent.style.transform = 'scale(0.9)';
        setTimeout(() => modalOverlay.remove(), 300);
    };

    document.getElementById('cancel-logout').onclick = closeModal;

    document.getElementById('confirm-logout').onclick = () => {
        closeModal();

        // Clear all authentication data
        authAPI.logout(); // Use api.js function which clears token and redirects
    };

    // Close on click outside
    modalOverlay.onclick = (e) => {
        if (e.target === modalOverlay) closeModal();
    };
}

// Go to dashboard
function goToDashboard() {
    redirectToDashboard();
}

// Initialize auth on page load
document.addEventListener('DOMContentLoaded', () => {
    updateNavigation();

    // Navbar scroll effect
    const navbar = document.getElementById('navbar');
    if (navbar) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        });
    }
});

// WhatsApp Button Injection
function injectWhatsAppButton() {
    // Don't show on admin/manager dashboards
    const path = window.location.pathname.toLowerCase();
    if (path.includes('/owner/') || path.includes('/manager/')) {
        return;
    }

    if (document.querySelector('.whatsapp-float')) return;

    const whatsappBtn = document.createElement('a');
    whatsappBtn.href = "https://wa.me/918962569111";
    whatsappBtn.className = "whatsapp-float";
    whatsappBtn.target = "_blank";
    whatsappBtn.innerHTML = `
        <svg viewBox="0 0 24 24">
            <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.438 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413Z"/>
        </svg>
    `;
    document.body.appendChild(whatsappBtn);
}

// Ensure WhatsApp button is injected
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', injectWhatsAppButton);
} else {
    injectWhatsAppButton();
}

// Handle login form submission
function handleLoginForm(event) {
    event.preventDefault();

    const form = event.target;
    const email = form.email.value;
    const password = form.password.value;
    const submitBtn = form.querySelector('button[type="submit"]');

    submitBtn.disabled = true;
    submitBtn.textContent = 'Logging in...';

    authAPI.login({ email, password })
        .then(response => {
            showNotification('Login successful!', 'success');
            setTimeout(() => {
                redirectToDashboard();
            }, 1000);
        })
        .catch(error => {
            showNotification('Login failed. Please check your credentials.', 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Login';
        });
}

// Handle registration form submission
function handleRegisterForm(event) {
    event.preventDefault();

    const form = event.target;
    const formData = {
        email: form.email.value,
        password: form.password.value,
        fullName: form.fullName.value,
        phone: form.phone.value,
        role: form.role.value
    };

    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Registering...';

    authAPI.register(formData)
        .then(response => {
            showNotification('Registration successful!', 'success');
            setTimeout(() => {
                redirectToDashboard();
            }, 1000);
        })
        .catch(error => {
            showNotification('Registration failed. Email may already exist.', 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Register';
        });
}

// Display user info in dashboard
function displayUserInfo(elementId = 'user-info') {
    const user = getCurrentUser();
    const element = document.getElementById(elementId);

    if (element && user) {
        element.innerHTML = `
            <div class="user-info-card">
                <h3>${user.fullName}</h3>
                <p>${user.email}</p>
                <span class="role-badge">${user.role.replace('_', ' ')}</span>
            </div>
        `;
    }
}
