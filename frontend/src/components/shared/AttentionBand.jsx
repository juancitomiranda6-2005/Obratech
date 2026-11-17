import { Link } from 'react-router-dom'

export function AttentionBand({ items = [], emptyMessage = 'Todo al día — no hay elementos urgentes' }) {
  if (!items.length) {
    return (
      <div className="client-attention-empty" role="status">
        <span className="material-symbols-outlined" aria-hidden="true">check_circle</span>
        <span>{emptyMessage}</span>
      </div>
    )
  }

  return (
    <section className="client-attention-band" aria-label="Requiere tu atención">
      <p className="client-attention-band-label">
        <span className="material-symbols-outlined" aria-hidden="true">notifications_active</span>
        Requiere tu atención
      </p>
      <ul className="client-attention-list" role="list">
        {items.map((item) => (
          <li className="client-attention-item" key={item.key}>
            <span className="material-symbols-outlined" aria-hidden="true">{item.icon}</span>
            <span className="client-attention-text">
              <strong>{item.label}</strong>
              {item.detail && <span>{item.detail}</span>}
            </span>
            {item.to && (
              item.to.startsWith('#') ? (
                <a className="client-attention-action" href={item.to}>
                  Ver
                  <span className="material-symbols-outlined" aria-hidden="true">chevron_right</span>
                </a>
              ) : (
                <Link className="client-attention-action" to={item.to}>
                  Ver
                  <span className="material-symbols-outlined" aria-hidden="true">chevron_right</span>
                </Link>
              )
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
