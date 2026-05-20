import { useEffect, useState, useCallback } from 'react'
import {
  myReservations,
  cancelReservation,
  confirmReservation,
  allReservations
} from '../api/reservations'

import { useAuth } from '../context/AuthContext'
import { format, addDays } from 'date-fns'

export default function MyReservations() {
  const { user } = useAuth()

  const today = new Date()
  const maxDate = addDays(today, 2)

  const [reservations, setReservations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 🔎 filters
  const [date, setDate] = useState('')
  const [clientSearch, setClientSearch] = useState('')

  const load = useCallback(() => {
    setLoading(true)

    const request =
      user?.role === 'ADMIN' || user?.role === 'MANAGER'
        ? allReservations()
        : myReservations()

    request
      .then(r => setReservations(r.data))
      .catch(() => setError('Failed to load reservations'))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => {
    load()
  }, [load])

  // 🔎 FILTER (DATE + CLIENT NAME)
  const filtered = reservations.filter(r => {
    const matchDate = date ? r.date === date : true

    const fullName =
      `${r.user.firstName} ${r.user.lastName}`.toLowerCase()

    const matchClient = clientSearch
      ? fullName.includes(clientSearch.toLowerCase())
      : true

    return matchDate && matchClient
  })

  // ❌ CANCEL
  const handleCancel = async (id) => {
    if (!confirm('Cancel this reservation?')) return

    try {
      await cancelReservation(id)

      setReservations(prev =>
        prev.map(r =>
          r.id === id ? { ...r, status: 'CANCELLED' } : r
        )
      )
    } catch (err) {
      setError(err.response?.data?.error || 'Cancellation failed')
    }
  }

  // ✅ CONFIRM
  const handleConfirmation = async (id) => {
    if (!confirm('Confirm this reservation?')) return

    try {
      await confirmReservation(id)

      setReservations(prev =>
        prev.map(r =>
          r.id === id ? { ...r, status: 'CONFIRMED' } : r
        )
      )
    } catch (err) {
      setError(err.response?.data?.error || 'Confirmation failed')
    }
  }

  const canCancel = (res) => {
    const bookingDt = new Date(`${res.date}T${res.startTime}`)
    return res.status !== 'CANCELLED' && bookingDt > new Date()
  }

  if (loading) {
    return (
      <div className="flex justify-center py-20 text-gray-500">
        Loading…
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">

      <h1 className="text-2xl font-bold text-gray-800 mb-6">
        My Reservations
      </h1>

      {/* 🔎 FILTER BAR */}
      <div className="card mb-6 flex flex-wrap gap-4 items-end">

        {/* 📅 DATE FILTER (allow past dates) */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Date
          </label>

          <input
            type="date"
            className="input-field w-48"
            min=""   // 🔥 important: allow ALL past dates
            max={format(maxDate, 'yyyy-MM-dd')}
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
        </div>

        {/* 🔎 CLIENT SEARCH */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Client
          </label>

          <input
            type="text"
            placeholder="Search client..."
            className="input-field w-64"
            value={clientSearch}
            onChange={(e) => setClientSearch(e.target.value)}
          />
        </div>

        {/* RESET */}
        <button
          onClick={() => {
            setDate('')
            setClientSearch('')
          }}
          className="btn-secondary"
        >
          Reset filters
        </button>
      </div>

      {/* ERROR */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
          {error}
        </div>
      )}

      {/* LIST */}
      {filtered.length === 0 ? (
        <div className="card text-center text-gray-500 py-12">
          No reservations found.
        </div>
      ) : (
        <div className="space-y-3">

          {filtered.map(r => (
            <div
              key={r.id}
              className="card flex items-center justify-between"
            >

              {/* LEFT */}
              <div>
                <p className="font-semibold text-gray-800">
                  Court {r.court.number} — {r.court.name}
                </p>

                <p className="text-sm text-gray-500 mt-1">
                  {r.date} · {r.startTime?.slice(0, 5)} – {r.endTime?.slice(0, 5)}
                </p>

                <p className="text-sm text-tennis-green font-medium mt-1">
                  {r.jetonCost} jetons
                </p>

                <p className="text-sm text-tennis-blue font-medium mt-1">
                  {r.user.firstName} {r.user.lastName}
                </p>
              </div>

              {/* RIGHT */}
              <div className="flex items-center gap-3">

                {r.status === 'CANCELLED' && (
                  <span className="text-xs text-red-500 font-medium">
                    Cancelled
                  </span>
                )}

                {r.status === 'CONFIRMED' && (
                  <span className="text-green-600 font-semibold">
                    Confirmed
                  </span>
                )}

                {canCancel(r) && (
                  <button
                    onClick={() => handleCancel(r.id)}
                    className="btn-danger text-sm py-1 px-3"
                  >
                    Cancel
                  </button>
                )}

                {r.status === 'PENDING' && (
                  <button
                    onClick={() => handleConfirmation(r.id)}
                    className="bg-green-500 hover:bg-green-600 text-white text-sm py-1 px-3 rounded-md"
                  >
                    Confirm
                  </button>
                )}

              </div>

            </div>
          ))}

        </div>
      )}
    </div>
  )
}