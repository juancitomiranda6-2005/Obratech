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
    description: 'Conecta con clientes, contratistas y trabajadores de la construcción.',
  },
  {
    icon: 'monitoring',
    title: 'Avances a la vista',
    description: 'Consulta el estado de cada proyecto y mantén al equipo coordinado.',
  },
  {
    icon: 'description',
    title: 'Información organizada',
    description: 'Centraliza documentos, postulaciones y reportes de tu trabajo.',
  },
]

const problemSolutions = [
  {
    dato: '79%',
    texto: 'de los trabajadores de la construcción de vivienda en Colombia tuvo empleo informal en 2024 (946 mil ocupados), sin historial que valide su experiencia.',
    fuente: 'Camacol, Radiografía del empleo informal en la construcción de vivienda (jun. 2025)',
    url: 'https://camacol.co/sites/default/files/descargables/Datos%20que%20construyen-09jun2025.pdf',
    solucionTitulo: 'Perfiles verificables',
    solucionTexto: 'Contratistas y trabajadores muestran su especialidad, experiencia y calificaciones en un perfil público. Tú decides con información, no a ciegas.',
  },
  {
    dato: '16,2%',
    texto: 'de las muertes laborales en Colombia en 2024 ocurrieron en construcción (66 de 408), el segundo sector más afectado. Sin registro claro de quién trabaja y cómo, el respaldo falla.',
    fuente: 'Consejo Colombiano de Seguridad / Fasecolda, siniestralidad 2024 (vía El Colombiano)',
    url: 'https://www.elcolombiano.com/empleos/contenidos/muertes-accidentes-laborales-colombia-2024-NG27035594',
    solucionTitulo: 'Historial y calificaciones',
    solucionTexto: 'Cada proyecto terminado queda en el historial y el cliente califica al contratista. La reputación se construye con hechos y queda registrada.',
  },
  {
    dato: '98%',
    texto: 'de los megaproyectos de construcción supera en más de 30% su presupuesto y 77% termina con al menos 40% de retraso.',
    fuente: 'McKinsey, The construction productivity imperative (megaproyectos de más de USD 1.000 millones)',
    url: 'https://www.mckinsey.com/ph/our-insights/the-construction-productivity-imperative',
    solucionTitulo: 'Todo el proyecto en un solo lugar',
    solucionTexto: 'Publica tu obra, recibe postulaciones, arma equipos y consulta reportes de avance sin depender de hojas sueltas ni llamadas.',
  },
  {
    dato: '-5,4%',
    texto: 'cayó la construcción en Colombia en el primer trimestre de 2026, mientras la economía creció 2,2%. Con menos obras, contratistas y trabajadores compiten por menos proyectos.',
    fuente: 'DANE / Camacol (vía Portafolio), 1T 2026',
    url: 'https://www.portafolio.co/mis-finanzas/vivienda/construccion-en-colombia-paso-de-crecer-5-1-en-2022-a-caer-5-4-en-2026-en-el-primer-trimestre-del-ano-494135',
    solucionTitulo: 'Visibilidad y postulaciones',
    solucionTexto: 'Directorio por especialidad y proyectos disponibles para postularte: que te encuentren y encuentres obra desde cualquier dispositivo.',
  },
]

