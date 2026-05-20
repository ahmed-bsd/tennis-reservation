import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuth } from './AuthContext'

const WSContext = createContext(null)

export function WebSocketProvider({ children }) {
  const { user } = useAuth()
  const clientRef = useRef(null)
  const [notifications, setNotifications] = useState([])

  useEffect(() => {
    if (!user) return

    const stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/api/ws'),
      onConnect: () => {
        stompClient.subscribe(`/topic/user/${user.id}`, msg => {
          const data = JSON.parse(msg.body)
          setNotifications(prev => [{ ...data, id: Date.now() }, ...prev.slice(0, 9)])
        })
        stompClient.subscribe('/topic/discounts', msg => {
          const data = JSON.parse(msg.body)
          setNotifications(prev => [{ type: 'DISCOUNT', ...data, id: Date.now() }, ...prev.slice(0, 9)])
        })
      },
      reconnectDelay: 5000,
    })

    stompClient.activate()
    clientRef.current = stompClient

    return () => stompClient.deactivate()
  }, [user])

  const clearNotification = (id) =>
    setNotifications(prev => prev.filter(n => n.id !== id))

  return (
    <WSContext.Provider value={{ notifications, clearNotification }}>
      {children}
    </WSContext.Provider>
  )
}

export const useWebSocket = () => useContext(WSContext)
