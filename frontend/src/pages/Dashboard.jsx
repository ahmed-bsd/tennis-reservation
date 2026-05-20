import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getBalance } from '../api/jetons'
import { myReservations } from '../api/reservations'
import { CalendarDays, Coins, Bot, Settings } from 'lucide-react'

export default function Dashboard() {
  const { user } = useAuth()
  const [balance, setBalance]   = useState(null)
  const [upcoming, setUpcoming] = useState([])

  useEffect(() => {
    getBalance().then(r => setBalance(r.data.balance))
    myReservations().then(r => {
      const today = new Date().toISOString().split('T')[0]
      setUpcoming(r.data.filter(res => res.date >= today && res.status !== 'CANCELLED').slice(0, 3))
    })
  }, [])

  const cards = [
    { to: '/book',         icon: <CalendarDays size={28} />, label: 'Book a Court',     color: 'bg-tennis-green' },
    { to: '/reservations', icon: <CalendarDays size={28} />, label: 'My Bookings',      color: 'bg-blue-600' },
    { to: '/ai',           icon: <Bot size={28} />,          label: 'AI Assistant',     color: 'bg-purple-600' },
    ...((['MANAGER','ADMIN'].includes(user?.role))
        ? [{ to: '/manager', icon: <Settings size={28} />, label: 'Manager Panel', color: 'bg-orange-500' }]
        : []),
    ...(user?.role === 'ADMIN'
        ? [{ to: '/admin', icon: <Settings size={28} />, label: 'Admin Panel', color: 'bg-red-600' }]
        : []),
  ]

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800">
          Welcome back, {user?.firstName} 👋
        </h1>
        <p className="text-gray-500 text-sm mt-1 capitalize">{user?.role?.toLowerCase()} account</p>
      </div>

      {/* Jeton balance */}
      <div className="card mb-6 flex items-center gap-4 bg-gradient-to-r from-tennis-green to-tennis-light text-white">
        <Coins size={40} />
        <div>
          <p className="text-sm opacity-80">Jeton Balance</p>
          <p className="text-3xl font-bold">{balance ?? '…'} <span className="text-lg font-normal">jetons</span></p>
        </div>
      </div>

      {/* Quick actions */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        {cards.map(c => (
          <Link key={c.to} to={c.to}
            className={`${c.color} text-white rounded-xl p-5 flex flex-col items-center gap-2 hover:opacity-90 transition-opacity`}>
            {c.icon}
            <span className="text-sm font-medium text-center">{c.label}</span>
          </Link>
        ))}
      </div>

      {/* Upcoming bookings */}
      <div className="card">
        <h2 className="font-semibold text-gray-700 mb-4">Upcoming Bookings</h2>
        {upcoming.length === 0
          ? <p className="text-gray-400 text-sm">No upcoming bookings. <Link to="/book" className="text-tennis-green hover:underline">Book a court →</Link></p>
          : upcoming.map(r => (
            <div key={r.id} className="flex items-center justify-between py-3 border-b last:border-0">
              <div>
                <p className="font-medium">Court {r.court.number} — {r.court.name}</p>
                <p className="text-sm text-gray-500">{r.date} at {r.startTime} · {r.durationMinutes} min</p>
              </div>
              <span className={`badge-${r.status.toLowerCase()}`}>{r.status}</span>
            </div>
          ))
        }
      </div>
    </div>
  )
}
