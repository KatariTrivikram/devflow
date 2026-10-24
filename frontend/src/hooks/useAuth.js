import { useContext } from 'react';
import { AuthContext } from '../context/authContextDef';

/**
 * Custom hook to access the DevFlow authentication context.
 * @returns {{
 *   user: { id: string, name: string, email: string, role: string, createdAt: string, updatedAt: string } | null,
 *   token: string | null,
 *   isAuthenticated: boolean,
 *   loading: boolean,
 *   login: Function,
 *   register: Function,
 *   logout: Function
 * }}
 */
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default useAuth;
