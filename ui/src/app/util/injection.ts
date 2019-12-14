import { TranslateService } from '@ngx-translate/core'

import { ClosedInjectionType, Injection, OpenInjectionType } from '../model/pea.model'

export function injectionShowUsers(injection: Injection) {
  switch (injection.type) {
    case OpenInjectionType.TYPE_NOTHING_FOR:
    case OpenInjectionType.TYPE_RAMP_USERS_PER_SEC:
    case ClosedInjectionType.TYPE_RAMP_CONCURRENT_USERS:
      return false
    default:
      return true
  }
}

export function injectionShowFrom(injection: Injection) {
  switch (injection.type) {
    case OpenInjectionType.TYPE_RAMP_USERS_PER_SEC:
    case OpenInjectionType.TYPE_INCREMENT_USERS_PER_SEC:
    case ClosedInjectionType.TYPE_RAMP_CONCURRENT_USERS:
    case ClosedInjectionType.TYPE_INCREMENT_CONCURRENT_USERS:
      return true
    default:
      return false
  }
}

export function injectionShowTo(injection: Injection) {
  switch (injection.type) {
    case OpenInjectionType.TYPE_RAMP_USERS_PER_SEC:
    case ClosedInjectionType.TYPE_RAMP_CONCURRENT_USERS:
      return true
    default:
      return false
  }
}

export function injectionShowDuration(injection: Injection) {
  switch (injection.type) {
    case OpenInjectionType.TYPE_AT_ONCE_USERS:
    case OpenInjectionType.TYPE_INCREMENT_USERS_PER_SEC:
    case ClosedInjectionType.TYPE_INCREMENT_CONCURRENT_USERS:
      return false
    default:
      return true
  }
}

export function injectionShowTimes(injection: Injection) {
  switch (injection.type) {
    case OpenInjectionType.TYPE_INCREMENT_USERS_PER_SEC:
    case ClosedInjectionType.TYPE_INCREMENT_CONCURRENT_USERS:
      return true
    default:
      return false
  }
}

export function injectionShowEachLevelLasting(injection: Injection) {
  switch (injection.type) {
    case OpenInjectionType.TYPE_INCREMENT_USERS_PER_SEC:
    case ClosedInjectionType.TYPE_INCREMENT_CONCURRENT_USERS:
      return true
    default:
      return false
  }
}

export function injectionShowSeparatedByRampsLasting(injection: Injection) {
  switch (injection.type) {
    case OpenInjectionType.TYPE_INCREMENT_USERS_PER_SEC:
    case ClosedInjectionType.TYPE_INCREMENT_CONCURRENT_USERS:
      return true
    default:
      return false
  }
}

export function copyCleanInjection(injection: Injection): Injection {
  switch (injection.type) {
    case OpenInjectionType.TYPE_NOTHING_FOR:
      return { type: injection.type, duration: { ...injection.duration } }
    case OpenInjectionType.TYPE_AT_ONCE_USERS:
      return { type: injection.type, users: injection.users }
    case OpenInjectionType.TYPE_RAMP_USERS:
      return { type: injection.type, users: injection.users, duration: { ...injection.duration } }
    case OpenInjectionType.TYPE_CONSTANT_USERS_PER_SEC:
      return { type: injection.type, users: injection.users, duration: { ...injection.duration } }
    case OpenInjectionType.TYPE_RAMP_USERS_PER_SEC:
      return { type: injection.type, from: injection.from, to: injection.to, duration: { ...injection.duration } }
    case OpenInjectionType.TYPE_HEAVISIDE_USERS:
      return { type: injection.type, users: injection.users, duration: { ...injection.duration } }
    case OpenInjectionType.TYPE_INCREMENT_USERS_PER_SEC:
      return { type: injection.type, users: injection.users, times: injection.times, eachLevelLasting: injection.eachLevelLasting, separatedByRampsLasting: injection.separatedByRampsLasting, from: injection.from }
    case ClosedInjectionType.TYPE_CONSTANT_CONCURRENT_USERS:
      return { type: injection.type, users: injection.users, duration: { ...injection.duration } }
    case ClosedInjectionType.TYPE_RAMP_CONCURRENT_USERS:
      return { type: injection.type, from: injection.from, to: injection.to, duration: { ...injection.duration } }
    case ClosedInjectionType.TYPE_INCREMENT_CONCURRENT_USERS:
      return { type: injection.type, users: injection.users, times: injection.times, eachLevelLasting: injection.eachLevelLasting, separatedByRampsLasting: injection.separatedByRampsLasting, from: injection.from }
  }
}

