export interface PeaMember {
  address?: string
  port?: number
  hostname?: string
}

export interface MonitorData {
  complete?: boolean
  start?: number
  run?: number
  total?: TotalCounters
  users?: { [k: string]: PeaUserCounters }
  requests?: { [k: string]: RequestCounters }
  global?: RequestCounters
  errors?: { [k: string]: number }
}

export interface TotalCounters {
  total?: number
  waiting?: number
  active?: number
}

export interface PeaUserCounters {
  total?: number
  active?: number
  done?: number
  waiting?: number
  percent?: number
}

export interface RequestCounters {
  successfulCount?: number
  failedCount?: number
}
