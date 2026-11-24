const libraryDate = new Intl.DateTimeFormat('pt-BR', { timeZone: 'America/Fortaleza' })

export function formatDate(value) {
  if (!value) return '—'
  const instant = value.length === 10 ? `${value}T12:00:00Z` : value
  return libraryDate.format(new Date(instant))
}
