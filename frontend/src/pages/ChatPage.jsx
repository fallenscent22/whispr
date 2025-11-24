import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import WebSocketService from '../services/websocket';

const debounce = (func, wait) => {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
};

const useMessagePagination = (roomId) => {
  const [messages, setMessages] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);

  const loadMoreMessages = async (page = 0) => {
    if (loading || !hasMore) return;

    setLoading(true);
    try {
      const history = await WebSocketService.getMessageHistory(roomId, page, 20);
      if (history.content && history.content.length > 0) {
        setMessages(prev => [...history.content.reverse(), ...prev]);
        setCurrentPage(page);
        setHasMore(!history.last);
      } else {
        setHasMore(false);
      }
    } catch (error) {
      console.error('Failed to load messages:', error);
    } finally {
      setLoading(false);
    }
  };

  const addNewMessage = (message) => {
    setMessages(prev => [...prev, message]);
  };

  const clearMessages = () => {
    setMessages([]);
    setCurrentPage(0);
    setHasMore(true);
  };

  return {
    messages,
    loadMoreMessages,
    hasMore,
    loading,
    currentPage,
    addNewMessage,
    clearMessages
  };
};

export const ChatPage = () => {
  const { user, logout } = useAuth();
  const [newMessage, setNewMessage] = useState('');
  const [typingUsers, setTypingUsers] = useState([]);
  const [onlineUsers, setOnlineUsers] = useState([]);
  const [connectionStatus, setConnectionStatus] = useState('connecting');
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const typingTimeoutRef = useRef(null);

  const {
    messages,
    loadMoreMessages,
    hasMore,
    loading: loadingHistory,
    addNewMessage,
    clearMessages
  } = useMessagePagination('global');

  const memoizedMessages = useMemo(() => messages, [messages]);
  const memoizedOnlineUsers = useMemo(() => onlineUsers, [onlineUsers]);

  const debouncedStopTyping = useCallback(
    debounce(() => {
      if (user) {
        WebSocketService.typing(user.username, false);
      }
    }, 1000),
    [user]
  );

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [memoizedMessages]);

  useEffect(() => {
    const setupWebSocket = async () => {
      try {
        console.log('WebSocket connecting to localhost:8080/ws');
        console.log('Auth token:', localStorage.getItem('token')?.substring(0, 20) + '...');

        WebSocketService.connect(
          (message) => {
            addNewMessage(message);
          },
          (userEvent) => {
            addNewMessage(userEvent);
          },
          (typingUser) => {
            if (typingUser.typing) {
              setTypingUsers(prev => [...prev.filter(u => u !== typingUser.username), typingUser.username]);
            } else {
              setTypingUsers(prev => prev.filter(u => u !== typingUser.username));
            }
          },
          (onlineUsersList) => {
            setOnlineUsers(onlineUsersList);
            setConnectionStatus('connected');
          },
          (error) => {
            console.error('WebSocket error:', error);
            setConnectionStatus('error');
          },
          (typingEvent) => {
            console.log('Typing event:', typingEvent);
          },
          (readReceipt) => {
            console.log('Read receipt:', readReceipt);
          },
          (presenceUpdate) => {
            console.log('Presence update:', presenceUpdate);
          }
        );

        await loadMoreMessages(0);

        if (user) {
          setTimeout(() => {
            WebSocketService.addUser(user.username);
          }, 500);
        }

      } catch (error) {
        console.error('Failed to setup WebSocket:', error);
        setConnectionStatus('error');
      }
    };

    setupWebSocket();

    const handleFocus = () => {
      if (user) {
        WebSocketService.markAsRead('global', user.username);
      }
    };

    const handleBeforeUnload = () => {
      WebSocketService.disconnect();
    };

    window.addEventListener('focus', handleFocus);
    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('focus', handleFocus);
      window.removeEventListener('beforeunload', handleBeforeUnload);
      WebSocketService.disconnect();
    };
  }, [user]);

  const handleSendMessage = (e) => {
    e.preventDefault();
    if (newMessage.trim() && user) {
      const message = {
        type: 'CHAT',
        content: newMessage,
        sender: user.username,
        timestamp: new Date(),
        roomId: 'global'
      };
      WebSocketService.sendMessage(message);
      setNewMessage('');

      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
      WebSocketService.typing(user.username, false);
    }
  };

  const handleTyping = (e) => {
    const value = e.target.value;
    setNewMessage(value);

    if (!user) return;

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    if (value.trim()) {
      WebSocketService.typing(user.username, true);
      typingTimeoutRef.current = setTimeout(() => {
        WebSocketService.typing(user.username, false);
      }, 1000);
    } else {
      WebSocketService.typing(user.username, false);
    }
  };

  const handleScroll = (e) => {
    const { scrollTop, scrollHeight, clientHeight } = e.target;

    if (scrollTop === 0 && hasMore && !loadingHistory) {
      loadMoreMessages();
    }
  };

  const formatTime = (timestamp) => {
    if (!timestamp) return '';

    try {
      const date = typeof timestamp === 'string' ? new Date(timestamp) : timestamp;
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (error) {
      console.error('Error formatting time:', error);
      return '';
    }
  };

  const getConnectionStatusColor = () => {
    switch (connectionStatus) {
      case 'connected': return 'bg-green-500';
      case 'connecting': return 'bg-yellow-500';
      case 'error': return 'bg-red-500';
      default: return 'bg-gray-500';
    }
  };

  const getConnectionStatusText = () => {
    switch (connectionStatus) {
      case 'connected': return 'Connected';
      case 'connecting': return 'Connecting...';
      case 'error': return 'Connection Error';
      default: return 'Disconnected';
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex">
      <div className="w-64 bg-white shadow-lg flex flex-col">
        <div className="p-4 border-b">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-xl font-semibold">Whispr</h1>
              <p className="text-sm text-gray-600">Welcome, {user?.username}!</p>
            </div>
            <div className="flex items-center space-x-2">
              <div className={`w-3 h-3 rounded-full ${getConnectionStatusColor()}`}></div>
              <span className="text-xs text-gray-500">{getConnectionStatusText()}</span>
            </div>
          </div>
        </div>

        <div className="p-4 flex-1">
          <h2 className="font-medium text-gray-700 mb-2">
            Online Users ({memoizedOnlineUsers.length})
          </h2>
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {memoizedOnlineUsers.map((onlineUser, index) => (
              <div key={`${onlineUser}-${index}`} className="flex items-center">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                <span className="text-sm truncate">{onlineUser}</span>
                {onlineUser === user?.username && (
                  <span className="ml-2 text-xs text-gray-500">(You)</span>
                )}
              </div>
            ))}
            {memoizedOnlineUsers.length === 0 && (
              <p className="text-sm text-gray-500 italic">No users online</p>
            )}
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
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-lg font-semibold">Global Chat</h2>
              <p className="text-sm text-gray-600">Chat with everyone online</p>
            </div>
            <div className="text-sm text-gray-500">
              {messages.length} messages
            </div>
          </div>

          {typingUsers.length > 0 && (
            <div className="mt-2 flex items-center space-x-2">
              <div className="flex space-x-1">
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
              </div>
              <p className="text-sm text-gray-600 italic">
                {typingUsers.filter(u => u !== user?.username).join(', ')} {typingUsers.length === 1 ? 'is' : 'are'} typing...
              </p>
            </div>
          )}
        </div>

        {/* Messages Area */}
        <div
          ref={messagesContainerRef}
          className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50"
          onScroll={handleScroll}
        >
          {loadingHistory && (
            <div className="text-center py-4">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500 mx-auto"></div>
              <p className="text-sm text-gray-500 mt-2">Loading older messages...</p>
            </div>
          )}

          {hasMore && !loadingHistory && (
            <div className="text-center">
              <button
                onClick={() => loadMoreMessages()}
                className="text-sm text-blue-600 hover:text-blue-800 underline"
              >
                Load older messages
              </button>
            </div>
          )}

          {memoizedMessages.length === 0 && !loadingHistory ? (
            <div className="text-center py-8">
              <p className="text-gray-500">No messages yet. Start the conversation!</p>
            </div>
          ) : (
            memoizedMessages.map((message, index) => {
              const isSystemMessage = message.type === 'JOIN' || message.type === 'LEAVE';
              const isMyMessage = message.sender === user?.username;

              return (
                <div
                  key={message.id || index}
                  className={`flex ${isMyMessage ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-xs lg:max-w-md rounded-lg p-3 ${isSystemMessage
                      ? 'bg-yellow-100 text-yellow-800 mx-auto text-center'
                      : isMyMessage
                        ? 'bg-blue-500 text-white'
                        : 'bg-white text-gray-800 shadow border'
                      }`}
                  >
                    {isSystemMessage ? (
                      <p className="text-sm italic">{message.content}</p>
                    ) : (
                      <>
                        {!isMyMessage && (
                          <p className="font-medium text-sm mb-1">
                            {message.sender}
                          </p>
                        )}
                        <p className="break-words">{message.content}</p>
                        <p className="text-xs opacity-75 mt-1">
                          {formatTime(message.timestamp)}
                        </p>
                      </>
                    )}
                  </div>
                </div>
              );
            })
          )}
          <div ref={messagesEndRef} />
        </div>

        <div className="bg-white border-t p-4">
          <form onSubmit={handleSendMessage} className="flex space-x-2">
            <Input
              type="text"
              value={newMessage}
              onChange={handleTyping}
              placeholder="Type your message..."
              className="flex-1"
              disabled={!user}
            />
            <Button
              type="submit"
              className="w-24"
              disabled={!newMessage.trim() || !user}
            >
              Send
            </Button>
          </form>
          <p className="text-xs text-gray-500 mt-2 text-center">
            Press Enter to send • Connection: {getConnectionStatusText()}
          </p>
        </div>
      </div>
    </div>
  );
};