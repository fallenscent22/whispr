import React from 'react';

export const TypingIndicator = ({ typingUsers, currentUser }) => {
  if (!typingUsers || typingUsers.length === 0) return null;

  // Filter out current user
  const otherTypingUsers = typingUsers.filter(user => user !== currentUser?.username);
  
  if (otherTypingUsers.length === 0) return null;

  const getTypingText = () => {
    if (otherTypingUsers.length === 1) {
      return `${otherTypingUsers[0]} is typing...`;
    } else if (otherTypingUsers.length === 2) {
      return `${otherTypingUsers[0]} and ${otherTypingUsers[1]} are typing...`;
    } else {
      return `${otherTypingUsers[0]} and ${otherTypingUsers.length - 1} others are typing...`;
    }
  };

  return (
    <div className="flex items-center space-x-2 p-2 text-gray-600 text-sm italic">
      <div className="flex space-x-1">
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
      </div>
      <span>{getTypingText()}</span>
    </div>
  );
};