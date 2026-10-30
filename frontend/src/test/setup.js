import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'
import { setToken } from '../api'

afterEach(() => {
  cleanup()
  setToken(null)
  sessionStorage.clear()
})
