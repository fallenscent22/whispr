import React from 'react';

export const RoomList = ({ rooms, currentRoom, onRoomSelect, onJoinRoom }) => {
  return (
    <div className="bg-white rounded-lg shadow-sm">
      <div className="p-4 border-b">
        <h3 className="font-semibold text-gray-800">Chat Rooms</h3>
      </div>
      
      <div className="max-h-96 overflow-y-auto">
        {rooms.map(room => (
          <div
            key={room.roomId}
            className={`p-4 border-b cursor-pointer hover:bg-gray-50 ${
              currentRoom?.roomId === room.roomId ? 'bg-blue-50 border-blue-200' : ''
            }`}
            onClick={() => onRoomSelect(room)}
          >
            <div className="flex justify-between items-start">
              <div className="flex-1">
                <h4 className="font-medium text-gray-900">{room.name}</h4>
                <p className="text-sm text-gray-600 mt-1">{room.description}</p>
                <div className="flex items-center mt-2 text-xs text-gray-500">
                  <span className={`inline-block w-2 h-2 rounded-full mr-1 ${
                    room.isPrivate ? 'bg-red-400' : 'bg-green-400'
                  }`}></span>
                  {room.isPrivate ? 'Private' : 'Public'} • {room.memberCount || 0} members
                </div>
              </div>
              
              {!currentRoom || currentRoom.roomId !== room.roomId ? (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onJoinRoom(room.roomId);
                  }}
                  className="ml-2 px-3 py-1 bg-blue-500 text-white text-xs rounded hover:bg-blue-600 transition-colors"
                >
                  Join
                </button>
              ) : (
                <span className="ml-2 px-2 py-1 bg-green-100 text-green-800 text-xs rounded">
                  Active
                </span>
              )}
            </div>
          </div>
        ))}
        
        {rooms.length === 0 && (
          <div className="p-8 text-center text-gray-500">
            <p>No rooms found. Create your first room!</p>
          </div>
        )}
      </div>
    </div>
  );
};