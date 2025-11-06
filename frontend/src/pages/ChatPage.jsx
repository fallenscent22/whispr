import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import WebSocketService from '../services/websocket';

export const ChatPage = () => {
  const { user, logout } = useAuth();
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [typingUsers, setTypingUsers] = useState([]);
  const [onlineUsers, setOnlineUsers] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [hasMoreHistory, setHasMoreHistory] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    const loadRecentMessages = async () => {
      try {
        const recentMessages = await WebSocketService.getRecentMessages('global');
        setMessages(recentMessages.reverse());
      } catch (error) {
        console.error('Failed to load recent messages:', error);
      }
    };

   
    WebSocketService.connect(
      (message) => {
        setMessages(prev => [...prev, message]);
      },
      
      (userEvent) => {
        setMessages(prev => [...prev, userEvent]);
      },

      (typingUser) => {
        if (typingUser.typing) {
          setTypingUsers(prev => [...prev.filter(u => u !== typingUser.username), typingUser.username]);
        } else {
          setTypingUsers(prev => prev.filter(u => u !== typingUser.username));
        }
      },
     
      (error) => {
        console.error('WebSocket error:', error);
      },
      // Handle online users updates
      (onlineUsersList) => {
        setOnlineUsers(onlineUsersList);
      }
    );

    loadRecentMessages();
    loadOnlineUsers();

   
    if (user) {
      setTimeout(() => {
        WebSocketService.addUser(user.username);
      }, 500);
    }

   
    const handleFocus = () => {
      if (user) {
        WebSocketService.markAsRead('global', user.username);
      }
    };

    window.addEventListener('focus', handleFocus);

    // Cleanup on component unmount
    return () => {
      window.removeEventListener('focus', handleFocus);
      WebSocketService.disconnect();
    };
  }, [user]);

  const loadOnlineUsers = async () => {
    try {
      const users = await WebSocketService.getOnlineUsers();
      setOnlineUsers(users);
    } catch (error) {
      console.error('Failed to load online users:', error);
    }
  };

  const loadMoreHistory = async () => {
    if (loadingHistory || !hasMoreHistory) return;

    setLoadingHistory(true);
    try {
      const nextPage = currentPage + 1;
      const history = await WebSocketService.getMessageHistory('global', nextPage, 20);
      
      if (history.content.length > 0) {
        setMessages(prev => [...history.content.reverse(), ...prev]);
        setCurrentPage(nextPage);
        setHasMoreHistory(!history.last);
      } else {
        setHasMoreHistory(false);
      }
    } catch (error) {
      console.error('Failed to load message history:', error);
    } finally {
      setLoadingHistory(false);
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
        roomId: 'global'
      };
      WebSocketService.sendMessage(message);
      setNewMessage('');
      // Stop typing indicator
      WebSocketService.typing(user.username, false);
    }
  };

  const handleTyping = (e) => {
    setNewMessage(e.target.value);
    if (e.target.value.trim()) {
      WebSocketService.typing(user.username, true);
    } else {
      WebSocketService.typing(user.username, false);
    }
  };

  const formatTime = (timestamp) => {
    if (typeof timestamp === 'string') {
      return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const handleScroll = (e) => {
    const { scrollTop } = e.target;
    if (scrollTop === 0 && hasMoreHistory) {
      loadMoreHistory();
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex">
      {/* Sidebar */}
      <div className="w-64 bg-white shadow-lg">
        <div className="p-4 border-b">
          <h1 className="text-xl font-semibold">Whispr</h1>
          <p className="text-sm text-gray-600">Welcome, {user?.username}!</p>
        </div>
        
        <div className="p-4">
          <h2 className="font-medium text-gray-700 mb-2">Online Users ({onlineUsers.length})</h2>
          <div className="space-y-2">
            {onlineUsers.map((onlineUser, index) => (
              <div key={index} className="flex items-center">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                <span className="text-sm">{onlineUser}</span>
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

      {/* Chat Area */}
      <div className="flex-1 flex flex-col">
        {/* Chat Header */}
        <div className="bg-white shadow-sm p-4">
          <h2 className="text-lg font-semibold">Global Chat</h2>
          {typingUsers.length > 0 && (
            <p className="text-sm text-gray-600 italic">
              {typingUsers.join(', ')} {typingUsers.length === 1 ? 'is' : 'are'} typing...
            </p>
          )}
        </div>

        {/* Messages Area */}
        <div 
          ref={messagesContainerRef}
          className="flex-1 overflow-y-auto p-4 space-y-4"
          onScroll={handleScroll}
        >
          {loadingHistory && (
            <div className="text-center text-gray-500">
              Loading older messages...
            </div>
          )}
          
          {messages.map((message, index) => (
            <div key={index} className={`flex ${message.sender === user?.username ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-xs lg:max-w-md rounded-lg p-3 ${
                message.type === 'JOIN' || message.type === 'LEAVE' 
                  ? 'bg-yellow-100 text-yellow-800 mx-auto text-center' 
                  : message.sender === user?.username 
                    ? 'bg-blue-500 text-white' 
                    : 'bg-white text-gray-800 shadow'
              }`}>
                {message.type === 'JOIN' || message.type === 'LEAVE' ? (
                  <p className="text-sm italic">{message.content}</p>
                ) : (
                  <>
                    <p className="font-medium text-sm">
                      {message.sender === user?.username ? 'You' : message.sender}
                    </p>
                    <p className="mt-1">{message.content}</p>
                    <p className="text-xs opacity-75 mt-1">
                      {formatTime(message.timestamp)}
                    </p>
                  </>
                )}
              </div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        {/* Message Input */}
        <div className="bg-white border-t p-4">
          <form onSubmit={handleSendMessage} className="flex space-x-2">
            <Input
              type="text"
              value={newMessage}
              onChange={handleTyping}
              placeholder="Type your message..."
              className="flex-1"
            />
            <Button type="submit" className="w-24">
              Send
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
};