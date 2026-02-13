// API Configuration and Utilities
const API_BASE_URL = '/api';

// API Client with JWT token management
class APIClient {
    constructor() {
        this.baseURL = API_BASE_URL;
        this.token = localStorage.getItem('authToken');
    }

    setToken(token) {
        this.token = token;
        localStorage.setItem('authToken', token);
    }

    clearToken() {
        this.token = null;
        localStorage.removeItem('authToken');
        localStorage.removeItem('userInfo');
    }

    getHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        return headers;
    }

    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        const config = {
            ...options,
            headers: {
                ...this.getHeaders(),
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, config);

            if (!response.ok) {
                if (response.status === 401) {
                    this.clearToken();
                    window.location.href = '/login.html';
                    throw new Error('Unauthorized');
                }

                let errorMessage = `HTTP error! status: ${response.status}`;
                try {
                    const errorText = await response.text();
                    if (errorText) {
                        try {
                            const errorJson = JSON.parse(errorText);
                            errorMessage = errorJson.message || errorText;
                        } catch (e) {
                            errorMessage = errorText;
                        }
                    }
                } catch (e) { }

                throw new Error(errorMessage);
            }

            // Handle empty response
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            return null;
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    }

    async get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    }

    async post(endpoint, data) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async put(endpoint, data) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
}

// Create global API instance
const api = new APIClient();

// Authentication APIs
const authAPI = {
    async register(userData) {
        const response = await api.post('/auth/register', userData);
        return response;
    },

    async sendOTP(email) {
        return await api.post('/auth/send-otp', { email });
    },

    async verify(email, otp) {
        const response = await api.post('/auth/verify', { email, otp });
        if (response.token) {
            api.setToken(response.token);
            localStorage.setItem('userInfo', JSON.stringify(response));
        }
        return response;
    },

    async login(credentials) {
        const response = await api.post('/auth/login', credentials);
        if (response.token) {
            api.setToken(response.token);
            localStorage.setItem('userInfo', JSON.stringify(response));
        }
        return response;
    },

    async googleLogin(idToken) {
        const response = await api.post('/auth/google', { idToken });
        if (response.token) {
            api.setToken(response.token);
            localStorage.setItem('userInfo', JSON.stringify(response));
        }
        return response;
    },

    logout() {
        api.clearToken();
        window.location.replace('/login.html');
    },

    getCurrentUser() {
        const userInfo = localStorage.getItem('userInfo');
        return userInfo ? JSON.parse(userInfo) : null;
    },

    isAuthenticated() {
        return !!api.token;
    }
};

// Public APIs
const publicAPI = {
    async getPackages() {
        return api.get('/public/packages');
    },

    async getPackageById(id) {
        return api.get(`/public/packages/${id}`);
    },

    async submitContactForm(formData) {
        return api.post('/public/contact', formData);
    },

    async getVisionaries() {
        return api.get('/public/visionaries');
    }
};

// Customer APIs
const customerAPI = {
    async purchasePackage(purchaseData) {
        return api.post('/customer/purchase', purchaseData);
    },

    async createProject(packageId, projectName, requirements, computedPrice) {
        return api.post('/customer/projects', { packageId, projectName, requirements, computedPrice });
    },

    async getProfile() {
        return api.get('/customer/profile');
    },

    async updateProject(projectId, updateData) {
        return api.put(`/customer/projects/${projectId}`, updateData);
    },

    async deleteProject(projectId) {
        return api.delete(`/customer/projects/${projectId}`);
    },

    async initiatePayment(paymentId) {
        return api.post(`/customer/payments/${paymentId}/initiate`);
    },

    async verifyPayment(paymentId, razorpayData) {
        return api.post(`/customer/payments/${paymentId}/verify`, razorpayData);
    },

    async getMyProjects() {
        return api.get('/customer/projects');
    },

    async getProjectDetails(projectId) {
        return api.get(`/customer/projects/${projectId}`);
    },

    async getProjectStages(projectId) {
        return api.get(`/customer/projects/${projectId}/stages`);
    }
};

// Manager APIs
const managerAPI = {
    async getAssignedProjects() {
        return api.get('/manager/projects');
    },

    async getProjectDetails(projectId) {
        return api.get(`/manager/projects/${projectId}`);
    },

    async updateProject(projectId, updateData) {
        return api.put(`/manager/projects/${projectId}`, updateData);
    },

    async getProjectStages(projectId) {
        return api.get(`/manager/projects/${projectId}/stages`);
    },

    async updateStage(stageId, updateData) {
        return api.put(`/manager/stages/${stageId}`, updateData);
    },

    async getProjectPayments(projectId) {
        return api.get(`/manager/projects/${projectId}/payments`);
    },

    async requestPayment(paymentData) {
        return api.post('/manager/payments/request', paymentData);
    },

    // Package Management (Services)
    async getAllPackages() {
        return api.get('/manager/packages');
    },

    async createPackage(packageData) {
        return api.post('/manager/packages', packageData);
    },

    async updatePackage(packageId, packageData) {
        return api.put(`/manager/packages/${packageId}`, packageData);
    },

    async deletePackage(packageId) {
        return api.delete(`/manager/packages/${packageId}`);
    }
};

