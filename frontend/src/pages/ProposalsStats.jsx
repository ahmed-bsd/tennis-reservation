import { useEffect, useState } from 'react'
import { getAllProposals } from '../api/pricing'
import { BarChart3, TrendingUp, Users, CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react'

export default function ProposalsStats() {

  const [proposals, setProposals] = useState([])
  const [filter, setFilter] = useState('ALL')
  const [selectedDate, setSelectedDate] = useState(new Date())

  useEffect(() => {
    getAllProposals().then(r => setProposals(r.data))
  }, [])

  const formatDate = (date) => date.toISOString().split('T')[0]

  const changeDay = (offset) => {
    const newDate = new Date(selectedDate)
    newDate.setDate(newDate.getDate() + offset)
    setSelectedDate(newDate)
  }

  const filteredProposals = proposals.filter(p => {
    const matchStatus = filter === 'ALL' || p.status === filter
    const matchDate = p.date === formatDate(selectedDate)
    return matchStatus && matchDate
  })

  const approved = proposals.filter(p => p.status === 'APPROVED').length
  const rejected = proposals.filter(p => p.status === 'REJECTED').length
  const pending  = proposals.filter(p => p.status === 'PENDING').length

  const stats = [
    { key: 'ALL', label: 'Total', value: proposals.length, icon: <BarChart3 size={22} />, color: 'text-blue-600 bg-blue-50' },
    { key: 'APPROVED', label: 'Approved', value: approved, icon: <TrendingUp size={22} />, color: 'text-green-600 bg-green-50' },
    { key: 'REJECTED', label: 'Rejected', value: rejected, icon: <Users size={22} />, color: 'text-red-600 bg-red-50' },
    { key: 'PENDING', label: 'Pending', value: pending, icon: <CalendarDays size={22} />, color: 'text-yellow-600 bg-yellow-50' },
  ]

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">

      <h1 className="text-2xl font-bold text-gray-800">
        Proposals Stats
      </h1>

      {/* ================= STATS ================= */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">

        {stats.map(s => {
          const isActive = filter === s.key

          return (
            <div
              key={s.key}
              onClick={() => setFilter(s.key)}
              className={`card text-center cursor-pointer transition-all duration-200 border-2
                ${isActive
                  ? 'border-tennis-green shadow-md scale-[1.02]'
                  : 'border-transparent hover:shadow-sm'
                }`}
            >
              <div className={`${s.color} rounded-full w-10 h-10 flex items-center justify-center mx-auto mb-2`}>
                {s.icon}
              </div>

              <p className="text-2xl font-bold text-gray-800">
                {s.value}
              </p>

              <p className="text-xs text-gray-500 mt-1">
                {s.label}
              </p>
            </div>
          )
        })}

      </div>

      {/* ================= DATE NAV (NOW UNDER STATS) ================= */}
      <div className="flex items-center justify-center gap-4 bg-white p-4 rounded-lg shadow-sm">

        <button
          onClick={() => changeDay(-1)}
          className="p-2 rounded-full hover:bg-gray-100"
        >
          <ChevronLeft />
        </button>

        <div className="font-semibold text-gray-700">
          {formatDate(selectedDate)}
        </div>

        <button
          onClick={() => changeDay(1)}
          className="p-2 rounded-full hover:bg-gray-100"
        >
          <ChevronRight />
        </button>

      </div>

      {/* ================= TABLE ================= */}
      <div className="card">

        <h2 className="font-semibold text-gray-700 mb-4">
          Discount Proposals History
        </h2>

        <div className="overflow-x-auto">

          <table className="w-full text-sm">

            <thead>
              <tr className="text-left text-gray-500 border-b">
                <th className="pb-2 pr-4">Slot</th>
                <th className="pb-2 pr-4">Date</th>
                <th className="pb-2 pr-4">Original</th>
                <th className="pb-2 pr-4">Discounted</th>
                <th className="pb-2 pr-4">Status</th>
              </tr>
            </thead>

            <tbody>
              {filteredProposals.map(p => (
                <tr key={p.id} className="border-b last:border-0 hover:bg-gray-50">

                  <td className="py-2 pr-4">
                    {p.startTime?.slice(0, 5)}
                  </td>

                  <td className="py-2 pr-4">
                    {p.date}
                  </td>

                  <td className="py-2 pr-4">
                    {p.originalPrice}j
                  </td>

                  <td className="py-2 pr-4 text-tennis-green font-medium">
                    {p.discountedPrice}j
                  </td>

                  <td className="py-2 pr-4">
                    <span className={`badge-${p.status.toLowerCase()}`}>
                      {p.status}
                    </span>
                  </td>

                </tr>
              ))}

              {filteredProposals.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-6 text-center text-gray-400">
                    No data for this date/filter.
                  </td>
                </tr>
              )}
            </tbody>

          </table>

        </div>
      </div>

    </div>
  )
}