import { useEffect, useState } from 'react'
import {
  getPendingProposals,
  approveProposal,
  rejectProposal,
  generateProposals
} from '../api/pricing'

import { topup } from '../api/jetons'
import { fetchUsers } from '../api/users'
import { CheckCircle, XCircle, Coins } from 'lucide-react'
import AdminPanel from './ProposalsStats'

export default function ManagerPanel() {

  // ---------------- DATA ----------------
  const [proposals, setProposals] = useState([])
  const [users, setUsers] = useState([])

  // ---------------- USER SEARCH ----------------
  const [search, setSearch] = useState('')
  const [selectedUser, setSelectedUser] = useState(null)
  const [showDropdown, setShowDropdown] = useState(false)

  // ---------------- TOPUP ----------------
  const [topupForm, setTopupForm] = useState({ amount: '' })
  const [topupMsg, setTopupMsg] = useState('')

  // ---------------- AI GENERATION ----------------
  const [generating, setGenerating] = useState(false)

  // ---------------- ERROR ----------------
  const [error, setError] = useState('')

  // ---------------- LOAD ----------------
  const loadProposals = () =>
    getPendingProposals().then(r => setProposals(r.data))

  const loadUsers = () =>
    fetchUsers().then(r => setUsers(r.data))

  useEffect(() => {
    loadProposals()
    loadUsers()
  }, [])

  // ---------------- FILTER USERS ----------------
  const filteredUsers =
    search.trim()
      ? users.filter(u =>
          `${u.firstName} ${u.lastName}`
            .toLowerCase()
            .includes(search.toLowerCase())
        )
      : []

  // ---------------- SELECT USER ----------------
  const handleSelectUser = (user) => {
    setSelectedUser(user)
    setSearch(`${user.firstName} ${user.lastName}`)
    setShowDropdown(false)
  }

  // ---------------- GENERATE AI ----------------
  const handleGenerate = async () => {
    try {
      setGenerating(true)
      setError('')

      await generateProposals()
      await loadProposals()

    } catch (e) {
      setError(e.response?.data?.error || 'Failed to generate proposals')
    } finally {
      setGenerating(false)
    }
  }

  // ---------------- APPROVE / REJECT ----------------
  const handleApprove = async (id) => {
    try {
      await approveProposal(id)
      loadProposals()
    } catch (e) {
      setError(e.response?.data?.error || 'Failed')
    }
  }

  const handleReject = async (id) => {
    try {
      await rejectProposal(id)
      loadProposals()
    } catch (e) {
      setError(e.response?.data?.error || 'Failed')
    }
  }

  // ---------------- TOPUP ----------------
  const handleTopup = async (e) => {
    e.preventDefault()

    if (!selectedUser) {
      setError("Please select a user")
      return
    }

    try {
      await topup(selectedUser.id, Number(topupForm.amount))

      setTopupMsg(
        `Added ${topupForm.amount} jetons to ${selectedUser.firstName} ${selectedUser.lastName}`
      )

      setTopupForm({ amount: '' })
      setSearch('')
      setSelectedUser(null)

    } catch (e) {
      setError(e.response?.data?.error || 'Topup failed')
    }
  }

  // ---------------- UI ----------------
  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">

      <h1 className="text-2xl font-bold text-gray-800">
        Manager Panel
      </h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm">
          {error}
        </div>
      )}

      {/* ================= TOPUP ================= */}
      <div className="card">
        <h2 className="font-semibold text-gray-700 mb-4 flex items-center gap-2">
          <Coins size={18} /> Add Jetons to Member
        </h2>

        {topupMsg && (
          <div className="bg-green-50 border border-green-200 text-green-700 rounded-lg p-3 mb-4 text-sm">
            {topupMsg}
          </div>
        )}

        <div className="relative mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Search user
          </label>

          <input
            className="input-field"
            value={search}
            placeholder="Type name..."
            onChange={(e) => {
              setSearch(e.target.value)
              setShowDropdown(true)
            }}
            onFocus={() => setShowDropdown(true)}
          />

          {showDropdown && filteredUsers.length > 0 && (
            <div className="absolute z-20 bg-white border w-full mt-1 rounded-lg shadow max-h-60 overflow-auto">
              {filteredUsers.map(user => (
                <div
                  key={user.id}
                  onClick={() => handleSelectUser(user)}
                  className="px-3 py-2 hover:bg-gray-100 cursor-pointer"
                >
                  <p className="font-medium">
                    {user.firstName} {user.lastName}
                  </p>
                  <p className="text-xs text-gray-500">{user.email}</p>
                </div>
              ))}
            </div>
          )}
        </div>

        {selectedUser && (
          <p className="text-sm text-green-600 mb-3">
            Selected: {selectedUser.firstName} {selectedUser.lastName}
          </p>
        )}

        <form onSubmit={handleTopup} className="flex gap-3">
          <input
            type="number"
            className="input-field flex-1"
            placeholder="Amount"
            value={topupForm.amount}
            onChange={e => setTopupForm({ amount: e.target.value })}
            required
          />

          <button className="btn-primary">
            Add Jetons
          </button>
        </form>
      </div>

      {/* ================= AI GENERATION PANEL ================= */}
      <div className="card border border-blue-100 bg-gradient-to-r from-blue-50 to-indigo-50">

        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-semibold text-gray-800">
              🤖 AI Pricing Engine
            </h2>
            <p className="text-sm text-gray-600 mt-1">
              Generate intelligent discount proposals based on weather, occupancy and demand.
            </p>
          </div>

          <button
            onClick={handleGenerate}
            disabled={generating}
            className={`px-5 py-2 rounded-lg font-medium transition-all
              ${generating
                ? 'bg-indigo-300 cursor-not-allowed'
                : 'bg-indigo-600 hover:bg-indigo-700 text-white'
              }`}
          >
            {generating ? 'Generating...' : 'Generate AI'}
          </button>
        </div>

        {/* ANIMATION */}
        {generating && (
          <div className="mt-4 flex items-center gap-3 text-indigo-700 animate-pulse">
            <div className="w-4 h-4 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
            <span className="text-sm">
              AI is analyzing weather, occupancy & demand...
            </span>
          </div>
        )}

        {/* LOADING BAR */}
        {generating && (
          <div className="mt-3 h-1 w-full bg-indigo-100 rounded overflow-hidden">
            <div className="h-full bg-indigo-500 animate-[loading_2s_linear_infinite]"></div>
          </div>
        )}
      </div>

      {/* ================= PROPOSALS ================= */}
      <div className="card">
        <h2 className="font-semibold text-gray-700 mb-4">
          Pending Discount Proposals
        </h2>

        {proposals.length === 0 ? (
          <p className="text-gray-400 text-sm">No pending proposals.</p>
        ) : (
          proposals.map(p => (
            <div
              key={p.id}
              className="flex items-center justify-between py-3 border-b last:border-0"
            >
              <div>
                <p className="font-medium text-gray-800">
                  Discount Slot — {p.date} at {p.startTime?.slice(0, 5)}
                </p>

                <p className="text-sm text-gray-500">{p.reason}</p>

                <p className="text-sm mt-1">
                  <span className="line-through text-gray-400">
                    {p.originalPrice}j
                  </span>
                  {' → '}
                  <span className="text-tennis-green font-semibold">
                    {p.discountedPrice}j
                  </span>
                </p>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => handleApprove(p.id)}
                  className="btn-primary text-sm px-3 py-1 flex items-center gap-1"
                >
                  <CheckCircle size={15} /> Approve
                </button>

                <button
                  onClick={() => handleReject(p.id)}
                  className="btn-danger text-sm px-3 py-1 flex items-center gap-1"
                >
                  <XCircle size={15} /> Reject
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}