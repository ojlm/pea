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
  done?: number
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

export interface JobWorkerStatus {
  status?: string
  errMsg?: string
  runId?: string
  start?: number
  end?: number
  code?: number
  label?: string
  oshi?: Oshi
}

export interface ReporterJobStatus {
  status?: string
  runId?: string
  start?: number
  end?: number
  workers?: { [k: string]: JobWorkerStatus }
  load?: LoadJob & UnionLoadMessage
}

export interface SimulationModel {
  name?: string
  protocols?: string[]
  description?: string
}

export interface Simulations {
  last?: number
  editorBaseUrl?: string
  simulations?: SimulationModel[]
}

export interface SingleRequest {
  name?: string
  url?: string
  method?: string
  headers?: Object
  body?: string
}

export interface DurationParam {
  value?: number
  unit?: string
}

export interface Injection {
  type?: string
  users?: number
  from?: number
  to?: number
  duration?: DurationParam
  times?: number
  eachLevelLasting?: DurationParam
  separatedByRampsLasting?: DurationParam
}

export interface LoadMessage {
  simulationId?: string
  start?: number
  report?: boolean
  verbose?: boolean
  type?: string
}

export interface SingleJob {
  worker?: PeaMember
  load?: UnionLoadMessage
}

export interface FinishedCallbackRequest {
  url?: string
  ext?: object
}

export interface LoadJob {
  workers?: PeaMember[]
  load?: UnionLoadMessage
  jobs?: SingleJob[]
  report?: boolean
  simulationId?: string
  start?: number
  type?: string
  callback?: FinishedCallbackRequest
  ext?: object
}

export interface SingleHttpScenarioMessage extends LoadMessage {
  name?: string
  request?: SingleRequest
  injections?: Injection[]
  feeder?: FeederParam
  loop?: LoopParam
  maxDuration?: DurationParam
  assertions?: HttpAssertionParam
  throttle?: ThrottleParam
}

export interface RunScriptMessage extends LoadMessage {
  simulation?: string
}

export interface RunProgramMessage extends LoadMessage {
  program?: string
}

export type UnionLoadMessage = SingleHttpScenarioMessage & RunScriptMessage & RunProgramMessage

export interface SingleHttpScenarioJob extends LoadJob {
}

export interface RunScriptJob extends LoadJob {
}

export interface RunProgramJob extends LoadJob {
}

export interface WorkersAvailable {
  available?: boolean
  errors?: { [k: string]: string }
  runId?: string
}

export interface ResourceInfo {
  filename?: string
  exists?: boolean
  isDirectory?: boolean
  size?: number
  modified?: number
  md5?: string
}

export interface FeederParam {
  type?: 'csv' | 'json'
  path?: string
}

export interface LoopParam {
  forever?: boolean
  repeat?: number
}

export interface AssertionItem {
  op?: string
  path?: string
  expect?: object
}

export interface AssertionsParam {
  list?: AssertionItem[]
}

export interface HttpAssertionParam {
  status?: AssertionsParam
  header?: AssertionsParam
  body?: AssertionsParam
}

export interface ThrottleStep {
  type?: string
  rps?: number
  duration?: DurationParam
}

export interface ThrottleParam {
  steps?: ThrottleStep[]
}

export interface Oshi {
  os?: string
  'memory.total'?: number
  'memory.available'?: number
  'cpu.name'?: number
  'cpu.physical.processor.count'?: number
  'cpu.logical.processor.count'?: number
}

export const TimeUnit = {
  TIME_UNIT_MILLI: 'milli',
  TIME_UNIT_SECOND: 'second',
  TIME_UNIT_MINUTE: 'minute',
  TIME_UNIT_HOUR: 'hour',
}

export const OpenInjectionType = {
  TYPE_NOTHING_FOR: 'nothingFor',
  TYPE_AT_ONCE_USERS: 'atOnceUsers',
  TYPE_RAMP_USERS: 'rampUsers',
  TYPE_CONSTANT_USERS_PER_SEC: 'constantUsersPerSec',
  TYPE_RAMP_USERS_PER_SEC: 'rampUsersPerSec',
  TYPE_HEAVISIDE_USERS: 'heavisideUsers',
  TYPE_INCREMENT_USERS_PER_SEC: 'incrementUsersPerSec',
}

export const ClosedInjectionType = {
  TYPE_CONSTANT_CONCURRENT_USERS: 'constantConcurrentUsers',
  TYPE_RAMP_CONCURRENT_USERS: 'rampConcurrentUsers',
  TYPE_INCREMENT_CONCURRENT_USERS: 'incrementConcurrentUsers',
}

export const HttpMethods = {
  OPTIONS: 'OPTIONS',
  GET: 'GET',
  HEAD: 'HEAD',
  POST: 'POST',
  PUT: 'PUT',
  PATCH: 'PATCH',
  DELETE: 'DELETE',
  TRACE: 'TRACE',
}

export const MemberStatus = {
  IDLE: 'idle',
  RUNNING: 'running',
  IIL: 'ill',
  GATHERING: 'gathering',
  REPORTING: 'reporting',
  FINISHED: 'finished',
}

export const LoadTypes = {
  SINGLE: 'single',
  SCRIPT: 'script',
  PROGRAM: 'program',
}

export const ThrottleTypes = {
  REACH: 'reach',
  HOLD: 'hold',
  JUMP: 'jump',
}

export const StatusColors = {
  idle: 'darkslategray',
  running: 'darkcyan',
  reporting: 'darkgoldenrod',
  finished: 'darkmagenta',
  ill: 'darkred',
  gathering: 'darkorchid'
}

export const WorkloadModels = {
  OPEN: 'open',
  CLOSED: 'closed'
}
