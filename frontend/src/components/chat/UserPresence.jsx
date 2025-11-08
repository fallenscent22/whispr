import React from 'react';

export const UserPresence = ({ user, onlineUsers, showLastSeen = true }) => {
  const isOnline = onlineUsers.includes(user.username);
  const lastSeen = user.lastSeen; // This would come from your user data

  const formatLastSeen = (timestamp) => {
    if (!timestamp) return 'Never';
    
    const now = new Date();
    const lastSeenDate = new Date(timestamp);
    const diffInMinutes = Math.floor((now - lastSeenDate) / (1000 * 60));
    
    if (diffInMinutes < 1) return 'Just now';
    if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}h ago`;
    return `${Math.floor(diffInMinutes / 1440)}d ago`;
  };

  return (
    <div className="flex items-center space-x-2">
      <div className="flex items-center space-x-1">
        <div 
          className={`w-2 h-2 rounded-full ${
            isOnline ? 'bg-green-500' : 'bg-gray-400'
          }`}
        ></div>
        <span className="text-sm font-medium">{user.username}</span>
      </div>
      
      {showLastSeen && !isOnline && lastSeen && (
        <span className="text-xs text-gray-500">
          Last seen {formatLastSeen(lastSeen)}
        </span>
      )}
      
      {showLastSeen && isOnline && (
        <span className="text-xs text-green-600">Online</span>
      )}
    </div>
  );
};