import { useState } from 'react'
import { Link } from 'react-router-dom'

const heroMessages = [
  'Conecta con los mejores profesionales.',
  'Gestiona tus proyectos de forma eficiente.',
  'Haz crecer tu negocio en la construcción.',
]

const steps = [
  { title: 'Regístrate', description: 'Crea tu cuenta como cliente o profesional en pocos minutos.' },
  { title: 'Encuentra y conecta', description: 'Publica tu proyecto o busca profesionales calificados para tus necesidades.' },
  { title: 'Gestiona y colabora', description: 'Utiliza nuestras herramientas para gestionar el avance, los pagos y la comunicación.' },
  { title: 'Finaliza y califica', description: 'Completa tu proyecto con éxito y valora la experiencia para ayudar a la comunidad.' },
]

const features = [
  { icon: 'verified_user', title: 'Profesionales verificados', description: 'Accede a una red de contratistas y trabajadores cuya experiencia ha sido confirmada por nuestro equipo.' },
  { icon: 'assignment', title: 'Gestión de proyectos', description: 'Herramientas intuitivas para seguir el avance de tu obra, gestionar presupuestos y comunicarte eficazmente.' },
  { icon: 'lock', title: 'Pagos seguros', description: 'Realiza y recibe pagos a través de nuestra plataforma segura, con garantías para ambas partes.' },
]

export function LandingPage() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="landing-page">
      <header className="landing-nav">
        <div className="landing-nav-inner">
          <Link aria-label="ObraTech inicio" className="landing-brand" to="/">
            <img alt="" src="/img/logo3.png" />
            <span>ObraTech</span>
          </Link>
          <nav aria-label="Navegación principal" className={`landing-nav-actions ${menuOpen ? 'is-open' : ''}`}>
            <a href="#contacto" onClick={() => setMenuOpen(false)}>Contacto</a>
            <Link onClick={() => setMenuOpen(false)} to="/registro">Registrar</Link>
            <Link onClick={() => setMenuOpen(false)} to="/login">Iniciar sesión</Link>
          </nav>
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
        </div>
      </header>

      <main className="landing-main">
        <section aria-label="Bienvenido a ObraTech" className="landing-hero">
          <figure aria-hidden="true" className="landing-visual">
            <img alt="" src="/img/image.png" />
          </figure>
          <div className="landing-hero-center">
            <img alt="ObraTech" className="landing-hero-logo" src="/img/logo3.png" />
            <h1>Construyendo el futuro de tus proyectos.</h1>
            <div aria-label="Conecta con los mejores profesionales. Gestiona tus proyectos de forma eficiente. Haz crecer tu negocio en la construcción." className="landing-carousel">
              <div aria-hidden="true" className="landing-carousel-track">
                {heroMessages.map((message) => <p className="landing-carousel-slide" key={message}>{message}</p>)}
              </div>
            </div>
          </div>
          <div className="landing-hero-actions">
            <Link className="landing-hero-login" to="/login">Iniciar sesión</Link>
            <Link className="landing-hero-register" to="/registro">Registrar</Link>
          </div>
        </section>

        <section className="landing-section landing-how" id="how-it-works">
          <div className="landing-section-inner">
            <h2>¿Cómo funciona?</h2>
            <div className="landing-step-grid">
              {steps.map((step, index) => (
                <article className="landing-step" key={step.title}>
                  <span className="landing-step-number">{index + 1}</span>
                  <h3>{step.title}</h3>
                  <p>{step.description}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="landing-section landing-features" id="features">
          <div className="landing-section-inner">
            <h2>Características principales</h2>
            <div className="landing-feature-grid">
              {features.map((feature) => (
                <article className="landing-feature" key={feature.title}>
                  <span className="material-symbols-outlined" aria-hidden="true">{feature.icon}</span>
                  <h3>{feature.title}</h3>
                  <p>{feature.description}</p>
                </article>
              ))}
            </div>
          </div>
        </section>
      </main>

      <footer className="landing-footer" id="contacto">
        <div className="landing-footer-grid">
          <section>
            <h2>ObraTech</h2>
            <p>Construyendo el futuro de tus proyectos. La plataforma que une a profesionales y clientes en la industria de la construcción.</p>
          </section>
          <section>
            <h2>Contacto</h2>
            <p>Email: <a href="mailto:canatemiranda2023@gmail.com">canatemiranda2023@gmail.com</a></p>
            <p>Teléfono: <a href="tel:+573246072562">3246072562</a></p>
            <p>Cartagena de Indias, Bolívar, Colombia</p>
          </section>
          <section>
            <h2>Explora ObraTech</h2>
            <a href="#how-it-works">Cómo funciona</a>
            <a href="#features">Características</a>
            <Link to="/registro">Crear una cuenta</Link>
          </section>
        </div>
        <div className="landing-footer-bottom">
          <span>© 2025 ObraTech. Todos los derechos reservados.</span>
          <span>Construye en equipo.</span>
        </div>
      </footer>
    </div>
  )
}
