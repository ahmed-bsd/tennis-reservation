import client from './client'

export const chatWithAI = message => client.post('/ai/chat', { message })
