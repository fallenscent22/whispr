import React from 'react';

export const Button = ({ 
  children, 
  variant = 'primary', 
  loading = false,
  disabled,
  className = '',
  ...props 
}) => {
  const baseClasses = 'w-full py-2 px-4 rounded font-bold focus:outline-none focus:shadow-outline transition-colors';
  const variants = {
    primary: 'bg-blue-500 hover:bg-blue-700 text-white',
    secondary: 'bg-gray-500 hover:bg-gray-700 text-white'
  };
  
  const combinedClasses = `${baseClasses} ${variants[variant]} ${
    disabled || loading ? 'opacity-50 cursor-not-allowed' : ''
  } ${className}`;

  return (
    <button {...props} disabled={disabled || loading} className={combinedClasses}>
      {loading ? (
        <div className="flex items-center justify-center">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
          Loading...
        </div>
      ) : (
        children
      )}
    </button>
  );
};