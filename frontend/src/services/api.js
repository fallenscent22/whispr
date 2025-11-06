import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: async (username, password) => {
    const response = await api.post('/auth/signin', { username, password });
    return response.data;
  },
  
  register: async (username, email, password) => {
    const response = await api.post('/auth/signup', { username, email, password });
    return response.data;
  },
};

export const messageAPI = {
  getRecentMessages: async (roomId) => {
    const response = await api.get(`/messages/recent/${roomId}`);
    return response.data;
  },
  
  getMessageHistory: async (roomId, page = 0, size = 20) => {
    const response = await api.get(`/messages/history/${roomId}?page=${page}&size=${size}`);
    return response.data;
  },
  
  getOnlineUsers: async () => {
    const response = await api.get('/messages/online-users');
    return response.data;
  },
  
  markMessagesAsRead: async (roomId, username) => {
    const response = await api.post(`/messages/mark-read/${roomId}?username=${username}`);
    return response.data;
  }
};

export default api;