import { STORAGE_KEYS } from '../constants/storage';

/**
 * Storage utility for client-side authentication persistence.
 *
 * Decision Documentation:
 * For this initial React SPA architecture, the short-lived JWT access token is stored
 * in browser localStorage to preserve user session across page refreshes.
 *
 * Security Constraints Enforced:
 * 1. Passwords, hashes, and internal secrets are NEVER stored.
 * 2. Tokens are never logged to console or network logs.
 * 3. Client-side storage is used strictly for presentation; the Spring Boot backend
 *    remains the authoritative source of truth for all authentication and authorization.
 */

export const getToken = () => {
  try {
    return localStorage.getItem(STORAGE_KEYS.TOKEN);
  } catch {
    return null;
  }
};

export const setToken = (token) => {
  try {
    if (token) {
      localStorage.setItem(STORAGE_KEYS.TOKEN, token);
    } else {
      localStorage.removeItem(STORAGE_KEYS.TOKEN);
    }
  } catch {
    // Storage quota or privacy sandbox error
  }
};

export const removeToken = () => {
  try {
    localStorage.removeItem(STORAGE_KEYS.TOKEN);
  } catch {
    // Graceful fallback
  }
};

export const getUser = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.USER);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
};

export const setUser = (user) => {
  try {
    if (user) {
      // Ensure no sensitive fields can ever be stored
      const { id, name, email, role, createdAt, updatedAt } = user;
      const safeUser = { id, name, email, role, createdAt, updatedAt };
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(safeUser));
    } else {
      localStorage.removeItem(STORAGE_KEYS.USER);
    }
  } catch {
    // Graceful fallback
  }
};

export const removeUser = () => {
  try {
    localStorage.removeItem(STORAGE_KEYS.USER);
  } catch {
    // Graceful fallback
  }
};

export const clearAuth = () => {
  removeToken();
  removeUser();
};