export function LandingPage() {
  const [menuOpen, setMenuOpen] = useState(false)

  const closeMenu = () => setMenuOpen(false)

  return (
    <div className="landing-page">
      <header className="landing-nav">
        <div className="landing-nav-inner">
          <Link aria-label="ObraTech inicio" className="landing-brand" onClick={closeMenu} to="/">
            <img alt="ObraTech" src="/images/obratech-logo.png" style={{ width: 127, height: 36 }} />
          </Link>
          <button
            aria-controls="landing-menu"
            aria-expanded={menuOpen}
            aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
            className="landing-menu-button"
            onClick={() => setMenuOpen((open) => !open)}
            type="button"
          >
            <span className="material-symbols-outlined" aria-hidden="true">{menuOpen ? 'close' : 'menu'}</span>
          </button>
          <nav aria-label="Navegación principal" className={`landing-nav-actions ${menuOpen ? 'is-open' : ''}`} id="landing-menu">
            <a href="#inicio" onClick={closeMenu}>Inicio</a>
            <a href="#beneficios" onClick={closeMenu}>Beneficios</a>
            <a href="#como-funciona" onClick={closeMenu}>Cómo funciona</a>
            <a href="#contacto" onClick={closeMenu}>Contacto</a>
            <Link className="landing-nav-login" onClick={closeMenu} to="/login">Iniciar sesión</Link>
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
              Gestión de obras, sin complicaciones
            </p>
            <h1 id="landing-title">Construye hoy<br /> el futuro de tus <span>obras.</span></h1>
            <p className="landing-hero-copy">
              Organiza tus proyectos, encuentra profesionales y mantén a todo tu equipo en la misma página.
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
              <span className="landing-preview-status"><span /> Proyecto de ejemplo</span>
            </div>
            <div className="landing-preview-heading">
              <div><span className="landing-preview-label">PANEL DEL PROYECTO</span><strong>Construcción Residencial </strong></div>
              <span className="material-symbols-outlined" aria-hidden="true">more_horiz</span>
            </div>
            <div className="landing-preview-progress">
              <div><span>Progreso del proyecto</span><strong>En curso</strong></div>
              <span className="landing-progress-track"><span /></span>
            </div>
            <div className="landing-preview-tasks">
              <div><span className="landing-task-icon task-orange material-symbols-outlined" aria-hidden="true">architecture</span><span><strong>Cimentación</strong><small>Etapa completada</small></span><span className="material-symbols-outlined task-complete" aria-hidden="true">check_circle</span></div>
              <div><span className="landing-task-icon task-blue material-symbols-outlined" aria-hidden="true">apartment</span><span><strong>Estructura</strong><small>En progreso</small></span><span className="task-live">EN CURSO</span></div>
              <div><span className="landing-task-icon task-purple material-symbols-outlined" aria-hidden="true">format_paint</span><span><strong>Acabados</strong><small>Siguiente etapa</small></span><span className="material-symbols-outlined task-pending" aria-hidden="true">schedule</span></div>
            </div>
            <div className="landing-preview-footer">
              <span className="landing-avatar-stack"><span>CA</span><span>MR</span></span>
              <span>Equipo del proyecto</span>
              <span className="material-symbols-outlined" aria-hidden="true">arrow_outward</span>
            </div>
          </div>
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
            <img alt="Plano arquitectonico con los detalles de un proyecto de construccion" className="landing-overview-main-photo" loading="lazy" src="/images/obra-planos.jpg" />
            <div className="landing-image-caption">
              <span className="material-symbols-outlined" aria-hidden="true">architecture</span>
              <span><strong>Tu obra, en perspectiva</strong><small>Planeación clara desde el primer día</small></span>
            </div>
            <figure className="landing-team-card">
              <img alt="Equipo de construcción trabajando en la estructura de una obra" loading="lazy" src="/images/obra-equipo.jpg" />
              <figcaption>
                <span className="material-symbols-outlined" aria-hidden="true">groups</span>
                <span><strong>Un equipo conectado</strong><small>Personas y progreso en un solo lugar</small></span>
              </figcaption>
            </figure>
          </div>
          <div className="landing-overview-copy">
            <p className="landing-eyebrow">Una mejor forma de construir</p>
            <h2>Todo tu proyecto.<br /><span>Un solo aliado.</span></h2>
            <p>ObraTech acerca a quienes hacen posible cada obra. Publica oportunidades, forma tu equipo y conserva la información clave organizada en una plataforma intuitiva.</p>
            <ul>
              <li><span className="material-symbols-outlined" aria-hidden="true">check_circle</span>Encuentra personas para tu equipo</li>
              <li><span className="material-symbols-outlined" aria-hidden="true">check_circle</span>Consulta el progreso de tus proyectos</li>
              <li><span className="material-symbols-outlined" aria-hidden="true">check_circle</span>Mantén documentos y reportes a mano</li>
            </ul>
            <Link className="landing-text-link" to="/registro">Explora ObraTech <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>
          </div>
        </section>

        <section aria-labelledby="landing-steps-title" className="landing-how" id="como-funciona">
          <img alt="" aria-hidden="true" className="landing-how-accent" loading="lazy" src="/images/obra-grua.jpg" />
          <div className="landing-section-heading">
            <p className="landing-eyebrow">Los retos de la construcción</p>
            <h2 id="landing-steps-title">Problemas reales, <span>soluciones concretas.</span></h2>
            <p>Conoce algunos de los desafíos del sector y cómo ObraTech ayuda a enfrentarlos.</p>
          </div>
          <div className="landing-step-grid">
            {problemSolutions.map((item) => (
              <article className="landing-step" key={item.dato}>
                <div className="landing-problem-card">
                  <span className="landing-solution-label">EL PROBLEMA</span>
                  <strong className="landing-problem-stat">{item.dato}</strong>
                  <p>{item.texto}</p>
                  <a className="landing-problem-source" href={item.url} rel="noreferrer" target="_blank">
                    {item.fuente}
                  </a>
                </div>
                <div aria-hidden="true" className="landing-step-connector">
                  <span className="material-symbols-outlined">arrow_forward</span>
                </div>
                <div className="landing-solution-card">
                  <div className="landing-solution-heading">
                    <span className="landing-solution-label">CON OBRATECH</span>
                    <span className="material-symbols-outlined" aria-hidden="true">check_circle</span>
                  </div>
                  <h3>{item.solucionTitulo}</h3>
                  <p>{item.solucionTexto}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="landing-cta">
          <div aria-hidden="true" className="landing-cta-image" />
          <div>
            <p className="landing-eyebrow">El siguiente paso empieza aquí</p>
            <h2>Haz que cada proyecto avance mejor.</h2>
            <p>Únete a ObraTech y organiza tu próxima obra desde un solo lugar.</p>
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
              <img alt="ObraTech" src="/images/obratech-logo.png" style={{ width: 127, height: 36 }} />
            </Link>
            <p>La plataforma que conecta personas, proyectos y oportunidades en la industria de la construcción.</p>
          </section>
          <section>
            <h2>Explora</h2>
            <a href="#beneficios">Beneficios</a>
            <a href="#como-funciona">Cómo funciona</a>
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
