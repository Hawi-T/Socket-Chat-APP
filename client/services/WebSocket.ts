import { Message, WebSocketPayload } from '../types';
import { BACKEND_WS_URL } from '../constants';

type MessageHandler = (message: any) => void;

class WebSocketService {
  private socket: WebSocket | null = null;
  private handlers: MessageHandler[] = [];
  private isConnected = false;
  private reconnectInterval = 5000;
  private token: string | null = null;

  connect(token: string): Promise<boolean> {
    this.token = token;
    return new Promise((resolve) => {
      this.initConnection(resolve);
    });
  }

  private initConnection(resolve?: (value: boolean) => void) {
    if (this.socket) {
        this.socket.close();
    }

    try {
      // Some backends prefer token in query, some in AUTH frame. We do both for maximum compatibility.
      const wsUrl = `${BACKEND_WS_URL}${BACKEND_WS_URL.includes('?') ? '&' : '?'}token=${encodeURIComponent(this.token || '')}`;
      console.log('[WS] Connecting to:', BACKEND_WS_URL);
      
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        console.log('[WS] Connected successfully');
        this.isConnected = true;
        if (resolve) resolve(true);

        // Aligned with DocumentationModal specification (Flat structure)
        this.sendPayload({
            type: 'AUTH',
            token: this.token
        });
      };

      this.socket.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[WS] Raw incoming:', data);
          this.handleServerMessage(data);
        } catch (e: any) {
          console.error('[WS] Error parsing message:', e.message || e);
        }
      };

      this.socket.onclose = (event) => {
        if (this.isConnected) {
            console.log(`[WS] Connection closed (code: ${event.code}). Reconnecting...`);
        }
        this.isConnected = false;
        setTimeout(() => this.initConnection(), this.reconnectInterval);
      };

      this.socket.onerror = (err) => {
        console.error('[WS] Connection Error:', err);
        this.isConnected = false;
      };
    } catch (err: any) {
      console.error('[WS] Setup error:', err.message || err);
    }
  }

  disconnect() {
    if (this.socket) {
      this.socket.onclose = null; 
      this.socket.close();
    }
    this.isConnected = false;
  }

  // src/services/WebSocket.ts

sendMessage(chatId: string, text: string) {
    if (!this.isConnected) return;

    const payload = {
        // Most Java handlers use "MESSAGE" or "SEND_CHAT_MESSAGE"
        // Try "MESSAGE" first as it's the standard
        type: 'MESSAGE', 
        payload: {
            chatId: chatId,
            text: text, // Your Java code uses .put("text", msg.getContent())
            timestamp: Date.now()
        }
    };
    
    this.sendPayload(payload);
}
  private sendPayload(payload: any) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify(payload));
    }
  }

  private handleServerMessage(data: any) {
    // Check for explicit type or inferred message structure
    const type = (data.type || '').toUpperCase();
    const payload = data.payload || data;

    if (type === 'NEW_MESSAGE' || type === 'MESSAGE' || type === 'CHAT_MESSAGE' || payload.chatId || payload.chat_id) {
        this.notify(payload);
    }
  }

  onMessage(handler: MessageHandler) {
    this.handlers.push(handler);
    return () => {
      this.handlers = this.handlers.filter(h => h !== handler);
    };
  }

  private notify(message: any) {
    this.handlers.forEach(h => h(message));
  }
}

export const wsService = new WebSocketService();