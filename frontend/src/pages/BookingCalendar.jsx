import { useState, useEffect } from 'react'
import { getAvailability } from '../api/courts'
import { createReservation } from '../api/reservations'
import { getBalance } from '../api/jetons'
import { format, addDays } from 'date-fns'

const DURATIONS = [30, 60]

export default function BookingCalendar() {
  const today    = new Date()
  const maxDate  = addDays(today, 2)
  const [date, setDate]         = useState(format(today, 'yyyy-MM-dd'))
  const [slots, setSlots]       = useState([])
  const [balance, setBalance]   = useState(0)
  const [selected, setSelected] = useState(null)
  const [duration, setDuration] = useState(30)
  const [loading, setLoading]   = useState(false)
  const [success, setSuccess]   = useState('')
  const [error, setError]       = useState('')

  useEffect(() => {
    getAvailability(date).then(r => setSlots(r.data))
    getBalance().then(r => setBalance(r.data.balance))
    setSelected(null)
  }, [date])

  const computeCost = (startTime, dur) => {
    const hour = parseInt(startTime.split(':')[0])
    const base = hour < 12 ? 10 : 20
    return base * (dur / 30)
  }

  const handleBook = async () => {
    if (!selected) return
    setLoading(true); setError(''); setSuccess('')
    try {
      await createReservation({
        courtId: selected.courtId,
        date,
        startTime: selected.startTime,
        durationMinutes: duration
      })
      setSuccess('Booking confirmed! Check "My Bookings" for details.')
      setSelected(null)
      getAvailability(date).then(r => setSlots(r.data))
      getBalance().then(r => setBalance(r.data.balance))
    } catch (err) {
      setError(err.response?.data?.error || 'Booking failed')
    } finally {
      setLoading(false)
    }
  }

  // Group slots by court
  const byCourt = slots.reduce((acc, s) => {
    const key = `${s.courtId}-${s.courtName}`
    if (!acc[key]) acc[key] = []
    acc[key].push(s)
    return acc
  }, {})

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Book a Court</h1>

      {/* Controls */}
      <div className="card mb-6 flex flex-wrap gap-4 items-center">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Date</label>
          <input type="date" className="input-field w-48"
            min={format(today, 'yyyy-MM-dd')}
            max={format(maxDate, 'yyyy-MM-dd')}
            value={date} onChange={e => setDate(e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Duration</label>
          <select className="input-field w-36" value={duration} onChange={e => setDuration(Number(e.target.value))}>
            {DURATIONS.map(d => <option key={d} value={d}>{d} min</option>)}
          </select>
        </div>
        <div className="ml-auto text-right">
          <p className="text-xs text-gray-500">Your balance</p>
          <p className="text-xl font-bold text-tennis-green">{balance} jetons</p>
        </div>
      </div>

      {success && <div className="bg-green-50 border border-green-200 text-green-700 rounded-lg p-3 mb-4 text-sm">{success}</div>}
      {error   && <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">{error}</div>}

      {/* Calendar grid */}
      <div className="space-y-6">
        {Object.entries(byCourt).map(([key, courtSlots]) => {
          const { courtId, courtName, courtNumber } = courtSlots[0]
          return (
            <div key={key} className="card">
              <h2 className="font-semibold text-gray-700 mb-3">Court {courtNumber} — {courtName}</h2>
              <div className="flex flex-wrap gap-2">
                {courtSlots.map(slot => {
                  const isSelected = selected?.courtId === courtId && selected?.startTime === slot.startTime
                  const cost = computeCost(slot.startTime, duration)
                  return (
                    <button key={slot.startTime}
                      disabled={!slot.available}
                      onClick={() => setSelected(slot.available ? slot : null)}
                      className={`px-3 py-2 rounded-lg text-sm font-medium border transition-all
                        ${!slot.available
                          ? 'bg-red-50 border-red-200 text-red-400 cursor-not-allowed line-through'
                          : isSelected
                            ? 'bg-tennis-green border-tennis-green text-white'
                            : 'bg-white border-gray-200 text-gray-700 hover:border-tennis-light hover:bg-green-50'
                        }`}>
                      {slot.startTime.slice(0,5)}
                      {slot.available && <span className="block text-xs opacity-70">{cost}j</span>}
                    </button>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>

      {/* Booking summary */}
      {selected && (
        <div className="fixed bottom-6 right-6 card shadow-xl border-tennis-green border-2 w-80">
          <h3 className="font-semibold text-gray-800 mb-2">Confirm Booking</h3>
          <p className="text-sm text-gray-600">Court {selected.courtNumber} — {selected.courtName}</p>
          <p className="text-sm text-gray-600">{date} at {selected.startTime?.slice(0,5)} · {duration} min</p>
          <p className="text-sm font-medium text-tennis-green mt-1">
            Cost: {computeCost(selected.startTime, duration)} jetons
          </p>
          <div className="flex gap-2 mt-3">
            <button onClick={handleBook} disabled={loading} className="btn-primary flex-1">
              {loading ? 'Booking…' : 'Confirm'}
            </button>
            <button onClick={() => setSelected(null)} className="btn-secondary flex-1">Cancel</button>
          </div>
        </div>
      )}
    </div>
  )
}
