package com.obratech.runners;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.obratech.entity.Cliente;
import com.obratech.entity.Contratista;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteProyecto;
import com.obratech.entity.Trabajador;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.entity.enums.EstadoFaseObra;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.entity.enums.FaseObra;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteProyectoRepository;
import com.obratech.repository.UsuarioRepository;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String ROLE_CONTRACTOR = "ROLE_CONTRACTOR";
    private static final String ROLE_WORKER = "ROLE_WORKER";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final EquipoTrabajoRepository equipoRepository;
    private final ReporteProyectoRepository reporteRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DemoDataInitializer(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            EquipoTrabajoRepository equipoRepository,
            ReporteProyectoRepository reporteRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.password:}") String seedPassword) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.equipoRepository = equipoRepository;
        this.reporteRepository = reporteRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new IllegalStateException(
                    "APP_SEED_PASSWORD es obligatorio cuando app.seed.enabled=true.");
        }

        Perfil clienteCamila = asegurarPerfil(
                "demo-perfil-camila-herrera", "camila.herrera@obratech.test", "Camila", "Herrera",
                ROLE_CLIENT, perfil -> {
                    Cliente cliente = new Cliente();
                    cliente.setEmpresa("Vivienda Raíz Demo");
                    perfil.setDetallesCliente(cliente);
                });
        Perfil clienteNicolas = asegurarPerfil(
                "demo-perfil-nicolas-pardo", "nicolas.pardo@obratech.test", "Nicolás", "Pardo",
                ROLE_CLIENT, perfil -> {
                    Cliente cliente = new Cliente();
                    cliente.setEmpresa("Inversiones La Arboleda Demo");
                    perfil.setDetallesCliente(cliente);
                });

        Perfil contratistaJulian = asegurarPerfil(
                "demo-perfil-julian-restrepo", "julian.restrepo@obratech.test", "Julián", "Restrepo",
                ROLE_CONTRACTOR, perfil -> completarContratista(perfil, "Obra civil y remodelación",
                        "Medellín", "Antioquia", 11, Set.of("Ampliaciones", "Acabados"),
                        "Perfil ficticio de demostración; no representa a una persona real."));
        Perfil contratistaValentina = asegurarPerfil(
                "demo-perfil-valentina-gomez", "valentina.gomez@obratech.test", "Valentina", "Gómez",
                ROLE_CONTRACTOR, perfil -> completarContratista(perfil, "Construcción residencial",
                        "Bogotá", "Cundinamarca", 8, Set.of("Estructuras", "Remodelaciones"),
                        "Perfil ficticio de demostración; no representa a una persona real."));

        Perfil trabajadorMateo = asegurarPerfil(
                "demo-perfil-mateo-giraldo", "mateo.giraldo@obratech.test", "Mateo", "Giraldo",
                ROLE_WORKER, perfil -> completarTrabajador(perfil, "Maestro de obra"));
        Perfil trabajadorLaura = asegurarPerfil(
                "demo-perfil-laura-castano", "laura.castano@obratech.test", "Laura", "Castaño",
                ROLE_WORKER, perfil -> completarTrabajador(perfil, "Instaladora eléctrica"));
        Perfil trabajadorDavid = asegurarPerfil(
                "demo-perfil-david-ospina", "david.ospina@obratech.test", "David", "Ospina",
                ROLE_WORKER, perfil -> completarTrabajador(perfil, "Instalador hidrosanitario"));
        Perfil trabajadorKaren = asegurarPerfil(
                "demo-perfil-karen-velez", "karen.velez@obratech.test", "Karen", "Vélez",
                ROLE_WORKER, perfil -> completarTrabajador(perfil, "Pintora de obra"));

        LocalDate hoy = LocalDate.now();
        Proyecto proyectoMedellin = asegurarProyecto(
                "demo-proyecto-casa-laureles", "Ampliación de vivienda en Laureles",
                "Escenario ficticio de demostración en Medellín. No corresponde a una obra contratada.",
                usuarioDe(clienteCamila), contratistaJulian, "Vivienda unifamiliar",
                "Laureles, Medellín, Antioquia", 148_000_000.0, hoy.minusDays(3), hoy.plusDays(105),
                FaseObra.PLANEACION, 3, 10, 145.0, 2);
        Proyecto proyectoBogota = asegurarProyecto(
                "demo-proyecto-local-chapinero", "Remodelación de local en Chapinero",
                "Escenario ficticio de demostración en Bogotá. Presupuesto y avance ilustrativos.",
                usuarioDe(clienteNicolas), contratistaValentina, "Remodelación comercial",
                "Chapinero, Bogotá, Cundinamarca", 96_500_000.0, hoy.minusDays(5), hoy.plusDays(80),
                FaseObra.PLANEACION, 2, 8, 92.0, 1);
        Proyecto proyectoDisponible = asegurarProyecto(
                "demo-proyecto-ampliacion-envigado", "Ampliación de segundo piso en Envigado",
                "Proyecto ficticio de demostración publicado para mostrar el flujo de postulaciones.",
                usuarioDe(clienteCamila), null, "Ampliación residencial",
                "Envigado, Antioquia", 121_000_000.0, hoy.plusDays(14), hoy.plusDays(145),
                FaseObra.PLANEACION, 3, 9, 110.0, 2);
        proyectoDisponible.setEstadoEjecucion(EstadoEjecucion.PENDIENTE);
        proyectoDisponible.setEstadoAsignacion(EstadoAsignacion.SIN_ASIGNAR);
        proyectoDisponible.setPostulacionesAbiertas(true);
        proyectoDisponible.setFechaLimitePostulacion(hoy.plusDays(10));
        proyectoRepository.save(proyectoDisponible);

        EquipoTrabajo equipoEstructura = asegurarEquipo(
                "demo-equipo-estructura", proyectoMedellin, "Cuadrilla de obra gris",
                "Revisión de planos, replanteo y preparación de estructura",
                FaseObra.PLANEACION, 42.0, List.of(trabajadorMateo, trabajadorDavid), 2);
        EquipoTrabajo equipoInstalaciones = asegurarEquipo(
                "demo-equipo-instalaciones", proyectoMedellin, "Cuadrilla de instalaciones",
                "Levantamiento de puntos eléctricos e hidrosanitarios",
                FaseObra.PLANEACION, 42.0, List.of(trabajadorLaura, trabajadorDavid), 1);
        EquipoTrabajo equipoAcabados = asegurarEquipo(
                "demo-equipo-acabados", proyectoBogota, "Cuadrilla de acabados",
                "Diagnóstico de superficies y selección de acabados",
                FaseObra.PLANEACION, 27.0, List.of(trabajadorKaren, trabajadorMateo), 3);

        asociarEquipo(proyectoMedellin, equipoEstructura.getActividad(), trabajadorMateo, trabajadorDavid);
        asociarEquipo(proyectoMedellin, equipoInstalaciones.getActividad(), trabajadorLaura, trabajadorDavid);
        asociarEquipo(proyectoBogota, equipoAcabados.getActividad(), trabajadorKaren, trabajadorMateo);
        proyectoRepository.save(proyectoMedellin);
        proyectoRepository.save(proyectoBogota);

        asegurarReporte("demo-reporte-casa-laureles", proyectoMedellin, contratistaJulian,
                "Reporte demostrativo: se revisaron los planos y se acordó la secuencia inicial de trabajo.", 1);
        asegurarReporte("demo-reporte-local-chapinero", proyectoBogota, contratistaValentina,
                "Reporte demostrativo: se realizó el levantamiento de áreas y el inventario preliminar de acabados.", 2);
    }

    private Perfil asegurarPerfil(
            String id,
            String email,
            String nombre,
            String apellido,
            String rol,
            Consumer<Perfil> completarDetalles) {
        if (usuarioRepository.findByUsernameIgnoreCase(email).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setId(id.replace("perfil", "usuario"));
            usuario.setUsername(email);
            usuario.setPassword(passwordEncoder.encode(seedPassword));
            usuario.setRoles(new HashSet<>(Set.of(rol)));
            usuario.setActivo(true);
            usuario.setVerificado(true);
            usuarioRepository.save(usuario);
        }

        Perfil existente = perfilRepository.findByUsernameIgnoreCase(email).orElse(null);
        if (existente != null) {
            return existente;
        }

        Perfil perfil = new Perfil(nombre, email);
        perfil.setId(id);
        perfil.setApellido(apellido);
        perfil.setEmail(email);
        perfil.setRole(rol);
        perfil.setActivo(true);
        perfil.setVerificado(true);
        completarDetalles.accept(perfil);
        return perfilRepository.save(perfil);
    }

    private Usuario usuarioDe(Perfil perfil) {
        return usuarioRepository.findByUsernameIgnoreCase(perfil.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la cuenta de demostración para " + perfil.getUsername()));
    }

    private static void completarContratista(
            Perfil perfil,
            String especialidad,
            String ciudad,
            String departamento,
            int experiencia,
            Set<String> subespecialidades,
            String descripcion) {
        Contratista contratista = new Contratista();
        contratista.setEspecialidad(especialidad);
        contratista.setUbicacion(ciudad + ", " + departamento);
        contratista.setCiudad(ciudad);
        contratista.setDepartamento(departamento);
        contratista.setExperiencia(experiencia);
        contratista.setSubespecialidades(new HashSet<>(subespecialidades));
        perfil.setDetallesContratista(contratista);
        perfil.setDescripcion(descripcion);
    }

    private static void completarTrabajador(Perfil perfil, String oficio) {
        Trabajador trabajador = new Trabajador();
        trabajador.setOficio(oficio);
        trabajador.setDisponibilidad(true);
        perfil.setTrabajador(trabajador);
    }

    private Proyecto asegurarProyecto(
            String id,
            String titulo,
            String descripcion,
            Usuario cliente,
            Perfil contratista,
            String tipo,
            String ubicacion,
            double presupuesto,
            LocalDate fechaInicio,
            LocalDate fechaEntrega,
            FaseObra fase,
            int semanasFase1,
            int semanasFase2,
            double area,
            int pisos) {
        Proyecto existente = proyectoRepository.findById(id).orElse(null);
        if (existente != null) {
            return existente;
        }

        Proyecto proyecto = new Proyecto(titulo, descripcion);
        proyecto.setId(id);
        proyecto.setTipoProyecto(tipo);
        proyecto.setUbicacion(ubicacion);
        proyecto.setPresupuesto(presupuesto);
        proyecto.setPlazoEstimado((int) java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaEntrega));
        proyecto.setFechaInicio(fechaInicio);
        proyecto.setFechaEntrega(fechaEntrega);
        proyecto.setFechaLimitePostulacion(LocalDate.now().plusDays(10));
        proyecto.setEstadoAsignacion(contratista == null ? EstadoAsignacion.SIN_ASIGNAR : EstadoAsignacion.ASIGNADO);
        proyecto.setEstadoEjecucion(EstadoEjecucion.EN_PROGRESO);
        proyecto.setFaseActual(fase);
        proyecto.setEstadoFaseActual(contratista == null
                ? EstadoFaseObra.BLOQUEADA : EstadoFaseObra.EN_EJECUCION);
        proyecto.setSemanasEstimadasFase1(semanasFase1);
        proyecto.setSemanasEstimadasFase2(semanasFase2);
        proyecto.setCuotasSemanaPagadas(0);
        proyecto.setCuotasTotalesEsperadas(semanasFase1 + semanasFase2 + 1);
        proyecto.setEstadoValidacion(EstadoValidacion.APROBADO);
        proyecto.setPostulacionesAbiertas(contratista == null);
        proyecto.setCliente(cliente);
        proyecto.setContratistaAsignado(contratista);
        proyecto.setFechaCreacion(LocalDateTime.now().minusDays(7));
        proyecto.setAreaTotal(area);
        proyecto.setNumeroPisos(pisos);
        proyecto.setTipoContratacion("Por obra");
        return proyectoRepository.save(proyecto);
    }

    private EquipoTrabajo asegurarEquipo(
            String id,
            Proyecto proyecto,
            String nombre,
            String actividad,
            FaseObra fase,
            double avance,
            List<Perfil> integrantes,
            int diasDesdeActualizacion) {
        return equipoRepository.findById(id).orElseGet(() -> {
            EquipoTrabajo equipo = new EquipoTrabajo();
            equipo.setId(id);
            equipo.setNombre(nombre);
            equipo.setActividad(actividad);
            equipo.setProyecto(proyecto);
            equipo.setFase(fase);
            equipo.setPorcentajeAvance(avance);
            equipo.setAvanceActualizadoEn(LocalDateTime.now().minusDays(diasDesdeActualizacion));
            equipo.setIntegrantes(integrantes);
            return equipoRepository.save(equipo);
        });
    }

    private static void asociarEquipo(
            Proyecto proyecto,
            String actividad,
            Perfil... integrantes) {
        if (proyecto.getEquipoTrabajo() == null) {
            proyecto.setEquipoTrabajo(new ArrayList<>());
        }
        for (Perfil integrante : integrantes) {
            if (proyecto.getEquipoTrabajo().stream()
                    .noneMatch(actual -> integrante.getId().equals(actual.getId()))) {
                proyecto.getEquipoTrabajo().add(integrante);
            }
        }
        if (proyecto.getActividadesAsignadas() == null) {
            proyecto.setActividadesAsignadas(new java.util.HashMap<>());
        }
        for (Perfil integrante : integrantes) {
            proyecto.getActividadesAsignadas().putIfAbsent(integrante.getId(), actividad);
        }
    }

    private void asegurarReporte(
            String id,
            Proyecto proyecto,
            Perfil contratista,
            String contenido,
            int diasDesdeCreacion) {
        if (reporteRepository.existsById(id)) {
            return;
        }
        ReporteProyecto reporte = new ReporteProyecto();
        reporte.setId(id);
        reporte.setProyectoId(proyecto.getId());
        reporte.setProyectoTitulo(proyecto.getTitulo());
        reporte.setContratistaId(contratista.getId());
        reporte.setContratistaUsername(contratista.getUsername());
        reporte.setContenido(contenido);
        reporte.setCreado(LocalDateTime.now().minusDays(diasDesdeCreacion));
        reporteRepository.save(reporte);
    }
}
