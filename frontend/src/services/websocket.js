import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { messageAPI } from './api';

class WebSocketService {
  constructor() {
    this.client = null;
    this.isConnected = false;
  }

  connect(
    onMessageReceived,
    onUserJoined,
    onUserTyping,
    onOnlineUsersUpdate,
    onError,
    onTypingEvent,
    onReadReceipt,
    onPresenceUpdate,
    roomId = 'global'
  ) {
    const socket = new SockJS('http://localhost:8080/ws');
    this.client = new Client({
      webSocketFactory: () => socket,
      debug: (str) => console.log(str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = (frame) => {
      this.isConnected = true;
      console.log('Connected: ' + frame);

      // Public chat
      this.client.subscribe('/topic/public', (message) => {
        const receivedMessage = JSON.parse(message.body);
        if (receivedMessage.type === 'JOIN' || receivedMessage.type === 'LEAVE') {
          onUserJoined(receivedMessage);
        } else {
          onMessageReceived(receivedMessage);
        }
      });

      // Typing indicator (global)
      this.client.subscribe('/topic/typing', (message) => {
        onUserTyping(JSON.parse(message.body));
      });

      // Online users
      this.client.subscribe('/topic/online.users', (message) => {
        const onlineUsers = JSON.parse(message.body);
        if (onOnlineUsersUpdate) {
          onOnlineUsersUpdate(onlineUsers);
        }
      });

      this.client.subscribe(`/topic/typing.${roomId}`, (message) => {
        if (onTypingEvent) onTypingEvent(JSON.parse(message.body));
      });

      this.client.subscribe(`/topic/read-receipt.${roomId}`, (message) => {
        if (onReadReceipt) onReadReceipt(JSON.parse(message.body));
      });

      this.client.subscribe('/topic/presence', (message) => {
        if (onPresenceUpdate) onPresenceUpdate(JSON.parse(message.body));
      });
    };

    this.client.onStompError = (frame) => {
      console.error('Broker error:', frame.headers['message']);
      console.error('Details:', frame.body);
      onError(frame);
    };

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.isConnected = false;
      console.log('Disconnected');
    }
  }

  sendMessage(message) {
    if (!this.isConnected) return console.error('WebSocket is not connected.');
    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(message),
    });
  }

  addUser(user) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.addUser',
        body: JSON.stringify({
          type: 'JOIN',
          sender: user,
          content: `${user} joined the chat`,
          timestamp: new Date(),
        }),
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
        body: roomId,
      });
    }
    messageAPI.markMessagesAsRead(roomId, username).catch(error => {
    console.warn('Failed to mark messages as read via API:', error);
  });
  }

  typing(user, isTyping) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify({ username: user, typing: isTyping }),
      });
    }
  }

  startTyping(roomId) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.typing.start',
        body: JSON.stringify({ roomId }),
      });
    }
  }

  stopTyping(roomId) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.typing.stop',
        body: JSON.stringify({ roomId }),
      });
    }
  }

  markMessageAsRead(messageId, roomId) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.message.read',
        body: JSON.stringify({ messageId, roomId }),
      });
    }
  }

  markMessageAsDelivered(messageId, roomId) {
    if (this.isConnected) {
      this.client.publish({
        destination: '/app/chat.message.delivered',
        body: JSON.stringify({ messageId, roomId }),
      });
    }
  }

  setupNotificationListener(onNotificationReceived) {
    if (this.isConnected) {
      this.client.subscribe('/user/queue/notifications', (message) => {
        const notification = JSON.parse(message.body);
        onNotificationReceived(notification);
        if (typeof this.onNotification === 'function') {
          this.onNotification(notification);
        }
      });
    }
  }
}

export default new WebSocketService();
