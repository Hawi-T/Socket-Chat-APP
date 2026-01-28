/*
 * Copyright 2025 Samuel Girma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useState, useEffect, useRef } from 'react';
import { Menu, Search, Send, Paperclip, MoreVertical, Smile, Mic, ArrowLeft, X, Users, MessageSquare, Plus, LogOut, Lock, User as UserIcon, Check, Loader2, Clock } from 'lucide-react';
import { Chat, Message, User, AuthState } from './types';
import { wsService } from './services/WebSocket'; 
import { authService } from './services/authService';
import { chatService } from './services/chatService';
import myFavicon from './assets/favicon_io/favicon-32x32.png';
// --- SHARED COMPONENTS ---

const formatTime = (timestamp: number) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const Avatar: React.FC<{ url?: string | null; name: string; size?: 'sm' | 'md' | 'lg' | 'xl' }> = ({ url, name, size = 'md' }) => {
  const sizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-12 h-12 text-sm',
    lg: 'w-16 h-16 text-base',
    xl: 'w-24 h-24 text-xl'
  };
  
  const displayName = name || '??';
  const initials = displayName.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
  
  return (
    <div className={`${sizeClasses[size]} rounded-full bg-gradient-to-tr from-blue-400 to-blue-600 flex-shrink-0 flex items-center justify-center text-white font-bold overflow-hidden select-none shadow-md`}>
      {url ? <img src={url} alt={displayName} className="w-full h-full object-cover" /> : initials}
    </div>
  );
};

// --- AUTH COMPONENT ---

const AuthScreen: React.FC<{ onAuthSuccess: (user: User, token: string) => void }> = ({ onAuthSuccess }) => {
  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isLogin) {
        const data = await authService.login(username, password);
        onAuthSuccess(data.user, data.token);
      } else {
        const data = await authService.register(fullName, username, password);
        onAuthSuccess(data.user, data.token);
      }
    } catch (err: any) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-[#0e1621] telegram-bg-pattern">
      <div className="bg-[#1c242f] p-8 rounded-2xl shadow-2xl w-full max-w-md border border-[#2b3c4f]">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-white-500 rounded-full flex items-center justify-center mx-auto mb-4 shadow-lg">
             <img 
                    src={myFavicon} 
                    alt="Lock Icon" 
                    style={{ width: '32px', height: '32px' }} 
                    className="text-white" // className might not work for color if it's a PNG/ICO
              />
          </div>
          <h1 className="text-2xl font-bold text-white mb-2">Negarit</h1>
          <p className="text-gray-400 text-sm">{isLogin ? 'Sign in to start messaging' : 'Create a new account'}</p>
        </div>
        {error && (
          <div className="bg-red-500/20 border border-red-500/50 text-red-200 p-3 rounded-lg mb-6 text-sm text-center">
            {error}
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          {!isLogin && (
            <div className="bg-[#0e1621] flex items-center rounded-lg border border-[#2b3c4f] focus-within:border-blue-500 transition-colors">
              <span className="pl-4 text-gray-500"><UserIcon size={18}/></span>
              <input type="text" placeholder="Full Name" className="w-full bg-transparent p-3 text-white focus:outline-none" value={fullName} onChange={e => setFullName(e.target.value)} required={!isLogin} />
            </div>
          )}
          <div className="bg-[#0e1621] flex items-center rounded-lg border border-[#2b3c4f] focus-within:border-blue-500 transition-colors">
            <span className="pl-4 text-gray-500">@</span>
            <input type="text" placeholder="Username" className="w-full bg-transparent p-3 text-white focus:outline-none" value={username} onChange={e => setUsername(e.target.value)} required />
          </div>
          <div className="bg-[#0e1621] flex items-center rounded-lg border border-[#2b3c4f] focus-within:border-blue-500 transition-colors">
            <span className="pl-4 text-gray-500"><Lock size={18}/></span>
            <input type="password" placeholder="Password" className="w-full bg-transparent p-3 text-white focus:outline-none" value={password} onChange={e => setPassword(e.target.value)} required />
          </div>
          <button type="submit" disabled={loading} className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold py-3 rounded-lg transition-all transform active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg">
            {loading ? 'Processing...' : (isLogin ? 'Log In' : 'Sign Up')}
          </button>
        </form>
        <div className="mt-6 text-center">
          <p className="text-gray-400 text-sm">
            {isLogin ? "Don't have an account? " : "Already have an account? "}
            <button onClick={() => { setIsLogin(!isLogin); setError(''); }} className="text-blue-400 hover:underline font-medium">
              {isLogin ? 'Register' : 'Log In'}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

// --- MODAL COMPONENT ---

const Modal: React.FC<{ title: string; onClose: () => void; children: React.ReactNode }> = ({ title, onClose, children }) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
    <div className="bg-[#1c242f] w-full max-w-md rounded-xl shadow-2xl flex flex-col border border-[#2b3c4f] animate-in fade-in zoom-in duration-200">
      <div className="p-4 border-b border-[#0e1621] flex justify-between items-center bg-[#17212b] rounded-t-xl">
        <h2 className="text-lg font-bold text-white">{title}</h2>
        <button onClick={onClose} className="text-gray-400 hover:text-white transition-colors"><X size={20} /></button>
      </div>
      <div className="p-4 max-h-[70vh] overflow-y-auto custom-scrollbar">{children}</div>
    </div>
  </div>
);

// --- MAIN APPLICATION ---

export default function App() {
  const [authState, setAuthState] = useState<AuthState>({
    isAuthenticated: false,
    currentUser: null,
    token: null
  });

  const [chats, setChats] = useState<Chat[]>([]);
  const [availableUsers, setAvailableUsers] = useState<User[]>([]);
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [activeChatId, setActiveChatId] = useState<string | null>(null);
  const [messages, setMessages] = useState<Record<string, Message[]>>({});
  const [inputText, setInputText] = useState('');
  const [isDataLoading, setIsDataLoading] = useState(false);
  const [isMessagesLoading, setIsMessagesLoading] = useState(false);
  
  const [menuOpen, setMenuOpen] = useState(false);
  const [activeModal, setActiveModal] = useState<'none' | 'newChat' | 'newGroup'>('none');
  const [searchUserQuery, setSearchUserQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const activeChatIdRef = useRef<string | null>(null);
  const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  
  const currentUser = authState.currentUser;

  useEffect(() => {
    activeChatIdRef.current = activeChatId;
  }, [activeChatId]);

  useEffect(() => {
    if (!authState.token || activeModal !== 'newChat') return;
    if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);
    if (!searchUserQuery.trim()) {
        setSearchResults([]);
        return;
    }
    setIsSearching(true);
    searchTimeoutRef.current = setTimeout(async () => {
        try {
            const results = await chatService.searchUsers(searchUserQuery, authState.token!);
            setSearchResults(results.filter(u => u.id !== currentUser?.id));
        } catch (err) {
            console.error('Search failed:', err);
        } finally {
            setIsSearching(false);
        }
    }, 400);
    return () => { if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current); };
  }, [searchUserQuery, authState.token, activeModal, currentUser?.id]);

  const refreshData = async () => {
    if (!authState.isAuthenticated || !authState.token) return;
    setIsDataLoading(true);
    try {
      const [userChats, users] = await Promise.all([
        chatService.getChats(authState.token!),
        chatService.getUsers(authState.token!)
      ]);

      console.log('[App] RefreshData - Chats received:', userChats.length);
      setChats(userChats);
      setAvailableUsers(users);

      const initialMsgs: Record<string, Message[]> = {};
      userChats.forEach(chat => {
        if (!messages[chat.id]) {
            initialMsgs[chat.id] = chat.lastMessage ? [chat.lastMessage] : [];
        }
      });
      setMessages(prev => ({ ...prev, ...initialMsgs }));
    } catch (err) {
      console.error('Failed to fetch data:', err);
    } finally {
      setIsDataLoading(false);
    }
  };

  useEffect(() => {
    if (authState.isAuthenticated) {
        refreshData();
        wsService.connect(authState.token!).then(() => console.log('[App] WebSocket Handshake Initiated'));
    }
    
    const unsubscribe = wsService.onMessage((rawMsg) => {
      console.log('[App] Received WebSocket message:', rawMsg);
      const msg = chatService.normalizeMessage(rawMsg);
      
      setMessages(prev => {
        const existing = Array.isArray(prev[msg.chatId]) ? prev[msg.chatId] : [];
        
        // Remove the optimistic pending message if it exists (check by content and sender if ID varies)
        const filtered = existing.filter(m => {
            const isOptimistic = m.id.startsWith('temp-');
            const isSameContent = m.text === msg.text;
            const isSameSender = m.senderId === msg.senderId;
            return !(isOptimistic && isSameContent && isSameSender);
        });

        // Prevent exact ID duplicates
        if (filtered.some(m => m.id === msg.id)) return prev;
        
        return { 
          ...prev, 
          [msg.chatId]: [...filtered, msg] 
        };
      });

      setChats(prevChats => {
        const chatExists = prevChats.some(c => c.id === msg.chatId);
        if (!chatExists) {
           refreshData();
           return prevChats;
        }
        return prevChats.map(c => {
          if (c.id === msg.chatId) {
            const isMe = currentUser && msg.senderId === currentUser.id;
            const isChatActive = activeChatIdRef.current === msg.chatId;
            return { 
                ...c, 
                lastMessage: msg, 
                unreadCount: isChatActive ? 0 : (!isMe ? (c.unreadCount || 0) + 1 : (c.unreadCount || 0))
            };
          }
          return c;
        });
      });
    });

    return () => {
        unsubscribe();
        wsService.disconnect();
    }
  }, [authState.isAuthenticated, authState.token, currentUser]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, activeChatId]);

  const selectChat = async (chatId: string) => {
    setActiveChatId(chatId);
    setChats(prev => prev.map(c => c.id === chatId ? { ...c, unreadCount: 0 } : c));
    
    if (authState.token) {
      setIsMessagesLoading(true);
      try {
        const history = await chatService.getMessages(chatId, authState.token);
        setMessages(prev => ({ ...prev, [chatId]: history }));
      } catch (err) {
        console.error('Failed to load message history:', err);
      } finally {
        setIsMessagesLoading(false);
      }
    }
  };

  const handleLogin = (user: User, token: string) => {
    setAuthState({ isAuthenticated: true, currentUser: user, token: token });
  };

  const handleLogout = () => {
    wsService.disconnect();
    setAuthState({ isAuthenticated: false, currentUser: null, token: null });
    setActiveChatId(null);
    setChats([]);
    setMessages({});
  };

  const handleSendMessage = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!inputText.trim() || !activeChatId || !currentUser) return;
    
    const text = inputText.trim();

    // OPTIMISTIC UI: Add message locally first
    const tempId = 'temp-' + Date.now();
    const localMsg: Message = {
        id: tempId,
        chatId: activeChatId,
        senderId: currentUser.id,
        text: text,
        timestamp: Date.now(),
        status: 'PENDING',
        type: 'text'
    };

    setMessages(prev => {
        const existing = prev[activeChatId] || [];
        return { ...prev, [activeChatId]: [...existing, localMsg] };
    });

    // Actually send through socket
    wsService.sendMessage(activeChatId, text);
    setInputText('');
  };

  const handleCreatePrivateChat = async (user: User) => {
    if (!currentUser || !authState.token) return;
    try {
        const existingChat = chats.find(c => c.type === 'private' && c.participants.some(p => p.id === user.id));
        if (existingChat) {
            selectChat(existingChat.id);
            closeModals();
        } else {
            const data = await chatService.createPrivateChat(user.id, authState.token);
            await refreshData();
            selectChat(data.chatId);
            closeModals();
        }
    } catch (err) {
        console.error('Failed to start chat:', err);
    }
  };

  const closeModals = () => {
      setActiveModal('none');
      setSearchUserQuery('');
      setMenuOpen(false);
  };

  if (!authState.isAuthenticated || !currentUser) {
    return <AuthScreen onAuthSuccess={handleLogin} />;
  }

  const activeChat = chats.find(c => c.id === activeChatId);
  const currentMessages = activeChatId && Array.isArray(messages[activeChatId]) ? messages[activeChatId] : [];
  const displayUsers = searchUserQuery.trim() ? searchResults : availableUsers.filter(u => u.id !== currentUser.id);
  const recienverUsername = activeChat 
    ? (activeChat.name.split(' & ')[0] === currentUser.username 
        ? activeChat.name.split(' & ')[1] 
        : activeChat.name.split(' & ')[0]) 
    : '';
  return (
    <div className="flex h-screen bg-[#0e1621] overflow-hidden text-gray-100 font-sans">
      
      {/* SIDEBAR */}
      <div className={`flex-col w-full md:w-[400px] border-r border-[#0e1621] bg-[#17212b] ${activeChatId ? 'hidden md:flex' : 'flex'}`}>
        <div className="p-3 flex items-center gap-4 relative z-20">
          <div className="relative">
             <button className={`text-gray-400 hover:text-white p-2 rounded-full transition-colors ${menuOpen ? 'bg-[#2b5278] text-white' : ''}`} onClick={() => setMenuOpen(!menuOpen)}>
                <Menu size={24} />
             </button>
             {menuOpen && (
                 <div className="absolute top-12 left-0 w-64 bg-[#1c242f] rounded-lg shadow-xl border border-[#0e1621] overflow-hidden py-1 z-50 animate-in fade-in slide-in-from-top-2 duration-150">
                     <div className="px-4 py-3 border-b border-[#0e1621] flex items-center gap-3">
                        <Avatar name={currentUser.name} url={currentUser.avatarUrl} size="sm" />
                        <div className="overflow-hidden"><div className="font-bold text-sm truncate">{currentUser.name}</div><div className="text-xs text-green-400">Online</div></div>
                     </div>
                     <button onClick={() => { setActiveModal('newChat'); setMenuOpen(false); }} className="w-full text-left px-4 py-3 hover:bg-[#2b3c4f] flex items-center gap-3 transition-colors text-sm">
                         <MessageSquare size={18} className="text-blue-400" /><span>New Private Chat</span>
                     </button>
                     <div className="border-t border-[#0e1621] mt-1">
                        <button onClick={handleLogout} className="w-full text-left px-4 py-3 hover:bg-[#2b3c4f] flex items-center gap-3 transition-colors text-red-400 text-sm">
                            <LogOut size={18} /><span>Log Out</span>
                        </button>
                     </div>
                 </div>
             )}
          </div>
          <div className="relative flex-1">
            <input type="text" placeholder="Search chats..." className="w-full bg-[#242f3d] text-gray-200 rounded-full py-2 pl-10 pr-4 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm border border-transparent focus:border-blue-500 transition-all placeholder-gray-500" />
            <Search size={18} className="absolute left-3 top-2.5 text-gray-500" />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto custom-scrollbar">
          {isDataLoading && (
            <div className="flex items-center justify-center p-8"><Loader2 className="animate-spin text-blue-500" /></div>
          )}
          
          {!isDataLoading && chats.map(chat => (
            <div key={chat.id} onClick={() => selectChat(chat.id)} className={`flex items-center gap-3 p-3 cursor-pointer transition-colors border-b border-black/10 ${activeChatId === chat.id ? 'bg-[#2b5278]' : 'hover:bg-[#202b36]'}`}>
              <Avatar name={chat.name.split(" & ")[0] == currentUser.name ? chat.name.split(" & ")[1] : chat.name.split(" & ")[0]} url={chat.avatarUrl} />
              <div className="flex-1 min-w-0">
                <div className="flex justify-between items-baseline">
                  <h3 className="font-semibold truncate text-[14px]">{chat.name.split(" & ")[0] == currentUser.name ? chat.name.split(" & ")[1] : chat.name.split(" & ")[0]}</h3>
                  {chat.lastMessage && (
                    <span className={`text-[11px] ${activeChatId === chat.id ? 'text-blue-200' : 'text-gray-500'}`}>
                      {formatTime(chat.lastMessage.timestamp)}
                    </span>
                  )}
                </div>
                <div className="flex justify-between items-center mt-0.5">
                  <p className={`text-[13px] truncate pr-2 ${activeChatId === chat.id ? 'text-blue-100' : 'text-gray-400'}`}>
                    {currentUser && chat.lastMessage?.senderId === currentUser.id && <span className="text-blue-400 mr-1">You:</span>}
                    {chat.lastMessage?.text || <span className="italic opacity-50 text-xs">No messages yet</span>}
                  </p>
                  {(chat.unreadCount || 0) > 0 && (
                    <span className="bg-blue-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full min-w-[18px] text-center shadow-sm">
                      {chat.unreadCount}
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
          {!isDataLoading && chats.length === 0 && <div className="p-8 text-center text-gray-500 text-sm">No active chats. Start a new conversation!</div>}
        </div>
      </div>

      {/* MAIN CHAT AREA */}

      <div className={`flex-col flex-1 relative bg-[#0e1621] telegram-bg-pattern ${!activeChatId ? 'hidden md:flex' : 'flex'}`}>
        {activeChatId && activeChat ? (
          <>
            <div className="h-14 bg-[#17212b] flex items-center justify-between px-4 shadow-sm z-10 shrink-0 border-b border-black/20">
              <div className="flex items-center gap-4">
                <button className="md:hidden text-gray-300 hover:text-white" onClick={() => setActiveChatId(null)}><ArrowLeft /></button>
                <div className="flex items-center gap-3">
                  <Avatar name={recienverUsername} url={activeChat.avatarUrl} size="sm" />
                  <div className="flex flex-col justify-center">
                    <h2 className="font-semibold text-sm leading-tight">{recienverUsername}</h2>
                    <span className="text-xs text-gray-400">{(activeChat.participants?.length || 0) > 2 ? `${activeChat.participants.length} members` : 'online'}</span>
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-4 text-gray-400">
                <Search size={20} className="hover:text-white cursor-pointer" />
                <MoreVertical size={20} className="hover:text-white cursor-pointer" />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-2 custom-scrollbar relative">
              {isMessagesLoading && (
                 <div className="absolute inset-x-0 top-0 flex justify-center pt-4 z-20">
                    <div className="bg-[#182533]/80 backdrop-blur px-3 py-1 rounded-full border border-[#2b3c4f] flex items-center gap-2">
                        <Loader2 size={14} className="animate-spin text-blue-400" />
                        <span className="text-xs text-gray-300">Syncing messages...</span>
                    </div>
                 </div>
              )}
              
              {currentMessages.length === 0 && !isMessagesLoading && (
                  <div className="h-full flex flex-col items-center justify-center text-gray-500 text-sm">
                      <div className="bg-[#182533] px-4 py-2 rounded-lg mb-2">No messages yet. Say hi!</div>
                  </div>
              )}

              {currentMessages.map((msg, idx) => {
                const isMe = currentUser && msg.senderId === currentUser.id;
                const showAvatar = !isMe && (idx === 0 || currentMessages[idx - 1].senderId !== msg.senderId);
                const sender = activeChat.participants?.find(p => p.id === msg.senderId) || 
                               availableUsers.find(u => u.id === msg.senderId) || 
                               { name: 'Unknown', avatarUrl: null };

                return (
                  <div key={msg.id || idx} className={`flex w-full ${isMe ? 'justify-end' : 'justify-start'} mb-1 animate-in fade-in slide-in-from-bottom-1 duration-200`}>
                     {!isMe && (
                        <div className={`w-8 mr-2 flex flex-col justify-end ${showAvatar ? 'opacity-100' : 'opacity-0'}`}>
                             <Avatar name={sender.name || '?'} url={sender.avatarUrl} size="sm" />
                        </div>
                     )}
                    <div className={`max-w-[75%] md:max-w-[60%] rounded-2xl px-3 py-1.5 relative shadow-sm text-[15px] ${isMe ? 'bg-[#2b5278] text-white rounded-tr-none' : 'bg-[#182533] text-gray-100 rounded-tl-none'}`}>
                      {!isMe && showAvatar && activeChat.type === 'group' && (
                         <div className="text-xs font-semibold text-blue-400 mb-0.5">{sender.name}</div>
                      )}
                      <p className="whitespace-pre-wrap leading-snug">{msg.text}</p>
                      <div className={`text-[11px] mt-1 float-right flex items-center gap-1 ${isMe ? 'text-blue-200' : 'text-gray-400'}`}>
                        <span>{formatTime(msg.timestamp)}</span>
                        {isMe && (
                          <span>
                            {msg.status === 'PENDING' ? (
                              <Clock size={10} className="inline opacity-60" />
                            ) : msg.status?.toLowerCase() === 'read' ? '✓✓' : '✓'}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>

            <div className="bg-[#17212b] px-2 py-2 md:px-4 md:py-3 flex items-end gap-2 shrink-0">
              <button className="p-2 text-gray-400 hover:text-gray-200 transition-colors"><Paperclip size={24} /></button>
              <div className="flex-1 bg-[#0e1621] rounded-2xl flex items-center min-h-[44px] border border-transparent focus-within:border-blue-500/50 transition-colors">
                <form className="w-full flex" onSubmit={handleSendMessage}>
                    <input type="text" value={inputText} onChange={(e) => setInputText(e.target.value)} placeholder="Write a message..." className="flex-1 bg-transparent px-4 py-2 text-white focus:outline-none placeholder-gray-500" />
                </form>
                <button className="p-2 text-gray-400 hover:text-gray-200 mr-1"><Smile size={24} /></button>
              </div>
              {inputText.trim() ? (
                <button onClick={() => handleSendMessage()} className="p-3 bg-blue-500 rounded-full text-white hover:bg-blue-600 transition-all transform hover:scale-105 shadow-lg flex items-center justify-center w-12 h-12"><Send size={20} className="ml-0.5" /></button>
              ) : (
                <button className="p-3 bg-[#242f3d] rounded-full text-white hover:bg-[#2b394a] transition-colors w-12 h-12 flex items-center justify-center"><Mic size={24} /></button>
              )}
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-500 p-8">
            <div className="bg-[#17212b] p-4 rounded-full mb-4"><span className="text-4xl">👋</span></div>
            <h3 className="text-lg font-medium text-gray-300 mb-2">Welcome to TeleClone</h3>
            <p className="text-sm text-center max-w-xs text-gray-400">Select a chat from the sidebar or create a new one to start messaging.</p>
          </div>
        )}
      </div>

      {/* MODALS */}
      {activeModal === 'newChat' && (
        <Modal title="New Private Chat" onClose={closeModals}>
          <div className="mb-4">
            <div className="relative">
                <div className="absolute left-3 top-3">
                    {isSearching ? <Loader2 size={16} className="text-blue-500 animate-spin" /> : <Search size={16} className="text-gray-500" />}
                </div>
                <input type="text" placeholder="Search by @username or name..." value={searchUserQuery} onChange={e => setSearchUserQuery(e.target.value)} className="w-full bg-[#0e1621] border border-[#2b3c4f] rounded-lg py-2 pl-9 pr-3 text-sm focus:outline-none focus:border-blue-500 text-white" autoFocus />
            </div>
          </div>
          <div className="space-y-1">
            {displayUsers.map(user => (
                <button key={user.id} onClick={() => handleCreatePrivateChat(user)} className="w-full flex items-center gap-3 p-2 hover:bg-[#2b3c4f] rounded-lg transition-colors text-left group">
                    <Avatar name={user.name} url={user.avatarUrl} size="md" />
                    <div className="flex-1">
                        <div className="font-medium text-white group-hover:text-blue-400 transition-colors">{user.name}</div>
                        <div className="text-xs text-gray-400">@{user.username} • {user.isOnline ? 'Online' : 'Offline'}</div>
                    </div>
                    <MessageSquare size={16} className="text-gray-600 group-hover:text-blue-400 transition-colors" />
                </button>
            ))}
          </div>
        </Modal>
      )}
    </div>
  );
}
