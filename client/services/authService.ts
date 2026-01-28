import { User } from '../types';
import { BACKEND_API_URL } from '../constants';

export const authService = {
  login: async (username: string, password: string): Promise<{ user: User; token: string }> => {
    try {
      console.log('[Auth] Attempting login for:', username);
      const response = await fetch(`${BACKEND_API_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      const json = await response.json();
      console.log('[Auth] Login raw response:', json);

      if (!response.ok || !json.success) {
        throw new Error(json.message || 'Invalid credentials');
      }

      if (!json.data || !json.data.user || !json.data.token) {
        console.error('[Auth] Response missing required data fields:', json.data);
        throw new Error('Malformed response from server');
      }

      return json.data;
    } catch (error: any) {
      console.error('[Auth] Login error:', error);
      throw error;
    }
  },

  register: async (fullName: string, username: string, password: string): Promise<{ user: User; token: string }> => {
    try {
      console.log('[Auth] Attempting register for:', username);
      const response = await fetch(`${BACKEND_API_URL}/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ fullName, username, password }),
      });

      const json = await response.json();
      console.log('[Auth] Register raw response:', json);

      if (!response.ok || !json.success) {
        throw new Error(json.message || 'Registration failed');
      }

      if (!json.data || !json.data.user || !json.data.token) {
        console.error('[Auth] Response missing required data fields:', json.data);
        throw new Error('Malformed response from server');
      }

      return json.data;
    } catch (error: any) {
      console.error('[Auth] Register error:', error);
      throw error;
    }
  }
};