// Owner APIs
const ownerAPI = {
    // Package Management
    async getAllPackages() {
        return api.get('/owner/packages');
    },

    async createPackage(packageData) {
        return api.post('/owner/packages', packageData);
    },

    async updatePackage(packageId, packageData) {
        return api.put(`/owner/packages/${packageId}`, packageData);
    },

    async deletePackage(packageId) {
        return api.delete(`/owner/packages/${packageId}`);
    },

    // Project Management
    async getAllProjects() {
        return api.get('/owner/projects');
    },

    async getProjectById(projectId) {
        return api.get(`/owner/projects/${projectId}`);
    },

    async assignManager(projectId, managerId) {
        return api.put(`/owner/projects/${projectId}/assign/${managerId}`);
    },

    async deleteProject(projectId) {
        return api.delete(`/owner/projects/${projectId}`);
    },

    // Manager Management
    async getAllManagers() {
        return api.get('/owner/managers');
    },

    async createManager(data) {
        return api.post('/owner/managers', data);
    },

    async deleteManager(id) {
        return api.delete(`/owner/managers/${id}`);
    },

    // Customer Management
    async getAllCustomers() {
        return api.get('/owner/customers');
    },

    async deleteCustomer(customerId) {
        return api.delete(`/owner/customers/${customerId}`);
    },

    // Enquiries
    async getAllEnquiries() {
        return api.get('/owner/enquiries');
    },

    async markEnquiryContacted(enquiryId) {
        return api.put(`/owner/enquiries/${enquiryId}/contacted`);
    },

    // Analytics
    async getAnalytics() {
        return api.get('/owner/analytics');
    },

    async getAllPayments() {
        return api.get('/owner/payments');
    },

    async getAllStages(projectId) {
        return api.get(`/owner/projects/${projectId}/stages`);
    },

    async updateStage(stageId, data) {
        return api.put(`/owner/stages/${stageId}`, data);
    },

    async getProjectPayments(projectId) {
        return api.get(`/owner/projects/${projectId}/payments`);
    },

    async requestPayment(data) {
        return api.post('/owner/payments/request', data);
    },

    // Visionary Management
    async getAllVisionaries() {
        return api.get('/owner/visionaries');
    },

    async createVisionary(visionaryData) {
        return api.post('/owner/visionaries', visionaryData);
    },

    async updateVisionary(visionaryId, visionaryData) {
        return api.put(`/owner/visionaries/${visionaryId}`, visionaryData);
    },

    async deleteVisionary(visionaryId) {
        return api.delete(`/owner/visionaries/${visionaryId}`);
    },

    async uploadVisionaryImage(file) {
        const formData = new FormData();
        formData.append('file', file);

        const url = `${API_BASE_URL}/owner/visionaries/upload`;
        const headers = {};
        if (api.token) {
            headers['Authorization'] = `Bearer ${api.token}`;
        }

        const response = await fetch(url, {
            method: 'POST',
            body: formData,
            headers: headers
        });

        if (!response.ok) {
            throw new Error('Upload failed');
        }

        return await response.json();
    }
};

// Payment APIs
const paymentAPI = {
    async createOrder(amount) {
        return api.post('/payment/create-order', { amount });
    },
    async getKey() {
        return api.get('/payment/key');
    },
    async verifyPayment(paymentData) {
        return api.post('/payment/verify-payment', paymentData);
    }
};

// File Management APIs
const fileAPI = {
    async uploadFile(projectId, file, description) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('projectId', projectId);
        if (description) {
            formData.append('description', description);
        }

        const url = `${API_BASE_URL}/files/upload`;
        const headers = {};
        if (api.token) {
            headers['Authorization'] = `Bearer ${api.token}`;
        }

        const response = await fetch(url, {
            method: 'POST',
            body: formData,
            headers: headers
        });

        if (!response.ok) {
            throw new Error('Upload failed');
        }

        return await response.json();
    },
    async getProjectFiles(projectId) {
        return api.get(`/files/project/${projectId}`);
    },
    async deleteFile(fileId) {
        return api.delete(`/files/${fileId}`);
    }
};

// Utility Functions
function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 1rem 2rem;
        background: ${type === 'success' ? '#4CAF50' : '#f44336'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.2);
        z-index: 10000;
        animation: slideIn 0.3s ease-out;
    `;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'fadeOut 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 4000);
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 0
    }).format(amount);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
}

function formatDateTime(dateString) {
    return new Date(dateString).toLocaleString('en-IN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}
// Hamburger Menu Toggle
document.addEventListener('DOMContentLoaded', () => {
    const hamburger = document.getElementById('hamburger');
    const navMenu = document.getElementById('nav-menu');

    if (hamburger && navMenu) {
        hamburger.addEventListener('click', (e) => {
            e.stopPropagation();
            hamburger.classList.toggle('active');
            navMenu.classList.toggle('active');
        });

        // Close menu when clicking outside
        document.addEventListener('click', (e) => {
            if (navMenu.classList.contains('active') && !navMenu.contains(e.target) && !hamburger.contains(e.target)) {
                hamburger.classList.remove('active');
                navMenu.classList.remove('active');
            }
        });

        // Use event delegation for closing menu on link clicks
        navMenu.addEventListener('click', (e) => {
            const link = e.target.closest('a');
            if (link) {
                hamburger.classList.remove('active');
                navMenu.classList.remove('active');
            }
        });
    }
});
