import axios from 'axios';

export const TOKEN_KEY = 'mm_token';
export const USER_KEY = 'mm_user';

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' });

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// An expired/invalid token (401 while we believed we were logged in) logs the user out.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && localStorage.getItem(TOKEN_KEY)) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (!window.location.pathname.startsWith('/login')) window.location.assign('/login');
    }
    return Promise.reject(err);
  }
);

/** Extract a human readable message from the backend's consistent error JSON. */
export function errMsg(err) {
  const data = err?.response?.data;
  if (data?.fieldErrors) {
    return Object.entries(data.fieldErrors).map(([f, m]) => `${f}: ${m}`).join('; ');
  }
  return data?.message || err?.message || 'Something went wrong';
}

export const money = (v) => `$${Number(v ?? 0).toFixed(2)}`;

export default api;
