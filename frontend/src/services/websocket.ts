type MessageHandler = (data: any) => void

class WebSocketService {
  private ws: WebSocket | null = null
  private handlers: Map<string, MessageHandler[]> = new Map()
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private ticket: string = ''
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null

  connect(ticket: string) {
    this.ticket = ticket
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url = `${protocol}//${location.host}/ws/chat/${ticket}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      console.log('WebSocket connected')
      this.startHeartbeat()
    }

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        const type = msg.type
        const listeners = this.handlers.get(type) ?? []
        listeners.forEach((h) => h(msg))
      } catch (e) {
        console.error('WebSocket message parse error', e)
      }
    }

    this.ws.onclose = (event) => {
      console.log('WebSocket closed', event.code)
      this.stopHeartbeat()
      if (!event.wasClean) {
        this.reconnectTimer = setTimeout(() => this.connect(this.ticket), 3000)
      }
    }

    this.ws.onerror = (err) => {
      console.error('WebSocket error', err)
    }
  }

  private startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      this.send('ping.request', {})
    }, 30000)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  on(type: string, handler: MessageHandler) {
    const existing = this.handlers.get(type) ?? []
    existing.push(handler)
    this.handlers.set(type, existing)
  }

  off(type: string, handler: MessageHandler) {
    const existing = this.handlers.get(type) ?? []
    this.handlers.set(type, existing.filter(h => h !== handler))
  }

  send(type: string, data: any) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type, data }))
    }
  }

  disconnect() {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.ws?.close()
    this.ws = null
  }
}

export const wsService = new WebSocketService()
