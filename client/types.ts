export interface User {
  id: string;
  name: string;
  username: string;
  avatarUrl: string | null;
  isOnline: boolean;
  lastSeen?: string;
}

export interface Message {
  id: string;
  chatId: string;
  senderId: string;
  text: string;
  timestamp: number;
  status: 'PENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'pending' | 'sent' | 'delivered' | 'read';
  type: 'text' | 'image' | 'system';
}

export interface Chat {
  id: string;
  type: 'private' | 'group' | 'PRIVATE' | 'GROUP';
  name: string;
  avatarUrl: string | null;
  participants: User[];
  lastMessage?: Message;
  unreadCount: number;
}

export interface AuthState {
  isAuthenticated: boolean;
  currentUser: User | null;
  token: string | null;
}

export interface WebSocketPayload {
  type: 'AUTH' | 'MESSAGE' | 'TYPING' | 'READ_RECEIPT' | 'SEND_MESSAGE' | 'NEW_MESSAGE';
  payload: any;
}