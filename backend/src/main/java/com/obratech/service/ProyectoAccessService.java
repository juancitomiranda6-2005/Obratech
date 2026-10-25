package com.obratech.service;

import org.springframework.stereotype.Service;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;

@Service
public class ProyectoAccessService {

    private final PerfilRepository perfilRepository;
    private final EquipoTrabajoRepository equipoTrabajoRepository;

    public ProyectoAccessService(
            PerfilRepository perfilRepository,
            EquipoTrabajoRepository equipoTrabajoRepository) {
        this.perfilRepository = perfilRepository;
        this.equipoTrabajoRepository = equipoTrabajoRepository;
    }

    public boolean puedeVerFases(Proyecto proyecto, Usuario usuario) {
        if (proyecto == null || usuario == null) {
            return false;
        }
        if (esAdmin(usuario) || esClientePropietario(proyecto, usuario)) {
            return true;
        }

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (perfil == null || perfil.getId() == null) {
            return false;
        }
        if (pertenece(proyecto.getContratistaAsignado(), perfil)) {
            return true;
        }
        if (proyecto.getEquipoTrabajo() != null
                && proyecto.getEquipoTrabajo().stream().anyMatch(miembro -> pertenece(miembro, perfil))) {
            return true;
        }
        return proyecto.getId() != null
                && !equipoTrabajoRepository.findByProyectoIdAndIntegrantesContaining(proyecto.getId(), perfil).isEmpty();
    }

    public boolean puedeAvanzarFase(Proyecto proyecto, Usuario usuario) {
        return proyecto != null && usuario != null
                && (esAdmin(usuario) || esClientePropietario(proyecto, usuario));
    }

    public boolean puedeGestionarTareasFase(Proyecto proyecto, Usuario usuario) {
        if (proyecto == null || usuario == null) {
            return false;
        }
        if (esAdmin(usuario)) {
            return true;
        }
        return proyecto.getContratistaAsignado() != null
                && proyecto.getContratistaAsignado().getUsername() != null
                && proyecto.getContratistaAsignado().getUsername().equalsIgnoreCase(usuario.getUsername());
    }

    private boolean esClientePropietario(Proyecto proyecto, Usuario usuario) {
        return proyecto.getCliente() != null
                && proyecto.getCliente().getUsername() != null
                && proyecto.getCliente().getUsername().equalsIgnoreCase(usuario.getUsername());
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario.getRoles() != null && usuario.getRoles().contains("ROLE_ADMIN");
    }

    private boolean pertenece(Perfil miembro, Perfil actual) {
        return miembro != null && actual != null
                && ((miembro.getId() != null && miembro.getId().equals(actual.getId()))
                        || (miembro.getUsername() != null && actual.getUsername() != null
                                && miembro.getUsername().equalsIgnoreCase(actual.getUsername())));
    }
}
