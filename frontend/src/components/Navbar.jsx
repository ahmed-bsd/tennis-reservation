import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../context/WebSocketContext'
import { Bell, LogOut } from 'lucide-react'
import { useState } from 'react'

export default function Navbar() {
  const { user, logout } = useAuth()
  const { notifications, clearNotification } = useWebSocket()
  const navigate = useNavigate()
  const [showNotifs, setShowNotifs] = useState(false)

  const handleLogout = () => { logout(); navigate('/login') }

  const isManager = ['MANAGER', 'ADMIN'].includes(user?.role)
  const isAdmin   = user?.role === 'ADMIN'

  return (
    <nav className="bg-tennis-green text-white shadow-md">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link to="/dashboard" className="text-xl font-bold tracking-tight">
          🎾 Tennis Club
        </Link>

        <div className="flex items-center gap-6 text-sm font-medium">
          <Link to="/book"         className="hover:text-tennis-light transition-colors">Book</Link>
          <Link to="/reservations" className="hover:text-tennis-light transition-colors">My Bookings</Link>
          <Link to="/ai"           className="hover:text-tennis-light transition-colors">AI Assistant</Link>
          {isManager && <Link to="/manager" className="hover:text-tennis-light transition-colors">Manager</Link>}
          {isAdmin    && <Link to="/admin"  className="hover:text-tennis-light transition-colors">Stats</Link>}
        </div>

        <div className="flex items-center gap-4">
          {/* Notifications */}
          <div className="relative">
            <button onClick={() => setShowNotifs(!showNotifs)} className="relative p-1">
              <Bell size={20} />
              {notifications.length > 0 && (
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center">
                  {notifications.length}
                </span>
              )}
            </button>
            {showNotifs && (
              <div className="absolute right-0 mt-2 w-80 bg-white text-gray-800 rounded-xl shadow-lg z-50 border">
                <div className="p-3 border-b font-semibold text-sm">Notifications</div>
                {notifications.length === 0 && (
                  <p className="p-4 text-sm text-gray-500">No notifications</p>
                )}
                {notifications.map(n => (
                  <div key={n.id} className="p-3 border-b text-sm flex justify-between items-start hover:bg-gray-50">
                    <span>{n.message || n.type}</span>
                    <button onClick={() => clearNotification(n.id)} className="text-gray-400 hover:text-gray-600 ml-2">✕</button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <span className="text-sm opacity-80">{user?.firstName}</span>
          <button onClick={handleLogout} className="hover:text-red-300 transition-colors">
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </nav>
  )
}
