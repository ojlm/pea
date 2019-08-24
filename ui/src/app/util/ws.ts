export function newWS(path: string, host: string = null): WebSocket {
  let url: string
  if (location.protocol.startsWith('https')) {
    url = `wss://${host || location.host}${path}`
  } else {
    url = `ws://${host || location.host}${path}`
  }
  return new WebSocket(url)
}
