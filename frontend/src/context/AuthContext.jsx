import React, { createContext, useState, useContext, useEffect } from 'react';
import { api, authAPI } from '../services/api';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');
    
    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    setIsLoading(false);
  }, []);

  const login = async (username, password) => {
    try {
      const response = await authAPI.login(username, password);
      const { token, id, username: userUsername, email } = response;
      
      const userData = { id, username: userUsername, email };
      
      setToken(token);
      setUser(userData);
      
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(userData));
      
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      return response;
    } catch (error) {
      console.error('Login error:', error);
      throw new Error(error.response?.data || 'Login failed. Please check your credentials.');
    }
  };

  const register = async (username, email, password) => {
    try {
      await authAPI.register(username, email, password);
    } catch (error) {
      throw new Error(error.response?.data || 'Registration failed');
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  };

  const value = {
    user,
    token,
    login,
    register,
    logout,
    isLoading,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};