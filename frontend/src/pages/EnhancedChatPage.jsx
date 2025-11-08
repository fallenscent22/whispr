import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { CreateRoomModal } from '../components/chat/CreateRoomModal';
import { RoomList } from '../components/chat/RoomList';
import { FileUpload } from '../components/chat/FileUpload';
import WebSocketService from '../services/websocket';
import { chatRoomAPI, fileAPI } from '../services/api';
import { NotificationBell } from '../components/notifications/NotificationBell';
import { TypingIndicator } from '../components/chat/TypingIndicator';
import { MessageStatus } from '../components/chat/MessageStatus';
import { UserPresence } from '../components/chat/UserPresence';

export const EnhancedChatPage = () => {
  const { user, logout } = useAuth();
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [onlineUsers, setOnlineUsers] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [currentRoom, setCurrentRoom] = useState(null);
  const [showCreateRoom, setShowCreateRoom] = useState(false);
  const [roomMembers, setRoomMembers] = useState([]);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const messagesEndRef = useRef(null);
  const [notifications, setNotifications] = useState([]);

  // Added states
  const [typingUsers, setTypingUsers] = useState([]);
  const [userPresence, setUserPresence] = useState({});
  const [typingTimeout, setTypingTimeout] = useState(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    loadUserRooms();
    setupWebSocket();
  }, [user]);

  // Updated WebSocket setup
  const setupWebSocket = () => {
    WebSocketService.connect(
      handleNewMessage,
      handleUserEvent,
      handleTypingEvent,
      handleError,
      handleOnlineUsersUpdate,
      handleNotificationReceived,
      handleReadReceipt,
      handlePresenceUpdate
    );

    if (user) {
      setTimeout(() => {
        WebSocketService.addUser(user.username);
      }, 500);
    }

    return () => {
      WebSocketService.disconnect();
    };
  };

  const loadUserRooms = async () => {
    setLoadingRooms(true);
    try {
      const roomsData = await chatRoomAPI.getMyRooms();
      setRooms(roomsData.content || roomsData);
    } catch (error) {
      console.error('Failed to load rooms:', error);
    } finally {
      setLoadingRooms(false);
    }
  };

  const handleNewMessage = (message) => {
    if (!currentRoom || message.roomId === currentRoom.roomId || message.roomId === 'global') {
      setMessages(prev => [...prev, message]);
    }
  };

  const handleUserEvent = (userEvent) => {
    setMessages(prev => [...prev, userEvent]);
  };

  // Added handlers
  const handleTypingEvent = (typingEvent) => {
    if (typingEvent.typing) {
      setTypingUsers(prev => [...prev.filter(u => u !== typingEvent.username), typingEvent.username]);
    } else {
      setTypingUsers(prev => prev.filter(u => u !== typingEvent.username));
    }
  };

  const handleReadReceipt = (readReceipt) => {
    setMessages(prev => prev.map(msg =>
      msg.id === readReceipt.messageId
        ? { ...msg, isRead: true, readBy: [...(msg.readBy || []), readReceipt.username] }
        : msg
    ));
  };

  const handlePresenceUpdate = (presenceUpdate) => {
    setUserPresence(prev => ({
      ...prev,
      [presenceUpdate.username]: {
        online: presenceUpdate.online,
        lastSeen: presenceUpdate.lastSeen
      }
    }));
  };

  const handleTyping = (e) => {
    setNewMessage(e.target.value);

    if (currentRoom && e.target.value.trim()) {
      WebSocketService.startTyping(currentRoom.roomId);

      if (typingTimeout) {
        clearTimeout(typingTimeout);
      }

      const timeout = setTimeout(() => {
        WebSocketService.stopTyping(currentRoom.roomId);
      }, 1000);

      setTypingTimeout(timeout);
    } else if (currentRoom) {
      WebSocketService.stopTyping(currentRoom.roomId);
    }
  };

  const handleMessageVisible = (messageId) => {
    if (currentRoom) {
      WebSocketService.markMessageAsRead(messageId, currentRoom.roomId);
    }
  };

  const handleError = (error) => {
    console.error('WebSocket error:', error);
  };

  const handleOnlineUsersUpdate = (onlineUsersList) => {
    setOnlineUsers(onlineUsersList);
  };

  const handleCreateRoom = async (roomData) => {
    try {
      const response = await chatRoomAPI.createRoom(roomData);
      await loadUserRooms();
      return response;
    } catch (error) {
      console.error('Failed to create room:', error);
      throw error;
    }
  };

  const handleRoomSelect = async (room) => {
    setCurrentRoom(room);
    setMessages([]);

    try {
      if (room.roomId !== 'global') {
      const members = await chatRoomAPI.getRoomMembers(room.roomId);
      setRoomMembers(members);
      }

      const recentMessages = await WebSocketService.getRecentMessages(room.roomId);
      setMessages(recentMessages.reverse());
    } catch (error) {
      console.error('Failed to load room data:', error);
    }
  };

  const handleJoinRoom = async (roomId) => {
    try {
      await chatRoomAPI.joinRoom(roomId);
      await loadUserRooms();

      const roomToJoin = rooms.find(room => room.roomId === roomId);
      if (roomToJoin) {
        handleRoomSelect(roomToJoin);
      }
    } catch (error) {
      console.error('Failed to join room:', error);
      alert('Failed to join room: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleFileUpload = async (file) => {
    try {
      const fileType = file.type.startsWith('image/')
        ? 'image'
        : file.type.startsWith('application/pdf')
          ? 'document'
          : 'document';

      const uploadResponse = await fileAPI.uploadFile(file, fileType);

      const message = {
        type: 'CHAT',
        content: `File uploaded: ${uploadResponse.fileName}`,
        sender: user.username,
        timestamp: new Date(),
        roomId: currentRoom?.roomId || 'global',
        fileUrl: uploadResponse.fileUrl,
        fileName: uploadResponse.fileName
      };

      WebSocketService.sendMessage(message);
    } catch (error) {
      console.error('File upload failed:', error);
      throw error;
    }
  };

  const handleSendMessage = (e) => {
    e.preventDefault();
    if (newMessage.trim()) {
      const message = {
        type: 'CHAT',
        content: newMessage,
        sender: user.username,
        timestamp: new Date(),
        roomId: currentRoom?.roomId || 'global'
      };
      WebSocketService.sendMessage(message);
      setNewMessage('');
      WebSocketService.typing(user.username, false);
    }
  };

  const handleNotificationReceived = (notification) => {
    setNotifications(prev => [notification, ...prev]);
    console.log('New notification:', notification);
  };

  const formatTime = (timestamp) => {
    if (typeof timestamp === 'string') {
      return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  // Updated renderMessage with MessageStatus
  const renderMessage = (message, index) => {
    const isSystemMessage = message.type === 'JOIN' || message.type === 'LEAVE';
    const isMyMessage = message.sender === user?.username;

    return (
      <div key={index} className={`flex ${isMyMessage ? 'justify-end' : 'justify-start'}`}>
        <div className={`max-w-xs lg:max-w-md rounded-lg p-3 ${isSystemMessage
          ? 'bg-yellow-100 text-yellow-800 mx-auto text-center'
          : isMyMessage
            ? 'bg-blue-500 text-white'
            : 'bg-white text-gray-800 shadow'
          }`}>
          {isSystemMessage ? (
            <p className="text-sm italic">{message.content}</p>
          ) : (
            <>
              {!isMyMessage && (
                <p className="font-medium text-sm">
                  {message.sender === user?.username ? 'You' : message.sender}
                </p>
              )}
              <p className="mt-1">{message.content}</p>

              {isMyMessage && (
                <MessageStatus
                  message={message}
                  isOwnMessage={true}
                  showReadReceipts={currentRoom?.type === 'DIRECT'}
                />
              )}

              <p className="text-xs opacity-75 mt-1">
                {formatTime(message.timestamp)}
              </p>
            </>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gray-100 flex">
      {/* Sidebar */}
      <div className="w-80 bg-white shadow-lg flex flex-col">
        <div className="p-4 border-b flex justify-between items-center">
          <div>
            <h1 className="text-xl font-semibold">Whispr</h1>
            <p className="text-sm text-gray-600">Welcome, {user?.username}!</p>
          </div>
          <NotificationBell />
        </div>

        {/* Room Management */}
        <div className="p-4 border-b">
          <div className="flex justify-between items-center mb-3">
            <h2 className="font-medium text-gray-700">Chat Rooms</h2>
            <Button onClick={() => setShowCreateRoom(true)} className="w-auto px-3 py-1 text-sm">
              New Room
            </Button>
          </div>

          {loadingRooms ? (
            <div className="text-center py-4">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500 mx-auto"></div>
            </div>
          ) : (
            <RoomList
              rooms={rooms}
              currentRoom={currentRoom}
              onRoomSelect={handleRoomSelect}
              onJoinRoom={handleJoinRoom}
            />
          )}
        </div>

        {/* Online Users */}
        <div className="p-4 flex-1">
          <h2 className="font-medium text-gray-700 mb-2">
            Online Users ({onlineUsers.length})
          </h2>
          <div className="space-y-2">
            {onlineUsers.map((onlineUser, index) => (
              <div key={index} className="flex items-center justify-between">
                <UserPresence
                  user={{ username: onlineUser, lastSeen: userPresence[onlineUser]?.lastSeen }}
                  onlineUsers={onlineUsers}
                  showLastSeen={true}
                />
              </div>
            ))}
          </div>
        </div>

        <div className="p-4 border-t">
          <Button variant="secondary" onClick={logout} className="w-full">
            Logout
          </Button>
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col">
        {/* Chat Header */}
        <div className="bg-white shadow-sm p-4">
          <h2 className="text-lg font-semibold">
            {currentRoom ? currentRoom.name : 'Global Chat'}
          </h2>
          {currentRoom && (
            <p className="text-sm text-gray-600">{currentRoom.description}</p>
          )}
          {typingUsers.length > 0 && (
            <p className="text-sm text-gray-600 italic">
              {typingUsers.join(', ')} {typingUsers.length === 1 ? 'is' : 'are'} typing...
            </p>
          )}
        </div>

        {/* Messages Area with TypingIndicator */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.map(renderMessage)}
          <TypingIndicator typingUsers={typingUsers} currentUser={user} />
          <div ref={messagesEndRef} />
        </div>

        {/* File Upload and Message Input */}
        <div className="bg-white border-t p-4 space-y-4">
          <FileUpload onFileUpload={handleFileUpload} disabled={!currentRoom} />

          <form onSubmit={handleSendMessage} className="flex space-x-2">
            <Input
              type="text"
              value={newMessage}
              onChange={handleTyping}
              placeholder={`Type your message in ${currentRoom ? currentRoom.name : 'global chat'}...`}
              className="flex-1"
              disabled={!currentRoom}
            />
            <Button type="submit" className="w-24" disabled={!currentRoom}>
              Send
            </Button>
          </form>
        </div>
      </div>

      {/* Create Room Modal */}
      <CreateRoomModal
        isOpen={showCreateRoom}
        onClose={() => setShowCreateRoom(false)}
        onCreateRoom={handleCreateRoom}
      />
    </div>
  );
};
