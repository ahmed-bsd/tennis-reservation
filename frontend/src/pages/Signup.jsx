import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { signup as signupApi } from '../api/auth'

export default function Signup() {
  const { login } = useAuth()
  const navigate  = useNavigate()
  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', password: '',
    dateOfBirth: '', gender: '', member: false
  })
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  const set = key => e => setForm({ ...form, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  const handleSubmit = async e => {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      const { data } = await signupApi(form)
      login(data)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.error || 'Signup failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-tennis-dark to-tennis-green py-8">
      <div className="card w-full max-w-md">
        <div className="text-center mb-6">
          <span className="text-4xl">🎾</span>
          <h1 className="text-2xl font-bold text-tennis-dark mt-2">Create Account</h1>
        </div>

        {error && <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
              <input className="input-field" value={form.firstName} onChange={set('firstName')} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
              <input className="input-field" value={form.lastName} onChange={set('lastName')} required />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input type="email" className="input-field" value={form.email} onChange={set('email')} required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input type="password" className="input-field" value={form.password} onChange={set('password')} minLength={8} required />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
              <input type="date" className="input-field" value={form.dateOfBirth} onChange={set('dateOfBirth')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Gender</label>
              <select className="input-field" value={form.gender} onChange={set('gender')}>
                <option value="">Select</option>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={form.member} onChange={set('member')} className="w-4 h-4 accent-tennis-green" />
            <span className="text-sm text-gray-700">I am a club member</span>
          </label>
          <button type="submit" disabled={loading} className="btn-primary w-full">
            {loading ? 'Creating account…' : 'Sign Up'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          Already have an account? <Link to="/login" className="text-tennis-green font-medium hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
