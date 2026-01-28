import { Chat, User, Message } from '../types';
import { BACKEND_API_URL } from '../constants';

export const chatService = {
  /**
   * Internal helper to handle fetch requests with consistent headers and error handling.
   */
  async request(path: string, token: string, options: RequestInit = {}) {
    const cleanToken = token.trim();
    const response = await fetch(`${BACKEND_API_URL}${path}`, {
      ...options,
      headers: {
        'Authorization': `Bearer ${cleanToken}`,
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      if (response.status === 401) {
        console.error("401 Unauthorized: Backend rejected the JWT.");
      }
      throw new Error(errorData.message || `Request failed: ${response.status}`);
    }
    return response.json();
  },

  /**
   * Normalizes a message object from the backend.
   * Handles discrepancies like 'content' vs 'text', 'sentAt' vs 'timestamp', 'chat_id' vs 'chatId'.
   */
  normalizeMessage(msg: any): Message {
      if (!msg) return null as any;
      const data = msg.payload || msg;

      // Mapping SQL 'content' and 'created_at' to Frontend 'text' and 'timestamp'
      return {
          id: data.id || data.messageId || `temp-${Math.random()}`,
          chatId: data.chatId || data.chat_id,
          senderId: data.senderId || data.sender_id,
          // CRITICAL: Map 'content' (from SQL) to 'text' (for UI)
          text: data.text || data.content || "", 
          // CRITICAL: Map 'created_at' (from SQL) to 'timestamp'
          timestamp: data.timestamp || (data.created_at ? new Date(data.created_at).getTime() : Date.now()),
          status: data.status || 'SENT',
          type: data.type || 'text'
      };
  },

  /**
   * Normalizes a chat object from the backend.
   */
  normalizeChat(chat: any): Chat {
    return {
      ...chat,
      // Default type to private if missing
      type: (chat.type || 'PRIVATE').toLowerCase() as any,
      avatarUrl: chat.avatarUrl || null,
      unreadCount: chat.unreadCount || 0,
      participants: chat.participants || [],
      name: chat.name || 'Unknown Chat',
      lastMessage: chat.lastMessage ? chatService.normalizeMessage(chat.lastMessage) : undefined
    };
  },

  /**
   * Fetches all chats for the current authenticated user.
   */
  getChats: async (token: string): Promise<Chat[]> => {
    try {
      const json = await chatService.request('/chats', token);
      // Backend log shows data.chats
      const rawChats = json.data?.chats || json.data || [];
      return Array.isArray(rawChats) ? rawChats.map((c: any) => chatService.normalizeChat(c)) : [];
    } catch (err) {
      console.error('[chatService] getChats failed:', err);
      return [];
    }
  },

  /**
   * Fetches message history for a specific chat.
   */
  getMessages: async (chatId: string, token: string): Promise<Message[]> => {
    try {
      const json = await chatService.request(`/chats/${chatId}/messages`, token);
      // Backend returns { success, message, data: { messages: [] } }
      const messages = json.data?.messages || json.data || [];
      console.log(`[chatService] API history response for ${chatId}:`, messages);
      return Array.isArray(messages) ? messages.map(m => chatService.normalizeMessage(m)) : [];
    } catch (err) {
      console.error(`[chatService] Failed to fetch messages for chat ${chatId}:`, err);
      return [];
    }
  },

  /**
   * Searches for users by username or name.
   */
  searchUsers: async (query: string, token: string): Promise<User[]> => {
    const json = await chatService.request(`/auth/users/search?query=${encodeURIComponent(query)}`, token);
    return json.data?.users || [];
  },

  /**
   * Fetches all users.
   */
  getUsers: async (token: string): Promise<User[]> => {
    const json = await chatService.request('/auth/users/search?query=', token);
    return json.data?.users || [];
  },

  /**
   * Creates a new private (1-on-1) chat.
   */
  createPrivateChat: async (targetUserId: string, token: string): Promise<{ chatId: string }> => {
    const json = await chatService.request('/chats', token, {
      method: 'POST',
      body: JSON.stringify({
        type: "PRIVATE",
        participantIds: [targetUserId]
      })
    });
    return json.data;
  }
};