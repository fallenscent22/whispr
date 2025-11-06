import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignUpPage';
import { ChatPage } from './pages/ChatPage';
import { ProtectedRoute } from './components/common/ProtectedRoute';

const AppRoutes = () => {
  const { user } = useAuth();

  return (
    <Routes>
      <Route 
        path="/login" 
        element={!user ? <LoginPage /> : <Navigate to="/chat" replace />} 
      />
      <Route 
        path="/signup" 
        element={!user ? <SignupPage /> : <Navigate to="/chat" replace />} 
      />
      <Route 
        path="/chat" 
        element={
          <ProtectedRoute>
            <ChatPage />
          </ProtectedRoute>
        } 
      />
      <Route path="/" element={<Navigate to={user ? "/chat" : "/login"} replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

function App() {
  return (
    <Router>
      <AuthProvider>
        <div className="App">
          <AppRoutes />
        </div>
      </AuthProvider>
    </Router>
  );
}

export default App;