export function injectionSumText(injection: Injection, i18nService: TranslateService) {
  const keyUsers = 'item.injectionUsers'
  const keyDuration = 'item.duration'
  const keyFrom = 'item.injectionFrom'
  const keyTo = 'item.injectionTo'
  const keyTimes = 'item.injectionTimes'
  const keyEachLevelLasting = 'item.eachLevelLasting'
  const keySeparatedByRampsLasting = 'item.separatedByRampsLasting'
  switch (injection.type) {
    case OpenInjectionType.TYPE_NOTHING_FOR:
      return `${injection.duration.value} ${i18nService.instant(`time.${injection.duration.unit}`)}.`
    case OpenInjectionType.TYPE_AT_ONCE_USERS:
      return `${i18nService.instant(keyUsers)}: ${injection.users}`
    case OpenInjectionType.TYPE_RAMP_USERS:
      return `${i18nService.instant(keyUsers)}: ${injection.users}, ${i18nService.instant(keyDuration)}: ${injection.duration.value} ${i18nService.instant(`time.${injection.duration.unit}`)}.`
    case OpenInjectionType.TYPE_CONSTANT_USERS_PER_SEC:
      return `${i18nService.instant(keyUsers)}: ${injection.users}, ${i18nService.instant(keyDuration)}: ${injection.duration.value} ${i18nService.instant(`time.${injection.duration.unit}`)}.`
    case OpenInjectionType.TYPE_RAMP_USERS_PER_SEC:
      return `${i18nService.instant(keyUsers)}${i18nService.instant(keyFrom)} ${injection.from} ${i18nService.instant(keyTo)} ${injection.to} ${i18nService.instant(keyDuration)}: ${injection.duration.value} ${i18nService.instant(`time.${injection.duration.unit}`)}.`
    case OpenInjectionType.TYPE_HEAVISIDE_USERS:
      return `${i18nService.instant(keyUsers)}: ${injection.users}, ${i18nService.instant(keyDuration)}: ${injection.duration.value} ${i18nService.instant(`time.${injection.duration.unit}`)}.`
    case ClosedInjectionType.TYPE_CONSTANT_CONCURRENT_USERS:
      return `${i18nService.instant(keyUsers)}: ${injection.users}, ${i18nService.instant(keyDuration)}: ${injection.duration.value} ${i18nService.instant(`time.${injection.duration.unit}`)}.`
    case ClosedInjectionType.TYPE_RAMP_CONCURRENT_USERS:
      return `${i18nService.instant(keyUsers)}${i18nService.instant(keyFrom)} ${injection.from} ${i18nService.instant(keyTo)} ${injection.to} ${i18nService.instant(keyDuration)}: ${injection.duration.value} ${i18nService.instant(`time.${injection.duration.unit}`)}.`
    case OpenInjectionType.TYPE_INCREMENT_USERS_PER_SEC:
    case ClosedInjectionType.TYPE_INCREMENT_CONCURRENT_USERS:
      return `${i18nService.instant(keyUsers)}: ${injection.users}, ${i18nService.instant(keyTimes)}: ${injection.times}, ${i18nService.instant(keyEachLevelLasting)}: ${injection.eachLevelLasting.value} ${i18nService.instant(`time.${injection.eachLevelLasting.unit}`)}, ${i18nService.instant(keySeparatedByRampsLasting)}: ${injection.separatedByRampsLasting.value} ${i18nService.instant(`time.${injection.separatedByRampsLasting.unit}`)}, ${i18nService.instant(keyFrom)}: ${injection.from}`
  }
}
