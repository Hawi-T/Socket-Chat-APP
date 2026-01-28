import React from 'react';
import { X, Server, Code, Database, Globe } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export const DocumentationModal: React.FC<Props> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
      <div className="bg-[#1c242f] w-full max-w-4xl h-[90vh] rounded-xl shadow-2xl flex flex-col overflow-hidden border border-[#2b3c4f]">
        
        {/* Header */}
        <div className="p-6 border-b border-[#0e1621] flex justify-between items-center bg-[#17212b]">
          <div className="flex items-center gap-3">
            <div className="bg-blue-500 p-2 rounded-lg">
                <Server size={24} className="text-white" />
            </div>
            <div>
                <h2 className="text-xl font-bold text-white">Backend Integration Docs</h2>
                <p className="text-sm text-gray-400">Protocol Specification for Java Backend Team</p>
            </div>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-white transition-colors">
            <X size={24} />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-8 space-y-8 text-gray-300">
          
          <section>
            <h3 className="text-lg font-semibold text-white mb-3 flex items-center gap-2">
                <Globe size={18} className="text-blue-400"/> WebSocket Connection
            </h3>
            <div className="bg-[#0e1621] p-4 rounded-lg border border-[#2b3c4f]">
                <p className="mb-2">The frontend expects a WebSocket connection at:</p>
                <code className="block bg-black/30 p-2 rounded text-green-400 font-mono mb-4">
                    ws://api.yourdomain.com/ws/chat
                </code>
                <p className="text-sm mb-2">On connection, the client will send an authentication frame:</p>
                <pre className="bg-black/30 p-3 rounded text-sm font-mono text-blue-300">
{`{
  "type": "AUTH",
  "token": "JWT_TOKEN_HERE"
}`}
                </pre>
            </div>
          </section>

          <section>
            <h3 className="text-lg font-semibold text-white mb-3 flex items-center gap-2">
                <Code size={18} className="text-purple-400"/> Payload Protocols
            </h3>
            <div className="grid md:grid-cols-2 gap-4">
                <div className="bg-[#0e1621] p-4 rounded-lg border border-[#2b3c4f]">
                    <h4 className="font-medium text-white mb-2">Client -> Server</h4>
                    <pre className="bg-black/30 p-3 rounded text-xs font-mono text-gray-300 overflow-x-auto">
{`// Sending a message
{
  "type": "SEND_MESSAGE",
  "payload": {
    "chatId": "uuid-v4",
    "text": "Hello World",
    "tempId": "local-id"
  }
}

// Typing Indicator
{
  "type": "TYPING_START",
  "payload": { "chatId": "..." }
}`}
                    </pre>
                </div>
                <div className="bg-[#0e1621] p-4 rounded-lg border border-[#2b3c4f]">
                    <h4 className="font-medium text-white mb-2">Server -> Client</h4>
                    <pre className="bg-black/30 p-3 rounded text-xs font-mono text-gray-300 overflow-x-auto">
{`// Incoming Message
{
  "type": "NEW_MESSAGE",
  "payload": {
    "id": "msg-uuid",
    "chatId": "uuid-v4",
    "senderId": "user-uuid",
    "text": "Hello!",
    "timestamp": 16234234234
  }
}`}
                    </pre>
                </div>
            </div>
          </section>

          <section>
            <h3 className="text-lg font-semibold text-white mb-3 flex items-center gap-2">
                <Database size={18} className="text-yellow-400"/> Recommended Java Schema
            </h3>
            <div className="bg-[#0e1621] p-4 rounded-lg border border-[#2b3c4f] space-y-4">
                <div>
                    <span className="text-yellow-500 font-mono">User</span>
                    <span className="text-gray-500 text-sm ml-2">@Entity @Table(name="users")</span>
                    <p className="text-sm pl-4 text-gray-400">id (UUID), username (VARCHAR), password_hash (VARCHAR), avatar_url (VARCHAR)</p>
                </div>
                <div>
                    <span className="text-yellow-500 font-mono">Chat</span>
                    <span className="text-gray-500 text-sm ml-2">@Entity @Table(name="chats")</span>
                    <p className="text-sm pl-4 text-gray-400">id (UUID), type (ENUM: PRIVATE/GROUP), name (VARCHAR)</p>
                </div>
                <div>
                    <span className="text-yellow-500 font-mono">Message</span>
                    <span className="text-gray-500 text-sm ml-2">@Entity @Table(name="messages")</span>
                    <p className="text-sm pl-4 text-gray-400">id (UUID), chat_id (FK), sender_id (FK), content (TEXT), sent_at (TIMESTAMP)</p>
                </div>
            </div>
          </section>
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-[#0e1621] bg-[#17212b] text-right">
            <button 
                onClick={onClose}
                className="px-6 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded font-medium transition-colors"
            >
                Close Documentation
            </button>
        </div>

      </div>
    </div>
  );
};