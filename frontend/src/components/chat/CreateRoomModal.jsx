import React, { useState } from 'react';
import { Button } from '../common/Button';
import { Input } from '../common/Input';

export const CreateRoomModal = ({ isOpen, onClose, onCreateRoom }) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    type: 'GROUP',
    isPrivate: false,
    maxMembers: 50
  });
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      await onCreateRoom(formData);
      setFormData({
        name: '',
        description: '',
        type: 'GROUP',
        isPrivate: false,
        maxMembers: 50
      });
      onClose();
    } catch (error) {
      console.error('Failed to create room:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 className="text-xl font-bold mb-4">Create New Chat Room</h2>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Room Name"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
            placeholder="Enter room name"
          />
          
          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2">
              Description
            </label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              rows="3"
              placeholder="Enter room description"
            />
          </div>
          
          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2">
              Room Type
            </label>
            <select
              name="type"
              value={formData.type}
              onChange={handleChange}
              className="shadow border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            >
              <option value="GROUP">Group Chat</option>
              <option value="CHANNEL">Channel</option>
            </select>
          </div>
          
          <div className="flex items-center">
            <input
              type="checkbox"
              name="isPrivate"
              checked={formData.isPrivate}
              onChange={handleChange}
              className="mr-2"
            />
            <label className="text-gray-700 text-sm">Private Room</label>
          </div>
          
          <Input
            label="Max Members"
            name="maxMembers"
            type="number"
            value={formData.maxMembers}
            onChange={handleChange}
            min="2"
            max="1000"
          />
          
          <div className="flex justify-end space-x-2">
            <Button
              type="button"
              variant="secondary"
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              loading={loading}
            >
              Create Room
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};