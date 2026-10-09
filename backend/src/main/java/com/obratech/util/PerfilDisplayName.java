package com.obratech.util;

import com.obratech.entity.Perfil;

public final class PerfilDisplayName {

    private PerfilDisplayName() {}

    public static String of(Perfil perfil) {
        if (perfil == null) {
            return "";
        }

        String nombre = perfil.getNombre() == null ? "" : perfil.getNombre().trim();
        String apellido = perfil.getApellido() == null ? "" : perfil.getApellido().trim();
        String completo = (nombre + " " + apellido).trim();
        return completo.isBlank() ? perfil.getUsername() : completo;
    }
}