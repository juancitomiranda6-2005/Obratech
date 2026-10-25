package com.obratech.runners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteProyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteProyectoRepository;
import com.obratech.repository.UsuarioRepository;

class DemoDataInitializerTest {

    @Test
    void createsDemoDataIdempotentlyWithoutFakePaymentsOrRatings() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        PerfilRepository perfiles = mock(PerfilRepository.class);
        ProyectoRepository proyectos = mock(ProyectoRepository.class);
        EquipoTrabajoRepository equipos = mock(EquipoTrabajoRepository.class);
        ReporteProyectoRepository reportes = mock(ReporteProyectoRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Map<String, Usuario> usuariosGuardados = new HashMap<>();
        Map<String, Perfil> perfilesGuardados = new HashMap<>();
        Map<String, Proyecto> proyectosGuardados = new HashMap<>();
        Map<String, EquipoTrabajo> equiposGuardados = new HashMap<>();
        Map<String, ReporteProyecto> reportesGuardados = new HashMap<>();

        when(usuarios.findByUsernameIgnoreCase(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(usuariosGuardados.get(invocation.getArgument(0))));
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuariosGuardados.put(usuario.getUsername(), usuario);
            return usuario;
        });
        when(perfiles.findByUsernameIgnoreCase(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(perfilesGuardados.get(invocation.getArgument(0))));
        when(perfiles.save(any(Perfil.class))).thenAnswer(invocation -> {
            Perfil perfil = invocation.getArgument(0);
            perfilesGuardados.put(perfil.getUsername(), perfil);
            return perfil;
        });
        when(proyectos.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(proyectosGuardados.get(invocation.getArgument(0))));
        when(proyectos.save(any(Proyecto.class))).thenAnswer(invocation -> {
            Proyecto proyecto = invocation.getArgument(0);
            proyectosGuardados.put(proyecto.getId(), proyecto);
            return proyecto;
        });
        when(equipos.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(equiposGuardados.get(invocation.getArgument(0))));
        when(equipos.save(any(EquipoTrabajo.class))).thenAnswer(invocation -> {
            EquipoTrabajo equipo = invocation.getArgument(0);
            equiposGuardados.put(equipo.getId(), equipo);
            return equipo;
        });
        when(reportes.existsById(anyString()))
                .thenAnswer(invocation -> reportesGuardados.containsKey(invocation.getArgument(0)));
        when(reportes.save(any(ReporteProyecto.class))).thenAnswer(invocation -> {
            ReporteProyecto reporte = invocation.getArgument(0);
            reportesGuardados.put(reporte.getId(), reporte);
            return reporte;
        });
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));

        DemoDataInitializer initializer = new DemoDataInitializer(
                usuarios, perfiles, proyectos, equipos, reportes, passwordEncoder, "local-demo-password");
        initializer.run();
        initializer.run();

        assertEquals(8, usuariosGuardados.size());
        assertEquals(8, perfilesGuardados.size());
        assertEquals(3, proyectosGuardados.size());
        assertEquals(3, equiposGuardados.size());
        assertEquals(2, reportesGuardados.size());
        assertEquals("hash:local-demo-password",
                usuariosGuardados.get("camila.herrera@obratech.test").getPassword());
        Proyecto abierto = proyectosGuardados.get("demo-proyecto-ampliacion-envigado");
        assertEquals(EstadoEjecucion.PENDIENTE, abierto.getEstadoEjecucion());
        assertEquals(3, proyectosGuardados.get("demo-proyecto-casa-laureles").getEquipoTrabajo().size());
        assertEquals(2, reportesGuardados.values().stream()
                .filter(reporte -> reporte.getContenido().startsWith("Reporte demostrativo:")).count());
    }

    @Test
    void refusesToCreateAccountsWithoutAnExplicitDemoPassword() {
        DemoDataInitializer initializer = new DemoDataInitializer(
                mock(UsuarioRepository.class),
                mock(PerfilRepository.class),
                mock(ProyectoRepository.class),
                mock(EquipoTrabajoRepository.class),
                mock(ReporteProyectoRepository.class),
                mock(PasswordEncoder.class),
                " ");

        assertThrows(IllegalStateException.class, initializer::run);
    }
}
