import { useState, useRef, useEffect } from 'react'
import { chatWithAI } from '../api/ai'
import { Bot, Send, User } from 'lucide-react'

const SUGGESTIONS = [
  'Find me an available court tomorrow morning',
  'What are the prices for evening slots?',
  'Show me courts available this afternoon',
  'How does cancellation work?',
]

export default function AIChat() {
  const [messages, setMessages] = useState([
    { role: 'assistant', text: "Hi! I'm your tennis assistant. I can help you find available courts, explain pricing, and guide your booking. What can I do for you?" }
  ])
  const [input, setInput]     = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)

  useEffect(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), [messages])

  const send = async (message) => {
    if (!message.trim()) return
    setMessages(prev => [...prev, { role: 'user', text: message }])
    setInput('')
    setLoading(true)
    try {
      const { data } = await chatWithAI(message)
      setMessages(prev => [...prev, { role: 'assistant', text: data.reply }])
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', text: 'Sorry, I could not process your request. Please try again.' }])
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = e => { e.preventDefault(); send(input) }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 flex flex-col h-[calc(100vh-5rem)]">
      <div className="flex items-center gap-3 mb-6">
        <div className="bg-purple-100 rounded-full p-2"><Bot className="text-purple-600" size={24} /></div>
        <div>
          <h1 className="text-xl font-bold text-gray-800">AI Assistant</h1>
          <p className="text-sm text-gray-500">Powered by Claude</p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto space-y-4 mb-4">
        {messages.map((msg, i) => (
          <div key={i} className={`flex gap-3 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            {msg.role === 'assistant' && (
              <div className="bg-purple-100 rounded-full p-1.5 h-8 w-8 flex items-center justify-center flex-shrink-0">
                <Bot size={16} className="text-purple-600" />
              </div>
            )}
            <div className={`max-w-[80%] rounded-2xl px-4 py-3 text-sm whitespace-pre-wrap
              ${msg.role === 'user'
                ? 'bg-tennis-green text-white rounded-br-none'
                : 'bg-white border border-gray-100 shadow-sm rounded-bl-none'}`}>
              {msg.text}
            </div>
            {msg.role === 'user' && (
              <div className="bg-gray-100 rounded-full p-1.5 h-8 w-8 flex items-center justify-center flex-shrink-0">
                <User size={16} className="text-gray-600" />
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div className="flex gap-3 justify-start">
            <div className="bg-purple-100 rounded-full p-1.5 h-8 w-8 flex items-center justify-center">
              <Bot size={16} className="text-purple-600" />
            </div>
            <div className="bg-white border border-gray-100 shadow-sm rounded-2xl rounded-bl-none px-4 py-3">
              <span className="flex gap-1">
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay:'0ms'}}/>
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay:'150ms'}}/>
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay:'300ms'}}/>
              </span>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Suggestions */}
      {messages.length === 1 && (
        <div className="flex flex-wrap gap-2 mb-3">
          {SUGGESTIONS.map(s => (
            <button key={s} onClick={() => send(s)}
              className="text-xs bg-white border border-gray-200 hover:border-tennis-light rounded-full px-3 py-1.5 text-gray-600 hover:text-tennis-green transition-colors">
              {s}
            </button>
          ))}
        </div>
      )}

      {/* Input */}
      <form onSubmit={handleSubmit} className="flex gap-2">
        <input className="input-field flex-1" placeholder="Ask me anything about courts or bookings…"
          value={input} onChange={e => setInput(e.target.value)} disabled={loading} />
        <button type="submit" disabled={loading || !input.trim()} className="btn-primary px-4">
          <Send size={18} />
        </button>
      </form>
    </div>
  )
}
