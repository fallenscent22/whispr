import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { messageAPI } from './api'; 

class WebSocketService {
  constructor() {
    this.client = null;
    this.isConnected = false;
  }

  connect(onMessageReceived, onUserJoined, onUserTyping, onOnlineUsersUpdate, onError) {
    const socket = new SockJS('http://localhost:8080/ws');
    this.client = new Client({
      webSocketFactory: () => socket,
      debug: (str) => {
        console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = (frame) => {
      this.isConnected = true;
      console.log('Connected: ' + frame);
      
      this.client.subscribe('/topic/public', (message) => {
        const receivedMessage = JSON.parse(message.body);
        if (receivedMessage.type === 'JOIN' || receivedMessage.type === 'LEAVE') {
          onUserJoined(receivedMessage);
        } else {
          onMessageReceived(receivedMessage);
        }
      });

      this.client.subscribe('/topic/typing', (message) => {
        onUserTyping(JSON.parse(message.body));
      });

      this.client.subscribe('/topic/online.users', (message) => {
        const onlineUsers = JSON.parse(message.body);
        if (onOnlineUsersUpdate) {
          onOnlineUsersUpdate(onlineUsers);
        }
      });
    };

    this.client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
      onError(frame);
    };

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.isConnected = false;
      console.log("Disconnected");
    }
  }

  sendMessage(message) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
      });
    } else {
      console.error('WebSocket is not connected.');
    }
  }

  addUser(user) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.addUser',
        body: JSON.stringify({
          type: 'JOIN',
          sender: user,
          content: `${user} joined the chat`,
          timestamp: new Date()
        })
      });
    }
  }

  getRecentMessages(roomId = 'global') {
  return messageAPI.getRecentMessages(roomId);
  }

  getMessageHistory(roomId = 'global', page = 0, size = 20) {
    return messageAPI.getMessageHistory(roomId, page, size);
  }

  markAsRead(roomId, username) {
    if (this.isConnected) {
      this.client.publish({
      destination: '/app/chat.markRead',
      body: roomId
    });
  }
    messageAPI.markMessagesAsRead(roomId, username);
  }

  typing(user, isTyping) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify({
          username: user,
          typing: isTyping
        })
      });
    }
  }
}

export default new WebSocketService();