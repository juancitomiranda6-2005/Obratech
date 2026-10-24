import { useState } from 'react'
import { Link } from 'react-router-dom'

const benefits = [
  {
    icon: 'task_alt',
    title: 'Control de tus obras',
    description: 'Organiza proyectos, responsables y avances desde un solo lugar.',
  },
  {
    icon: 'groups',
    title: 'Talento para cada proyecto',
    description: 'Conecta con clientes, contratistas y trabajadores de la construccion.',
  },
  {
    icon: 'monitoring',
    title: 'Avances a la vista',
    description: 'Consulta el estado de cada proyecto y mantén al equipo coordinado.',
  },
  {
    icon: 'description',
    title: 'Informacion organizada',
    description: 'Centraliza documentos, postulaciones y reportes de tu trabajo.',
  },
]

const steps = [
  { number: '01', title: 'Crea tu cuenta', description: 'Elige el perfil que mejor describe tu trabajo en la construccion.' },
  { number: '02', title: 'Encuentra tu oportunidad', description: 'Publica un proyecto, explora obras o busca profesionales.' },
  { number: '03', title: 'Coordina el trabajo', description: 'Organiza el equipo y sigue el progreso desde ObraTech.' },
]

export function LandingPage() {
  const [menuOpen, setMenuOpen] = useState(false)

  const closeMenu = () => setMenuOpen(false)

  return (
    <div className="landing-page">
      <header className="landing-nav">
        <div className="landing-nav-inner">
          <Link aria-label="ObraTech inicio" className="landing-brand" onClick={closeMenu} to="/">
            <img alt="" src="/img/logo3.png" />
            <span>Obra<span>Tech</span></span>
          </Link>
          <button
            aria-controls="landing-menu"
            aria-expanded={menuOpen}
            aria-label={menuOpen ? 'Cerrar menu' : 'Abrir menu'}
            className="landing-menu-button"
            onClick={() => setMenuOpen((open) => !open)}
            type="button"
          >
            <span className="material-symbols-outlined" aria-hidden="true">{menuOpen ? 'close' : 'menu'}</span>
          </button>
          <nav aria-label="Navegacion principal" className={`landing-nav-actions ${menuOpen ? 'is-open' : ''}`} id="landing-menu">
            <a href="#inicio" onClick={closeMenu}>Inicio</a>
            <a href="#beneficios" onClick={closeMenu}>Beneficios</a>
            <a href="#como-funciona" onClick={closeMenu}>Como funciona</a>
            <a href="#contacto" onClick={closeMenu}>Contacto</a>
            <Link className="landing-nav-login" onClick={closeMenu} to="/login">Iniciar sesion</Link>
            <Link className="landing-nav-register" onClick={closeMenu} to="/registro">Comenzar gratis</Link>
          </nav>
        </div>
      </header>

      <main className="landing-main">
        <section aria-labelledby="landing-title" className="landing-hero" id="inicio">
          <div aria-hidden="true" className="landing-hero-image" />
          <div className="landing-hero-content">
            <p className="landing-eyebrow">
              <span className="material-symbols-outlined" aria-hidden="true">construction</span>
              Gestion de obras, sin complicaciones
            </p>
            <h1 id="landing-title">Construye hoy<br />el futuro de tus <span>obras.</span></h1>
            <p className="landing-hero-copy">
              Organiza tus proyectos, encuentra profesionales y mantén a todo tu equipo en la misma pagina.
            </p>
            <div className="landing-hero-actions">
              <Link className="landing-button landing-button-primary" to="/registro">
                Comenzar ahora
                <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
              </Link>
              <a className="landing-button landing-button-secondary" href="#beneficios">
                Conoce la plataforma
                <span className="material-symbols-outlined" aria-hidden="true">expand_more</span>
              </a>
            </div>
            <div className="landing-trust-points">
              <span><span className="material-symbols-outlined" aria-hidden="true">check_circle</span>Gestiona desde cualquier dispositivo</span>
              <span><span className="material-symbols-outlined" aria-hidden="true">check_circle</span>Personas y proyectos en un solo lugar</span>
            </div>
          </div>
          <div aria-label="Vista previa del control de proyectos" className="landing-project-preview" role="img">
            <div className="landing-preview-top">
              <span className="landing-preview-brand"><span className="material-symbols-outlined" aria-hidden="true">construction</span> ObraTech</span>
              <span className="landing-preview-status"><span /> Proyecto activo</span>
            </div>
            <div className="landing-preview-heading">
              <div><span className="landing-preview-label">PANEL DEL PROYECTO</span><strong>Construccion residencial</strong></div>
              <span className="material-symbols-outlined" aria-hidden="true">more_horiz</span>
            </div>
            <div className="landing-preview-progress">
              <div><span>Progreso de la obra</span><strong>68%</strong></div>
              <span className="landing-progress-track"><span /></span>
            </div>
            <div className="landing-preview-tasks">
              <div><span className="landing-task-icon task-orange material-symbols-outlined" aria-hidden="true">architecture</span><span><strong>Cimentacion</strong><small>Etapa completada</small></span><span className="material-symbols-outlined task-complete" aria-hidden="true">check_circle</span></div>
              <div><span className="landing-task-icon task-blue material-symbols-outlined" aria-hidden="true">apartment</span><span><strong>Estructura</strong><small>En progreso</small></span><span className="task-live">EN CURSO</span></div>
              <div><span className="landing-task-icon task-purple material-symbols-outlined" aria-hidden="true">format_paint</span><span><strong>Acabados</strong><small>Siguiente etapa</small></span><span className="material-symbols-outlined task-pending" aria-hidden="true">schedule</span></div>
            </div>
            <div className="landing-preview-footer">
              <span className="landing-avatar-stack"><span>CA</span><span>MR</span><span>+2</span></span>
              <span>Equipo del proyecto</span>
              <span className="material-symbols-outlined" aria-hidden="true">arrow_outward</span>
            </div>
          </div>
          <a aria-label="Descubrir beneficios" className="landing-scroll-cue" href="#beneficios">
            <span className="material-symbols-outlined" aria-hidden="true">keyboard_arrow_down</span>
          </a>
        </section>

        <section aria-label="Herramientas principales" className="landing-benefit-strip" id="beneficios">
          {benefits.map((benefit) => (
            <article className="landing-benefit" key={benefit.title}>
              <span className="material-symbols-outlined landing-benefit-icon" aria-hidden="true">{benefit.icon}</span>
              <div><h2>{benefit.title}</h2><p>{benefit.description}</p></div>
            </article>
          ))}
        </section>

        <section className="landing-overview">
          <div className="landing-overview-image">
            <img alt="Plano arquitectonico que representa la planeacion de un proyecto de construccion" loading="lazy" src="/img/image.png" />
            <div className="landing-image-caption">
              <span className="material-symbols-outlined" aria-hidden="true">location_on</span>
              <span><strong>Tu obra, en perspectiva</strong><small>Planeacion clara desde el primer dia</small></span>
            </div>
          </div>
          <div className="landing-overview-copy">
            <p className="landing-eyebrow">Una mejor forma de construir</p>
            <h2>Todo tu proyecto.<br /><span>Un solo aliado.</span></h2>
            <p>ObraTech acerca a quienes hacen posible cada obra. Publica oportunidades, forma tu equipo y conserva la informacion clave organizada en una plataforma intuitiva.</p>
            <ul>
              <li><span className="material-symbols-outlined" aria-hidden="true">check_circle</span>Encuentra personas para tu equipo</li>
              <li><span className="material-symbols-outlined" aria-hidden="true">check_circle</span>Consulta el progreso de tus proyectos</li>
              <li><span className="material-symbols-outlined" aria-hidden="true">check_circle</span>Mantén documentos y reportes a mano</li>
            </ul>
            <Link className="landing-text-link" to="/registro">Explora ObraTech <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>
          </div>
        </section>

        <section aria-labelledby="landing-steps-title" className="landing-how" id="como-funciona">
          <div className="landing-section-heading">
            <p className="landing-eyebrow">Empieza en pocos pasos</p>
            <h2 id="landing-steps-title">De la idea a la obra, <span>conectados.</span></h2>
            <p>Una plataforma pensada para hacer mas sencilla la colaboracion en cada proyecto.</p>
          </div>
          <div className="landing-step-grid">
            {steps.map((step) => (
              <article className="landing-step" key={step.number}>
                <span className="landing-step-number">{step.number}</span>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="landing-cta">
          <div>
            <p className="landing-eyebrow">El siguiente paso empieza aqui</p>
            <h2>Haz que cada proyecto avance mejor.</h2>
            <p>Unete a ObraTech y organiza tu proxima obra desde un solo lugar.</p>
          </div>
          <Link className="landing-button landing-button-primary" to="/registro">
            Crear mi cuenta
            <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
          </Link>
        </section>
      </main>

      <footer className="landing-footer" id="contacto">
        <div className="landing-footer-grid">
          <section className="landing-footer-about">
            <Link aria-label="ObraTech inicio" className="landing-brand" to="/">
              <img alt="" src="/img/logo3.png" />
              <span>Obra<span>Tech</span></span>
            </Link>
            <p>La plataforma que conecta personas, proyectos y oportunidades en la industria de la construccion.</p>
          </section>
          <section>
            <h2>Explora</h2>
            <a href="#beneficios">Beneficios</a>
            <a href="#como-funciona">Como funciona</a>
            <Link to="/registro">Crear una cuenta</Link>
          </section>
          <section>
            <h2>Contacto</h2>
            <a href="mailto:canatemiranda2023@gmail.com">canatemiranda2023@gmail.com</a>
            <a href="tel:+573246072562">324 607 2562</a>
            <span>Cartagena de Indias, Colombia</span>
          </section>
        </div>
        <div className="landing-footer-bottom">
          <span>© 2026 ObraTech. Todos los derechos reservados.</span>
          <span>Construye en equipo.</span>
        </div>
      </footer>
    </div>
  )
}
