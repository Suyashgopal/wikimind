import { useState } from 'react'
import * as api from './api'
import { useAuth } from './AuthContext'

function validate(mode, { username, password, organization }) {
  const errors = {}
  if (mode === 'login') {
    if (!username.trim()) errors.username = 'Enter your username.'
    if (!password) errors.password = 'Enter your password.'
    return errors
  }
  if (username.trim().length < 3 || username.trim().length > 50) {
    errors.username = 'Username must be 3 to 50 characters.'
  }
  if (password.length < 8) errors.password = 'Password must be at least 8 characters.'
  if (!organization.trim()) errors.organization = 'Enter your organization.'
  return errors
}

function Field({ id, label, error, ...inputProps }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} {...inputProps} />
      {error && <p id={`${id}-error`} className="field-error">{error}</p>}
    </div>
  )
}

export default function AuthForm() {
  const { login } = useAuth()
  const [mode, setMode] = useState('login')
  const [values, setValues] = useState({ username: '', password: '', organization: '' })
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [busy, setBusy] = useState(false)
  const isRegister = mode === 'register'

  const bind = (name) => ({
    value: values[name],
    onChange: (event) => setValues({ ...values, [name]: event.target.value }),
  })

  function switchMode() {
    setMode(isRegister ? 'login' : 'register')
    setErrors({})
    setFormError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError('')
    const found = validate(mode, values)
    setErrors(found)
    if (Object.keys(found).length > 0) return

    setBusy(true)
    const username = values.username.trim()
    try {
      const auth = isRegister
        ? await api.register(username, values.password, values.organization.trim())
        : await api.login(username, values.password)
      login(username, auth)
    } catch (error) {
      setFormError(error.message)
      setBusy(false)
    }
  }

  return (
    <main className="auth">
      <h1>WikiMind</h1>
      <p className="muted">Ask questions and get answers from your organization's documents.</p>
      <form className="card" onSubmit={handleSubmit} noValidate>
        <h2>{isRegister ? 'Create an account' : 'Log in'}</h2>
        <Field id="username" label="Username" autoComplete="username" error={errors.username} {...bind('username')} />
        <Field
          id="password"
          label="Password"
          type="password"
          autoComplete={isRegister ? 'new-password' : 'current-password'}
          error={errors.password}
          {...bind('password')}
        />
        {isRegister && (
          <Field id="organization" label="Organization" autoComplete="organization" error={errors.organization} {...bind('organization')} />
        )}
        {formError && <p role="alert" className="alert">{formError}</p>}
        <button type="submit" disabled={busy}>
          {busy ? 'Please wait...' : isRegister ? 'Create account' : 'Log in'}
        </button>
        <button type="button" className="link" onClick={switchMode}>
          {isRegister ? 'Already have an account? Log in' : 'New here? Create an account'}
        </button>
      </form>
    </main>
  )
}
