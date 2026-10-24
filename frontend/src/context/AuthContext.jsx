import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { AuthContext } from './authContextDef';
import { getToken, setToken, getUser, setUser, clearAuth } from '../utils/storage';
import authService from '../services/authService';

export const AuthProvider = ({ children }) => {
  const [user, setCurrentUser] = useState(() => getUser());
  const [token, setCurrentToken] = useState(() => getToken());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Reactive logout on 401 dispatched by Axios interceptor
    const handleUnauthorized = () => {
      clearAuth();
      setCurrentToken(null);
      setCurrentUser(null);
    };

    window.addEventListener('devflow:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('devflow:unauthorized', handleUnauthorized);
    };
  }, []);

  const login = useCallback(async ({ email, password }) => {
    setLoading(true);
    try {
      const data = await authService.login({ email, password });
      if (data?.accessToken && data?.user) {
        setToken(data.accessToken);
        setUser(data.user);
        setCurrentToken(data.accessToken);
        setCurrentUser(data.user);
      }
      return data;
    } finally {
      setLoading(false);
    }
  }, []);

  const register = useCallback(async ({ name, email, password, role }) => {
    setLoading(true);
    try {
      const data = await authService.register({ name, email, password, role });
      if (data?.accessToken && data?.user) {
        setToken(data.accessToken);
        setUser(data.user);
        setCurrentToken(data.accessToken);
        setCurrentUser(data.user);
      }
      return data;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setCurrentToken(null);
    setCurrentUser(null);
  }, []);

  const isAuthenticated = Boolean(token && user);

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated,
      loading,
      login,
      register,
      logout,
    }),
    [user, token, isAuthenticated, loading, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthProvider;
