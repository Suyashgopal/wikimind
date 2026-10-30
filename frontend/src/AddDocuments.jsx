import { useState } from 'react'
import * as api from './api'

export const MAX_FILE_BYTES = 100 * 1024

const chunkLabel = (count) => `${count} ${count === 1 ? 'chunk' : 'chunks'} stored`

export default function AddDocuments() {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [pasteStatus, setPasteStatus] = useState(null)
  const [pasteBusy, setPasteBusy] = useState(false)
  const [uploads, setUploads] = useState([])
  const [uploading, setUploading] = useState(false)

  async function handlePaste(event) {
    event.preventDefault()
    setPasteBusy(true)
    setPasteStatus(null)
    try {
      const result = await api.addDocument(title.trim(), content)
      setPasteStatus({ ok: true, text: `"${result.title}": ${chunkLabel(result.chunksStored)}` })
      setTitle('')
      setContent('')
    } catch (error) {
      setPasteStatus({ ok: false, text: error.message })
    } finally {
      setPasteBusy(false)
    }
  }

  async function handleFiles(event) {
    const files = [...event.target.files]
    event.target.value = ''
    if (files.length === 0) return

    const update = (index, change) =>
      setUploads((rows) => rows.map((row, i) => (i === index ? { ...row, ...change } : row)))

    setUploads(files.map((file) =>
      file.size > MAX_FILE_BYTES
        ? { name: file.name, state: 'failed', text: 'Too large: the limit is 100 KB' }
        : { name: file.name, state: 'waiting', text: 'Waiting' },
    ))
    setUploading(true)

    // One at a time: each chunk is a Gemini call, and parallel uploads would hit rate limits.
    for (const [index, file] of files.entries()) {
      if (file.size > MAX_FILE_BYTES) continue
      update(index, { state: 'uploading', text: 'Uploading...' })
      try {
        const result = await api.addDocument(file.name, await file.text())
        update(index, { state: 'done', text: chunkLabel(result.chunksStored) })
      } catch (error) {
        update(index, { state: 'failed', text: error.message })
      }
    }
    setUploading(false)
  }

  return (
    <section>
      <h2>Add documents</h2>

      <div className="card">
        <h3>Upload files</h3>
        <label htmlFor="files">Choose .md or .txt files, up to 100 KB each</label>
        <input id="files" type="file" accept=".md,.txt" multiple disabled={uploading} onChange={handleFiles} />
        {uploads.length > 0 && (
          <ul className="uploads" aria-live="polite">
            {uploads.map((row, index) => (
              <li key={`${row.name}-${index}`} className={row.state}>
                <span className="upload-name">{row.name}</span>
                <span>{row.text}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      <form className="card" onSubmit={handlePaste}>
        <h3>Or paste text</h3>
        <div className="field">
          <label htmlFor="title">Title</label>
          <input id="title" value={title} onChange={(event) => setTitle(event.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="content">Content</label>
          <textarea id="content" rows={8} value={content} onChange={(event) => setContent(event.target.value)} />
        </div>
        {pasteStatus && (
          <p role={pasteStatus.ok ? 'status' : 'alert'} className={pasteStatus.ok ? 'success' : 'alert'}>
            {pasteStatus.text}
          </p>
        )}
        <button type="submit" disabled={pasteBusy || !title.trim() || !content.trim()}>
          {pasteBusy ? 'Saving...' : 'Save document'}
        </button>
      </form>
    </section>
  )
}
