import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar          from './components/Navbar'
import ProtectedRoute  from './components/ProtectedRoute'
import Login           from './pages/Login'
import Signup          from './pages/Signup'
import Dashboard       from './pages/Dashboard'
import BookingCalendar from './pages/BookingCalendar'
import MyReservations  from './pages/MyReservations'
import AIChat          from './pages/AIChat'
import ManagerPanel    from './pages/ManagerPanel'
import ProposalsStats      from './pages/ProposalsStats'

export default function App() {
  const { user } = useAuth()

  return (
    <>
      {user && <Navbar />}
      <Routes>
        <Route path="/login"  element={user ? <Navigate to="/dashboard" /> : <Login />} />
        <Route path="/signup" element={user ? <Navigate to="/dashboard" /> : <Signup />} />

        <Route path="/dashboard"    element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="/book"         element={<ProtectedRoute><BookingCalendar /></ProtectedRoute>} />
        <Route path="/reservations" element={<ProtectedRoute><MyReservations /></ProtectedRoute>} />
        <Route path="/ai"           element={<ProtectedRoute><AIChat /></ProtectedRoute>} />

        <Route path="/manager" element={
          <ProtectedRoute roles={['MANAGER','ADMIN']}><ManagerPanel /></ProtectedRoute>
        } />
        <Route path="/admin" element={
          <ProtectedRoute roles={['ADMIN']}><ProposalsStats /></ProtectedRoute>
        } />

        <Route path="*" element={<Navigate to={user ? '/dashboard' : '/login'} />} />
      </Routes>
    </>
  )
}
